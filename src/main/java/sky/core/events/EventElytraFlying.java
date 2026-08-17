package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.util.math.vector.Vector3d;

@Data
@AllArgsConstructor
public class EventElytraFlying extends EventCancellable implements Event {
    private Vector3d vector3d;
    private float f;
}
