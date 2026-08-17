package sky.core.modules.impl.combat;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import sky.core.SkyCore;
import sky.core.events.EventMotion;
import sky.core.events.EventPacket;
import sky.core.events.EventPostUpdate;
import sky.core.events.EventRender3D;
import sky.core.events.EventUpdate;
import sky.core.handlers.impl.Rotation;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.MultiBooleanSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.component.impl.RotationComponent;
import sky.core.utils.component.impl.SensUtil;
import sky.core.utils.math.MathUtil;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static sky.core.utils.Wrapper.mc;

public class AimBot extends Module {

    private final MultiBooleanSetting targetTypes = new MultiBooleanSetting("Типы целей",
            new BooleanSetting("Игроки", true),
            new BooleanSetting("В броне", true),
            new BooleanSetting("Без брони", false),
            new BooleanSetting("Мобы", false),
            new BooleanSetting("Зомби", false));

    private final MultiBooleanSetting items = new MultiBooleanSetting("Предметы",
            new BooleanSetting("Лук", true),
            new BooleanSetting("Арбалет", true),
            new BooleanSetting("Трезубец", true));

    private final SliderSetting range = new SliderSetting("Дистанция", 40.0F, 10.0F, 100.0F, 1.0F);
    private final SliderSetting aimTime = new SliderSetting("Время наводки (тики)", 10.0F, 0.0F, 40.0F, 1.0F);
    private final BooleanSetting silentRotations = new BooleanSetting("Тихие повороты", true);
    private final BooleanSetting showCrosshair = new BooleanSetting("Показать прицел", true);
    private final SliderSetting crosshairSize = new SliderSetting("Размер прицела", 1.0F, 0.3F, 3.0F, 0.1F,
            () -> showCrosshair.get());

    private LivingEntity target;
    private boolean aiming;
    private float aimProgress;
    private Rotation targetRotation;
    private int lastUseTicks;

    private boolean sendingDeferredRelease;
    private int deferredReleaseTicks;
    private CPlayerDiggingPacket deferredReleasePacket;
    private float deferredYaw;
    private float deferredPitch;

    public AimBot() {
        super("Projectile Helper", "Авто-наведение для лука и арбалета", Category.Combat);
        addSettings(targetTypes, items, range, aimTime, silentRotations, showCrosshair, crosshairSize);
    }

    public LivingEntity getTarget() {
        return target;
    }

    public boolean isAiming() {
        return aiming;
    }

    private boolean isHoldingProjectile() {
        if (mc.player == null) {
            return false;
        }
        ItemStack main = mc.player.getHeldItemMainhand();
        ItemStack off = mc.player.getHeldItemOffhand();
        return isProjectileItem(main) || isProjectileItem(off);
    }

    private boolean isProjectileItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (items.is("Лук") && stack.getItem() instanceof BowItem) {
            return true;
        }
        if (items.is("Арбалет") && stack.getItem() instanceof CrossbowItem) {
            return true;
        }
        return items.is("Трезубец") && stack.getItem() instanceof TridentItem;
    }

    private boolean isUsingProjectile() {
        return mc.player != null && mc.player.isHandActive() && isHoldingProjectile();
    }

    private boolean isBot(LivingEntity entity) {
        AntiBot antiBot = (AntiBot) SkyCore.getInstance().getModuleManager().getModule(AntiBot.class);
        return antiBot != null && antiBot.isEnabled() && AntiBot.bot.contains(entity);
    }

    private boolean isFriendPlayer(PlayerEntity player) {
        if (player == null || SkyCore.getInstance().getFriendManager() == null) {
            return false;
        }
        if (player.getGameProfile() != null) {
            String profileName = player.getGameProfile().getName();
            if (profileName != null && SkyCore.getInstance().getFriendManager().isFriend(profileName)) {
                return true;
            }
        }
        return SkyCore.getInstance().getFriendManager().isFriend(player.getName().getString());
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity == null || entity == mc.player) {
            return false;
        }
        if (!entity.isAlive() || entity.getHealth() <= 0.0F) {
            return false;
        }
        if (isBot(entity)) {
            return false;
        }

        if (entity instanceof PlayerEntity player) {
            if (!targetTypes.is("Игроки")) {
                return false;
            }
            if (isFriendPlayer(player)) {
                return false;
            }

            boolean hasArmor = false;
            for (ItemStack armor : player.getArmorInventoryList()) {
                if (!armor.isEmpty()) {
                    hasArmor = true;
                    break;
                }
            }

            if (targetTypes.is("В броне") && hasArmor) {
                return true;
            }
            if (targetTypes.is("Без брони") && !hasArmor) {
                return true;
            }
            return !targetTypes.is("В броне") && !targetTypes.is("Без брони");
        }

        if (entity instanceof ZombieEntity) {
            return targetTypes.is("Зомби");
        }

        if (entity instanceof MonsterEntity) {
            return targetTypes.is("Мобы");
        }

        return false;
    }

    private LivingEntity findBestTarget() {
        if (mc.player == null || mc.world == null) {
            return null;
        }

        float maxRange = range.get();
        AxisAlignedBB box = mc.player.getBoundingBox().grow(maxRange);
        List<LivingEntity> targets = new ArrayList<>();

        for (LivingEntity entity : mc.world.getEntitiesWithinAABB(LivingEntity.class, box, e -> true)) {
            if (!isValidTarget(entity)) {
                continue;
            }
            if (mc.player.getDistanceSq(entity) > maxRange * maxRange) {
                continue;
            }
            if (!mc.player.canEntityBeSeen(entity)) {
                continue;
            }
            targets.add(entity);
        }

        if (targets.isEmpty()) {
            return null;
        }

        targets.sort(Comparator.comparingDouble(mc.player::getDistance));
        return targets.get(0);
    }

    private Rotation calculateRotation(LivingEntity aimTarget) {
        ItemStack active = mc.player.getActiveItemStack();
        if (!active.isEmpty() && active.getItem() instanceof TridentItem && items.is("Трезубец")) {
            Vector2f rot = calculateTridentRotation(aimTarget);
            return new Rotation(rot.x, rot.y);
        }

        Vector3d eyes = mc.player.getEyePosition(1.0F);
        Vector3d targetPos = aimTarget.getBoundingBox().getCenter();
        double dx = targetPos.x - eyes.x;
        double dy = targetPos.y - eyes.y;
        double dz = targetPos.z - eyes.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        return new Rotation(yaw, pitch);
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null || mc.player.isElytraFlying()) {
            aiming = false;
            target = null;
            return;
        }

        boolean offHandBow = mc.player.getHeldItemOffhand().getItem() instanceof BowItem && items.is("Лук");
        boolean mainHandBow = mc.player.getHeldItemMainhand().getItem() instanceof BowItem && items.is("Лук");
        boolean offHandTrident = mc.player.getHeldItemOffhand().getItem() instanceof TridentItem && items.is("Трезубец");
        boolean mainHandTrident = mc.player.getHeldItemMainhand().getItem() instanceof TridentItem && items.is("Трезубец");

        if ((offHandBow || mainHandBow || offHandTrident || mainHandTrident) && mc.player.isHandActive()) {
            lastUseTicks = mc.player.getItemInUseMaxCount();
        }

        aiming = isUsingProjectile();

        if (aiming) {
            LivingEntity newTarget = findBestTarget();
            if (newTarget != null) {
                if (target != newTarget) {
                    target = newTarget;
                    aimProgress = 0.0F;
                }

                Rotation newRotation = calculateRotation(target);
                float maxStep = 1.0F / Math.max(1.0F, aimTime.get());
                aimProgress = Math.min(aimProgress + maxStep, 1.0F);

                float currentYaw = mc.player.rotationYaw;
                float currentPitch = mc.player.rotationPitch;
                float yawDiff = MathHelper.wrapDegrees(newRotation.getYaw() - currentYaw);
                float pitchDiff = newRotation.getPitch() - currentPitch;

                float stepYaw = yawDiff * aimProgress;
                float stepPitch = pitchDiff * aimProgress;
                targetRotation = new Rotation(currentYaw + stepYaw, currentPitch + stepPitch);
            }
        } else {
            target = null;
            targetRotation = null;
            aimProgress = 0.0F;
        }

        if (target != null && aiming && targetRotation != null) {
            if (silentRotations.get()) {
                float yaw = applyGcd(targetRotation.getYaw(), mc.player.rotationYaw);
                float pitch = applyGcd(targetRotation.getPitch(), mc.player.rotationPitch);
                RotationComponent.update(new Rotation(yaw, pitch), 180.0F, 180.0F, 45.0F, 45.0F, 0, 2, false);
            } else {
                mc.player.rotationYaw = targetRotation.getYaw();
                mc.player.rotationPitch = targetRotation.getPitch();
            }
        }
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (!isEnabled() || !showCrosshair.get() || target == null || !aiming) {
            return;
        }

        Vector3d targetPos = MathUtil.interpolate(target, event.getPartialTicks())
                .add(0.0D, target.getHeight() * 0.5D, 0.0D);
        Vector3d cameraPos = mc.gameRenderer.getActiveRenderInfo().getProjectedView();

        MatrixStack matrices = event.getMatrixStack();
        double renderX = targetPos.x - cameraPos.x;
        double renderY = targetPos.y - cameraPos.y;
        double renderZ = targetPos.z - cameraPos.z;

        ActiveRenderInfo camera = mc.gameRenderer.getActiveRenderInfo();
        float size = crosshairSize.get() * 0.5F;
        int alpha = (int) (255.0F * aimProgress);
        int color = ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL), alpha / 255.0F);

        matrices.push();
        matrices.translate(renderX, renderY, renderZ);
        matrices.rotate(camera.getRotation().copy());

        RenderSystem.pushMatrix();
        RenderSystem.multMatrix(matrices.getLast().getMatrix());
        RenderSystem.disableDepthTest();
        RenderSystem.disableAlphaTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderUtil.drawImage3D(
                new net.minecraft.util.ResourceLocation("minecraft", "SkyCore/icons/world_render/target.png"),
                -size / 2.0F,
                -size / 2.0F,
                0.0F,
                size,
                size,
                color,
                true
        );

        RenderSystem.enableCull();
        RenderSystem.enableAlphaTest();
        RenderSystem.enableDepthTest();
        RenderSystem.popMatrix();
        matrices.pop();
    }

    @EventTarget
    public void onSync(EventMotion event) {
        if (deferredReleasePacket == null) {
            return;
        }
        event.setYaw(deferredYaw);
        event.setPitch(deferredPitch);
    }

    @EventTarget
    public void onTick(EventPostUpdate event) {
        if (mc.player == null || mc.player.connection == null || deferredReleasePacket == null) {
            return;
        }
        if (deferredReleaseTicks > 0) {
            deferredReleaseTicks--;
        }
        if (deferredReleaseTicks <= 0) {
            try {
                sendingDeferredRelease = true;
                mc.player.connection.sendPacket(deferredReleasePacket);
            } finally {
                sendingDeferredRelease = false;
            }
            deferredReleasePacket = null;
        }
    }

    @EventTarget
    public void onSendPacket(EventPacket event) {
        if (mc.player == null || mc.player.connection == null || sendingDeferredRelease || deferredReleasePacket != null) {
            return;
        }
        if (!(event.getPacket() instanceof CPlayerDiggingPacket diggingPacket)) {
            return;
        }
        if (diggingPacket.getAction() != CPlayerDiggingPacket.Action.RELEASE_USE_ITEM) {
            return;
        }
        if (!isHoldingProjectile()) {
            return;
        }

        LivingEntity releaseTarget = findBestTarget();
        if (releaseTarget == null) {
            return;
        }

        target = releaseTarget;
        Rotation rotation = calculateRotation(releaseTarget);
        deferredYaw = rotation.getYaw();
        deferredPitch = rotation.getPitch();
        RotationComponent.update(rotation, 360.0F, 360.0F, 360.0F, 360.0F, 1, 10, false);

        event.setCancelled(true);
        deferredReleasePacket = diggingPacket;
        deferredReleaseTicks = 1;
    }

    private float applyGcd(float targetAngle, float currentAngle) {
        float delta = targetAngle - currentAngle;
        float gcd = SensUtil.getGCDValue();
        return currentAngle + delta - delta % gcd;
    }

    private Vector2f calculateTridentRotation(LivingEntity aimTarget) {
        int useTicks = mc.player.isHandActive() ? mc.player.getItemInUseMaxCount() : lastUseTicks;
        float currentDuration = useTicks / 20.0F;
        currentDuration = (currentDuration * currentDuration + currentDuration * 2.0F) / 3.0F;
        if (currentDuration >= 1.0F) {
            currentDuration = 1.0F;
        }
        float pitch = (float) (-Math.toDegrees(calculateArc(aimTarget, currentDuration * 3.0F)));
        double iX = aimTarget.getPosX() - aimTarget.prevPosX;
        double iZ = aimTarget.getPosZ() - aimTarget.prevPosZ;
        double distance = mc.player.getDistance(aimTarget);
        distance -= distance % 2.0;
        iX = distance / 2.0 * iX * (mc.player.isSprinting() ? 1.3 : 1.1);
        iZ = distance / 2.0 * iZ * (mc.player.isSprinting() ? 1.3 : 1.1);
        float yaw = (float) Math.toDegrees(Math.atan2(aimTarget.getPosZ() + iZ - mc.player.getPosZ(),
                aimTarget.getPosX() + iX - mc.player.getPosX())) - 90.0F;
        return new Vector2f(yaw, pitch);
    }

    private float calculateArc(LivingEntity aimTarget, double duration) {
        double yArc = aimTarget.getPosY() + aimTarget.getEyeHeight() - (mc.player.getPosY() + mc.player.getEyeHeight());
        double dX = aimTarget.getPosX() - mc.player.getPosX();
        double dZ = aimTarget.getPosZ() - mc.player.getPosZ();
        double dirRoot = Math.sqrt(dX * dX + dZ * dZ);
        return calculateArc(duration, dirRoot, yArc);
    }

    private float calculateArc(double d, double dr, double y) {
        y = 2.0 * y * (d * d);
        y = 0.05 * ((0.05 * (dr * dr)) + y);
        y = Math.sqrt(d * d * d * d - y);
        d = d * d - y;
        y = Math.atan2(d * d + y, 0.05 * dr);
        d = Math.atan2(d, 0.05 * dr);
        return (float) Math.min(y, d);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        target = null;
        aiming = false;
        aimProgress = 0.0F;
        targetRotation = null;
        deferredReleasePacket = null;
        deferredReleaseTicks = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        target = null;
        aiming = false;
        aimProgress = 0.0F;
        targetRotation = null;
        deferredReleasePacket = null;
        deferredReleaseTicks = 0;
    }
}