package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EventDamage extends EventCancellable implements Event {
    private final DamageType damageType;

    public enum DamageType {
        FALL,
        ARROW,
        ENDER_PEARL,
        BLOCK;
    }
}
