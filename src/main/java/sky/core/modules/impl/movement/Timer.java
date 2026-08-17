package sky.core.modules.impl.movement;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.client.CConfirmTransactionPacket;
import net.minecraft.network.play.server.SEntityVelocityPacket;
import net.minecraft.network.play.server.SPlayerPositionLookPacket;
import net.minecraft.util.math.MathHelper;
import com.adl.nativeprotect.Native;
import sky.core.events.EventKey;
import sky.core.events.EventMotion;
import sky.core.events.EventPacket;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BindSetting;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.utils.math.TimeUtil;


public class Timer extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Обычный", "Обычный", "Grim");
    private final BindSetting grimBind = new BindSetting("Кнопка буста", () -> mode.is("Grim"));
    private final SliderSetting timerAmount = new SliderSetting("Скорость", 2.0F, 1.0F, 5.0F, 0.025F);

    private final BooleanSetting smart = new BooleanSetting("Умный", true, () -> !mode.is("Grim"));
    private final BooleanSetting movingUp = new BooleanSetting("Добавлять в движении", false, () -> !mode.is("Grim"));
    private final SliderSetting upValue = new SliderSetting("Значение", 0.02F, 0.01F, 0.5F, 0.01F, () -> movingUp.get());
    private final SliderSetting ticks = new SliderSetting("Скорость убывания", 1.0F, 0.15F, 3.0F, 0.1F, () -> !mode.is("Grim"));

    private final float maxViolation = 100.0F;
    private float violation = 0.0F;

    private double prevPosX;
    private double prevPosY;
    private double prevPosZ;
    private float yaw;
    private float pitch;
    private boolean isBoost;
    private final TimeUtil timerUtil = new TimeUtil();

    public Timer() {
        super("Timer", "Изменяет скорость игры", Category.Movement);
        addSettings(mode, grimBind, timerAmount, smart, movingUp, upValue, ticks);
    }

    @EventTarget
    public void onEvent(EventKey e) {
        if (mode.is("Grim") && e.getKey() == grimBind.get()) {
            isBoost = true;
        }
    }

    @Native
    @EventTarget
    public void onEvent(EventMotion e) {
        if (mc.player == null) return;
        updateTimer(e.getYaw(), e.getPitch(),
                mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ());
    }

    @EventTarget
    public void onEvent(EventUpdate e) {
        if (mc.player == null) return;

        if (timerUtil.hasTimeElapsed(25000L)) {
            reset();
            timerUtil.reset();
        }

        if (!mc.player.isOnGround() && !isBoost) {
            violation += 0.1F;
            violation = MathHelper.clamp(violation, 0.0F, maxViolation / (mode.is("Grim") ? 1.0F : timerAmount.get()));
        }

        if (!mode.is("Grim") || isBoost) {
            mc.timer.setSpeed(timerAmount.get());
            if (smart.get() && !(mc.timer.getSpeed() <= 1.0F)) {
                if (violation < maxViolation / timerAmount.get()) {
                    violation += mode.is("Grim") ? 0.05F : ticks.get();
                    violation = MathHelper.clamp(violation, 0.0F, maxViolation / (mode.is("Grim") ? 1.0F : timerAmount.get()));
                } else {
                    resetSpeed();
                }
            }
        }
    }

    @EventTarget
    public void onEvent(EventPacket e) {
        if (!mode.is("Grim") || mc.player == null) return;

        IPacket<?> packet = e.getPacket();
        if (e.isReceive()) {
            if (packet instanceof SPlayerPositionLookPacket) {
                if (isBoost) {
                    resetSpeed();
                    reset();
                }
            }
            if (packet instanceof SEntityVelocityPacket vel) {
                if (vel.getEntityID() == mc.player.getEntityId()) {
                    reset();
                    resetSpeed();
                }
            }
        }

        if (e.isSend() && packet instanceof CConfirmTransactionPacket) {
            e.setCancelled(true);
        }
    }

    @Native
    private void updateTimer(float yaw, float pitch, double posX, double posY, double posZ) {
        if (notMoving(posX, posY, posZ, yaw, pitch)) {
            if (mode.is("Grim")) {
                violation = (float) (violation - 0.05D);
            } else {
                violation = (float) (violation - (ticks.get() + 0.4D));
            }
        } else if (movingUp.get() && !mode.is("Grim")) {
            violation -= upValue.get();
        }

        violation = (float) MathHelper.clamp(violation, 0.0D, Math.floor(maxViolation));
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    private boolean notMoving(double posX, double posY, double posZ, float yaw, float pitch) {
        return prevPosX == posX
                && prevPosY == posY
                && prevPosZ == posZ
                && this.yaw == yaw
                && this.pitch == pitch;
    }

    private void resetSpeed() {
        mc.timer.resetSpeed();
        setEnabled(false);
    }

    private void reset() {
        if (mode.is("Grim")) {
            violation = maxViolation / timerAmount.get();
            isBoost = false;
        }
    }

    @java.lang.Override
    public void onEnable() {
        reset();
        mc.timer.resetSpeed();
        super.onEnable();
    }

    @java.lang.Override
    public void onDisable() {
        reset();
        mc.timer.resetSpeed();
        timerUtil.reset();
        super.onDisable();
    }
}