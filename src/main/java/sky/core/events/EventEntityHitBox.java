package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.entity.Entity;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class EventEntityHitBox extends EventCancellable implements Event {
    private Entity entity;
    private float size;
}
