package sky.core.modules.impl.movement;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.util.math.MathHelper;
import sky.core.events.EventFireworkRocket;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.SliderSetting;


public class SuperFirework extends Module {
    public SuperFirework() {
        super("Super Firework", "Увеличивает скорость и дальность полета при использовании фейерверков на элитре", Category.Movement);
        addSettings(yaw05, yaw510, yaw1015, yaw1520, yaw2025, yaw2530, yaw3035, yaw3540, yaw4045,pitch05, pitch510, pitch1015, pitch1520, pitch2025, pitch2530, pitch3035, pitch3540, pitch4045);
    }

    private final SliderSetting yaw05 = new SliderSetting("Yaw 0-5°", 1.63f, 1.5f, 2.5f, 0.01f);
    private final SliderSetting yaw510 = new SliderSetting("Yaw 5-10°", 1.62f, 1.5f, 2.5f, 0.01f);
    private final SliderSetting yaw1015 = new SliderSetting("Yaw 10-15°", 1.69f, 1.5f, 2.5f, 0.01f);
    private final SliderSetting yaw1520 = new SliderSetting("Yaw 15-20°", 1.76f, 1.5f, 2.5f, 0.01f);
    private final SliderSetting yaw2025 = new SliderSetting("Yaw 20-25°", 1.77f, 1.5f, 2.5f, 0.01f);
    private final SliderSetting yaw2530 = new SliderSetting("Yaw 25-30°", 1.83f, 1.5f, 2.5f, 0.01f);
    private final SliderSetting yaw3035 = new SliderSetting("Yaw 30-35°", 1.92f, 1.5f, 2.5f, 0.01f);
    private final SliderSetting yaw3540 = new SliderSetting("Yaw 35-40°", 1.96f, 1.5f, 2.5f, 0.01f);
    private final SliderSetting yaw4045 = new SliderSetting("Yaw 40-45°", 1.94f, 1.5f, 2.5f, 0.01f);

    private final SliderSetting pitch05 = new SliderSetting("Pitch 0-5°", 1.62f, 1.5f, 2.5f, 0.01f);
    private final SliderSetting pitch510 = new SliderSetting("Pitch 5-10°", 1.63f, 1.5f, 2.5f, 0.01f);
    private final SliderSetting pitch1015 = new SliderSetting("Pitch 10-15°", 1.63f, 1.5f, 2.5f, 0.01f);
    private final SliderSetting pitch1520 = new SliderSetting("Pitch 15-20°", 1.66f, 1.5f, 2.5f, 0.01f);
    private final SliderSetting pitch2025 = new SliderSetting("Pitch 20-25°", 1.8f, 1.5f, 2.5f, 0.01f);
    private final SliderSetting pitch2530 = new SliderSetting("Pitch 25-30°", 1.84f, 1.5f, 2.5f, 0.01f);
    private final SliderSetting pitch3035 = new SliderSetting("Pitch 30-35°", 1.97f, 1.5f, 2.5f, 0.01f);
    private final SliderSetting pitch3540 = new SliderSetting("Pitch 35-40°", 1.99f, 1.5f, 2.5f, 0.01f);
    private final SliderSetting pitch4045 = new SliderSetting("Pitch 40-45°", 1.99f, 1.5f, 2.5f, 0.01f);

    private final SliderSetting[] yawSettings = {
            yaw05, yaw510, yaw1015, yaw1520, yaw2025, yaw2530, yaw3035, yaw3540, yaw4045
    };

    private final SliderSetting[] pitchSettings = {
            pitch05, pitch510, pitch1015, pitch1520, pitch2025, pitch2530, pitch3035, pitch3540, pitch4045
    };

    @EventTarget
    public void onEvent(final EventFireworkRocket event) {
        float convertedYaw = convertAngleToRange(MathHelper.wrapDegrees(mc.player.rotationYaw));
        float convertedPitch = convertAngleToRange(Math.abs(mc.player.rotationPitch));

        float xzSpeed = getSpeedForYaw(convertedYaw);
        float ySpeed = getSpeedForPitch(convertedPitch);

        if (ySpeed > xzSpeed) {
            xzSpeed = ySpeed;
        }

        event.setSpeed(xzSpeed);
        event.setYSpeed(ySpeed);
    }

    private float getSpeedForYaw(float yaw) {
        int index = (int) (yaw / 5.0f);
        if (index >= yawSettings.length) {
            index = yawSettings.length - 1;
        }
        if (index < 0) {
            index = 0;
        }
        return yawSettings[index].get().floatValue();
    }

    private float getSpeedForPitch(float pitch) {
        int index = (int) (pitch / 5.0f);
        if (index >= pitchSettings.length) {
            index = pitchSettings.length - 1;
        }
        if (index < 0) {
            index = 0;
        }
        return pitchSettings[index].get().floatValue();
    }

    private float convertAngleToRange(float angle) {
        float absAngle = Math.abs(angle);

        if (absAngle > 90.0f) {
            absAngle = 180.0f - absAngle;
        }

        if (absAngle > 45.0f) {
            absAngle = 90.0f - absAngle;
        }

        return absAngle;
    }
}
