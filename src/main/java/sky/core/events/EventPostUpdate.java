package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.Data;

@Data
public class EventPostUpdate extends EventCancellable implements Event {
}
