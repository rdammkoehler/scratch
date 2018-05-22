package scratch;

import java.util.Map;

public class MutableStatImpl implements MutableStat {
  private StatImpl stat;

  public MutableStatImpl(StatImpl stat) {
    this.stat = stat;
  }

  @Override
  public Map<String, String> getEntries() {
    return stat.getEntries();
  }

  public void addEntry(String key, String value) {
    stat.entries.put(key, value);
  }
}
