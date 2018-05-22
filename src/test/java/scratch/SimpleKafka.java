package scratch;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.integration.utils.EmbeddedKafkaCluster;
import org.apache.kafka.streams.integration.utils.IntegrationTestUtils;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.StateStore;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.test.StreamsTestUtils;
import org.apache.kafka.test.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static scratch.KafkaApplication.DEVICE_STATES;
import static scratch.KafkaApplication.SOURCE_TOPIC;

class MessagePreprocessor implements ValueTransformerWithKey<String, String, String> {

  private KeyValueStore<String,String> stateStore;

  @Override
  public void init(ProcessorContext context) {
    System.out.println(this.getClass().getSimpleName() + ".init(" + context + ")");
    stateStore = (KeyValueStore<String,String>)context.getStateStore(DEVICE_STATES);
  }

  @Override
  public String transform(String readOnlyKey, String value) {
    // value here is the message
    System.out.println(this.getClass().getSimpleName() + ".transform(" + readOnlyKey + ", " + new String(value) + ")");
    String storedValue = stateStore.get(readOnlyKey);
    if (null==storedValue) {
      storedValue="v1:0,v2:0.0"; // TODO this is an init, doesn't seem right
    }
    System.out.println(this.getClass().getSimpleName() + ".transform(...) >> " + storedValue);
    // if we return null, the stream processing stops!
    return storedValue;
  }

  @Override
  public void close() {
    System.out.println(this.getClass().getSimpleName() + ".close()");
  }
}

class MessagePreprocessorSupplier implements ValueTransformerWithKeySupplier<String, String, String> {

  @Override
  public ValueTransformerWithKey<String, String, String> get() {
    System.out.println(this.getClass().getSimpleName() + ".get()");
    return new MessagePreprocessor();
  }
}

class ThingAggregator implements Aggregator<String, String, String> {

  @Override
  public String apply(String key, String value, String aggregate) {
    //value here is the output of the MessagePreprocessor.transform(...), the message is lost
    System.out.println(this.getClass().getSimpleName() + ".apply(" + key + ", " + value + ", " + aggregate + ")");
    StringBuilder out = new StringBuilder();
    String[] g = new String(value).split(",");
    for (String s : g) {
      String[] v = s.split(":");
      out.append(v[0]).append(":");
      if ("v1".equals(v[0])) {
        int iv = new Integer(v[1]).intValue();
        Integer nv = new Integer(++iv);
        out.append(nv.toString());
      } else {
        out.append(v[1]);
      }
      out.append(",");
    }
    out.deleteCharAt(out.length() - 1);
    System.out.println(this.getClass().getSimpleName() + " >> " + out);
    return out.toString();
  }
}

class KafkaApplication {
  public static final String SOURCE_TOPIC = "source_topic";
  public static final String DEVICE_STATES = "device_states";
  private KafkaStreams kafkaStreams;
  String applicationId = "test_kafka";
  String keySerdeClassName = Serdes.String().getClass().getName();
  String valueSerdeClassName = Serdes.String().getClass().getName();
  Properties additionalProperties = new Properties();

  KafkaApplication(EmbeddedKafkaCluster cluster) {
    Properties config = StreamsTestUtils.getStreamsConfig(applicationId,
        cluster.bootstrapServers(),
        keySerdeClassName,
        valueSerdeClassName,
        additionalProperties);
    config.setProperty(IntegrationTestUtils.INTERNAL_LEAVE_GROUP_ON_CLOSE, "true");
    config.put(SOURCE_TOPIC, SOURCE_TOPIC);
    config.put(DEVICE_STATES, DEVICE_STATES);

    StreamsBuilder streamsBuilder = new StreamsBuilder();

    Materialized<String, String, KeyValueStore<Bytes, byte[]>> device_states =
        Materialized.<String, String, KeyValueStore<Bytes, byte[]>>as(DEVICE_STATES)
            .withKeySerde(Serdes.String())
            .withValueSerde(Serdes.String());
    streamsBuilder.table(DEVICE_STATES, device_states);

    StreamsConfig streamsConfig = new StreamsConfig(config);
    ValueTransformerWithKeySupplier<String, String, String> transformerProvider = new MessagePreprocessorSupplier();
    String[] storeNames = new String[]{DEVICE_STATES};
    Initializer<String> initializer = () -> null;
    ThingAggregator aggregator = new ThingAggregator();

    streamsBuilder.<String, String>stream(SOURCE_TOPIC)
        .transformValues(
            transformerProvider,
            storeNames
        )
        .groupByKey()
        .aggregate(
            initializer,
            aggregator
        )
        .toStream()
        .to(
            DEVICE_STATES,
            Produced.with(Serdes.String(), Serdes.String())
        )
    ;

    kafkaStreams = new KafkaStreams(streamsBuilder.build(), streamsConfig);
  }

  void start() {
    kafkaStreams.start();
  }

  void stop() {
    kafkaStreams.close();
  }
}


public class SimpleKafka {

  class SingleBrokerEmbeddedKafkaCluster extends EmbeddedKafkaCluster {
    public SingleBrokerEmbeddedKafkaCluster() {
      super(1);
    }

    public void stop() {
      super.after();
    }
  }

  SingleBrokerEmbeddedKafkaCluster cluster;
  KafkaApplication kafkaApplication;

  @BeforeEach
  void setup() throws IOException, InterruptedException {
    cluster = new SingleBrokerEmbeddedKafkaCluster();
    cluster.start();

    kafkaApplication = new KafkaApplication(cluster);
    kafkaApplication.start();
  }

  @AfterEach
  void teardown() {
    kafkaApplication.stop();
    kafkaApplication = null;

    cluster.stop();
    cluster = null;
  }

  /**
   * Assume the key exists in the store
   * Send a new value for v1 or v2
   * Expect to get the state back
   */
  @Test
  void t() throws InterruptedException, ExecutionException {
    // assume m1 is v1:0,v2:0.0
    String expectedBody = "v1:5,v2:3.8";

    send(SOURCE_TOPIC, "m1", "v1:5");

    String actualBody = wait(DEVICE_STATES, 1).next();
    System.out.println(this.getClass().getSimpleName() + ".t() actualBody: " + actualBody);

    assertThat(actualBody, is(equalTo(expectedBody)));
  }

  private void send(String topicName, String key, String body) throws ExecutionException, InterruptedException {
    System.out.println(this.getClass().getSimpleName() + ".send(" + topicName + ", " + key + ", " + body + ")");
    Properties producerConfig = TestUtils.producerConfig(cluster.bootstrapServers(),
        StringSerializer.class,
        StringSerializer.class);
    List<KeyValue<String, String>> valuesToSend = new ArrayList<>();
    valuesToSend.add(new KeyValue<>(key, body));
    IntegrationTestUtils.produceKeyValuesSynchronously(topicName, valuesToSend, producerConfig, Time.SYSTEM);
  }

  private Iterator<String> wait(String topicName, int recordCount) throws InterruptedException {
    Properties consumerConfig = TestUtils.consumerConfig(cluster.bootstrapServers(),
        StringDeserializer.class,
        StringDeserializer.class);
    List<String> actualValues = IntegrationTestUtils
        .waitUntilMinValuesRecordsReceived(consumerConfig, topicName, recordCount);
    return actualValues.iterator();
  }
}
