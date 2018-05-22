package scratch;

import scratch.Stat;

public interface MutableStat extends Stat {
  void addEntry(String key, String value);
}
