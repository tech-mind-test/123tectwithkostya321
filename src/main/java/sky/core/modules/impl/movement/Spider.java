package sky.core.modules.impl.movement;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import com.adl.nativeprotect.Native;
import sky.core.events.EventMotion;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.utils.player.InventoryUtil;

import java.util.Timer;
import java.util.TimerTask;

public class Spider extends Module {
    public ModeSetting mode = new ModeSetting("Режим", "Байпас", "Байпас", "Шар", "Вода", "Пакетная Вода");
    public SliderSetting speed = new SliderSetting("Скорость", 0.45f, 0.1f, 0.55f, 0.025f, () -> mode.is("Байпас"));
    public final BooleanSetting shift = new BooleanSetting("Эксплойт шифта", true, () -> mode.is("Байпас") || mode.is("Пакетная Вода"));
    public SliderSetting delay = new SliderSetting("Задержка", 0.31f, 0.1f, 1.0f, 0.001f, () -> mode.is("Пакетная Вода"));

    private boolean wasSneaking;
    private long lastPlacementTime;
    private Timer timer = new Timer();
    private boolean canUse = true;
    private long lastWallJumpMs = 0L;
    private static final long WALL_JUMP_COOLDOWN_MS = 250L;
    private int originalSlot = -1;

    public Spider() {
        super("Spider", "Лазит по стенам", Category.Movement);
        addSettings(mode, speed, shift, delay);
    }

    @Override
    public void onEnable() {
        if (mode.is("Пакетная Вода") && mc.player != null) {
            originalSlot = mc.player.inventory.currentItem;
        }
        super.onEnable();
    }

    @EventTarget
    public void onMotion(EventMotion motion) {
        if (mc.player == null || mc.world == null) return;

        if (mode.is("Байпас")) {
            handleBypassMode();
        } else if (mode.is("Шар")) {
            handleSphereMode(motion);
        } else if (mode.is("Пакетная Вода")) {
            handleWaterBucketMode(motion);
        } else if (mode.is("Вода")) {
            handleWaterMode();
        }
    }

    private void handleBypassMode() {
        if (!mc.player.collidedHorizontally) {
            if (shift.get() && wasSneaking && mc.gameSettings.keyBindSneak.isKeyDown()) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKey(), false);
                wasSneaking = false;
            }
            return;
        }

        mc.player.setMotion(mc.player.getMotion().x, speed.get().doubleValue(), mc.player.getMotion().z);
        if (shift.get() && !mc.gameSettings.keyBindSneak.isKeyDown()) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKey(), true);
            wasSneaking = true;
        }
    }

    private void handleSphereMode(EventMotion motion) {
        if (!mc.player.collidedHorizontally) return;

        if (mc.player.isOnGround()) {
            mc.player.jump();
        }
        if (mc.player.fallDistance <= 0 || mc.player.fallDistance >= 2) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPlacementTime < 100) return;

        ItemStack offHandItem = mc.player.getHeldItemOffhand();
        if (offHandItem.getItem() != Items.PLAYER_HEAD) return;

        RayTraceResult trace = mc.player.pick(3.0D, 1.0F, false);
        if (!(trace instanceof BlockRayTraceResult blockTrace)) return;

        BlockPos hitPos = blockTrace.getPos();
        if (!mc.world.getBlockState(hitPos).isSolid()) return;

        motion.setPitch(80f);
        motion.setYaw(mc.player.getHorizontalFacing().getHorizontalAngle());
        mc.player.rotationPitchHead = 80f;

        mc.player.swingArm(Hand.OFF_HAND);
        mc.playerController.rightClickBlock(mc.player, mc.world, Hand.OFF_HAND, blockTrace);

        if (mc.gameSettings.keyBindSneak.isKeyDown()) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKey(), false);
            wasSneaking = false;
        }

        mc.player.fallDistance = 0;
        lastPlacementTime = currentTime;
    }

    private void handleWaterMode() {
        if (!mc.player.collidedHorizontally) return;

        int bucketSlot = findWaterBucketInHotbar();
        if (bucketSlot == -1) return;

        if (mc.player.inventory.currentItem != bucketSlot) {
            mc.player.inventory.currentItem = bucketSlot;
        }

        mc.playerController.processRightClick(mc.player, mc.world, Hand.MAIN_HAND);
        mc.player.setMotion(mc.player.getMotion().x, 0.36, mc.player.getMotion().z);
    }

    private void handleWaterBucketMode(EventMotion motion) {
        if (mc.player.isInWater() || mc.player.isInLava()) {
            mc.player.setMotion(mc.player.getMotion().x, 0.30f, mc.player.getMotion().z);
            return;
        }

        int waterSlot = findWaterBucketInHotbar();
        boolean hasWaterBucket = waterSlot != -1 || InventoryUtil.getSlot(Items.WATER_BUCKET) != -1;

        if (!hasWaterBucket || !mc.player.collidedHorizontally) {
            if (shift.get() && wasSneaking && mc.gameSettings.keyBindSneak.isKeyDown()) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKey(), false);
                wasSneaking = false;
            }
            if (mc.gameSettings.keyBindJump.isKeyDown()) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKey(), false);
            }
            return;
        }

        if (shift.get() && !mc.gameSettings.keyBindSneak.isKeyDown()) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKey(), true);
            wasSneaking = true;
        }

        if (mc.player.isOnGround()) {
            long now = System.currentTimeMillis();
            if (now - lastWallJumpMs > WALL_JUMP_COOLDOWN_MS) {
                mc.player.jump();
                lastWallJumpMs = now;
            }
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKey(), true);
        }

        if (!canUse) return;

        motion.setPitch(70f);
        mc.player.rotationPitchHead = 70f;

        if (waterSlot == -1) {
            InventoryUtil.inventorySwapClick(Items.WATER_BUCKET);
        } else {
            int currentSlot = mc.player.inventory.currentItem;
            if (waterSlot != currentSlot) {
                mc.player.connection.sendPacket(new CHeldItemChangePacket(waterSlot));
            }
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
            if (waterSlot != currentSlot) {
                mc.player.connection.sendPacket(new CHeldItemChangePacket(currentSlot));
            }
        }

        mc.player.setMotion(mc.player.getMotion().x, 0.43, mc.player.getMotion().z);
        canUse = false;

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                canUse = true;
            }
        }, getDelayMs());
    }

    private int getDelayMs() {
        return (int) (delay.get().doubleValue() * 1000f);
    }

    private int findWaterBucketInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == Items.WATER_BUCKET) {
                return i;
            }
        }
        return -1;
    }

    @Native
    @Override
    public void onDisable() {
        if (mc.player == null || mc.gameSettings == null) {
            super.onDisable();
            return;
        }

        if (shift.get() && wasSneaking && mc.gameSettings.keyBindSneak.isKeyDown()) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKey(), false);
        }
        wasSneaking = false;
        lastPlacementTime = 0;
        mc.player.fallDistance = 0;

        timer.cancel();
        timer = new Timer();
        canUse = true;

        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKey(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKey(), false);

        if (mode.is("Пакетная Вода") && originalSlot != -1 && mc.player.inventory.currentItem != originalSlot) {
            mc.player.inventory.currentItem = originalSlot;
            mc.player.connection.sendPacket(new CHeldItemChangePacket(originalSlot));
        }
        originalSlot = -1;

        super.onDisable();
    }
}