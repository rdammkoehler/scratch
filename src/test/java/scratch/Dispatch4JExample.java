//package scratch;
//
//import io.github.szkiba.dispatch4j.ConsumerDispatcher;
//
//import java.util.function.Consumer;
//
//public class Dispatch4JExample {
//
//  interface Device {}
//  interface Event {}
//  class LoadValueUpdated implements Event {}
//  class LoadValueUpdatedHandler {
//    public void updateLoadValue(Device device, LoadValueUpdated message) {
//      System.out.println("updated load value");
//    }
//  }
//  class SystemAlarmAdded implements Event  {}
//  class SystemAlarmAddedHandler {
//    public void addSystemAlarm(Device device, SystemAlarmAdded message) {
//      System.out.println("system alarm added");
//    }
//  }
//  class MessageHandler {
//    private final Consumer<Event> dispatcher = ConsumerDispatcher.<Event>onNullThrow()
//        .on(LoadValueUpdated.class, this::updateLoadValue)
//        .on(SystemAlarmAdded.class, new SystemAlarmAddedHandler()::addSystemAlarm);
//
//    void updateLoadValue(LoadValueUpdated loadValueUpdated) {
//      new LoadValueUpdatedHandler().updateLoadValue(device, loadValueUpdated); //so how do we get device?
//    }
//
//    public void onMessage(final Event event) {
//      dispatcher.accept(event);
//    }
//  }
//}
