package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Slot;

@Data
@AllArgsConstructor
public class EventHandleMouseClick extends EventCancellable implements Event {
    private Slot slotIn;
    private int slotId;
    private int mouseButton;
    private ClickType type;
}
