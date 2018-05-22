package scratch;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class StatImpl implements Stat {

  protected Map<String, String> entries = new HashMap<>();

  @Override
  public Map<String, String> getEntries() {
    return Collections.unmodifiableMap(entries);
  }
}

