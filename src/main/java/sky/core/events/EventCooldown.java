package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.item.Item;

@Setter
@Getter
@AllArgsConstructor
public class EventCooldown extends EventCancellable implements Event {
    private final Item item;
    private int ticks;
}
