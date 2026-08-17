package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

@Data
@AllArgsConstructor
public class EventRightClickItemCheck extends EventCancellable implements Event {
    private ItemStack itemStack;
    private Hand hand;
} 