package sky.core.events;

import lombok.AllArgsConstructor;
import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.Data;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;

@Data
@AllArgsConstructor
public class EventRotation extends EventCancellable implements Event {
    private Vector3d position;
    private Vector2f rotation;
}