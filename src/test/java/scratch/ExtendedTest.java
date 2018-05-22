package scratch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(FooParameterResolver.class)
public class ExtendedTest {

  Foo foo, beforeFoo=null;

  ExtendedTest(Foo allTestsFoo) {
    this.foo = allTestsFoo;
  }

  @BeforeEach
  void be(Foo bef) {
    beforeFoo = bef;
  }
  @AfterEach
  void ae(Foo foo) {
    System.out.println(foo);
  }
  @Test
  void testFoo(Foo f) {
    System.out.println(foo);
    System.out.println(beforeFoo);
    System.out.println(f);
  }
}
