package scratch;

import org.junit.jupiter.api.Test;

import java.util.*;

public class Iffy {
  class D {
    String id,value;

    public D(String id, String value) {
      this.id = id;
      this.value = value;
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof D)) return false;
      D d = (D) o;
      return Objects.equals(getId(), d.getId()) &&
          Objects.equals(getValue(), d.getValue());
    }

    @Override
    public int hashCode() {
      return Objects.hash(getId());
    }

    @Override
    public String toString() {
      return "D{" +
          "id='" + id + '\'' +
          ", value='" + value + '\'' +
          '}';
    }
  }

  private void explain(Set<D> s) {
    System.out.println(s);                  // []
    s.add(new D("1", "1"));
    System.out.println(s);                  // [D{id='1', value='1'}]
    s.add(new D("2", "2"));
    System.out.println(s);                  // [D{id='1', value='1'}, D{id='2', value='2'}]
    s.add(new D("1", "3"));
    System.out.println(s);                  // For HashSet: [D{id='1', value='1'}, D{id='2', value='2'}, D{id='1', value='3']
                                            // For TreeSet: [D{id='1', value='1'}, D{id='2', value='2'}]  <<-- almost what I wanted.
  }

  @Test
  void thisIsIffy(){
    { //normal set
      Set<D> s = new HashSet<>();
      explain(s);
    }
    { //JDK only
      Set<D> s = new TreeSet<>(Comparator.comparing(D::getId));
      explain(s);
    }
//    { // with Guava
//      Set<D> s = Sets.newTreeSet(Comparator.comparing(D::getId)); //same as: (a,b)->a.getId().compareTo(b.getId()));
//      explain(s);
//    }
  }
}
