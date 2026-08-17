package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.item.Item;


@Data
@AllArgsConstructor
public class EventCooldownTracker extends EventCancellable implements Event {
    private Item item;
    private float partialTicks;
}
