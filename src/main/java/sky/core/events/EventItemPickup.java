package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;

@Data
@AllArgsConstructor
public class EventItemPickup extends EventCancellable implements Event {
    private Entity collectedEntity;
    private Entity collector;
    private ItemStack itemStack;
    private int amount;
}
