package sky.core.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import net.minecraft.entity.Entity;
import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;


@Data
public class EventEntityRayTrace extends EventCancellable implements Event {
    private final Entity entity;
}