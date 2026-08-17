package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.item.ItemStack;

@Data
@AllArgsConstructor
public class EventUseEnderPearl extends EventCancellable implements Event {
    private ItemStack itemStack;
    private int cooldownTicks;
}
