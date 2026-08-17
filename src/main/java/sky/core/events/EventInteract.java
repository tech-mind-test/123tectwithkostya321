package sky.core.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.entity.Entity;
import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;

@Data
@AllArgsConstructor
public class EventInteract extends EventCancellable implements Event {
    private Entity entity;
}