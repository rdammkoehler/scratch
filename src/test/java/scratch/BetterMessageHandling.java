package scratch;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class BetterMessageHandling {

  public static final String CCI_START_SESSION_MSG = "{\"@RemoteConfig\":{\"cmd\":\"start_session\", \"source\":\"CCI\", \"sessionId\":\"this_is_a_sample\", \"auid\":\"ABCD\"}}";
  public static final String RPS_WAIT_MSG = "{\"@RemoteConfig\":{\"cmd\":\"wait\", \"source\":\"RS\"}}";
  public static final String STA_START_SESSION_MSG = "{\"@RemoteConfig\":{\"cmd\":\"start_session\", \"source\":\"STAT\", \"sessionId\":\"this_is_a_sample\", \"auid\":\"ABCD\"}}";
  public static final String RPS_START_DATA_EXCHANGE_MSG = "{\"@RemoteConfig\":{\"cmd\":\"start_data_exchange\", \"source\":\"RS\"}}";
  public static final String CCI_STARTING_DATA_EXCHANGE_MSG = "{\"@RemoteConfig\":{\"cmd\":\"starting_data_exchange\", \"source\":\"CCI\"}}";
  public static final String STA_STARTING_DATA_EXCHANGE_MSG = "{\"@RemoteConfig\":{\"cmd\":\"starting_data_exchange\", \"source\":\"STAT\"}}";

  public static final String RPS_ERROR_MSG = "{\"@RemoteConfig\":{\"cmd\":\"error\", \"source\":\"RS\", \"error_code\":\"an_error_code\"}}";

  public static final String STA_STOP_SESSION_MSG = "{\"@RemoteConfig\":{\"cmd\":\"stop_session\", \"source\":\"STAT\", \"origin\":\"NEXIA\"}}";
  public static final String RPS_STOP_SESSION_MSG = "{\"@RemoteConfig\":{\"cmd\":\"stop_session\", \"source\":\"RS\", \"origin\":\"NEXIA\"}}";

  interface Client {
    void send(String message);

    void recv(String message);
  }

  interface MessageHandler {
    void handleMessage(Client sender, ByteBuf message);
  }

  static abstract class BaseMessageHandler implements MessageHandler {
    protected Connection connection;

    BaseMessageHandler(Connection conn) {
      connection = conn;
    }

    protected void setNextMessageHandlerTo(MessageHandler handler) {
      ((BaseConnection) connection).messageHandler = handler;
    }
  }

  static class Waiting extends BaseMessageHandler implements MessageHandler {

    Waiting(Connection conn) {
      super(conn);
    }

    @Override
    public void handleMessage(Client sender, ByteBuf message) {
      System.out.println(this.getClass().getSimpleName() + " handle " + message.toString(CharsetUtil.UTF_8));
      sender.recv(RPS_WAIT_MSG);
      setNextMessageHandlerTo(new Started(connection));
    }
  }

  static class Started extends BaseMessageHandler implements MessageHandler {

    boolean cciStarted = false, staStarted = false;

    Started(Connection conn) {
      super(conn);
      cciStarted |= null != conn.getCci();
      staStarted |= null != conn.getSta();
    }

    @Override
    public void handleMessage(Client sender, ByteBuf message) {
      System.out.println(this.getClass().getSimpleName() + " handle " + message.toString(CharsetUtil.UTF_8));
      cciStarted |= connection.getCci() == sender;
      staStarted |= connection.getSta() == sender;
      if (cciStarted && staStarted) {
        connection.getSta().recv(RPS_START_DATA_EXCHANGE_MSG);
        connection.getCci().recv(RPS_START_DATA_EXCHANGE_MSG);
        setNextMessageHandlerTo(new Starting(connection));
      }
    }
  }

  static class Starting extends BaseMessageHandler implements MessageHandler {
    boolean cciStarted = false, staStarted = false;

    Starting(Connection conn) {
      super(conn);
    }

    @Override
    public void handleMessage(Client sender, ByteBuf message) {
      System.out.println(this.getClass().getSimpleName() + " handle " + message.toString(CharsetUtil.UTF_8));
      cciStarted |= connection.getCci() == sender;
      staStarted |= connection.getSta() == sender;
      if (cciStarted && staStarted) {
        setNextMessageHandlerTo(new Relaying(connection));
      }
    }
  }

  static class Relaying extends BaseMessageHandler implements MessageHandler {
    Relaying(Connection conn) {
      super(conn);
    }

    @Override
    public void handleMessage(Client sender, ByteBuf message) {
      System.out.println(this.getClass().getSimpleName() + " handle " + message.toString(CharsetUtil.UTF_8));
      Client receipient = (connection.getCci()==sender)?connection.getSta():connection.getCci();
      receipient.recv(message.toString(CharsetUtil.UTF_8));
    }
  }

//  static class Errored extends BaseMessageHandler implements MessageHandler {
//  }
//
//  static class Stopped extends BaseMessageHandler implements MessageHandler {
//  }


  interface Connection extends MessageHandler {
    Client getCci();

    Client getSta();

    void setPair(Client client);
  }

  static abstract class BaseConnection implements Connection {
    protected Client cci, sta;
    MessageHandler messageHandler = new Waiting(this);

    @Override
    public Client getCci() {
      return cci;
    }

    @Override
    public Client getSta() {
      return sta;
    }

    @Override
    public void handleMessage(Client sender, ByteBuf message) {
      messageHandler.handleMessage(sender, message);
    }
  }

  static class CciConnection extends BaseConnection {

    CciConnection(Client cci) {
      this.cci = cci;
    }

    @Override
    public void setPair(Client client) {
      this.sta = client;
    }
  }

  static class StaConnection extends BaseConnection implements Connection {
    StaConnection(Client sta) {
      this.sta = sta;
    }

    @Override
    public void setPair(Client client) {
      this.cci = client;
    }
  }

  static class ConnectionFactory {
    public static final Connection makeConnection(String source, Client client) {
      if ("CCI".equals(source)) {
        return new CciConnection(client);
      }
      return new StaConnection(client);
    }
  }

  static class ConnectionManager {
    private Map<Client, Connection> connections;
    private Map<String, Connection> connectionsBySessionId = new HashMap<>();

    ConnectionManager(Map<Client, Connection> connections) {
      this.connections = connections;
    }

    Connection get(String message, Client sender) {
      Connection connection = null;
      try {
        Map<String, Object> result = (Map<String, Object>) new ObjectMapper().readValue(message, HashMap.class).get("@RemoteConfig");
        if (result.containsKey("sessionId")) {
          if (connectionsBySessionId.containsKey(result.get("sessionId"))) {
            connection = connectionsBySessionId.get(result.get("sessionId"));
            connection.setPair(sender);
          } else {
            connection = newConnection(result, sender);
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
      return connection;
    }

    private Connection newConnection(Map<String, Object> result, Client sender) {
      Connection connection = ConnectionFactory.makeConnection((String) result.get("source"), sender);
      connections.put(sender, connection);
      connectionsBySessionId.put((String) result.get("sessionId"), connection);
      return connection;
    }
  }

  static class FakeServer {
    private static final FakeServer self = new FakeServer();
    static FakeServer getInstance() {
      return self;
    }

    private Map<Client, Client> clients = new HashMap<>();
    private Map<Client, Connection> connections = new HashMap<>();
    private ConnectionManager connectionManager = new ConnectionManager(connections);

    void onMessage(Client sender, ByteBuf message) {
      String sMessage = to_s(message);

      Connection connection = connections.getOrDefault(sender, connectionManager.get(sMessage, sender));
      connection.handleMessage(sender, message);
    }

    void addClient(Client client) {
      clients.put(client, client);
    }

    String to_s(ByteBuf bbuf) {
      return bbuf.toString(CharsetUtil.UTF_8);
    }
  }

  class RelayClient implements Client {
    private FakeServer server;
    private Client delegate;

    RelayClient(FakeServer server, Client delegate) {
      this.server = server;
      server.addClient(this);
      this.delegate = delegate;
    }

    @Override
    public void send(String message) {
      server.onMessage(this, bb(message));
    }

    @Override
    public void recv(String message) {
      delegate.recv(message);
    }

    private ByteBuf bb(String message) {
      return wrappedBuffer(message.getBytes());
    }
  }

  class ValidationClient implements Client {
    private RelayClient relayClient = new RelayClient(FakeServer.getInstance(), this);
    private String received;

    @Override
    public void send(String message) {
      relayClient.send(message);
    }

    @Override
    public void recv(String message) {
      received = message;
    }

    public void validate(String expectation) {
      assertThat(received, is(expectation));
    }
  }

  private ValidationClient cci, sta;

  @BeforeEach
  public void setup() {
    cci = new ValidationClient();
    sta = new ValidationClient();
  }

  @Test
  void t() {
    cci.send(CCI_START_SESSION_MSG);               // server creates connection, tells us to wait
    cci.validate(RPS_WAIT_MSG);                    // we should have gotten a wait

    sta.send(STA_START_SESSION_MSG);               // server adds sta to connection
    sta.validate(RPS_START_DATA_EXCHANGE_MSG);     // server tells sta it's ready
    cci.validate(RPS_START_DATA_EXCHANGE_MSG);     // server tells cci it's ready

    cci.send(CCI_STARTING_DATA_EXCHANGE_MSG);      // cci starts data ex
    sta.send(CCI_STARTING_DATA_EXCHANGE_MSG);      // sta starts data ex

    cci.send("random data");                       // exchange data
    sta.validate("random data");

    sta.send("more data");                         // exchange data
    cci.validate("more data");

//    sta.send(STA_STOP_SESSION_MSG);
//    cci.validate(RPS_STOP_SESSION_MSG);
  }
}
