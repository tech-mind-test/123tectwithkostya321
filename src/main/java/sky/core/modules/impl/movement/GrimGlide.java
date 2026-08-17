package sky.core.modules.impl.movement;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.item.UseAction;
import net.minecraft.util.math.vector.Vector3d;
import sky.core.SkyCore;
import sky.core.events.EventMotion;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.ModuleManager;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.utils.StopWatch;
import sky.core.utils.math.MathUtil;

public class GrimGlide extends Module {

    private static final float MOTION_RANDOM_MIN = 1.001f;
    private static final float MOTION_RANDOM_MAX = 1.0021f;
    private static final double MOTION_Y_BOOST = 0.00600000075995922D;
    private static final long POSITION_BURST_MS = 30L;
    private static final int MOTION_BURST_TICKS = 30;

    private final ModeSetting mode = new ModeSetting("Режим", "Обычный", "Обычный", "Рывки");

    private long lastTickTime = 0;
    private int ticksTwo = 0;
    private final StopWatch glideTicks = new StopWatch();

    public GrimGlide() {
        super("Glide Fly", "Флай без фейерверков (Grim)", Category.Movement);
        addSettings(mode);
    }

    public static boolean isGlideFlyActive() {
        if (mc.player == null || !mc.player.isElytraFlying()) {
            return false;
        }
        ModuleManager manager = SkyCore.getInstance().getModuleManager();
        if (manager == null) {
            return false;
        }
        GrimGlide grimGlide = (GrimGlide) manager.getModule(GrimGlide.class);
        if (grimGlide != null && grimGlide.isEnabled()) {
            return true;
        }
        Flight flight = (Flight) manager.getModule(Flight.class);
        return flight != null && flight.isEnabled() && flight.isGlideFlyMode();
    }

    private static void blockEating() {
        if (mc.player == null) {
            return;
        }
        if (mc.player.isHandActive() && isConsumableUse(mc.player.getActiveItemStack().getUseAction())) {
            mc.playerController.onStoppedUsingItem(mc.player);
        }
        mc.gameSettings.keyBindUseItem.setPressed(false);
    }

    private static boolean isConsumableUse(UseAction action) {
        return action == UseAction.EAT || action == UseAction.DRINK;
    }

    @EventTarget
    public void onBlockEat(EventUpdate event) {
        if (!isGlideFlyActive()) {
            return;
        }
        blockEating();
    }

    private double getGlideForward() {
        return mc.player.ticksExisted % 2 == 0 ? 0.087D : 0.09D;
    }

    private void applyGlideMotion(double dx, double dz) {
        mc.player.setMotion(
                dx * MathUtil.randomtest(MOTION_RANDOM_MIN, MOTION_RANDOM_MAX),
                mc.player.getMotion().y + MOTION_Y_BOOST,
                dz * MathUtil.randomtest(MOTION_RANDOM_MIN, MOTION_RANDOM_MAX)
        );
    }

    private double[] getGlideDelta(float yaw) {
        double forward = getGlideForward();
        return new double[]{
                -Math.sin(Math.toRadians(yaw)) * forward,
                Math.cos(Math.toRadians(yaw)) * forward
        };
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (!mode.is("Обычный") || mc.player == null || mc.world == null || !mc.player.isElytraFlying()) {
            return;
        }

        ticksTwo++;
        Vector3d pos = mc.player.getPositionVec();
        float yaw = mc.player.rotationYaw;
        double[] delta = getGlideDelta(yaw);
        double dx = delta[0];
        double dz = delta[1];

        if (System.currentTimeMillis() - lastTickTime >= POSITION_BURST_MS) {
            mc.player.setPosition(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
            lastTickTime = System.currentTimeMillis();
        }

        if (ticksTwo % MOTION_BURST_TICKS == 0) {
            applyGlideMotion(dx, dz);
        }
    }

    @EventTarget
    public void onMotion(EventMotion event) {
        if (!mode.is("Рывки") || mc.player == null || !mc.player.isElytraFlying()) {
            return;
        }

        ticksTwo++;
        Vector3d pos = mc.player.getPositionVec();
        float yaw = mc.player.rotationYaw;
        double[] delta = getGlideDelta(yaw);
        double dx = delta[0];
        double dz = delta[1];
        float multX = MathUtil.randomtest(MOTION_RANDOM_MIN, MOTION_RANDOM_MAX);
        float multZ = MathUtil.randomtest(MOTION_RANDOM_MIN, MOTION_RANDOM_MAX);

        mc.player.setVelocity(dx * multX, mc.player.getMotion().y, dz * multZ);

        if (glideTicks.finished(POSITION_BURST_MS)) {
            mc.player.setPosition(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
            glideTicks.reset();
        }

        mc.player.setVelocity(
                dx * multX,
                mc.player.getMotion().y + MOTION_Y_BOOST,
                dz * multZ
        );
        event.setX(event.getX() + dx);
        event.setZ(event.getZ() + dz);
    }

    @Override
    public void onEnable() {
        super.onEnable(); 
        ticksTwo = 0;
        lastTickTime = System.currentTimeMillis();
        glideTicks.reset(); // ЭТО Я ЛОМАЛ КЕРНЕЛА НО НЕ В ЭТОМ ПРОЕКТЕ ПОЧЕМУ ЕТА ХУЙНЯ ТУТ
    }

    @Override
    public void onDisable() {
        ticksTwo = 0;
        blockEating();
        super.onDisable();
    }
}
