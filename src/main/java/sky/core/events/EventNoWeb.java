package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.BlockPos;

@Getter
@Setter
@AllArgsConstructor
public class EventNoWeb extends EventCancellable implements Event {
    private BlockPos blockPos;
   // private float speed, speedY;
}
