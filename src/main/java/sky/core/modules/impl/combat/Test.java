package sky.core.modules.impl.combat;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.item.EnderCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.Items;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.Explosion;
import org.lwjgl.opengl.GL11;
import sky.core.SkyCore;
import sky.core.utils.component.impl.RotationComponent;
import sky.core.events.EventRender3D;
import sky.core.events.EventUpdate;
import sky.core.handlers.impl.Rotation;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.utils.math.AuraUtil;
import sky.core.utils.player.InventoryUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.hypot;
import static net.minecraft.util.math.MathHelper.wrapDegrees;

public class Test extends Module {
    private final SliderSetting placeRange = new SliderSetting("Дистанция установки", 4.0F, 1.0F, 5.0F, 0.1F);
    private final SliderSetting breakRange = new SliderSetting("Дистанция удара", 3.5F, 1.0F, 4.5F, 0.1F);
    private final SliderSetting wallRange = new SliderSetting("Дистанция стен", 3.0F, 1.0F, 4.0F, 0.1F);
    private final SliderSetting placeDelay = new SliderSetting("Задержка установки", 100F, 0F, 500F, 10F);
    private final SliderSetting breakDelay = new SliderSetting("Задержка удара", 100F, 0F, 500F, 10F);
    private final SliderSetting minDamage = new SliderSetting("Мин. урон", 5F, 1F, 36F, 0.5F);
    private final SliderSetting maxSelfDamage = new SliderSetting("Макс. самоурон", 8F, 1F, 36F, 0.5F);
    private final BooleanSetting prioritizeLow = new BooleanSetting("Приоритет ниже", true);
    private final BooleanSetting facePlace = new BooleanSetting("FacePlace", true);
    private final SliderSetting facePlaceHP = new SliderSetting("FacePlace HP", 8F, 1F, 20F, 0.5F, facePlace::get);
    private final BooleanSetting antiSuicide = new BooleanSetting("Анти-суицид", true);
    private final BooleanSetting checkRegion = new BooleanSetting("Проверка региона", true);
    private final ModeSetting rotateMode = new ModeSetting("Ротация", "Плавная", "Плавная", "Мгновенная");
    private final SliderSetting rotateSpeed = new SliderSetting("Скорость ротации", 45F, 10F, 180F, 5F, () -> rotateMode.is("Плавная"));
    private final BooleanSetting strictMotion = new BooleanSetting("Строгий спринт", true);
    private final BooleanSetting rayTrace = new BooleanSetting("RayTrace", true);
    private final BooleanSetting swing = new BooleanSetting("Свинг", true);
    private final BooleanSetting render = new BooleanSetting("Подсветка", true);

    private long lastPlaceTime = 0, lastBreakTime = 0;
    private BlockPos bestPlacePos = null;
    private EnderCrystalEntity targetCrystal = null;
    private LivingEntity target = null;
    private boolean isRotating = false;
    private final List<BlockPos> renderPositions = new ArrayList<>();
    private Field overlayMessageField = null;

    public Test() {
        super("CrystalAura", "Универсальный авто кристалл", Category.Combat);
        addSettings(placeRange, breakRange, wallRange, placeDelay, breakDelay, minDamage, maxSelfDamage, prioritizeLow, facePlace, facePlaceHP, antiSuicide, checkRegion, rotateMode, rotateSpeed, strictMotion, rayTrace, swing, render);
    }

    @java.lang.Override
    public void onDisable() {
        bestPlacePos = null;
        targetCrystal = null;
        target = null;
        isRotating = false;
        renderPositions.clear();
        super.onDisable();
    }

    private boolean isBot(PlayerEntity p) {
        if (p == null) return false;
        AntiBot ab = (AntiBot) SkyCore.getInstance().getModuleManager().getModule(AntiBot.class);
        return ab != null && ab.isEnabled() && AntiBot.bot.contains(p);
    }

    @EventTarget
    private void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;
        if (checkRegion.get() && isInProtectedRegion()) {
            reset();
            return;
        }
        renderPositions.clear();
        bestPlacePos = null;
        targetCrystal = null;
        target = findTarget();
        if (target == null) {
            isRotating = false;
            return;
        }
        List<BlockPos> validPositions = findValidPlacePositions();
        if (render.get()) renderPositions.addAll(validPositions);
        targetCrystal = findBestCrystal();
        bestPlacePos = findBestPlacePosition(validPositions);
        Vector3d rotationTarget = null;
        boolean attacking = false;
        long currentTime = System.currentTimeMillis();

        if (targetCrystal != null && (currentTime - lastBreakTime) >= breakDelay.get()) {
            if (canAttackCrystal(targetCrystal)) {
                rotationTarget = targetCrystal.getPositionVec().add(0, 0.5, 0);
                attacking = true;
            }
        } else if (bestPlacePos != null && (currentTime - lastPlaceTime) >= placeDelay.get()) {
            rotationTarget = AuraUtil.getClosestVec(mc.player.getEyePosition(1.0F), new AxisAlignedBB(bestPlacePos));
            attacking = false;
        }

        if (rotationTarget != null) {
            isRotating = true;
            Vector3d eyeVec = mc.player.getEyePosition(1.0F);
            Vector3d offset = rotationTarget.subtract(eyeVec);
            float targetYaw = (float) wrapDegrees(Math.toDegrees(Math.atan2(offset.z, offset.x)) - 90);
            float targetPitch = (float) (-Math.toDegrees(Math.atan2(offset.y, hypot(offset.x, offset.z))));
            RotationComponent.update(new Rotation(targetYaw, targetPitch), rotateSpeed.get(), 1, 6);

            if (isLookingAt(rotationTarget, targetYaw, targetPitch)) {
                if (attacking && targetCrystal != null && targetCrystal.isAlive()) performAttack(targetCrystal);
                else if (!attacking && bestPlacePos != null) performPlace(bestPlacePos, rotationTarget);
            }
            if (strictMotion.get() && Math.abs(MathHelper.wrapDegrees(targetYaw - mc.player.rotationYaw)) > 20)
                mc.player.setSprinting(false);
        } else {
            isRotating = false;
        }
    }

    private boolean isLookingAt(Vector3d t, float y, float p) {
        if (!rayTrace.get())
            return Math.abs(MathHelper.wrapDegrees(y - mc.player.rotationYaw)) < 5F && Math.abs(p - mc.player.rotationPitch) < 5F;
        RayTraceResult r = mc.world.rayTraceBlocks(new RayTraceContext(mc.player.getEyePosition(1F), t, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, mc.player));
        return r == null || r.getType() == RayTraceResult.Type.MISS || r.getHitVec().squareDistanceTo(t) < 1.0;
    }

    private boolean isInProtectedRegion() {
        try {
            if (overlayMessageField == null) {
                overlayMessageField = IngameGui.class.getDeclaredField("overlayMessage");
                overlayMessageField.setAccessible(true);
            }
            String t = (String) overlayMessageField.get(mc.ingameGUI);
            if (t != null && !t.isEmpty()) {
                String c = TextFormatting.getTextWithoutFormattingCodes(t).toLowerCase();
                return c.contains("регион в привате") || c.contains("запривачено");
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private void performAttack(EnderCrystalEntity c) {
        if (c == null || !c.isAlive()) return;
        mc.playerController.attackEntity(mc.player, c);
        if (swing.get()) mc.player.swingArm(Hand.MAIN_HAND);
        lastBreakTime = System.currentTimeMillis();
    }

    private void performPlace(BlockPos p, Vector3d h) {
        if (p == null || !isValidPlacePosition(p)) return;
        int s = InventoryUtil.getItemSlot(Items.END_CRYSTAL);
        boolean o = mc.player.getHeldItemOffhand().getItem() == Items.END_CRYSTAL, m = mc.player.getHeldItemMainhand().getItem() == Items.END_CRYSTAL;
        if (s == -1 && !o && !m) return;
        Vector3d off = h.subtract(mc.player.getEyePosition(1.0F)).inverse();
        BlockRayTraceResult res = new BlockRayTraceResult(h, Direction.getFacingFromVector(off.x, off.y, off.z), p, false);
        if (m) {
            mc.playerController.func_217292_a(mc.player, mc.world, Hand.MAIN_HAND, res);
            if (swing.get()) mc.player.swingArm(Hand.MAIN_HAND);
        } else if (o) {
            mc.playerController.func_217292_a(mc.player, mc.world, Hand.OFF_HAND, res);
            if (swing.get()) mc.player.swingArm(Hand.OFF_HAND);
        } else if (s != -1) {
            int ws = s < 9 ? s + 36 : s;
            mc.playerController.windowClick(0, ws, 40, ClickType.SWAP, mc.player);
            mc.playerController.func_217292_a(mc.player, mc.world, Hand.OFF_HAND, res);
            if (swing.get()) mc.player.swingArm(Hand.OFF_HAND);
            mc.playerController.windowClick(0, ws, 40, ClickType.SWAP, mc.player);
        }
        lastPlaceTime = System.currentTimeMillis();
    }

    private boolean isValidPlacePosition(BlockPos p) {
        if (p == null) return false;
        Block b = mc.world.getBlockState(p).getBlock();
        if ((b != Blocks.OBSIDIAN && b != Blocks.BEDROCK) || !mc.world.getBlockState(p.up()).isAir()) return false;
        AxisAlignedBB box = new AxisAlignedBB(p.getX(), p.getY() + 1, p.getZ(), p.getX() + 1, p.getY() + 3, p.getZ() + 1);
        for (Entity e : mc.world.getEntitiesWithinAABBExcludingEntity(null, box))
            if (e != null && e.isAlive() && !e.isSpectator()) return false;
        return true;
    }

    private List<BlockPos> findValidPlacePositions() {
        List<BlockPos> l = new ArrayList<>();
        if (mc.player == null || mc.world == null) return l;
        int r = (int) Math.ceil(placeRange.get());
        BlockPos pp = mc.player.getPosition();
        for (int x = -r; x <= r; x++)
            for (int y = -r; y <= r; y++)
                for (int z = -r; z <= r; z++) {
                    BlockPos p = pp.add(x, y, z);
                    if (mc.player.getDistanceSq(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5) <= placeRange.get() * placeRange.get() && isValidPlacePosition(p))
                        l.add(p);
                }
        return l;
    }

    private boolean canPlaceAt(BlockPos p) {
        if (!isValidPlacePosition(p)) return false;
        Vector3d c = new Vector3d(p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5);
        if (rayTrace.get()) {
            RayTraceResult r = mc.world.rayTraceBlocks(new RayTraceContext(mc.player.getEyePosition(1F), c, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, mc.player));
            if (r.getType() == RayTraceResult.Type.BLOCK) {
                BlockRayTraceResult br = (BlockRayTraceResult) r;
                if (!br.getPos().equals(p) && !br.getPos().equals(p.up()) && mc.player.getDistanceSq(c) > wallRange.get() * wallRange.get())
                    return false;
            }
        }
        return true;
    }

    private LivingEntity findTarget() {
        double range = placeRange.get() + 8, closest = Double.MAX_VALUE;
        LivingEntity t = null;
        for (Entity e : mc.world.getAllEntities()) {
            if (!(e instanceof PlayerEntity) || e == mc.player || !e.isAlive() || SkyCore.getInstance().getFriendManager().isFriend(e.getName().getString()) || isBot((PlayerEntity) e))
                continue;
            double d = mc.player.getDistance(e);
            if (d < closest && d <= range) {
                closest = d;
                t = (LivingEntity) e;
            }
        }
        return t;
    }

    private EnderCrystalEntity findBestCrystal() {
        if (target == null) return null;
        EnderCrystalEntity best = null;
        float max = 0;
        for (Entity e : mc.world.getAllEntities()) {
            if (!(e instanceof EnderCrystalEntity) || !e.isAlive() || e.ticksExisted < 2 || mc.player.getDistance(e) > breakRange.get())
                continue;
            float dmg = calculateDamage(e.getPositionVec(), target), self = calculateDamage(e.getPositionVec(), mc.player);
            if ((antiSuicide.get() && self >= mc.player.getHealth() + mc.player.getAbsorptionAmount()) || dmg < getMinDamage() || self > maxSelfDamage.get())
                continue;
            if (dmg > max) {
                max = dmg;
                best = (EnderCrystalEntity) e;
            }
        }
        return best;
    }

    private BlockPos findBestPlacePosition(List<BlockPos> l) {
        if (target == null || l.isEmpty()) return null;
        BlockPos best = null;
        double score = -1;
        for (BlockPos p : l) {
            if (!canPlaceAt(p)) continue;
            Vector3d v = new Vector3d(p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5);
            float dmg = calculateDamage(v, target), self = calculateDamage(v, mc.player);
            if ((antiSuicide.get() && self >= mc.player.getHealth() + mc.player.getAbsorptionAmount()) || dmg < getMinDamage() || self > maxSelfDamage.get())
                continue;
            double s = dmg - (self * 0.5);
            if (prioritizeLow.get() && p.getY() + 1 < target.getPosY()) s += 2;
            if (s > score) {
                score = s;
                best = p;
            }
        }
        return best;
    }

    private float getMinDamage() {
        return (facePlace.get() && target != null && (target.getHealth() + target.getAbsorptionAmount()) <= facePlaceHP.get()) ? 2.0F : minDamage.get();
    }

    private boolean canAttackCrystal(EnderCrystalEntity c) {
        if (c == null || !c.isAlive()) return false;
        if (rayTrace.get()) {
            RayTraceResult r = mc.world.rayTraceBlocks(new RayTraceContext(mc.player.getEyePosition(1F), c.getPositionVec().add(0, 0.5, 0), RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, mc.player));
            if (r.getType() == RayTraceResult.Type.BLOCK && mc.player.getDistance(c) > wallRange.get()) return false;
        }
        return true;
    }

    private float calculateDamage(Vector3d p, LivingEntity t) {
        if (t == null) return 0;
        double d = t.getPositionVec().distanceTo(p);
        if (d > 12) return 0;
        double ex = Explosion.getBlockDensity(p, t), im = (1.0 - (d / 12.0)) * ex;
        float dmg = (float) ((int) ((im * im + im) / 2.0 * 7.0 * 12.0 + 1.0));
        dmg = CombatRules.getDamageAfterAbsorb(dmg, t.getTotalArmorValue(), (float) t.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        return Math.max(dmg - EnchantmentHelper.getEnchantmentModifierDamage(t.getArmorInventoryList(), DamageSource.causeExplosionDamage((LivingEntity) null)), 0.0F);
    }

    private void reset() {
        bestPlacePos = null;
        targetCrystal = null;
        isRotating = false;
        renderPositions.clear();
    }

    @EventTarget
    private void onRender(EventRender3D e) {
        if (!render.get() || renderPositions.isEmpty()) return;
        Vector3d c = mc.getRenderManager().info.getProjectedView();
        RenderSystem.pushMatrix();
        RenderSystem.translated(-c.x, -c.y, -c.z);
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        for (BlockPos p : renderPositions) {
            boolean b = p.equals(bestPlacePos), cp = canPlaceAt(p);
            if (!cp && !b) continue;
            int col = b ? 0xFF00FF00 : 0xFFFFFF00;
            AxisAlignedBB bb = new AxisAlignedBB(p);
            drawBox(bb, col, b ? 0.4f : 0.2f);
            drawOutline(bb, col, 1.5f);
        }
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.depthMask(true);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.popMatrix();
    }

    private void drawBox(AxisAlignedBB bb, int c, float a) {
        float r = ((c >> 16) & 0xFF) / 255f, g = ((c >> 8) & 0xFF) / 255f, b = (c & 0xFF) / 255f;
        GL11.glColor4f(r, g, b, a);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
        GL11.glEnd();
    }

    private void drawOutline(AxisAlignedBB bb, int c, float w) {
        float r = ((c >> 16) & 0xFF) / 255f, g = ((c >> 8) & 0xFF) / 255f, b = (c & 0xFF) / 255f;
        GL11.glColor4f(r, g, b, 1.0f);
        GL11.glLineWidth(w);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.minX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
        GL11.glEnd();
    }
}