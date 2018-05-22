package scratch;

import org.junit.jupiter.api.Test;


public class BadIdeaJeans {

  private MutableStat statMutator;

  @Test
  void t() {
    Stat s = new StatImpl();
    statMutator = new MutableStatImpl((StatImpl) s);
    System.out.println("underlying stat " + s.getEntries().toString());
    statMutator.addEntry("key", "blarg");
    System.out.println("underlying stat " + s.getEntries().toString());
    System.out.println("    outter stat " + statMutator.getEntries().toString());
  }

}
