package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.entity.Entity;

@Data
@AllArgsConstructor
public class EventAttack extends EventCancellable implements Event {
    private Entity target;
}