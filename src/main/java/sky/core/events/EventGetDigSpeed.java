package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.block.BlockState;

@Data
@AllArgsConstructor
public class EventGetDigSpeed extends EventCancellable implements Event {
    private BlockState blockState;
    private float digSpeed;
}
