package sky.core.modules.impl.movement;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import lombok.Getter;
import net.minecraft.entity.LivingEntity;
import sky.core.SkyCore;
import sky.core.utils.component.impl.ElytraComponent;
import sky.core.events.EventRender3D;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.modules.api.constructors.impl.TextSetting;
import sky.core.modules.impl.combat.AttackAura;
import sky.core.utils.math.TimeUtil;

@Getter
public class ElytraTarget extends Module {



    //predict
    private final TextSetting info1 = new TextSetting("Перегон");

    public final BooleanSetting target = new BooleanSetting("Перегонять", false);

    public static SliderSetting pursuitdistance = new SliderSetting("Растояние преследования", 30, 10, 100, 5);
    public final ModeSetting predictMode = new ModeSetting("Режим предикта", "ReallyWorld", target::get, "ReallyWorld", "ReallyWorld - 2", "Default");
    public final BooleanSetting predictCube = new BooleanSetting("Рисовать предикт", true, () -> target.get() && supportsPredictCube());
    public final SliderSetting predictFillAlpha = new SliderSetting("Прозрачность", 40f, 0f, 255f, 1f,
            () -> target.get() && predictCube.get() && supportsPredictCube());
    public final BooleanSetting predictFromTheme = new BooleanSetting("От темы", true,
            () -> target.get() && predictCube.get() && supportsPredictCube());
    public final ModeSetting predictBoxMode = new ModeSetting("Вид квадрата", "Обычный",
            () -> target.get() && predictCube.get() && supportsPredictCube(),
            "Обычный", "Пунктир", "Диагонали");
    public final BooleanSetting visualReverse = new BooleanSetting("Разворот на 180", false, target::get);
    public final SliderSetting distance = new SliderSetting("Сила предикта", 2.7f, 1.0f, 5.0f, 0.1f, target::get);
    private final BooleanSetting box = new BooleanSetting("Рендерить Бокс", true);

    boolean non = false;
    int lastslot = 0;
    private final TimeUtil timerUtils = new TimeUtil();

    public boolean status = true;
    public boolean disableForward = false;
    private final TimeUtil hurtTimer = new TimeUtil();
    private boolean visualReverseWasActive = false;

    public ElytraTarget() {
        super("ElytraTarget", "Преследует таргета на элитре", Category.Combat);
        addSettings(info1, predictMode, predictCube, predictFillAlpha, predictFromTheme, predictBoxMode,
                visualReverse, target, distance, pursuitdistance);
    }

    public boolean isReallyworldPredict() {
        return predictMode.is("ssdaasdasd") || predictMode.is("asdasd");
    }

    public boolean isTestPredict() {
        return predictMode.is("ReallyWorld");
    }

    public boolean isPedikPredict() {
        return predictMode.is("ReallyWorld - 2");
    }

    public boolean usesVisualReverseLogic() {
        return isTestPredict() || isPedikPredict();
    }

    public boolean supportsPredictCube() {
        return isReallyworldPredict() || isTestPredict() || isPedikPredict();
    }

    @EventTarget
    public void update(EventUpdate eventUpdate) {

        if (!this.non) {
            this.lastslot = mc.player.inventory.currentItem;
        }

        AttackAura aura = SkyCore.getInstance().getModuleManager().attackAura;
        this.status = this.target.get();

        if (aura.getTarget() == null) {
            this.disableForward = false;
            return;
        }

        if (mc.player.hurtTime > 0 && !aura.getTarget().lastSwing.finished(500L)) {
            this.disableForward = true;
            this.hurtTimer.reset();
        }

        if (this.hurtTimer.finished(500L)) {
            this.disableForward = false;
        }
    }

    @EventTarget
    private void onRender(EventRender3D event) {
        ElytraComponent.renderPredictCube(event, this);
    }

    public boolean shouldTarget(LivingEntity livingEntity) {
        if (this.isEnabled() && livingEntity != null && !this.disableForward) {
            boolean isTargetValid = livingEntity.isElytraFlying();
            return this.status && mc.player.isElytraFlying() && isTargetValid;
        } else {
            return false;
        }
    }

    public boolean isReverseActive() {
        if (!this.usesVisualReverseLogic() || !this.isEnabled() || !this.target.get() || !this.visualReverse.get() || this.disableForward || mc.player == null) {
            visualReverseWasActive = false;
            return false;
        }

        AttackAura aura = SkyCore.getInstance().getModuleManager().attackAura;
        LivingEntity auraTarget = aura != null ? aura.getTarget() : null;
        if (auraTarget == null || !this.shouldTarget(auraTarget)) {
            visualReverseWasActive = false;
            return false;
        }

        float distanceToTarget = mc.player.getDistance(auraTarget);
        float enableDistance = 2.5F;
        float predictDistance = Math.max(enableDistance, this.distance.get());
        float disableDistance = predictDistance + 3.0F;

        if (!visualReverseWasActive && distanceToTarget <= enableDistance) {
            visualReverseWasActive = true;
        }
        if (visualReverseWasActive && distanceToTarget >= disableDistance) {
            visualReverseWasActive = false;
        }

        return visualReverseWasActive;
    }

    @EventTarget
    public void onDisable() {
        super.onDisable();
    }
}