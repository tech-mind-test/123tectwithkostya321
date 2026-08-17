package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.item.ItemStack;

import java.util.List;

@Data
@AllArgsConstructor
public class EventContainerTick extends EventCancellable implements Event {
    private int windowId;
    private List<ItemStack> items;
}




