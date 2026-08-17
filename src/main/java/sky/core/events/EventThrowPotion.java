package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class EventThrowPotion extends EventCancellable implements Event {
    private final LivingEntity entity;
    private final ItemStack itemStack;
    private final double distanceFactor;
    private final List<EffectInstance> effects;
}