package sky.core.modules.impl.movement;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import sky.core.SkyCore;
import sky.core.utils.component.impl.RotationComponent;
import sky.core.events.EventUpdate;
import sky.core.handlers.impl.Rotation;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.utils.math.AuraUtil;
import sky.core.utils.math.TimeUtil;
import sky.core.utils.player.InventoryUtil;

import java.util.Comparator;
import java.util.stream.StreamSupport;

import static java.lang.Math.hypot;
import static net.minecraft.util.math.MathHelper.wrapDegrees;

public class AutoTrap extends Module {
    private final SliderSetting range = new SliderSetting("Range", 3.5f, 2.0f, 6.0f, 0.1F);
    private final SliderSetting tickDelay = new SliderSetting("Delay (ticks)", 4.0f, 0.0f, 20.0f, 1.0F);
    private final BooleanSetting ignoreOpenInventory = new BooleanSetting("Ignore open inventory", true);

    private final TimeUtil timer = new TimeUtil();
    private int lastSwitchedSlot = -1;

    public AutoTrap() {
        super("AutoTrap", Category.Combat);
        addSettings(range, tickDelay, ignoreOpenInventory);
    }

    @java.lang.Override
    public void onDisable() {
        if (lastSwitchedSlot != -1 && mc.player != null) {
            mc.player.inventory.currentItem = lastSwitchedSlot;
            mc.playerController.syncCurrentPlayItem();
            lastSwitchedSlot = -1;
        }
        super.onDisable();
    }

    @EventTarget
    private void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;
        if (!ignoreOpenInventory.get() && mc.currentScreen != null && mc.currentScreen != SkyCore.getInstance().getDropDown()) return;

        if (!timer.hasTimeElapsed((long) (tickDelay.get().floatValue() * 50f))) return;

        LivingEntity target = findTarget();
        if (target == null) return;

        BlockPos placePos = simpleTrapPos(target);
        if (placePos == null) return;
        if (!mc.world.isBlockPresent(placePos) || !mc.world.getBlockState(placePos).isAir()) return;

        int webSlot = InventoryUtil.getSlot(Items.COBWEB);
        if (webSlot == -1) return;

        int old = mc.player.inventory.currentItem;
        boolean needSyncBack = false;
        if (webSlot < 9) {
            if (old != webSlot) {
                mc.player.inventory.currentItem = webSlot;
                mc.playerController.syncCurrentPlayItem();
                needSyncBack = true;
            }
        } else {
            return;
        }

        BlockPos support = findPlaceSupport(placePos);
        if (support == null) {
            if (needSyncBack) {
                mc.player.inventory.currentItem = old;
                mc.playerController.syncCurrentPlayItem();
            }
            return;
        }

        BlockRayTraceResult hit = buildHitResultTowards(placePos, support);
        if (hit == null) {
            if (needSyncBack) {
                mc.player.inventory.currentItem = old;
                mc.playerController.syncCurrentPlayItem();
            }
            return;
        }

        Vector3d eyeVec = mc.player.getEyePosition(1.0F);
        Vector3d hitVec = hit.getHitVec();
        Vector3d offset = hitVec.subtract(eyeVec);
        float targetYaw = (float) wrapDegrees(Math.toDegrees(Math.atan2(offset.z, offset.x)) - 90);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(offset.y, hypot(offset.x, offset.z))));
        RotationComponent.update(new Rotation(targetYaw, targetPitch), 180, 1, 6);

        ActionResultType result = mc.playerController.func_217292_a(mc.player, mc.world, Hand.MAIN_HAND, hit);
        mc.player.swingArm(Hand.MAIN_HAND);

        if (needSyncBack) {
            mc.player.inventory.currentItem = old;
            mc.playerController.syncCurrentPlayItem();
        }
        if (result != null && result.isSuccessOrConsume()) {
            timer.reset();
        }
    }

    private LivingEntity findTarget() {
        return StreamSupport.stream(mc.world.getAllEntities().spliterator(), false)
                .filter(e -> e instanceof LivingEntity)
                .map(e -> (LivingEntity) e)
                .filter(this::isValidTarget)
                .sorted(Comparator.comparingDouble(entity -> entity.getDistance(mc.player)))
                .findFirst()
                .orElse(null);
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity == null || !entity.isAlive() || entity == mc.player) return false;
        if (AuraUtil.getVector(entity).length() > range.get()) return false;
        if (entity instanceof PlayerEntity player) {
            if (SkyCore.getInstance().getFriendManager().isFriend(player.getGameProfile().getName())) return false;
        }
        return true;
    }

    private BlockPos simpleTrapPos(LivingEntity entity) {
        Vector3d pos = entity.getPositionVec();
        BlockPos feet = new BlockPos(pos.x, Math.floor(pos.y), pos.z);
        if (mc.world.getBlockState(feet).isAir()) return feet;
        BlockPos eye = new BlockPos(pos.x, Math.floor(pos.y + entity.getEyeHeight()), pos.z);
        if (mc.world.getBlockState(eye).isAir()) return eye;
        return null;
    }

    private BlockPos findPlaceSupport(BlockPos target) {
        if (!mc.world.getBlockState(target).isAir()) return null;
        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP) continue;
            BlockPos against = target.offset(dir);
            if (!mc.world.getBlockState(against).isAir() && mc.world.getBlockState(against).getBlock() != Blocks.COBWEB) {
                return against;
            }
        }
        return null;
    }

    private BlockRayTraceResult buildHitResultTowards(BlockPos target, BlockPos against) {
        Direction face = Direction.getFacingFromVector(
                target.getX() - against.getX(),
                target.getY() - against.getY(),
                target.getZ() - against.getZ()
        );
        Vector3d faceCenter = new Vector3d(
                against.getX() + 0.5D + face.getXOffset() * 0.5D,
                against.getY() + 0.5D + face.getYOffset() * 0.5D,
                against.getZ() + 0.5D + face.getZOffset() * 0.5D
        );
        return new BlockRayTraceResult(faceCenter, face, against, false);
    }
}
