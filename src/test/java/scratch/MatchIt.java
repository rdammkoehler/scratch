package scratch;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class One {
}

class Two {
}

class Three {
}

class FinalProcessorThing {

  private static final Map<Class, Method> preLoadedMethods = new HashMap() {
    {
      Method[] methods = FinalProcessorThing.class.getDeclaredMethods();
      for (Method method : methods) {
        if ("process".equals(method.getName())) {
          Class argType = method.getParameterTypes()[0];
          put(argType, method);
        }
      }
    }

  };


  public Object process(Object obj) {
    try {
      Method method = preLoadedMethods.get(obj.getClass());
      if (null != method) {
        return method.invoke(this, obj);
      } else {
        System.out.println("no matching method for " + obj.getClass().getName());
        return null;
      }
    } catch (IllegalAccessException | InvocationTargetException e) {
      e.printStackTrace();
    }
    return null;
  }

//  public Object process(Object obj) {
//    Method method = null;
//    try {
//      method = this.getClass().getMethod("process", obj.getClass());
//      if (null != method) {
//        try {
//          return method.invoke(this, obj);
//        } catch (IllegalAccessException | InvocationTargetException e) {
//          e.printStackTrace();
//          return null;
//        }
//      }
//    } catch (NoSuchMethodException e) {
//      e.printStackTrace();
//    }
//    System.out.println("no matching method for " + obj.getClass().getName());
//    return null;
//  }

  private void process(One one) {
    System.out.println("I see One");
  }

  private void process(Two two) {
    System.out.println("I see Two");
  }
}


public class MatchIt {

  public static void main(String[] args) {
    Object[] objects = new Object[]{new One(), new Two(), new Three()};
    int count = 10000;
    long startTime = System.nanoTime();
    for (int i = 0; i < count; i++) {
      FinalProcessorThing fProcThing = new FinalProcessorThing();
      for (Object o : objects) {
        fProcThing.process(o);
      }
    }
    long duration = (System.nanoTime() - startTime);
    System.out.println(count + " executions in " + duration + "ns");
    /*
     Preloaded: 10000 executions in 318587557ns
    On The Fly: 10000 executions in 1438030259ns
     */
  }

}
