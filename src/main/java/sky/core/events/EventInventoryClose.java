package sky.core.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;

@Data
@AllArgsConstructor
public class EventInventoryClose extends EventCancellable implements Event {
    private int windowId;
}
