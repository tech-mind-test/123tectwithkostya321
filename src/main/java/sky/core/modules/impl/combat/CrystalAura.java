package sky.core.modules.impl.combat;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import mods.viaversion.viamcp.fixes.AttackOrder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.item.EnderCrystalEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.network.play.server.SSpawnObjectPacket;
import net.minecraft.util.CombatRules;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Explosion;
import org.lwjgl.opengl.GL11;
import sky.core.SkyCore;
import sky.core.events.EventPacket;
import sky.core.events.EventRender3D;
import sky.core.events.EventUpdate;
import sky.core.handlers.impl.Rotation;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.utils.component.impl.RotationComponent;
import sky.core.utils.player.InventoryUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.Math.hypot;
import static net.minecraft.util.math.MathHelper.wrapDegrees;

public class CrystalAura extends Module {
    private static final double CRYSTAL_POWER = 12.0D;
    private static final int PENDING_TICKS = 8;

    private final BooleanSetting autoSwitch = new BooleanSetting("Авто свап", true);
    private final BooleanSetting placeCrystals = new BooleanSetting("Ставить кристалл", true);
    private final BooleanSetting breakCrystals = new BooleanSetting("Взрывать кристалл", true);
    private final BooleanSetting placeObsidian = new BooleanSetting("Ставить обсидиан", true);
    private final BooleanSetting mineBlocks = new BooleanSetting("Подкоп", true);
    private final BooleanSetting elytraSetup = new BooleanSetting("Сетап элитр", true);
    private final BooleanSetting throughWalls = new BooleanSetting("Через стены", true);
    private final BooleanSetting antiSelf = new BooleanSetting("Не взрывать себя", true);
    private final BooleanSetting antiFriend = new BooleanSetting("Не дамажить друзей", true);
    private final BooleanSetting fastSpawn = new BooleanSetting("Быстрый взрыв", true);
    private final BooleanSetting render = new BooleanSetting("Подсветка", true);

    private final SliderSetting placeRange = new SliderSetting("Дистанция установки", 4.8F, 1.0F, 6.0F, 0.1F);
    private final SliderSetting breakRange = new SliderSetting("Дистанция взрыва", 4.8F, 1.0F, 6.0F, 0.1F);
    private final SliderSetting wallRange = new SliderSetting("Дистанция стен", 3.0F, 1.0F, 6.0F, 0.1F);
    private final SliderSetting targetRange = new SliderSetting("Дистанция цели", 12.0F, 3.0F, 18.0F, 0.5F);
    private final SliderSetting minDamage = new SliderSetting("Мин. урон", 4.0F, 0.0F, 20.0F, 0.5F);
    private final SliderSetting facePlaceHealth = new SliderSetting("Добивание HP", 8.0F, 1.0F, 20.0F, 0.5F);
    private final SliderSetting maxSelfDamage = new SliderSetting("Макс. себе", 7.0F, 0.0F, 20.0F, 0.5F);
    private final SliderSetting predictTicks = new SliderSetting("Предикт", 2.0F, 0.0F, 6.0F, 0.5F);
    private final SliderSetting placeDelay = new SliderSetting("Задержка установки", 0.0F, 0.0F, 10.0F, 1.0F);
    private final SliderSetting breakDelay = new SliderSetting("Задержка взрыва", 0.0F, 0.0F, 10.0F, 1.0F);
    private final SliderSetting mineDelay = new SliderSetting("Задержка подкопа", 2.0F, 0.0F, 12.0F, 1.0F);

    private CrystalPlaceResult currentPlaceResult = CrystalPlaceResult.none();
    private BlockPos pendingCrystalBase;
    private Vector3d pendingCrystalPos;
    private int pendingCrystalId = -1;
    private int pendingTicks;
    private int placeCooldown;
    private int breakCooldown;
    private int mineCooldown;
    private List<PlayerEntity> nearbyFriends = Collections.emptyList();

    public CrystalAura() {
        super("CrystalAura", "Автоматически ставит, копает и взрывает кристаллы", Category.Combat);
        addSettings(autoSwitch, placeCrystals, breakCrystals, placeObsidian, mineBlocks,
                elytraSetup, throughWalls, antiSelf, antiFriend, fastSpawn, render,
                placeRange, breakRange, wallRange, targetRange, minDamage, facePlaceHealth,
                maxSelfDamage, predictTicks, placeDelay, breakDelay, mineDelay);
    }

    @Override
    public void onDisable() {
        resetState();
        super.onDisable();
    }

    @EventTarget
    private void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) {
            return;
        }

        tickCooldowns();

        PlayerEntity target = findTarget();
        if (target == null) {
            resetState();
            nearbyFriends = Collections.emptyList();
            return;
        }

        if (tryAttackPendingCrystal(target)) {
            return;
        }
        if (pendingTicks > 0 && pendingCrystalBase != null) {
            return;
        }

        List<Entity> entities = collectRelevantEntities();
        nearbyFriends = collectNearbyFriends(entities);
        CrystalPlaceResult strict = findStrictPlacePosition(target, mc.player, entities);
        CrystalPlaceResult direct = placeCrystals.get()
                ? findPlacePosition(target, mc.player, entities)
                : CrystalPlaceResult.none();
        CrystalPlaceResult crystal = findCrystalTarget(target, mc.player, entities);
        CrystalPlaceResult selected = selectResult(strict, direct, crystal);

        currentPlaceResult = selected;
        if (selected.getActionType() == CrystalActionType.NONE) {
            return;
        }

        rotateTo(selected.getAimVec());
        execute(selected, target);
    }

    @EventTarget
    private void onPacket(EventPacket event) {
        if (!fastSpawn.get() || !event.isReceive() || pendingTicks <= 0) {
            return;
        }
        if (!(event.getPacket() instanceof SSpawnObjectPacket)) {
            return;
        }

        SSpawnObjectPacket packet = (SSpawnObjectPacket) event.getPacket();
        if (packet.getType() != EntityType.END_CRYSTAL || pendingCrystalBase == null) {
            return;
        }

        Vector3d spawn = new Vector3d(packet.getX(), packet.getY(), packet.getZ());
        if (pendingCrystalPos != null && spawn.squareDistanceTo(pendingCrystalPos) > 4.0D) {
            return;
        }

        pendingCrystalId = packet.getEntityID();
        Entity entity = mc.world == null ? null : mc.world.getEntityByID(pendingCrystalId);
        if (entity instanceof EnderCrystalEntity) {
            PlayerEntity target = findTarget();
            if (target != null && canAttackPending((EnderCrystalEntity) entity, target)) {
                attackCrystal((EnderCrystalEntity) entity);
            }
            clearPendingCrystal();
        }
    }

    @EventTarget
    private void onRender(EventRender3D event) {
        if (mc.player == null || mc.world == null || !render.get() || currentPlaceResult == null || currentPlaceResult.getActionType() == CrystalActionType.NONE) {
            return;
        }

        Vector3d camera = mc.getRenderManager().info.getProjectedView();
        RenderSystem.pushMatrix();
        RenderSystem.translated(-camera.x, -camera.y, -camera.z);
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        AxisAlignedBB box = getRenderBox(currentPlaceResult, event.getPartialTicks());
        if (box != null) {
            int color = getActionColor(currentPlaceResult.getActionType());
            drawBox(box, color, 0.24F);
            drawOutline(box, color, 1.4F);
        }

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.depthMask(true);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.popMatrix();
    }

    private CrystalPlaceResult selectResult(CrystalPlaceResult strict, CrystalPlaceResult direct, CrystalPlaceResult crystal) {
        CrystalPlaceResult selected = strict;
        if (scorePlaceResult(direct) > scorePlaceResult(selected)) {
            selected = direct;
        }
        if (crystal.getActionType() == CrystalActionType.BREAK || autoSwitch.get()) {
            if (scorePlaceResult(crystal) > scorePlaceResult(selected)) {
                selected = crystal;
            }
        }
        if (strict.getActionType() == CrystalActionType.NONE && strict.isHardStop()) {
            selected = strict;
        }
        return selected;
    }

    private CrystalPlaceResult findPlacePosition(PlayerEntity target, PlayerEntity self, Iterable<Entity> entities) {
        CrystalPlaceResult best = CrystalPlaceResult.none();
        Vector3d predicted = predict(target);
        BlockPos center = new BlockPos(predicted);
        int horizontal = target.isElytraFlying() && elytraSetup.get() ? 3 : 4;
        int minY = target.isElytraFlying() && elytraSetup.get() ? -8 : -3;
        int maxY = target.isElytraFlying() && elytraSetup.get() ? 1 : 3;
        double maxRange = placeRange.get() + 0.75D;
        double maxRangeSq = maxRange * maxRange;

        for (int x = -horizontal; x <= horizontal; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = -horizontal; z <= horizontal; z++) {
                    BlockPos base = center.add(x, y, z);
                    if (mc.player.getDistanceSq(base.getX() + 0.5D, base.getY() + 0.5D, base.getZ() + 0.5D) > maxRangeSq) {
                        continue;
                    }
                    CrystalPlaceResult result = evaluateCrystalBase(base, target, self, entities, CrystalActionType.PLACE_AND_BREAK);
                    if (scorePlaceResult(result) > scorePlaceResult(best)) {
                        best = result;
                    }
                }
            }
        }

        return best;
    }

    private CrystalPlaceResult findStrictPlacePosition(PlayerEntity target, PlayerEntity self, Iterable<Entity> entities) {
        if (!placeObsidian.get() && !mineBlocks.get()) {
            return CrystalPlaceResult.none();
        }

        CrystalPlaceResult best = CrystalPlaceResult.none();
        Vector3d predicted = predict(target);
        BlockPos center = new BlockPos(predicted);
        int horizontal = target.isElytraFlying() && elytraSetup.get() ? 3 : 2;
        int minY = target.isElytraFlying() && elytraSetup.get() ? -9 : -3;
        int maxY = target.isElytraFlying() && elytraSetup.get() ? 0 : 2;
        double maxRange = placeRange.get() + 1.25D;
        double maxRangeSq = maxRange * maxRange;

        for (int x = -horizontal; x <= horizontal; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = -horizontal; z <= horizontal; z++) {
                    BlockPos obsidianPos = center.add(x, y, z);
                    if (mc.player.getDistanceSq(obsidianPos.getX() + 0.5D, obsidianPos.getY() + 0.5D, obsidianPos.getZ() + 0.5D) > maxRangeSq) {
                        continue;
                    }
                    CrystalPlaceResult result = evaluateSetupPosition(obsidianPos, target, self, entities);
                    if (result.isHardStop()) {
                        return result;
                    }
                    if (scorePlaceResult(result) > scorePlaceResult(best)) {
                        best = result;
                    }
                }
            }
        }

        return best;
    }

    private CrystalPlaceResult findCrystalTarget(PlayerEntity target, PlayerEntity self, Iterable<Entity> entities) {
        CrystalPlaceResult best = CrystalPlaceResult.none();
        if (!breakCrystals.get()) {
            return best;
        }

        for (Entity entity : entities) {
            if (!(entity instanceof EnderCrystalEntity) || !entity.isAlive()) {
                continue;
            }
            if (self.getDistance(entity) > breakRange.get()) {
                continue;
            }

            Vector3d crystalPos = entity.getPositionVec();
            Vector3d aim = crystalPos.add(0.0D, 0.5D, 0.0D);
            if (!canReach(aim, breakRange.get(), wallRange.get())) {
                continue;
            }

            double targetDamage = calculateDamage(crystalPos, target);
            double selfDamage = calculateDamage(crystalPos, self);
            if (!isDamageAllowed(crystalPos, target, targetDamage, selfDamage)) {
                continue;
            }
            if (antiFriend.get() && hurtsFriend(crystalPos)) {
                continue;
            }

            double score = targetDamage - selfDamage * 0.55D + 0.35D;
            CrystalPlaceResult result = new CrystalPlaceResult(null, Direction.UP, targetDamage,
                    selfDamage, score, CrystalActionType.BREAK, entity, aim, null, false);
            if (scorePlaceResult(result) > scorePlaceResult(best)) {
                best = result;
            }
        }

        return best;
    }

    private CrystalPlaceResult evaluateCrystalBase(BlockPos base, PlayerEntity target, PlayerEntity self, Iterable<Entity> entities, CrystalActionType action) {
        Vector3d aim = blockCenter(base);
        if (!canReach(aim, placeRange.get(), wallRange.get())) {
            return CrystalPlaceResult.none();
        }

        if (!canPlaceCrystalOn(base, entities)) {
            return CrystalPlaceResult.none();
        }

        Vector3d crystalPos = crystalVec(base);
        if (action == CrystalActionType.PLACE_AND_BREAK && !canReach(crystalPos, breakRange.get(), wallRange.get())) {
            return CrystalPlaceResult.none();
        }

        double targetDamage = calculateDamage(crystalPos, target);
        double selfDamage = calculateDamage(crystalPos, self);
        if (!isDamageAllowed(crystalPos, target, targetDamage, selfDamage)) {
            return CrystalPlaceResult.none();
        }
        if (antiFriend.get() && hurtsFriend(crystalPos)) {
            return CrystalPlaceResult.none();
        }

        double score = scoreDamage(base, target, targetDamage, selfDamage);
        return new CrystalPlaceResult(base, Direction.UP, targetDamage, selfDamage, score,
                action, null, crystalPos, null, false);
    }

    private CrystalPlaceResult evaluateSetupPosition(BlockPos obsidianPos, PlayerEntity target, PlayerEntity self, Iterable<Entity> entities) {
        BlockState state = mc.world.getBlockState(obsidianPos);
        Block block = state.getBlock();

        if (block == Blocks.OBSIDIAN || block == Blocks.BEDROCK) {
            return evaluateCrystalBase(obsidianPos, target, self, entities, CrystalActionType.PLACE_AND_BREAK);
        }

        BlockPlacement placement = null;
        boolean canPlaceObsidian = false;
        if (placeObsidian.get() && isReplaceable(obsidianPos)) {
            placement = findBlockPlacement(obsidianPos);
            canPlaceObsidian = placement != null && canReach(placement.hitVec, placeRange.get(), wallRange.get());
        }

        PlaceScanResult scan = scanCrystalSpace(entities, obsidianPos.up());
        boolean crystalSpaceCanOpen = scan == PlaceScanResult.VALID || scan == PlaceScanResult.OUT_OF_RANGE;
        Vector3d mineAim = blockCenter(obsidianPos);
        boolean canMine = crystalSpaceCanOpen && mineBlocks.get() && canMineAsSetup(obsidianPos) && canReach(mineAim, placeRange.get(), wallRange.get());
        if (!canPlaceObsidian && !canMine) {
            return CrystalPlaceResult.none();
        }

        Vector3d crystalPos = crystalVec(obsidianPos);
        double targetDamage = calculateDamage(crystalPos, target);
        double selfDamage = calculateDamage(crystalPos, self);
        if (!isDamageAllowed(crystalPos, target, targetDamage, selfDamage)) {
            return CrystalPlaceResult.none();
        }
        if (antiFriend.get() && hurtsFriend(crystalPos)) {
            return CrystalPlaceResult.none();
        }

        if (canPlaceObsidian) {
            double score = scoreDamage(obsidianPos, target, targetDamage, selfDamage) - 0.45D;
            return new CrystalPlaceResult(obsidianPos, placement.face, targetDamage, selfDamage, score,
                    CrystalActionType.PLACE, null, placement.hitVec, placement, false);
        }

        if (canMine) {
            double score = scoreDamage(obsidianPos, target, targetDamage, selfDamage) - 0.75D;
            return new CrystalPlaceResult(obsidianPos, Direction.UP, targetDamage, selfDamage, score,
                    CrystalActionType.ATTACK, null, mineAim, null, false);
        }

        return CrystalPlaceResult.none();
    }

    private boolean canMineAsSetup(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        if (state.isAir() || state.getBlock() == Blocks.BEDROCK || state.getBlock() == Blocks.OBSIDIAN) {
            return false;
        }
        if (InventoryUtil.findBestToolForBlock(state) == -1 && !state.getMaterial().isReplaceable()) {
            return false;
        }

        BlockPos crystal = pos.up();
        BlockState first = mc.world.getBlockState(crystal);
        BlockState second = mc.world.getBlockState(crystal.up());
        if (!first.isAir() && !first.getMaterial().isReplaceable()) {
            return false;
        }
        if (!second.isAir() && !second.getMaterial().isReplaceable()) {
            return false;
        }
        return true;
    }

    private PlaceScanResult scanCrystalSpace(Iterable<Entity> entities, BlockPos crystalPos) {
        AxisAlignedBB box = crystalBox(crystalPos);
        for (Entity entity : mc.world.getEntitiesWithinAABBExcludingEntity(null, box)) {
            if (entity == null || !entity.isAlive() || entity.isSpectator()) {
                continue;
            }
            if (entity == mc.player) {
                if (entity.getBoundingBox().intersects(box)) {
                    return PlaceScanResult.OUT_OF_RANGE;
                }
                continue;
            }
            if (!entity.getBoundingBox().intersects(box)) {
                continue;
            }
            if (entity instanceof EnderCrystalEntity) {
                continue;
            }
            if (entity instanceof PlayerEntity) {
                return PlaceScanResult.OUT_OF_RANGE;
            }
            if (entity instanceof ItemEntity) {
                return PlaceScanResult.BLOCKED;
            }
            return PlaceScanResult.BLOCKED;
        }
        return PlaceScanResult.VALID;
    }

    private boolean canPlaceCrystalOn(BlockPos base, Iterable<Entity> entities) {
        Block block = mc.world.getBlockState(base).getBlock();
        if (block != Blocks.OBSIDIAN && block != Blocks.BEDROCK) {
            return false;
        }
        if (!mc.world.isAirBlock(base.up()) || !mc.world.isAirBlock(base.up(2))) {
            return false;
        }
        return scanCrystalSpace(entities, base.up()) == PlaceScanResult.VALID;
    }

    private void execute(CrystalPlaceResult result, PlayerEntity target) {
        switch (result.getActionType()) {
            case BREAK:
                if (breakCooldown <= 0) {
                    executeBreak(result, target);
                }
                break;
            case PLACE_AND_BREAK:
                if (placeCooldown <= 0) {
                    executeCrystalPlace(result, target);
                }
                break;
            case PLACE:
                if (placeCooldown <= 0) {
                    executeObsidianPlace(result);
                }
                break;
            case ATTACK:
                if (mineCooldown <= 0) {
                    executeMine(result);
                }
                break;
            default:
                break;
        }
    }

    private void executeBreak(CrystalPlaceResult result, PlayerEntity target) {
        if (!(result.getEntity() instanceof EnderCrystalEntity) || !result.getEntity().isAlive()) {
            return;
        }
        EnderCrystalEntity crystal = (EnderCrystalEntity) result.getEntity();
        Vector3d crystalPos = crystal.getPositionVec();
        double targetDamage = calculateDamage(crystalPos, target);
        double selfDamage = calculateDamage(crystalPos, mc.player);
        if (!isDamageAllowed(crystalPos, target, targetDamage, selfDamage)) {
            return;
        }

        attackCrystal(crystal);
        breakCooldown = Math.round(breakDelay.get());
        currentPlaceResult = CrystalPlaceResult.none();
    }

    private void executeCrystalPlace(CrystalPlaceResult result, PlayerEntity target) {
        if (result.getBlockPos() == null || !canUseItem(Items.END_CRYSTAL)) {
            return;
        }
        if (!canPlaceCrystalOn(result.getBlockPos(), collectRelevantEntities())) {
            currentPlaceResult = CrystalPlaceResult.none();
            return;
        }

        SlotSwap swap = switchToItem(Items.END_CRYSTAL);
        if (!swap.valid) {
            return;
        }

        BlockPos base = result.getBlockPos();
        Vector3d hitVec = blockCenter(base);
        mc.playerController.func_217292_a(mc.player, mc.world, swap.hand,
                new BlockRayTraceResult(hitVec, Direction.UP, base, false));
        mc.player.swingArm(swap.hand);
        restoreSwap(swap);

        placeCooldown = Math.round(placeDelay.get());
        schedulePendingCrystal(base);
        tryAttackPendingCrystal(target);
    }

    private void executeObsidianPlace(CrystalPlaceResult result) {
        if (result.getBlockPos() == null || result.getPlacement() == null || !canUseItem(Items.OBSIDIAN)) {
            return;
        }
        if (!isReplaceable(result.getBlockPos())) {
            currentPlaceResult = CrystalPlaceResult.none();
            return;
        }

        SlotSwap swap = switchToItem(Items.OBSIDIAN);
        if (!swap.valid) {
            return;
        }

        BlockPlacement placement = result.getPlacement();
        mc.playerController.func_217292_a(mc.player, mc.world, swap.hand,
                new BlockRayTraceResult(placement.hitVec, placement.face, placement.supportPos, false));
        mc.player.swingArm(swap.hand);
        restoreSwap(swap);

        placeCooldown = Math.max(1, Math.round(placeDelay.get()));
        currentPlaceResult = CrystalPlaceResult.none();
    }

    private void executeMine(CrystalPlaceResult result) {
        if (result.getBlockPos() == null) {
            return;
        }

        BlockState state = mc.world.getBlockState(result.getBlockPos());
        if (state.isAir()) {
            currentPlaceResult = CrystalPlaceResult.none();
            return;
        }

        int oldSlot = mc.player.inventory.currentItem;
        int toolSlot = InventoryUtil.findBestToolForBlock(state);
        if (toolSlot != -1 && toolSlot != oldSlot) {
            switchToHotbar(toolSlot);
        }

        Direction face = getMiningFace(result.getBlockPos());
        mc.player.connection.sendPacket(new CPlayerDiggingPacket(CPlayerDiggingPacket.Action.START_DESTROY_BLOCK, result.getBlockPos(), face));
        mc.player.connection.sendPacket(new CPlayerDiggingPacket(CPlayerDiggingPacket.Action.STOP_DESTROY_BLOCK, result.getBlockPos(), face));
        mc.player.swingArm(Hand.MAIN_HAND);

        if (toolSlot != -1 && oldSlot != toolSlot) {
            switchToHotbar(oldSlot);
        }

        mineCooldown = Math.round(mineDelay.get());
        currentPlaceResult = CrystalPlaceResult.none();
    }

    private boolean tryAttackPendingCrystal(PlayerEntity target) {
        if (pendingTicks <= 0 || pendingCrystalBase == null || !breakCrystals.get()) {
            return false;
        }

        pendingTicks--;
        Entity byId = pendingCrystalId == -1 ? null : mc.world.getEntityByID(pendingCrystalId);
        if (byId instanceof EnderCrystalEntity && canAttackPending((EnderCrystalEntity) byId, target)) {
            attackCrystal((EnderCrystalEntity) byId);
            clearPendingCrystal();
            return true;
        }

        AxisAlignedBB box = new AxisAlignedBB(pendingCrystalBase.up()).grow(1.0D, 1.5D, 1.0D);
        for (EnderCrystalEntity crystal : mc.world.getEntitiesWithinAABB(EnderCrystalEntity.class, box)) {
            if (!canAttackPending(crystal, target)) {
                continue;
            }
            attackCrystal(crystal);
            clearPendingCrystal();
            return true;
        }

        if (pendingTicks <= 0) {
            clearPendingCrystal();
        }
        return false;
    }

    private boolean canAttackPending(EnderCrystalEntity crystal, PlayerEntity target) {
        if (crystal == null || !crystal.isAlive() || mc.player.getDistance(crystal) > breakRange.get()) {
            return false;
        }
        if (target == null) {
            return false;
        }
        Vector3d pos = crystal.getPositionVec();
        double targetDamage = calculateDamage(pos, target);
        double selfDamage = calculateDamage(pos, mc.player);
        return isDamageAllowed(pos, target, targetDamage, selfDamage);
    }

    private void attackCrystal(EnderCrystalEntity crystal) {
        rotateTo(crystal.getPositionVec().add(0.0D, 0.5D, 0.0D));
        AttackOrder.sendFixedAttack(mc.player, crystal);
        mc.player.swingArm(Hand.MAIN_HAND);
        breakCooldown = Math.round(breakDelay.get());
    }

    private List<Entity> collectRelevantEntities() {
        double range = Math.max(placeRange.get(), breakRange.get()) + 4.0D;
        return new ArrayList<>(mc.world.getEntitiesWithinAABBExcludingEntity(null, mc.player.getBoundingBox().grow(range)));
    }

    private List<PlayerEntity> collectNearbyFriends(List<Entity> entities) {
        if (!antiFriend.get()) {
            return Collections.emptyList();
        }

        List<PlayerEntity> friends = new ArrayList<>();
        for (Entity entity : entities) {
            if (!(entity instanceof PlayerEntity) || entity == mc.player) {
                continue;
            }
            PlayerEntity player = (PlayerEntity) entity;
            if (player.isAlive() && isFriend(player)) {
                friends.add(player);
            }
        }
        return friends;
    }

    private PlayerEntity findTarget() {
        PlayerEntity best = null;
        double bestScore = Double.MAX_VALUE;

        for (PlayerEntity player : mc.world.getEntitiesWithinAABB(PlayerEntity.class, mc.player.getBoundingBox().grow(targetRange.get()))) {
            if (player == mc.player || !player.isAlive() || player.isSpectator() || isFriend(player) || isBot(player)) {
                continue;
            }

            double distance = mc.player.getDistance(player);
            if (distance > targetRange.get()) {
                continue;
            }

            double score = distance;
            if (canSee(player.getPositionVec().add(0.0D, player.getHeight() * 0.5D, 0.0D))) {
                score -= 2.0D;
            }
            score += (player.getHealth() + player.getAbsorptionAmount()) * 0.02D;

            if (score < bestScore) {
                bestScore = score;
                best = player;
            }
        }

        return best;
    }

    private boolean isBot(PlayerEntity player) {
        AntiBot antiBot = (AntiBot) SkyCore.getInstance().getModuleManager().getModule(AntiBot.class);
        return antiBot != null && antiBot.isEnabled() && AntiBot.bot.contains(player);
    }

    private boolean isFriend(PlayerEntity player) {
        return SkyCore.getInstance().getFriendManager().isFriend(player.getName().getString());
    }

    private boolean hurtsFriend(Vector3d crystalPos) {
        for (PlayerEntity player : nearbyFriends) {
            if (calculateDamage(crystalPos, player) > 3.5D) {
                return true;
            }
        }
        return false;
    }

    private boolean isDamageAllowed(Vector3d crystalPos, PlayerEntity target, double targetDamage, double selfDamage) {
        if (targetDamage < requiredDamage(target)) {
            return false;
        }
        if (!antiSelf.get()) {
            return true;
        }

        double health = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        if (selfDamage >= health - 1.0D) {
            return false;
        }
        if (maxSelfDamage.get() > 0.0F && selfDamage > maxSelfDamage.get()) {
            return false;
        }
        return targetDamage > selfDamage * 0.72D || targetDamage >= target.getHealth() + target.getAbsorptionAmount();
    }

    private double requiredDamage(PlayerEntity target) {
        if (target.getHealth() + target.getAbsorptionAmount() <= facePlaceHealth.get()) {
            return Math.min(2.0D, minDamage.get());
        }
        return minDamage.get();
    }

    private double scoreDamage(BlockPos base, PlayerEntity target, double targetDamage, double selfDamage) {
        double distance = mc.player.getDistanceSq(base.getX() + 0.5D, base.getY() + 0.5D, base.getZ() + 0.5D);
        double score = targetDamage - selfDamage * 0.55D - distance * 0.015D;
        if (base.getY() + 1 < target.getPosY()) {
            score += 0.35D;
        }
        if (target.isElytraFlying() && elytraSetup.get()) {
            score += 0.5D;
        }
        return score;
    }

    private double scorePlaceResult(CrystalPlaceResult result) {
        if (result == null || result.getActionType() == CrystalActionType.NONE) {
            return 0.0D;
        }
        double multiplier = result.getActionType() == CrystalActionType.ATTACK ? 0.88D : 1.0D;
        return Math.max(0.0D, result.getScore() * multiplier);
    }

    private double calculateDamage(Vector3d pos, LivingEntity target) {
        if (target == null || target.isInvulnerable()) {
            return 0.0D;
        }

        double distance = target.getPositionVec().distanceTo(pos);
        if (distance > CRYSTAL_POWER) {
            return 0.0D;
        }

        double density = Explosion.getBlockDensity(pos, target);
        double exposure = (1.0D - distance / CRYSTAL_POWER) * density;
        double damage = (exposure * exposure + exposure) * 0.5D * 7.0D * CRYSTAL_POWER + 1.0D;
        damage = Math.floor(damage);
        damage = CombatRules.getDamageAfterAbsorb((float) damage, target.getTotalArmorValue(),
                (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        damage -= EnchantmentHelper.getEnchantmentModifierDamage(target.getArmorInventoryList(),
                DamageSource.causeExplosionDamage((LivingEntity) null));
        return Math.max(0.0D, damage);
    }

    private Vector3d predict(PlayerEntity target) {
        Vector3d motion = target.getMotion();
        double ticks = predictTicks.get();
        if (target.isElytraFlying() && elytraSetup.get()) {
            ticks += 1.5D;
        }
        return target.getPositionVec().add(motion.x * ticks, motion.y * ticks, motion.z * ticks);
    }

    private void rotateTo(Vector3d target) {
        if (target == null || target == Vector3d.ZERO) {
            return;
        }

        Vector3d eye = mc.player.getEyePosition(1.0F);
        Vector3d offset = target.subtract(eye);
        float yaw = (float) wrapDegrees(Math.toDegrees(Math.atan2(offset.z, offset.x)) - 90.0D);
        float pitch = (float) (-Math.toDegrees(Math.atan2(offset.y, hypot(offset.x, offset.z))));
        RotationComponent.update(new Rotation(yaw, pitch), 180.0F, 1, 8);
    }

    private boolean canReach(Vector3d point, double range, double wall) {
        if (point == null) {
            return false;
        }
        double distanceSq = mc.player.getEyePosition(1.0F).squareDistanceTo(point);
        if (distanceSq > range * range) {
            return false;
        }
        if (throughWalls.get()) {
            return true;
        }
        if (canSee(point)) {
            return true;
        }
        return distanceSq <= wall * wall;
    }

    private boolean canSee(Vector3d point) {
        RayTraceResult result = mc.world.rayTraceBlocks(new RayTraceContext(
                mc.player.getEyePosition(1.0F),
                point,
                RayTraceContext.BlockMode.COLLIDER,
                RayTraceContext.FluidMode.NONE,
                mc.player
        ));
        return result == null || result.getType() == RayTraceResult.Type.MISS || result.getHitVec().squareDistanceTo(point) < 0.7D;
    }

    private BlockPlacement findBlockPlacement(BlockPos placePos) {
        if (!isReplaceable(placePos)) {
            return null;
        }

        Direction[] order = {
                Direction.DOWN,
                Direction.NORTH,
                Direction.SOUTH,
                Direction.WEST,
                Direction.EAST,
                Direction.UP
        };

        for (Direction direction : order) {
            BlockPos support = placePos.offset(direction);
            if (isReplaceable(support)) {
                continue;
            }
            Direction face = direction.getOpposite();
            Vector3d hit = faceHitVec(support, face);
            return new BlockPlacement(placePos, support, face, hit);
        }

        return null;
    }

    private Direction getMiningFace(BlockPos pos) {
        Vector3d eye = mc.player.getEyePosition(1.0F);
        Vector3d center = blockCenter(pos);
        Vector3d diff = eye.subtract(center);
        return Direction.getFacingFromVector(diff.x, diff.y, diff.z);
    }

    private boolean isReplaceable(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        return state.isAir() || state.getMaterial().isReplaceable();
    }

    private boolean canUseItem(Item item) {
        return getHandWith(item) != null || autoSwitch.get() && InventoryUtil.getItemSlot(item) != -1;
    }

    private Hand getHandWith(Item item) {
        if (mc.player.getHeldItemOffhand().getItem() == item) {
            return Hand.OFF_HAND;
        }
        if (mc.player.getHeldItemMainhand().getItem() == item) {
            return Hand.MAIN_HAND;
        }
        return null;
    }

    private SlotSwap switchToItem(Item item) {
        Hand hand = getHandWith(item);
        if (hand != null) {
            return new SlotSwap(true, hand, -1, -1);
        }
        if (!autoSwitch.get()) {
            return SlotSwap.invalid();
        }

        int slot = InventoryUtil.getItemSlot(item);
        if (slot == -1) {
            return SlotSwap.invalid();
        }

        int current = mc.player.inventory.currentItem;
        if (slot >= 36 && slot <= 44) {
            int hotbar = slot - 36;
            switchToHotbar(hotbar);
            return new SlotSwap(true, Hand.MAIN_HAND, -1, current);
        }

        mc.playerController.windowClick(0, slot, current, ClickType.SWAP, mc.player);
        return new SlotSwap(true, Hand.MAIN_HAND, slot, current);
    }

    private void restoreSwap(SlotSwap swap) {
        if (swap == null || !swap.valid) {
            return;
        }
        if (swap.inventorySlot != -1) {
            mc.playerController.windowClick(0, swap.inventorySlot, swap.previousHotbar, ClickType.SWAP, mc.player);
            return;
        }
        if (swap.previousHotbar != -1 && mc.player.inventory.currentItem != swap.previousHotbar) {
            switchToHotbar(swap.previousHotbar);
        }
    }

    private void switchToHotbar(int hotbar) {
        if (hotbar < 0 || hotbar > 8 || mc.player.inventory.currentItem == hotbar) {
            return;
        }
        mc.player.inventory.currentItem = hotbar;
        mc.player.connection.sendPacket(new CHeldItemChangePacket(hotbar));
    }

    private void schedulePendingCrystal(BlockPos base) {
        pendingCrystalBase = base;
        pendingCrystalPos = crystalVec(base);
        pendingCrystalId = -1;
        pendingTicks = PENDING_TICKS;
    }

    private void clearPendingCrystal() {
        pendingCrystalBase = null;
        pendingCrystalPos = null;
        pendingCrystalId = -1;
        pendingTicks = 0;
    }

    private void tickCooldowns() {
        if (placeCooldown > 0) {
            placeCooldown--;
        }
        if (breakCooldown > 0) {
            breakCooldown--;
        }
        if (mineCooldown > 0) {
            mineCooldown--;
        }
    }

    private void resetState() {
        currentPlaceResult = CrystalPlaceResult.none();
        clearPendingCrystal();
        nearbyFriends = Collections.emptyList();
        placeCooldown = 0;
        breakCooldown = 0;
        mineCooldown = 0;
    }

    private AxisAlignedBB crystalBox(BlockPos crystalPos) {
        return new AxisAlignedBB(
                crystalPos.getX(),
                crystalPos.getY(),
                crystalPos.getZ(),
                crystalPos.getX() + 1.0D,
                crystalPos.getY() + 2.0D,
                crystalPos.getZ() + 1.0D
        );
    }

    private Vector3d crystalVec(BlockPos base) {
        return new Vector3d(base.getX() + 0.5D, base.getY() + 1.0D, base.getZ() + 0.5D);
    }

    private Vector3d blockCenter(BlockPos pos) {
        return new Vector3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    private Vector3d faceHitVec(BlockPos pos, Direction face) {
        return blockCenter(pos).add(
                face.getXOffset() * 0.5D,
                face.getYOffset() * 0.5D,
                face.getZOffset() * 0.5D
        );
    }

    private AxisAlignedBB getRenderBox(CrystalPlaceResult result, float partialTicks) {
        if (result.getEntity() instanceof EnderCrystalEntity && result.getEntity().isAlive()) {
            Entity entity = result.getEntity();
            double x = MathHelper.lerp(partialTicks, entity.lastTickPosX, entity.getPosX());
            double y = MathHelper.lerp(partialTicks, entity.lastTickPosY, entity.getPosY());
            double z = MathHelper.lerp(partialTicks, entity.lastTickPosZ, entity.getPosZ());
            return new AxisAlignedBB(x - 0.5D, y, z - 0.5D, x + 0.5D, y + 1.0D, z + 0.5D);
        }
        if (result.getBlockPos() != null) {
            return new AxisAlignedBB(result.getBlockPos());
        }
        return null;
    }

    private int getActionColor(CrystalActionType actionType) {
        switch (actionType) {
            case BREAK:
                return 0xFF3030;
            case ATTACK:
                return 0xFFB02E;
            case PLACE:
                return 0x6A80FF;
            case PLACE_AND_BREAK:
                return 0x4AFF76;
            default:
                return 0xFFFFFF;
        }
    }

    private void drawBox(AxisAlignedBB box, int color, float alpha) {
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        GL11.glColor4f(r, g, b, alpha);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3d(box.minX, box.minY, box.minZ);
        GL11.glVertex3d(box.maxX, box.minY, box.minZ);
        GL11.glVertex3d(box.maxX, box.minY, box.maxZ);
        GL11.glVertex3d(box.minX, box.minY, box.maxZ);
        GL11.glVertex3d(box.minX, box.maxY, box.minZ);
        GL11.glVertex3d(box.minX, box.maxY, box.maxZ);
        GL11.glVertex3d(box.maxX, box.maxY, box.maxZ);
        GL11.glVertex3d(box.maxX, box.maxY, box.minZ);
        GL11.glVertex3d(box.minX, box.minY, box.minZ);
        GL11.glVertex3d(box.minX, box.maxY, box.minZ);
        GL11.glVertex3d(box.maxX, box.maxY, box.minZ);
        GL11.glVertex3d(box.maxX, box.minY, box.minZ);
        GL11.glVertex3d(box.maxX, box.minY, box.maxZ);
        GL11.glVertex3d(box.maxX, box.maxY, box.maxZ);
        GL11.glVertex3d(box.minX, box.maxY, box.maxZ);
        GL11.glVertex3d(box.minX, box.minY, box.maxZ);
        GL11.glVertex3d(box.minX, box.minY, box.minZ);
        GL11.glVertex3d(box.minX, box.maxY, box.minZ);
        GL11.glVertex3d(box.minX, box.maxY, box.maxZ);
        GL11.glVertex3d(box.minX, box.minY, box.maxZ);
        GL11.glVertex3d(box.maxX, box.minY, box.maxZ);
        GL11.glVertex3d(box.maxX, box.maxY, box.maxZ);
        GL11.glVertex3d(box.maxX, box.maxY, box.minZ);
        GL11.glVertex3d(box.maxX, box.minY, box.minZ);
        GL11.glEnd();
    }

    private void drawOutline(AxisAlignedBB box, int color, float width) {
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        GL11.glColor4f(r, g, b, 1.0F);
        GL11.glLineWidth(width);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(box.minX, box.minY, box.minZ);
        GL11.glVertex3d(box.maxX, box.minY, box.minZ);
        GL11.glVertex3d(box.maxX, box.minY, box.minZ);
        GL11.glVertex3d(box.maxX, box.minY, box.maxZ);
        GL11.glVertex3d(box.maxX, box.minY, box.maxZ);
        GL11.glVertex3d(box.minX, box.minY, box.maxZ);
        GL11.glVertex3d(box.minX, box.minY, box.maxZ);
        GL11.glVertex3d(box.minX, box.minY, box.minZ);
        GL11.glVertex3d(box.minX, box.minY, box.minZ);
        GL11.glVertex3d(box.minX, box.maxY, box.minZ);
        GL11.glVertex3d(box.maxX, box.minY, box.minZ);
        GL11.glVertex3d(box.maxX, box.maxY, box.minZ);
        GL11.glVertex3d(box.maxX, box.minY, box.maxZ);
        GL11.glVertex3d(box.maxX, box.maxY, box.maxZ);
        GL11.glVertex3d(box.minX, box.minY, box.maxZ);
        GL11.glVertex3d(box.minX, box.maxY, box.maxZ);
        GL11.glVertex3d(box.minX, box.maxY, box.minZ);
        GL11.glVertex3d(box.maxX, box.maxY, box.minZ);
        GL11.glVertex3d(box.maxX, box.maxY, box.minZ);
        GL11.glVertex3d(box.maxX, box.maxY, box.maxZ);
        GL11.glVertex3d(box.maxX, box.maxY, box.maxZ);
        GL11.glVertex3d(box.minX, box.maxY, box.maxZ);
        GL11.glVertex3d(box.minX, box.maxY, box.maxZ);
        GL11.glVertex3d(box.minX, box.maxY, box.minZ);
        GL11.glEnd();
    }

    private enum CrystalActionType {
        PLACE,
        BREAK,
        PLACE_AND_BREAK,
        ATTACK,
        NONE
    }

    private enum PlaceScanResult {
        VALID,
        BLOCKED,
        OUT_OF_RANGE
    }

    private static final class BlockPlacement {
        private final BlockPos placePos;
        private final BlockPos supportPos;
        private final Direction face;
        private final Vector3d hitVec;

        private BlockPlacement(BlockPos placePos, BlockPos supportPos, Direction face, Vector3d hitVec) {
            this.placePos = placePos;
            this.supportPos = supportPos;
            this.face = face;
            this.hitVec = hitVec;
        }
    }

    private static final class SlotSwap {
        private final boolean valid;
        private final Hand hand;
        private final int inventorySlot;
        private final int previousHotbar;

        private SlotSwap(boolean valid, Hand hand, int inventorySlot, int previousHotbar) {
            this.valid = valid;
            this.hand = hand;
            this.inventorySlot = inventorySlot;
            this.previousHotbar = previousHotbar;
        }

        private static SlotSwap invalid() {
            return new SlotSwap(false, Hand.MAIN_HAND, -1, -1);
        }
    }

    private static final class CrystalPlaceResult {
        private final BlockPos blockPos;
        private final Direction direction;
        private final double damage;
        private final double selfDamage;
        private final double score;
        private final CrystalActionType actionType;
        private final Entity entity;
        private final Vector3d aimVec;
        private final BlockPlacement placement;
        private final boolean hardStop;

        private CrystalPlaceResult(BlockPos blockPos, Direction direction, double damage, double selfDamage,
                                   double score, CrystalActionType actionType, Entity entity, Vector3d aimVec,
                                   BlockPlacement placement, boolean hardStop) {
            this.blockPos = blockPos;
            this.direction = direction;
            this.damage = damage;
            this.selfDamage = selfDamage;
            this.score = score;
            this.actionType = actionType;
            this.entity = entity;
            this.aimVec = aimVec;
            this.placement = placement;
            this.hardStop = hardStop;
        }

        private static CrystalPlaceResult none() {
            return new CrystalPlaceResult(null, null, 0.0D, 0.0D, 0.0D,
                    CrystalActionType.NONE, null, Vector3d.ZERO, null, false);
        }

        private BlockPos getBlockPos() {
            return blockPos;
        }

        private Direction getDirection() {
            return direction;
        }

        private double getDamage() {
            return damage;
        }

        private double getSelfDamage() {
            return selfDamage;
        }

        private double getScore() {
            return score;
        }

        private CrystalActionType getActionType() {
            return actionType;
        }

        private Entity getEntity() {
            return entity;
        }

        private Vector3d getAimVec() {
            return aimVec;
        }

        private BlockPlacement getPlacement() {
            return placement;
        }

        private boolean isHardStop() {
            return hardStop;
        }
    }
}
