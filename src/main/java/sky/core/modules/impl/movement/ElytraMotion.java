package sky.core.modules.impl.movement;


import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import sky.core.SkyCore;
import sky.core.events.EventMoving;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.modules.impl.combat.AttackAura;
import sky.core.utils.math.TimeUtil;
import sky.core.utils.player.InventoryUtil;

public class ElytraMotion extends Module {
    public final SliderSetting attackDistance = new SliderSetting("Дистанция", 2.5f, 1, 3, 0.1f);
    private final BooleanSetting auto = new BooleanSetting("AutoFireworks", false);

    public boolean freeze;


    public ElytraMotion() {
        super("ElytraMotion", "Зависает на элитрах перед противником", Category.Movement);
        addSettings(attackDistance, auto);
    }


    private final TimeUtil timer = new TimeUtil();

    @EventTarget
    public void update(EventUpdate eventUpdate) {
        if (!mc.player.isElytraFlying()) {
            freeze = false;
            return;
        }
        AttackAura killAura = SkyCore.getInstance().getModuleManager().getAttackAura();
        ElytraTarget elytraTarget = SkyCore.getInstance().getModuleManager().getElytraTarget();

        if (check(killAura, elytraTarget)) {
            mc.gameSettings.keyBindForward.setPressed(false);
            freeze = true;
        } else {
            mc.gameSettings.keyBindForward.setPressed(true);
            freeze = false;
        }
        if (auto.get()) {
            if (killAura.getTarget() != null && timer.hasTimeElapsed(500)) {
                InventoryUtil.inventorySwapClick(Items.FIREWORK_ROCKET);
                timer.reset();
            }
        }

    }

    @EventTarget
    private void onMotion(EventMoving eventMotion) {

        if (freeze) {
            eventMotion.getMotion().x = 0;
            eventMotion.getMotion().y = 0;
            eventMotion.getMotion().z = 0;
        }
    }

    public double lastSpeed = 0;

    public boolean check(AttackAura killAura, ElytraTarget elytraTarget) {
        LivingEntity target = killAura.getTarget();
        if (target == null || !mc.player.isElytraFlying()) return false;
        double dx = killAura.getTarget().getPosX() - killAura.getTarget().prevPosX;
        double dy = killAura.getTarget().getPosY() - killAura.getTarget().prevPosY;
        double dz = killAura.getTarget().getPosZ() - killAura.getTarget().prevPosZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double speed = distance * 20.0;

        boolean flag = speed < 25;
        return target.getDistance(mc.player) < attackDistance.get() + (target.isElytraFlying() ? 0.5f : 0) && mc.player.isElytraFlying() && flag;
    }


    @java.lang.Override
    public void onDisable() {
        freeze = false;
        super.onDisable();
    }
}

