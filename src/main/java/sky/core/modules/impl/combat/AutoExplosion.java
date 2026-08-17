package sky.core.modules.impl.combat;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import mods.viaversion.viamcp.fixes.AttackOrder;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.EnderCrystalEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.network.play.server.SSpawnObjectPacket;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import com.adl.nativeprotect.Native;
import sky.core.events.*;
import sky.core.handlers.impl.Rotation;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.utils.component.impl.RotationComponent;
import sky.core.utils.math.AuraUtil;
import sky.core.utils.player.InventoryUtil;

import static java.lang.Math.hypot;
import static net.minecraft.util.math.MathHelper.wrapDegrees;

public class AutoExplosion extends Module {
    private BlockPos targetPos;
    private int targetSlot;
    private int oldSlot;
    private boolean needSync;
    private AxisAlignedBB crystalArea;
    private boolean blocked;
    private int explodeTicks;
    private int pendingCrystalId = -1;
    private Vector3d pendingCrystalPos;
    private int pendingCrystalTicks;

    private BlockPos pkmPlacePos;
    private int pkmCrystalSlot = -1;
    private boolean pkmBlocked;

    private final BooleanSetting test = new BooleanSetting("Взрыв по пкм", true);
    private final BooleanSetting fast = new BooleanSetting("Быстрый", true);
    private final BooleanSetting noExplodeResources = new BooleanSetting("Не взрывать ресурсы", false);

    public AutoExplosion() {
        super("Auto Explosion", "Автоматически взрывает кристаллы в радиусе 3 блоков", Category.Combat);
        addSettings(test, fast, noExplodeResources);
    }

    @Override
    public void onDisable() {
        clearExplosionState();
        targetPos = null;
        pkmPlacePos = null;
        pkmCrystalSlot = -1;
        pkmBlocked = false;
        super.onDisable();
    }

    private boolean proverkacrystala() {
        return CrystalOptimizer.Rabotaet;
    }

    @EventTarget
    public void onEvent(EventPlaceBlock e) {
        if (proverkacrystala()) return;

        if (e.getBlock() == Blocks.OBSIDIAN) {
            if (mc.player.getCooldownTracker().hasCooldown(Items.END_CRYSTAL)) {
                return;
            }
            int slotInHotBar = InventoryUtil.getSlot(Items.END_CRYSTAL);
            if (slotInHotBar == -1) {
                return;
            }
            if (slotInHotBar < 9) {
                targetPos = e.getPos();
                targetSlot = slotInHotBar;
                scheduleCrystalExplosion(e.getPos());
                blocked = true;
            }
        }
    }

    @Native
    @EventTarget
    private void onEvent(EventUpdate e) {
        if (!proverkacrystala()) {
            handlePkmPlace();
            handleObsidianPlace();
        }

        if (explodeTicks > 0) explodeTicks--;
        tryAttackPendingCrystal();
        if (crystalArea != null) {
            attackCrystalInArea();
        }
    }

    private void handleObsidianPlace() {
        if (needSync) {
            needSync = false;
            mc.player.inventory.currentItem = oldSlot;
            mc.playerController.syncCurrentPlayItem();
            return;
        }

        if (targetPos == null) {
            return;
        }

        if (mc.world.getBlockState(targetPos).isAir()) {
            targetPos = null;
            return;
        }

        if (blocked) {
            blocked = false;
            return;
        }

        placeCrystalOnObsidian(targetPos, targetSlot, Hand.MAIN_HAND);
        needSync = true;
        targetPos = null;
    }

    private void handlePkmPlace() {
        if (!test.get() || pkmPlacePos == null) {
            return;
        }

        if (mc.world.getBlockState(pkmPlacePos).getBlock() != Blocks.OBSIDIAN
                || !mc.world.getBlockState(pkmPlacePos.up()).isAir()) {
            pkmPlacePos = null;
            pkmCrystalSlot = -1;
            pkmBlocked = false;
            return;
        }

        if (pkmBlocked) {
            pkmBlocked = false;
            return;
        }

        if (pkmCrystalSlot == -1) {
            pkmPlacePos = null;
            return;
        }

        rotateToObsidian(pkmPlacePos);

        Vector3d hitVec = new Vector3d(
                pkmPlacePos.getX() + 0.5,
                pkmPlacePos.getY() + 0.5,
                pkmPlacePos.getZ() + 0.5
        );
        BlockRayTraceResult rayTrace = new BlockRayTraceResult(hitVec, Direction.UP, pkmPlacePos, false);

        mc.playerController.windowClick(0, pkmCrystalSlot < 9 ? pkmCrystalSlot + 36 : pkmCrystalSlot, 40, ClickType.SWAP, mc.player);
        mc.playerController.func_217292_a(mc.player, mc.world, Hand.OFF_HAND, rayTrace);
        mc.playerController.windowClick(0, pkmCrystalSlot < 9 ? pkmCrystalSlot + 36 : pkmCrystalSlot, 40, ClickType.SWAP, mc.player);

        scheduleCrystalExplosion(pkmPlacePos);

        pkmPlacePos = null;
        pkmCrystalSlot = -1;
    }

    private void placeCrystalOnObsidian(BlockPos obsidianPos, int crystalSlot, Hand hand) {
        Vector3d eyeVec = mc.player.getEyePosition(1.0F);
        Vector3d hitVec = AuraUtil.getClosestVec(eyeVec, new AxisAlignedBB(obsidianPos));
        rotateToObsidian(obsidianPos);

        oldSlot = mc.player.inventory.currentItem;
        mc.player.inventory.currentItem = crystalSlot;
        mc.playerController.syncCurrentPlayItem();

        Vector3d offset = hitVec.subtract(eyeVec).inverse();
        mc.playerController.func_217292_a(
                mc.player,
                mc.world,
                hand,
                new BlockRayTraceResult(hitVec, Direction.getFacingFromVector(offset.x, offset.y, offset.z), obsidianPos, false)
        );
        mc.player.swingArm(hand);

        scheduleCrystalExplosion(obsidianPos);
    }

    private void rotateToObsidian(BlockPos obsidianPos) {
        Vector3d eyeVec = mc.player.getEyePosition(1.0F);
        Vector3d hitVec = AuraUtil.getClosestVec(eyeVec, new AxisAlignedBB(obsidianPos));
        applyRotationToPoint(hitVec);
    }

    private void applyRotationToPoint(Vector3d target) {
        if (mc.player == null) return;
        Vector3d eyeVec = mc.player.getEyePosition(1.0F);
        Vector3d offset = target.subtract(eyeVec);
        float targetYaw = (float) wrapDegrees(Math.toDegrees(Math.atan2(offset.z, offset.x)) - 90);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(offset.y, hypot(offset.x, offset.z))));
        RotationComponent.update(new Rotation(targetYaw, targetPitch), 180, 1, 6);
    }

    private void rotateToCrystal(Entity entity) {
        if (entity == null || mc.player == null) return;
        if (!fast.get() && !entity.getBoundingBox().contains(mc.player.getEyePosition(1.0F))) {
            Vector3d direction = AuraUtil.getVector(entity);
            float targetYaw = (float) wrapDegrees(Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90);
            float targetPitch = (float) (-Math.toDegrees(Math.atan2(direction.y, hypot(direction.x, direction.z))));
            RotationComponent.update(new Rotation(targetYaw, targetPitch), 180, 1, 6);
        }
    }

    private void scheduleCrystalExplosion(BlockPos obsidianPos) {
        BlockPos crystalPos = obsidianPos.up();
        crystalArea = new AxisAlignedBB(crystalPos).grow(fast.get() ? 0.5 : 0.1);
        explodeTicks = fast.get() ? 7 : 5;
        pendingCrystalId = -1;
        pendingCrystalPos = new Vector3d(crystalPos.getX() + 0.5, crystalPos.getY() + 0.5, crystalPos.getZ() + 0.5);
        pendingCrystalTicks = fast.get() ? 8 : 6;
        attackCrystalInArea();
    }

    private void clearExplosionState() {
        crystalArea = null;
        pendingCrystalId = -1;
        pendingCrystalPos = null;
        pendingCrystalTicks = 0;
    }

    @Native
    private void attackCrystalInArea() {
        if (crystalArea == null || mc.player == null || mc.world == null) return;
        for (EnderCrystalEntity entity : mc.world.getEntitiesWithinAABB(EnderCrystalEntity.class, crystalArea.grow(0.25))) {
            if (shouldNotExplodeBecauseOfResources(entity)) {
                clearExplosionState();
                return;
            }

            rotateToCrystal(entity);
            attackCrystal(entity);
            clearExplosionState();
            return;
        }
    }

    @Native
    private void attackCrystal(Entity entity) {
        if (entity == null || mc.player == null) return;
        AttackOrder.sendFixedAttack(mc.player, entity);
    }

    private void tryAttackPendingCrystal() {
        if (pendingCrystalTicks <= 0 || mc.world == null) return;
        pendingCrystalTicks--;

        Entity byId = pendingCrystalId == -1 ? null : mc.world.getEntityByID(pendingCrystalId);
        if (byId instanceof EnderCrystalEntity crystalById && !shouldNotExplodeBecauseOfResources(crystalById)) {
            rotateToCrystal(crystalById);
            attackCrystal(crystalById);
            clearExplosionState();
            return;
        }

        if (pendingCrystalPos != null) {
            AxisAlignedBB spawnAabb = new AxisAlignedBB(
                    pendingCrystalPos.x - 0.8, pendingCrystalPos.y - 0.8, pendingCrystalPos.z - 0.8,
                    pendingCrystalPos.x + 0.8, pendingCrystalPos.y + 1.8, pendingCrystalPos.z + 0.8
            );
            for (EnderCrystalEntity crystal : mc.world.getEntitiesWithinAABB(EnderCrystalEntity.class, spawnAabb)) {
                if (shouldNotExplodeBecauseOfResources(crystal)) continue;
                rotateToCrystal(crystal);
                attackCrystal(crystal);
                clearExplosionState();
                return;
            }
        }
    }

    @EventTarget
    private void onPacket(EventPacket event) {
        if (!event.isReceive()) return;
        if (!(event.getPacket() instanceof SSpawnObjectPacket packet)) return;
        if (packet.getType() != EntityType.END_CRYSTAL) return;
        if (crystalArea == null) return;

        double px = packet.getX();
        double py = packet.getY();
        double pz = packet.getZ();
        if (!crystalArea.grow(0.5).contains(new Vector3d(px, py, pz))) return;

        if (fast.get() && mc.world != null) {
            pendingCrystalId = packet.getEntityID();
            pendingCrystalPos = new Vector3d(px, py, pz);
            pendingCrystalTicks = 8;
            tryAttackPendingCrystal();
            Entity spawned = mc.world.getEntityByID(packet.getEntityID());
            if (spawned instanceof EnderCrystalEntity crystal && !shouldNotExplodeBecauseOfResources(crystal)) {
                rotateToCrystal(crystal);
                attackCrystal(crystal);
                clearExplosionState();
                return;
            }
        }

        explodeTicks = fast.get() ? 7 : 3;
        attackCrystalInArea();
    }

    @EventTarget
    private void onClick(final EventClick e) {
        if (proverkacrystala()) return;
        if (!test.get()) return;

        int slotCrystal = InventoryUtil.getItemSlot(Items.END_CRYSTAL);
        if (slotCrystal == -1) return;

        if ((mc.player.getHeldItemMainhand().getItem() instanceof BlockItem
                && mc.player.getHeldItemMainhand().getItem() != Items.PLAYER_HEAD)
                || mc.player.getHeldItemOffhand().getItem() instanceof BlockItem
                && mc.player.getHeldItemOffhand().getItem() != Items.PLAYER_HEAD) {
            return;
        }

        if (!(mc.objectMouseOver instanceof BlockRayTraceResult)) return;

        BlockPos obsidianPos = ((BlockRayTraceResult) mc.objectMouseOver).getPos();
        if (mc.world.getBlockState(obsidianPos).getBlock() != Blocks.OBSIDIAN
                || mc.world.getBlockState(obsidianPos.up()).getBlock() != Blocks.AIR) {
            return;
        }

        pkmPlacePos = obsidianPos;
        pkmCrystalSlot = slotCrystal;
        pkmBlocked = true;
        scheduleCrystalExplosion(obsidianPos);
        e.setCancelled(true);
    }

    private boolean shouldNotExplodeBecauseOfResources(Entity crystalEntity) {
        if (!noExplodeResources.get()) return false;
        if (mc.world == null) return false;

        return mc.world.getEntitiesWithinAABB(ItemEntity.class, crystalEntity.getBoundingBox().grow(6.0))
                .stream()
                .map(itemEntity -> itemEntity.getItem().getItem())
                .anyMatch(item -> item == Items.TOTEM_OF_UNDYING ||
                        item == Items.END_CRYSTAL ||
                        item == Items.ENCHANTED_GOLDEN_APPLE ||
                        item == Items.NETHERITE_HELMET || item == Items.NETHERITE_CHESTPLATE ||
                        item == Items.NETHERITE_LEGGINGS || item == Items.NETHERITE_BOOTS ||
                        item == Items.NETHERITE_SWORD || item == Items.DIAMOND_SWORD ||
                        item == Items.ELYTRA || item == Items.TRIDENT);
    }

    @EventTarget
    private void onEvent(EventPostUpdate e) {
    }
}
