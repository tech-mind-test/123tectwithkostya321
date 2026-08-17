package sky.core.modules.impl.movement;

import net.minecraft.util.math.vector.Vector3d;
import sky.core.modules.Category;
import sky.core.modules.Module;

public class FastLeaveFt extends Module {

    public FastLeaveFt() {
        super("FastLeaveFt", "Тепает обрано где вы были до прыжка и только на снегу", Category.Movement);
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null) {
            toggle();
            return;
        }

        float yaw = mc.player.rotationYaw;
        double x = -Math.sin(Math.toRadians(yaw));
        double z = Math.cos(Math.toRadians(yaw));

        Vector3d currentPos = mc.player.getPositionVec();
        Vector3d newPos = currentPos.add(x * 50, 0, z * 50);

        mc.player.setPosition(newPos.x, newPos.y, newPos.z);

        new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            toggle();
        }).start();
    }
}