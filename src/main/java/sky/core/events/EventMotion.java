package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import lombok.Data;
import lombok.AllArgsConstructor;

import com.darkmagician6.eventapi.events.callables.EventCancellable;

@Data
@AllArgsConstructor
public class EventMotion extends EventCancellable implements Event {
    private double x, y, z;
    private float yaw, pitch;
    private boolean onGround;
    private boolean isSneaking;
    private boolean isSprinting;
}