package scratch;

interface I {
  default String go() { return "I"; }
}
interface J extends I {
  default String go() { return "J"; }
}
interface K extends I {
  default String go() { return "K"; }
}
class AnI implements I { }
class AnJ implements J { }
class AnK implements K { }
class AnJK implements J,K {
  //inherits unrelated defaults for go() from J and K, therefore
  public String go() { return "JK"; } // Forced to Override in order to disambiguate
}
public class Diamonds {
}
