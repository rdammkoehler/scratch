package scratch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Props {
  private java.util.Properties defaultProperties = new java.util.Properties();

  @BeforeEach
  void setup() {
    defaultProperties.setProperty("a", "b");
    defaultProperties.setProperty("c", "d");
    defaultProperties.setProperty("e", "f");
    defaultProperties.setProperty("g", "h");
  }

  @Test
  void t() {
    java.util.Properties properties = new java.util.Properties(defaultProperties);
    List<String> names = Collections.list(properties.propertyNames()).stream().map(Object::toString).collect(Collectors.toList());
    for (String name : names) {
      System.out.println(name);
    }
  }

  class Properties extends java.util.Properties {
    public Properties() {
      super();
    }

    public Properties(java.util.Properties defaultProperties) {
      super(defaultProperties);
    }

    public List<String> getPropertyNames() {
      return Collections.list(super.propertyNames()).stream().map(Object::toString).collect(Collectors.toList());
    }

  }

  @Test
  void t2() {
    Properties properties = new Properties(defaultProperties);
    for (String name : properties.getPropertyNames()) {
      System.out.println(name);
    }
  }
}
