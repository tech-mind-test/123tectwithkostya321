package sky.core.modules.impl.movement;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.util.math.vector.Vector3d;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.utils.player.MoveUtil;

public class NoWeb extends Module {

    public NoWeb() {
        super("No Web", "Убирает замедление в паутине", Category.Movement);
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        if (!mc.player.isInWeb) {
            return;
        }

        Vector3d motion = mc.player.getMotion();
        mc.player.setMotion(motion.x, 0.0D, motion.z);

        if (mc.gameSettings.keyBindJump.isKeyDown()) {
            motion = mc.player.getMotion();
            mc.player.setMotion(motion.x, 0.9D, motion.z);
        }

        if (mc.gameSettings.keyBindSneak.isKeyDown()) {
            motion = mc.player.getMotion();
            mc.player.setMotion(motion.x, -0.9D, motion.z);
        }

        MoveUtil.setMotion(0.21);
    }
}
