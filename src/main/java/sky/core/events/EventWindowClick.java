package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.ItemStack;

@Data
@AllArgsConstructor
public class EventWindowClick extends EventCancellable implements Event {
    private int windowId;
    private int slotId;
    private int mouseButton;
    private ClickType type;
    private ItemStack clickedItemIn;
    private short actionNumberIn;
}
