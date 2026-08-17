package sky.core.modules.impl.combat;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.SkyCore;
import sky.core.events.EventAttack;
import sky.core.modules.Category;
import sky.core.modules.Module;
import net.minecraft.entity.player.PlayerEntity;

public class NoFriendDamage extends Module {

    public NoFriendDamage() {
        super("NoFriendDamage", "Отключает возможность наносить урон друзьям" ,Category.Combat);
    }

    @EventTarget
    public void onEvent(EventAttack event) {
        AttackAura attackAura = (AttackAura) SkyCore.getInstance().getModuleManager().getModule(AttackAura.class);
        if (event.getTarget() instanceof PlayerEntity player && SkyCore.getInstance().getFriendManager().isFriend(player.getGameProfile().getName()) && !(attackAura.isEnabled() && attackAura.target == event.getTarget())) {
            event.setCancelled(true);
        }
    }
}