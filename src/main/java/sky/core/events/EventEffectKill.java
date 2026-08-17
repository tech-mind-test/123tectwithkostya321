package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.DamageSource;
@Getter
@AllArgsConstructor
public class EventEffectKill  implements Event {
    private LivingEntity entity;
    private DamageSource cause;
}
