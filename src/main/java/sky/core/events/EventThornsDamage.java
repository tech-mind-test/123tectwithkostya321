package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

@Data
@EqualsAndHashCode(callSuper = false)
public class EventThornsDamage extends EventCancellable implements Event {
    private final Entity attacker;
    private final LivingEntity user;
    private final int level;
    private float damage;

    public EventThornsDamage(Entity attacker, LivingEntity user, int level, float damage) {
        this.attacker = attacker;
        this.user = user;
        this.level = level;
        this.damage = damage;
    }
}
