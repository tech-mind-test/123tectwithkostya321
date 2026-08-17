package sky.core.events;


import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EventThirdPersonRender extends EventCancellable implements Event {
    private boolean thirdperson;
}
