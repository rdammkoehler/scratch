package scratch;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class DynamicMethodBinding {
  interface S {
  }

  interface M {
  }

  interface I {
    void hm(S s, M m);
  }

  @Nested
  class Conventional {

    class M1 implements M {
    }

    class S1 implements S {
    }

    class S1I implements I {
      @Override
      public void hm(S s, M m) {   // always called
        System.out.println("s");
      }

      public void hm(S1 s, M m) {  // never called
        System.out.println("s1");
      }
    }

    @Test
    void t() {
      Map<Class, I> t = new HashMap<>();
      t.put(S1.class, new S1I());

      // with ref to parent (S)
      {
        S s1 = new S1();
        M m1 = new M1();

        t.get(s1.getClass()).hm(s1, m1); //executes I.hm of S1I
      }
      // with ref to child (S1)
      {
        S1 s1 = new S1();
        M m1 = new M1();

        t.get(s1.getClass()).hm(s1, m1); //executes I.hm of S1I
      }
    }
  }

  @Nested
  class StackOverflow {
    class A {
      void m1() {
        System.out.println("Inside A's m1 method");
      }
    }

    class B extends A {
      @Override
      void m1() {
        System.out.println("Inside B's m1 method");
      }
    }

    class C extends A {
      @Override
      void m1() {
        System.out.println("Inside C's m1 method");
      }
    }

    @Test
    void t() {
      A a = new A();
      B b = new B();
      C c = new C();

      A ref;    // obtain a reference of type A
      ref = a;  // ref refers to an A object

      ref.m1();  // calling A's version of m1()

      ref = b;   // now ref refers to a B object
      ref.m1();  // calling B's version of m1()

      ref = c;  // now ref refers to a C object
      ref.m1();  // calling C's version of m1()
    }
  }

  @Nested
  class UsesInstanceOf {
    class S1 implements S {
    }

    class S2 implements S {
    }

    class M1 implements M {
    }

    class S1I implements I {

      @Override
      public void hm(S s, M m) {
        System.out.println("s");
      }

      public void hm(S1 s, M m) {
        System.out.println("s1");
      }

      public void hm(S2 s, M m) {
        System.out.println("s2");
      }
    }

    @Test
    void t() {
//      if (s instanceof UsesInstanceOf.S1) {
    }
  }

  @Nested
  class UsesDefaultSelector {
  }
}
