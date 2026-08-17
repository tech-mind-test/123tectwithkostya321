package sky.core.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import com.darkmagician6.eventapi.events.Event;

@Data
@AllArgsConstructor
public class EventInput implements Event {
    private float forward, strafe;
    private boolean forwardKeyDown, backKeyDown, leftKeyDown, rightKeyDown;
    private boolean jump, sneak;
    private double sneakSlowDownMultiplier;
}