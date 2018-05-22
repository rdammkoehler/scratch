package scratch;


import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class FindMyInterface {
  interface A {}
  interface B extends A{}
  interface C extends A{}
  class D implements B{}
  class E implements C{}
  @Test
  void t() {
    Map<Class<?>,Map<Class<?>,A>> m = new HashMap<>();
    m.put(B.class,new HashMap<>());
    m.get(B.class).put(Integer.class,new D());
    m.put(C.class,new HashMap<>());
    m.get(C.class).put(String.class, new E());

  }
}
