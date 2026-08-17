package sky.core.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import net.minecraft.util.math.vector.Vector2f;


@Data
@AllArgsConstructor
public class EventLook extends EventCancellable implements Event  {
    public double yaw, pitch;
}
