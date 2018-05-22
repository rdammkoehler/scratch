package scratch;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * IRL what I'm trying to do is get all the classes that
 * have a static method (go(String arg)) and put them into
 * a map where I can look them up and execute them based on
 * a scalar value. I wanted to see if there was a way to use
 * a lambda, method reference, or other 'sexy' Java 8 entries
 * to do this.
 * <p>
 * e.g. -
 */
class A {
  public static String go(String arg) {
    return arg;
  }
}

class B {
  public static String go(String arg) {
    return arg.toLowerCase();
  }
}

class C {
  public static String go(String arg) {
    return arg.toUpperCase();
  }
}

class Bucket {
  Integer id;
  String value;

  Bucket(Integer id, String value) {
    this.id = id;
    this.value = value;
  }
}

class Processor {
  Map<Integer, Function<String, String>> goers = new HashMap<>();

  Processor() {
    goers.put(1, A::go); //can I make the class A go away
    goers.put(2, B::go);
    goers.put(3, C::go);
  }

  public String process(Bucket b) {
    return goers.get(b.id).apply(b.value);
  }
}

class Y {
  public static String go(String arg) {

    char[] chars = arg.toCharArray();
    Arrays.sort(chars);
    return new String(chars);
  }
}

public class BetterInvoke {
  @Test
  void t() {
    Bucket[] buckets = {
        new Bucket(1, "samesame"),
        new Bucket(2, "TOLOWER"),
        new Bucket(3, "toupper")
    };

    Processor processor = new Processor();
    for (Bucket bucket : buckets) {
      System.out.println(processor.process(bucket));
    }
  }
}
