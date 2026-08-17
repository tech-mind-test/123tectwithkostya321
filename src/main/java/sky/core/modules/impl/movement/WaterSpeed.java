package sky.core.modules.impl.movement;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.block.Blocks;
import com.adl.nativeprotect.Native;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.utils.math.TimeUtil;
import sky.core.utils.player.MoveUtil;

public class WaterSpeed extends Module {

    public WaterSpeed() {
        super("Water Speed", "Даёт плавать в воде быстрее", Category.Movement);
    }

    private final TimeUtil time = new TimeUtil();
    float acceletion = 1.01f;

    @Native
    @EventTarget
    public void onUpdate(EventUpdate e) {
        if ((mc.world.getBlockState(mc.player.getPosition().up()).getBlock() != Blocks.WATER && mc.world.getBlockState(mc.player.getPosition().up()).getBlock() != Blocks.LAVA && mc.gameSettings.keyBindJump.isKeyDown()) || (!mc.player.isInWater() && !mc.player.isInLava())) {
            time.reset();
        }

        if ((mc.player.isInWater() || mc.player.isInLava()) && time.finished(500)) {
            float ySpeed = mc.gameSettings.keyBindJump.isKeyDown() ? 0.05f : (mc.gameSettings.keyBindSneak.isKeyDown()) ? -0.05f : !mc.player.isSprinting() ? 0.005f : 0;
            if (mc.player.isSprinting() || MoveUtil.isMoving()) {
                time.reset();
                ySpeed = 0;
            }
            mc.player.movementInput.sneaking = false;
            mc.player.setVelocity(mc.player.getMotion().x, mc.player.getMotion().y + ySpeed, mc.player.getMotion().z);
        }
    }

    @Native
    @java.lang.Override
    public void onDisable() {
        super.onDisable();
        acceletion = 1.01f;
    }
}
