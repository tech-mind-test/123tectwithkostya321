package sky.core.modules.impl.combat;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EnderCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CUseEntityPacket;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import org.lwjgl.glfw.GLFW;
import sky.core.events.EventAttack;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BindSetting;
import sky.core.modules.api.constructors.impl.BooleanSetting;

import java.util.List;

public class CrystalOptimizer extends Module {

    public static boolean Rabotaet = false;

    private final BindSetting bind = new BindSetting("Bind");
    private final BooleanSetting test = new BooleanSetting("Убирать Задержку", false);

    private boolean wasPressed;
    private int previousSlot = -1;

    public CrystalOptimizer() {
        super("CrystalSpam", "Спамит Криссами", Category.Combat);
        addSettings(bind, test);
    }

    @java.lang.Override
    public void onDisable() {
        super.onDisable();
        Rabotaet = false;
    }

    @EventTarget
    private void update(EventUpdate event) {
        if (mc.player == null || mc.world == null) {
            Rabotaet = false;
            return;
        }

        int key = bind.get();
        if (key == -1) {
            Rabotaet = false;
            return;
        }

        boolean pressed;
        if (key < 8) {
            pressed = GLFW.glfwGetMouseButton(mc.getMainWindow().getHandle(), key) == GLFW.GLFW_PRESS;
        } else {
            pressed = InputMappings.isKeyDown(mc.getMainWindow().getHandle(), key);
        }

        Rabotaet = pressed;

        if (!pressed) {
            if (wasPressed) {
                restoreSlot();
                wasPressed = false;
            }
            return;
        }

        if (!wasPressed) {
            previousSlot = mc.player.inventory.currentItem;
            wasPressed = true;
        }

        if (mc.currentScreen != null) return;

        handleCrystalLogic();
    }

    @EventTarget
    private void onAttack(EventAttack e) {
        if (test.get()) {
            if (e.getTarget() instanceof EnderCrystalEntity entity) {
                entity.remove();
            }
        }
    }

    private void handleCrystalLogic() {
        if (mc.objectMouseOver instanceof EntityRayTraceResult ray) {
            if (ray.getEntity() instanceof EnderCrystalEntity) {
                attackCrystal(ray.getEntity());
                return;
            }
        }

        if (mc.objectMouseOver instanceof BlockRayTraceResult ray) {
            BlockPos pos = ray.getPos();
            if (mc.world.getBlockState(pos).getBlock() == Blocks.OBSIDIAN) {
                int crystalSlot = getHotbarSlotWithCrystal();
                if (crystalSlot != -1) {
                    switchToSlot(crystalSlot);
                    placeCrystalOn(obsidianTopPos(pos));
                    explodeCrystalsNear(obsidianTopPos(pos));
                }
            }
        }
    }

    private void attackCrystal(Entity entity) {
        if (mc.player.connection != null) {
            mc.player.connection.sendPacket(new CUseEntityPacket(entity, mc.player.isSneaking()));
        } else {
            mc.playerController.attackEntity(mc.player, entity);
        }
        mc.player.swingArm(Hand.MAIN_HAND);
    }

    private BlockPos obsidianTopPos(BlockPos obsidianPos) {
        return obsidianPos.up();
    }

    private void placeCrystalOn(BlockPos placePos) {
        BlockPos basePos = placePos.down();

        if (!mc.world.isAirBlock(placePos)) return;

        AxisAlignedBB aabb = new AxisAlignedBB(
                placePos.getX(),
                placePos.getY(),
                placePos.getZ(),
                placePos.getX() + 1.0,
                placePos.getY() + 2.0,
                placePos.getZ() + 1.0
        );

        if (!mc.world.getEntitiesWithinAABBExcludingEntity(null, aabb).isEmpty()) return;

        Vector3d hitVec = new Vector3d(basePos.getX() + 0.5D, basePos.getY() + 0.5D, basePos.getZ() + 0.5D);
        BlockRayTraceResult rayTraceResult = new BlockRayTraceResult(hitVec, Direction.UP, basePos, false);

        ActionResultType result = mc.playerController.func_217292_a(mc.player, mc.world, Hand.MAIN_HAND, rayTraceResult);
        if (result == ActionResultType.SUCCESS || result == ActionResultType.CONSUME) {
            mc.player.swingArm(Hand.MAIN_HAND);
        }
    }

    private void explodeCrystalsNear(BlockPos placePos) {
        double range = 4.5D;
        AxisAlignedBB aabb = new AxisAlignedBB(
                placePos.getX() - range,
                placePos.getY() - 2.0D,
                placePos.getZ() - range,
                placePos.getX() + 1.0D + range,
                placePos.getY() + 2.0D + range,
                placePos.getZ() + 1.0D + range
        );

        List<Entity> crystals = mc.world.getEntitiesWithinAABBExcludingEntity(null, aabb)
                .stream()
                .filter(entity -> entity instanceof EnderCrystalEntity)
                .filter(entity -> mc.player.getDistance(entity) <= (float) range)
                .toList();

        for (Entity entity : crystals) {
            if (mc.player.connection != null) {
                mc.player.connection.sendPacket(new CUseEntityPacket(entity, mc.player.isSneaking()));
            } else {
                mc.playerController.attackEntity(mc.player, entity);
            }
            mc.player.swingArm(Hand.MAIN_HAND);
        }
    }

    private int getHotbarSlotWithCrystal() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.inventory.getStackInSlot(i).getItem() == Items.END_CRYSTAL) {
                return i;
            }
        }
        return -1;
    }

    private void switchToSlot(int hotbarSlot) {
        if (hotbarSlot < 0 || hotbarSlot > 8) return;
        if (mc.player.inventory.currentItem == hotbarSlot) return;

        mc.player.inventory.currentItem = hotbarSlot;
        if (mc.player.connection != null) {
            mc.player.connection.sendPacket(new CHeldItemChangePacket(hotbarSlot));
        }
    }

    private void restoreSlot() {
        if (previousSlot < 0 || previousSlot > 8) {
            previousSlot = -1;
            return;
        }

        switchToSlot(previousSlot);
        previousSlot = -1;
    }
}