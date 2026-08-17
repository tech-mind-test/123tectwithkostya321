package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.entity.Entity;

@Getter
@AllArgsConstructor
public class EventCriticalHit extends EventCancellable implements Event {
    private final Entity targetEntity;

    public Entity getTarget() {
        return targetEntity;
    }
}