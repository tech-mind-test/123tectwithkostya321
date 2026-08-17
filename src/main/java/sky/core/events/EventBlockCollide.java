package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.util.math.BlockPos;

@Data
@AllArgsConstructor
public class EventBlockCollide extends EventCancellable implements Event {
    private BlockPos pos;
}
