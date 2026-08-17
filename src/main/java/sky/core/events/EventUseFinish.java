package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

@Data
@AllArgsConstructor
public class EventUseFinish extends EventCancellable implements Event {
    private ItemStack itemStack;
    private World worldIn;
    private LivingEntity entityLiving;
}
