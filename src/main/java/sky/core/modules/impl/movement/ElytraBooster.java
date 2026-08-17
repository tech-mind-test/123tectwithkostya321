package sky.core.modules.impl.movement;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.util.math.MathHelper;
import sky.core.SkyCore;
import sky.core.events.EventFireworkRocket;
import sky.core.events.EventRender2D;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.modules.api.constructors.impl.MultiBooleanSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.modules.impl.combat.AttackAura;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.font.Fonts;

public class ElytraBooster extends Module {

    private final BooleanSetting boost = new BooleanSetting("Ускорение", true);
    private final ModeSetting mode = new ModeSetting("Режим ускорения", "ReallyWorld", () -> boost.get(),
            "ReallyWorld", "Bravo", "BravoGrief", "Кастомный");

    public final MultiBooleanSetting render = new MultiBooleanSetting("Отображать", () -> mode.is("Кастомный"),
            new BooleanSetting("Углы", false));

    private final BooleanSetting individualY = new BooleanSetting("Индивидуальный Y", false,
            () -> boost.get() && mode.is("Кастомный"));

    // XZ слайдеры (0-45°, разбивка по 5°)
    private final SliderSetting xz0_5 = new SliderSetting("XZ 0-5°", 1.52f, 1.5f, 3.0f, 0.01f,
            () -> boost.get() && mode.is("Кастомный"));
    private final SliderSetting xz5_10 = new SliderSetting("XZ 5-10°", 1.53f, 1.5f, 3.0f, 0.01f,
            () -> boost.get() && mode.is("Кастомный"));
    private final SliderSetting xz10_15 = new SliderSetting("XZ 10-15°", 1.54f, 1.5f, 3.0f, 0.01f,
            () -> boost.get() && mode.is("Кастомный"));
    private final SliderSetting xz15_20 = new SliderSetting("XZ 15-20°", 1.55f, 1.5f, 3.0f, 0.01f,
            () -> boost.get() && mode.is("Кастомный"));
    private final SliderSetting xz20_25 = new SliderSetting("XZ 20-25°", 1.56f, 1.5f, 3.0f, 0.01f,
            () -> boost.get() && mode.is("Кастомный"));
    private final SliderSetting xz25_30 = new SliderSetting("XZ 25-30°", 1.57f, 1.5f, 3.0f, 0.01f,
            () -> boost.get() && mode.is("Кастомный"));
    private final SliderSetting xz30_35 = new SliderSetting("XZ 30-35°", 1.58f, 1.5f, 3.0f, 0.01f,
            () -> boost.get() && mode.is("Кастомный"));
    private final SliderSetting xz35_40 = new SliderSetting("XZ 35-40°", 1.59f, 1.5f, 3.0f, 0.01f,
            () -> boost.get() && mode.is("Кастомный"));
    private final SliderSetting xz40_45 = new SliderSetting("XZ 40-45°", 1.60f, 1.5f, 3.0f, 0.01f,
            () -> boost.get() && mode.is("Кастомный"));

    private final SliderSetting y0_5 = new SliderSetting("Y 0-5°", 1.51f, 1.5f, 3.0f, 0.01f,
            () -> boost.get() && mode.is("Кастомный") && !individualY.get());
    private final SliderSetting y5_10 = new SliderSetting("Y 5-10°", 1.52f, 1.5f, 3.0f, 0.01f,
            () -> boost.get() && mode.is("Кастомный") && !individualY.get());
    private final SliderSetting y10_15 = new SliderSetting("Y 10-15°", 1.53f, 1.5f, 3.0f, 0.01f,
            () -> boost.get() && mode.is("Кастомный") && !individualY.get());
    private final SliderSetting y15_20 = new SliderSetting("Y 15-20°", 1.54f, 1.5f, 3.0f, 0.01f,
            () -> boost.get() && mode.is("Кастомный") && !individualY.get());
    private final SliderSetting y20_25 = new SliderSetting("Y 20-25°", 1.55f, 1.5f, 3.0f, 0.01f,
            () -> boost.get() && mode.is("Кастомный") && !individualY.get());
    private final SliderSetting y25_30 = new SliderSetting("Y 25-30°", 1.56f, 1.5f, 3.0f, 0.01f,
            () -> boost.get() && mode.is("Кастомный") && !individualY.get());
    private final SliderSetting y30_35 = new SliderSetting("Y 30-35°", 1.57f, 1.5f, 3.0f, 0.01f,
            () -> boost.get() && mode.is("Кастомный") && !individualY.get());
    private final SliderSetting y35_40 = new SliderSetting("Y 35-40°", 1.58f, 1.5f, 3.0f, 0.01f,
            () -> boost.get() && mode.is("Кастомный") && !individualY.get());
    private final SliderSetting y40_45 = new SliderSetting("Y 40-45°", 1.59f, 1.5f, 3.0f, 0.01f,
            () -> boost.get() && mode.is("Кастомный") && !individualY.get());
    private final SliderSetting y45_50 = new SliderSetting("Y 45-50°", 1.60f, 1.5f, 3.0f, 0.01f,
            () -> boost.get() && mode.is("Кастомный") && !individualY.get());
    private final SliderSetting y50_55 = new SliderSetting("Y 50-55°", 1.61f, 1.5f, 3.0f, 0.01f,
            () -> boost.get() && mode.is("Кастомный") && !individualY.get());

    // Индивидуальные слайдеры для каждого градуса по Y (0-55) - показываются только если individualY включён
    private final SliderSetting[] individualYSettings = new SliderSetting[56];

    // Массивы для групповой работы
    private final SliderSetting[] xzSliders = {xz0_5, xz5_10, xz10_15, xz15_20, xz20_25, xz25_30, xz30_35, xz35_40, xz40_45};
    private final SliderSetting[] yGroupSliders = {y0_5, y5_10, y10_15, y15_20, y20_25, y25_30, y30_35, y35_40, y40_45, y45_50, y50_55};

    private static final float[] BRAVO_GRIEF_XZ = {1.67f, 1.68f, 1.69f, 1.72f, 1.72f, 1.73f, 1.74f, 1.75f, 1.76f};
    private static final float[] BRAVO_GRIEF_Y = {1.72f, 1.72f, 1.72f, 1.72f, 1.72f, 1.73f, 1.74f, 1.76f, 1.8f};
    private static final float[] REALLY_WORLD_XZ = {1.6f, 1.63f, 1.66f, 1.71f, 1.73f, 1.81f, 1.81f, 1.81f, 1.81f};
    private static final float[] REALLY_WORLD_Y = new float[]{1.6f, 1.62f, 1.6f, 1.62f, 1.64f, 1.68f, 1.75f, 1.9f, 2.16f};

    public ElytraBooster() {
        super("Super Firework", "Ускоряет полёт на элитрах", Category.Movement);

        // Инициализация индивидуальных слайдеров для каждого градуса Y (0-55)
        // Показываются только когда individualY включён
        for (int i = 0; i <= 55; i++) {
            float defaultValue = 1.5f + (i / 55f) * 1.5f;
            final int degree = i;
            individualYSettings[i] = new SliderSetting("Y " + i + "°", defaultValue, 1.5f, 3.0f, 0.01f,
                    () -> boost.get() && mode.is("Кастомный") && individualY.get());
        }

        // Добавление всех настроек
        addSettings(boost, mode, individualY);
        addSettings(xzSliders);
        addSettings(yGroupSliders);
        addSettings(individualYSettings);
        addSettings(render);
    }

    private void addSettings(SliderSetting[] settings) {
        for (SliderSetting setting : settings) {
            addSettings(setting);
        }
    }

    @EventTarget
    public void onFirework(EventFireworkRocket event) {
        if (!boost.get() || mc.player == null) {
            return;
        }

        AttackAura aura = SkyCore.getInstance().getModuleManager().attackAura;
        if (aura == null) {
            return;
        }

        float yaw = MathHelper.wrapDegrees(mc.player.rotationYaw);
        float pitch = mc.player.rotationPitch;

        if (aura.isEnabled() && AttackAura.rotate != null) {
            yaw = MathHelper.wrapDegrees(AttackAura.rotate.x);
            pitch = AttackAura.rotate.y;
        }

        if (mode.is("Кастомный")) {
            float speedXZ = getCustomSpeed(yaw, xzSliders);
            float speedY;

            if (individualY.get()) {
                speedY = getIndividualYSpeed(pitch);
            } else {
                speedY = getCustomSpeed(pitch, yGroupSliders);
            }

            if (speedY > speedXZ) {
                speedXZ = speedY;
            }
            event.setSpeed(speedXZ);
            event.setYSpeed(speedY);
        } else if (mode.is("Bravo")) {
            event.setSpeed(getBravoSpeedXZ(pitch, yaw));
            event.setYSpeed(getBravoSpeedY(pitch));
        } else if (mode.is("BravoGrief")) {
            event.setSpeed(getTableSpeed(BRAVO_GRIEF_XZ, yaw, 9));
            event.setYSpeed(getTableSpeed(BRAVO_GRIEF_Y, pitch, 9));
        } else if (mode.is("ReallyWorld")) {
            float speedXZ = getTableSpeed(REALLY_WORLD_XZ, yaw, 9);
            float speedY = getTableSpeed(REALLY_WORLD_Y, pitch, 9);
            if (speedY > speedXZ) {
                speedXZ = speedY;
            }
            event.setSpeed(speedXZ);
            event.setYSpeed(speedY);
        }
    }

    // Получение скорости по индивидуальным градусам Y
    private float getIndividualYSpeed(float pitch) {
        float absPitch = convertAngleForIndividual(pitch);
        int degree = Math.round(absPitch);

        if (degree < 0) degree = 0;
        if (degree > 55) degree = 55;

        return individualYSettings[degree].get();
    }

    // Конвертация угла для индивидуального режима
    private float convertAngleForIndividual(float angle) {
        float absAngle = Math.abs(angle);
        if (absAngle > 90.0f) {
            absAngle = 180.0f - absAngle;
        }
        return absAngle;
    }

    private float getCustomSpeed(float angle, SliderSetting[] sliders) {
        float converted = convertAngle(angle);
        int index = (int) (converted / 5.0f);
        if (index >= sliders.length) {
            index = sliders.length - 1;
        }
        if (index < 0) {
            index = 0;
        }
        return sliders[index].get();
    }

    private float convertAngle(float angle) {
        float absAngle = Math.abs(angle);
        if (absAngle > 90.0f) {
            absAngle = 180.0f - absAngle;
        }
        if (absAngle > 45.0f) {
            absAngle = 90.0f - absAngle;
        }
        return absAngle;
    }

    private static float getTableSpeed(float[] table, float angle, int tableLength) {
        int index = (int) (convertAngleStatic(angle) / 5.0f);
        if (index >= tableLength) {
            index = tableLength - 1;
        }
        if (index < 0) {
            index = 0;
        }
        return table[index];
    }

    private static float convertAngleStatic(float angle) {
        float absAngle = Math.abs(angle);
        if (absAngle > 90.0f) {
            absAngle = 180.0f - absAngle;
        }
        if (absAngle > 45.0f) {
            absAngle = 90.0f - absAngle;
        }
        return absAngle;
    }

    private float getBravoSpeedXZ(float pitch, float yaw) {
        float absPitch = Math.abs(pitch);
        float absYaw = Math.abs(MathHelper.wrapDegrees(yaw) % 90.0f);
        float speed = absPitch >= 38.0f && absPitch <= 52.0f ? 2.0f
                : absPitch >= 32.0f && absPitch <= 58.0f ? 1.96f
                : absPitch >= 28.0f && absPitch <= 62.0f ? 1.95f
                : (absYaw >= 29.0f && absYaw <= 61.0f) || (absPitch >= 29.0f && absPitch <= 61.0f) ? 1.963f
                : (absYaw >= 28.0f && absYaw <= 60.0f) || (absPitch >= 28.0f && absPitch <= 60.0f) ? 1.954f
                : (absYaw >= 26.0f && absYaw <= 64.0f) || (absPitch >= 26.0f && absPitch <= 64.0f) ? 1.874f
                : (absYaw >= 24.0f && absYaw <= 66.0f) || (absPitch >= 24.0f && absPitch <= 66.0f) ? 1.75f
                : (absYaw >= 15.0f && absYaw <= 75.0f) || (absPitch >= 15.0f && absPitch <= 75.0f) ? 1.75f
                : (absYaw >= 13.0f && absYaw <= 77.0f) || (absPitch >= 13.0f && absPitch <= 77.0f) ? 1.75f
                : (absYaw >= 12.0f && absYaw <= 78.0f) || (absPitch >= 12.0f && absPitch <= 78.0f) ? 1.75f
                : (absYaw >= 8.0f && absYaw <= 82.0f) || (absPitch >= 11.0f && absPitch <= 79.0f) ? 1.75f
                : (absYaw >= 5.0f && absYaw <= 85.0f) || (absPitch >= 8.0f && absPitch <= 82.0f) ? 1.67f
                : (absYaw <= 90.0f || absPitch <= 90.0f) ? 1.67f : 1.66f;
        return pitch > 15.0f ? speed - 0.068f : speed;
    }

    private float getBravoSpeedY(float pitch) {
        float absPitch = Math.abs(pitch);
        if (absPitch >= 37.0f && absPitch <= 38.0f) {
            return 2.03f;
        }
        if (absPitch >= 25.0f && absPitch <= 30.0f) {
            return 2.0f;
        }
        if (absPitch >= 35.0f && absPitch <= 45.0f) {
            return 1.99f;
        }
        if (absPitch >= 40.0f && absPitch <= 50.0f) {
            return 1.97f;
        }
        if (absPitch >= 50.0f && absPitch <= 60.0f) {
            return 1.96f;
        }
        if (absPitch >= 51.0f && absPitch <= 61.0f) {
            return 1.85f;
        }
        if (absPitch >= 52.0f && absPitch <= 65.0f) {
            return 1.8f;
        }
        return 1.59f;
    }

    @EventTarget
    private void onRender(EventRender2D event) {
        if (mc.player == null || mc.world == null || mc.gameSettings.showDebugInfo) return;
        if (!mode.is("Кастомный")) return;

        int screenCenterX = mc.getMainWindow().getScaledWidth() / 2;

        if (Boolean.TRUE.equals(render.is("Углы"))) {
            String pitch = String.valueOf((int) mc.player.getPitch(event.getPartialTicks()));
            String pitchText = "Pitch: " + pitch;
            float pitchWidth = Fonts.sf_medium[16].getWidth(pitchText);
            float x = screenCenterX - pitchWidth / 2f;
            float y = mc.getMainWindow().getScaledHeight() / 2f + 12f;
            Fonts.sf_medium[16].drawString(event.getStack(), pitchText, x, y + 13, ColorUtil.getColor(255, 255, 255, 255));
        }
    }
}