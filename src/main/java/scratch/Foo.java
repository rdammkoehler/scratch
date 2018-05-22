package scratch;

import java.util.Date;

public class Foo {
  private static int instanceCount = 0;
  int instanceId = instanceCount++;
  Date now = new Date();

  @Override
  public String toString() {
    return now.toString() + " id: " + instanceId;
  }
}
