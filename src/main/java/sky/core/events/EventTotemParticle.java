package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EventTotemParticle extends EventCancellable implements Event {
    private double x;
    private double y;
    private double z;
    private double xSpeed;
    private double ySpeed;
    private double zSpeed;
}


