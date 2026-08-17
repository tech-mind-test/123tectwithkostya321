package sky.core.modules.impl.combat;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.merchant.villager.WanderingTraderEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.monster.PhantomEntity;
import net.minecraft.entity.monster.ShulkerEntity;
import net.minecraft.entity.monster.SlimeEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.passive.fish.CodEntity;
import net.minecraft.entity.passive.fish.SalmonEntity;
import net.minecraft.entity.passive.fish.TropicalFishEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.client.CAnimateHandPacket;
import net.minecraft.network.play.client.CEntityActionPacket;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.potion.Effects;
import net.minecraft.fluid.FluidState;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import com.adl.nativeprotect.Native;
import sky.core.SkyCore;
import sky.core.utils.component.RotationAccess;
import sky.core.events.*;
import sky.core.modules.impl.miscellaneous.TargetPearl;
import sky.core.modules.impl.movement.AirStuck;
import sky.core.modules.impl.movement.ElytraTarget;
import sky.core.modules.impl.movement.Sprint;
import sky.core.modules.impl.movement.Speed;
import sky.core.utils.component.impl.*;
import sky.core.utils.player.*;
import sky.core.handlers.impl.LookHandler;
import sky.core.handlers.impl.Rotation;
import sky.core.handlers.impl.TPSHandler;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.modules.api.constructors.impl.MultiBooleanSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.utils.math.AuraUtil;
import sky.core.utils.math.NewPredictRotations;
import sky.core.utils.math.TimeUtil;
import sky.core.utils.misc.ServerUtil;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Math.hypot;
import static java.lang.Math.toDegrees;
import static net.minecraft.util.math.MathHelper.clamp;
import static net.minecraft.util.math.MathHelper.wrapDegrees;


@SuppressWarnings("all")

public class AttackAura extends Module {
    private static float lonyJirSpeed;
    @Getter
    public static LivingEntity target = null;
    @Getter
    public static Vector2f rotate = new Vector2f(0, 0);
    private static boolean visualReverseYaw;
    private boolean visualReverseWasActive;

    public final ModeSetting componentMode = new ModeSetting("Режим ротации", "ReallyWorld", "ReallyWorld", "SunWay", "LonyGrief");
    public final ModeSetting smoothAimType = new ModeSetting("Вид наводки", "Плавный", () -> componentMode.is("ReallyWorld"), "Плавный", "Резкий"
    );
    public final MultiBooleanSetting targets = new MultiBooleanSetting("Цели", new BooleanSetting("Игроки", true), new BooleanSetting("Друзья", false), new BooleanSetting("Голые", true), new BooleanSetting("Животные", false), new BooleanSetting("Мобы", false));

    public final SliderSetting attackRange = new SliderSetting("Радиус атаки", 3.0F, 2.5F, 6.0F, 0.1F);
    public final SliderSetting lerpRotationDistance = new SliderSetting("Радиус преследования", 1.5F, 0.0F, 3.0F, 0.1F);
    public final BooleanSetting disableOnDeath = new BooleanSetting("Выключить после смерти", true);
    public final BooleanSetting ignoreEat = new BooleanSetting("Не бить когда ешь", false);
    public final BooleanSetting onlyWeapon = new BooleanSetting("Бить только с оружием", false);
    public final BooleanSetting tpsSync = new BooleanSetting("TPSSync", false);

    @Getter
    private BooleanSetting onlycrit = new BooleanSetting("Бить только критами", true);
    private BooleanSetting bypassWalls = new BooleanSetting("Обход стен RW", false);
    public final BooleanSetting ray = new BooleanSetting("Проверка наведения", false, () -> componentMode.is("ReallyWorld"));
    @Getter
    public final ModeSetting correctionType = new ModeSetting("Коррекция движения", "Свободная", "Свободная", "Сфокусированная", "Таргет");


    @Getter
    private final BooleanSetting throughWalls = new BooleanSetting("Сквозь стены", true);

    @Getter
    private int tickssnap;


    private final BooleanSetting SHEIS = new BooleanSetting("Отжимать Щит ", true);

    public final BooleanSetting setPitch = new BooleanSetting("Поворачивать pitch", false);
    public final BooleanSetting onlySpaceCritical = new BooleanSetting("Умные криты", false);
    private long lastTickTime2 = 0;

    @Getter
    public static double bpsTarget = 0.0f;
    public boolean canCrit;
    PerfectDelay perfectDelay = new PerfectDelay();
    TimeUtil stopWatch = new TimeUtil();
    int rayTicks = 0;
    @Getter
    private Vector2f lerpRotation = Vector2f.ZERO;
    private Vector2f lerpRot = Vector2f.ZERO;
    private Vector2f lerprRot = Vector2f.ZERO;
    private int count;
    private int counter;
    public float yawDelta;

    public double lastSpeed = 0;

    public static float acceleration;
    public static boolean isBack;
    public static float lastYaw;
    public static float lastPitch;
    private boolean lonyReverseSmooth;
    private float lonySmooth;
    private float lonyLastYaw;
    private float lonyLastPitch;
    private LivingEntity lonyLastTarget;

    public static float randomOffsetX;
    public static float randomOffsetY;
    public static float randomOffsetZ;

    @Getter
    private long cps = 0L;
    private int legitSprintResetTicks;
    public double prevSpeed = 0;

    private Vector3d defensivePos;
    private final TimeUtil defensiveTimer = new TimeUtil();
    private Vector3d leaveVec = Vector3d.ZERO;
    private Vector3d lastVec = Vector3d.ZERO;
    private final NewPredictRotations newPredictRotations = new NewPredictRotations();

    private float lastSpookyYaw = 0f;
    private float lastSpookyPitch = 0f;
    private LivingEntity selected = null;
    private static final TimeUtil tickStopWatch = new TimeUtil();

    public AttackAura() {
        super("Attack Aura", "Бьет маму ирибы берцами", Category.Combat);
        addSettings(componentMode, smoothAimType, targets, correctionType, attackRange, lerpRotationDistance, disableOnDeath, ignoreEat, onlyWeapon, tpsSync, onlycrit, onlySpaceCritical, SHEIS, bypassWalls, ray);
    }

    @EventTarget
    public void onEvent(EventSwapWorld event) {
        reset();
    }


    final Map<Entity, Vector3d> previousPositions = new HashMap<>();

    public double getEntitySpeed(Entity entity) {
        Vector3d currentPos = entity.getPositionVec(); // spasibo xorosho eby matb obessal dlc
        Vector3d previousPos = previousPositions.getOrDefault(entity, currentPos);

        double dx = currentPos.x - previousPos.x;
        double dz = currentPos.z - previousPos.z;
        double speed = Math.sqrt(dx * dx + dz * dz) * 20.0;

        previousPositions.put(entity, currentPos);

        return speed;
    }


    @EventTarget
    public void onEvent(EventInput event) {
        if (TargetPearl.isRotationActive()) {
            return;
        }
        if (RotationComponent.getInstance().isRotating() || SmoothRotationComponent.getInstance().isRotating() && target != null) {
            if (this.correctionType.is("Свободная")) {
                MoveUtil.fixMovement(event, LookHandler.getFreeYaw());
            } else if (this.correctionType.is("Таргет")) {
                Vector3d targetVector = target.getPositionVec();
                new Speed();
                if (SkyCore.getInstance().getModuleManager().getModule(Speed.class).isEnabled() && this.getEntitySpeed(target) > (double)5.0F) {
                    targetVector = target.getPositionVec().add(target.getForward().normalize().scale((double)2.0F));
                }

                if (!MoveComponent.isStop()) {
                    Minecraft var10002 = mc;
                    MoveUtil.moveToPosition(event, targetVector, Minecraft.player.rotationYaw);
                }
            } else {
                Vector3d direction = AuraUtil.getClosestVec(target);
                float targetYaw = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(direction.z, direction.x)) - (double)90.0F);
                MoveUtil.fixMovement(event, targetYaw);
            }
        }

        if (this.legitSprintResetTicks > 0) {
            event.setForward(0.0F);
            event.setStrafe(0.0F);
            --this.legitSprintResetTicks;
        }

    }

    @EventTarget
    public void onEvent(EventInteract event) {
        if (target != null) event.setCancelled(true);
    }

    @EventTarget
    private void attack(EventAttack e) {
        if (target == null) return;
        ElytraComponent.attack(e);
    }

    @EventTarget
    public void onEvent(EventWillLand event) {
        if (target == null) {
            canCrit = false;
            return;
        }
        if (SkyCore.getInstance().getModuleManager().getModule(AirStuck.class).isEnabled()) {
            canCrit = true;
            return;
        }
        if (mc.player.fallDistance > 0.1F && event.isWillLand()) {
            canCrit = false;
        } else {
            canCrit = true;
        }
    }

    @EventTarget
    public void onEvent(EventUpdate e) {
        if (disableOnDeath.get() && !mc.player.isAlive()) {
            toggle();
            return;
        }

        if (target == null || !this.isValidTarget(target)) {
            target = this.findTarget();
        }
        if (target == null || mc.player == null || mc.world == null) {
            prev = null;
            reset();
            return;
        }

        if (target != null) {
            RotationAccess.updateTarget(target);
            ElytraComponent.smartPredict();
            ElytraComponent.processTargetLogic(SkyCore.getInstance().getModuleManager().getElytraTarget());
        }


        ElytraComponent.updateDefensiveState(target);
        if (SkyCore.getInstance().getModuleManager().getModule(AirStuck.class).isEnabled()) {
            canCrit = true;
        }


        if (!SkyCore.getInstance().getModuleManager().getModule(PacketCriticals.class).isEnabled() && target != null) {
            if (canAttack() && onlycrit.get() && cps <= System.currentTimeMillis()) {
                updateAttack();
                cps = System.currentTimeMillis() + 460L;
            }
        }

        // ИСПРАВЛЕНИЕ: Проверка ray НЕ РАБОТАЕТ на элитрах
        if (componentMode.is("ReallyWorld") && ray.get() && target != null) {
            boolean isElytraFlying = mc.player.isElytraFlying();

            if (!isElytraFlying && RayTraceUtil.rayTraceSingleEntity(mc.player.rotationYaw, mc.player.rotationPitch, attackDistance(), target)) {
                rayTicks++;
            } else if (!isElytraFlying) {
                rayTicks = 0;
            }
            // Если игрок на элитрах - rayTicks НЕ ИЗМЕНЯЕТСЯ (проверка наведения отключена)
        } else {
            rayTicks = 0;
        }

        if (shouldBlockAttack()) return;

        if (componentMode.is("ReallyWorld")) {
            if (ray.get()) {
                // ИСПРАВЛЕНИЕ: Атака происходит без проверки ray на элитрах
                boolean isElytraFlying = mc.player.isElytraFlying();
                if (isElytraFlying) {
                    // На элитрах игнорируем rayTicks и атакуем
                    updateAttack();
                } else if (rayTicks > 1) {
                    updateAttack();
                }
            } else {
                updateAttack();
            }
        } else {
            updateAttack();
        }

        updateVisualReverseYawState();
    }

    private void updateVisualReverseYawState() {
        visualReverseYaw = false;
        if (mc.player == null || target == null) {
            visualReverseWasActive = false;
            return;
        }
        ElytraTarget elytraTargetModule = SkyCore.getInstance().getModuleManager().getElytraTarget();
        if (elytraTargetModule == null || !elytraTargetModule.isEnabled() || !elytraTargetModule.usesVisualReverseLogic() || !elytraTargetModule.visualReverse.get()) {
            visualReverseWasActive = false;
            return;
        }

        if (!elytraTargetModule.shouldTarget(target)) {
            visualReverseWasActive = false;
            return;
        }

        float distanceToTarget = mc.player.getDistance(target);
        float enableDistance = 2.5F;
        float predictDistance = Math.max(enableDistance, elytraTargetModule.distance.get());
        float disableDistance = predictDistance;

        if (!visualReverseWasActive && distanceToTarget <= enableDistance) {
            visualReverseWasActive = true;
        }

        if (visualReverseWasActive && distanceToTarget >= disableDistance) {
            visualReverseWasActive = false;
        }

        visualReverseYaw = visualReverseWasActive;
    }

    public static float getVisualYaw(float yaw) {
        return visualReverseYaw ? MathHelper.wrapDegrees(yaw + 180.0F) : yaw;
    }


    @EventTarget
    public void onEvent(EventPacket event) {
        IPacket<?> packet = event.getPacket();
        if (packet instanceof CHeldItemChangePacket) {
            perfectDelay.reset(650L);
        } else if (packet instanceof CAnimateHandPacket) {
            perfectDelay.reset(500L);
        }
    }


    @EventTarget
    public void Event(EventPostUpdate event) {

        if (SkyCore.getInstance().getModuleManager().getModule(PacketCriticals.class).isEnabled() && target != null) {


            if (canAttack() && onlycrit.get() && (mc.player.isPotionActive(Effects.SLOW_FALLING) && cps <= System.currentTimeMillis())) {
                updateAttack();
                cps = System.currentTimeMillis() + 460L;
            }
        }
    }


    @EventTarget
    public void onEvent(EventGameUpdate event) {
        if (target != null) {
            ElytraComponent.setVector(ElytraComponent.getVector3d(mc.player, target));
        }

        if (mc.player == null || mc.world == null) return;

        if (target == null || mc.player == null || mc.world == null) {
            reset();
            return;
        }

        if (shouldBlockRotationUpdate()) return;
        updateRotation();


    }


    private float[] prev = null;

    private float smoothYaw = 0;
    private float smoothPitch = 0;

    private void updateRotation() {
        double maxHeight = (AuraUtil.getStrictDistance((target)) / attackDistance());
        Vector3d vec = target.getPositionVec()
                .add(0, clamp(mc.player.getEyePosition(mc.getRenderPartialTicks()).y - target.getPosY(), 0, maxHeight), 0)
                .subtract(mc.player.getEyePosition(mc.getRenderPartialTicks()))
                .normalize();

        float rawYaw = (float) toDegrees(Math.atan2(-vec.x, vec.z));
        float rawPitch = (float) clamp(-toDegrees(Math.atan2(vec.y, hypot(vec.x, vec.z))), -90F, 90F);

        float speed = new SecureRandom().nextBoolean() ? randomLerp(0.3F, 0.4F) : randomLerp(0.5F, 0.6F);

        float cos = (float) Math.cos(System.currentTimeMillis() / 70D);
        float sin = (float) Math.sin(System.currentTimeMillis() / 115D);
        float cosF = (float) Math.cos(System.currentTimeMillis() / 44D);

        float yawF = (float) Math.ceil(randomLerp(25F, 35) * cosF);
        float yaw = (float) Math.ceil(randomLerp(1F, 3) * cos);
        float pitch = (float) Math.ceil(randomLerp(1F, 2) * sin);
        float pitchF = (float) Math.ceil(randomLerp(7F, 15) * sin);

        if (componentMode.is("Обычный")) {
            int suck = count % 3;
            float random = stopWatch.getElapsedTime() / 40F + (count % 6);
            Rotation randomAngle = switch (suck) {
                case 0 -> new Rotation((float) Math.cos(random), (float) Math.sin(random));
                case 1 -> new Rotation((float) Math.sin(random), (float) Math.cos(random));
                case 2 -> new Rotation((float) Math.sin(random), (float) -Math.cos(random));
                default -> new Rotation((float) -Math.cos(random), (float) Math.sin(random));
            };

            float yawadd = randomLerp(3, 5) * randomAngle.getYaw();
            float pitch2 = randomLerp(0, 2) * (float) Math.cos((double) System.currentTimeMillis() / 5000);
            float pitchadd = randomLerp(2, 4) * randomAngle.getPitch() + pitch2;
            if (canCrit) pitchadd = yawadd = 0;
            float addition = (1F - cooldownFromLastSwing()) * (randomLerp(20, 40));
            yaw = (canCrit ? 0 : 19.23253f) * (count % 2 == 0 ? -1 : 1) + addition * (count % 2 == 0 ? -1 : 1) + yawadd;
            pitch = (-addition + pitchadd);
        }

        lerpRotation = new Vector2f(wrapLerp(speed, lerpRotation.x, rawYaw + yaw), wrapLerp(speed / 2F, lerpRotation.y, clamp(rawPitch + pitch, -90F, 90F)));
        lerpRot = new Vector2f(wrapLerp(speed, lerpRot.x, LookHandler.getFreeYaw() + yawF), wrapLerp(speed / 2F, lerpRot.y, clamp(pitchF, -90F, 90F)));
        lerprRot = new Vector2f(wrapLerp(speed, lerprRot.x, RotationUtil.calculateLimitedAim(target, 12).x + yaw), wrapLerp(speed, lerprRot.y, RotationUtil.calculateLimitedAim(target, 12).y + pitch));

        Rotation rRot = new Rotation((mc.player.rotationYaw + (float) Math.ceil(lerprRot.x - mc.player.rotationYaw)), (mc.player.rotationPitch + (float) Math.ceil(lerprRot.y - mc.player.rotationPitch)));
        Rotation rRotka = new Rotation(!canFTlerpRotation() ? LookHandler.getFreeYaw() : (mc.player.rotationYaw + (float) Math.ceil(lerprRot.x - mc.player.rotationYaw)), !canFTlerpRotation() ? LookHandler.getFreePitch() : (mc.player.rotationPitch + (float) Math.ceil(lerprRot.y - mc.player.rotationPitch)));
        Rotation rotation = new Rotation(mc.player.rotationYaw + (float) Math.ceil(lerpRotation.x - mc.player.rotationYaw), mc.player.rotationPitch + (float) Math.ceil(MathHelper.wrapDegrees(lerpRotation.y) - MathHelper.wrapDegrees(mc.player.rotationPitch)));


        float fov = (float) AuraUtil.calculateFOVFromCamera(target);
        float baseFov = 360;
        float sign = wrapDegrees(rotation.getYaw() - wrapDegrees(mc.player.rotationYaw));
        yawDelta = ((rotation.getYaw() - mc.player.rotationYaw) % 360 + 540) % 360 - 180;

        if (Math.abs(fov) < baseFov) {

            AxisAlignedBB box = target.getBoundingBox();
            Vector3d eyes = mc.player.getEyePosition(1.0f);
            Vector3d centerPoint = box.getCenter().add(0, randomOffsetY, 0);
            Vector3d toTarget = centerPoint.subtract(eyes);
            Vector2f targetRot;
            int forwardTicks = 3;

            if (componentMode.is("FunTime")) {
                Rotation rotacia = rotka(new Rotation(mc.player.rotationYaw, mc.player.rotationPitch), rRotka);
                float speedY = 15;
                if (canFTlerpRotation()) speedY = 360;
                RotationComponent.update(rotacia, speedY, speedY, 45, 45, 10, 5, false);
            }

            if (componentMode.is("LonyGrief")) {
                if (mc.player.isElytraFlying()) {
                    ElytraTarget elytraTarget = SkyCore.getInstance().getModuleManager().getElytraTarget();

                    //   float centerYaw1 = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(xuesos.z, xuesos.x)) - 90);
//float centerPitch1 = (float) (-Math.toDegrees(Math.atan2(xuesos.y, Math.hypot(xuesos.x, xuesos.z))));

                    //     targetRot = new Vector2f(centerYaw1, centerPitch1);
                } else {
                    updateLonyGriefRotation(target);
                }
            }


            if (componentMode.is("SmooDth")) {
                RotationComponent.update(rRot, 43, 8, 35, 23, 1, 5, false);
            }

            if (componentMode.is("SpookyTime")) {
                Vector2f spookyRot = calculateSpookyRotation(new Vector2f(mc.player.rotationYaw, mc.player.rotationPitch), true);

                float finalYaw = spookyRot.x;
                float finalPitch = spookyRot.y;

                lerpRotation = new Vector2f(finalYaw, finalPitch);

                Rotation rotation2 = new Rotation(
                        mc.player.rotationYaw + (float) Math.ceil(MathHelper.wrapDegrees(finalYaw) - MathHelper.wrapDegrees(mc.player.rotationYaw)),
                        mc.player.rotationPitch + (float) Math.ceil(MathHelper.wrapDegrees(finalPitch) - MathHelper.wrapDegrees(mc.player.rotationPitch))
                );

                SmoothRotationComponent.update(rotation2, 3F, 10F, 4F, 19F, 1, 0, false);
            }
        }
        Vector3d vector3d = AuraUtil.getClosestVec(target);
        if (componentMode.is("ReallyWorld")) {
            if (mc.player.isElytraFlying()) {
                ElytraComponent.updateRotation(new Vector2f(mc.player.rotationYaw, mc.player.rotationPitch));
            } else {
                if (smoothAimType.is("Резкий")) {
                    // Резкий - как в Grim режиме
                    Vector3d vector3dSmooth = AuraUtil.getClosestVec(target);
                    grimSmooth(vector3dSmooth);
                } else {
                    // Плавный - стандартный fastRotation
                    fastRotation();
                }
            }
        }

        if (componentMode.is("Grim")) {
            if (mc.player.isElytraFlying()) {
                ElytraComponent.updateRotation(new Vector2f(mc.player.rotationYaw, mc.player.rotationPitch));
            } else {
                Grim(vector3d);
            }
        }

        if (componentMode.is("SunWay")) {
            if (mc.player.isElytraFlying()) {
                ElytraComponent.updateRotation(new Vector2f(mc.player.rotationYaw, mc.player.rotationPitch));
            } else {
                sunwayRotation(vector3d);
            }
        }

        rotate = new Vector2f(lerpRotation.x, lerpRotation.y);
    }

    private void grimSmooth(Vector3d vector3d) {
        if (target == null || mc.player == null || mc.world == null) {
            return;
        }

        float targetYaw = (float) wrapDegrees(Math.toDegrees(Math.atan2(vector3d.z, vector3d.x)) - 90);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(vector3d.y, hypot(vector3d.x, vector3d.z))));

        float finalYaw, finalPitch;

        if (canAttack() && vector3d.length() <= attackRange.get()) {
            finalYaw = targetYaw;
            finalPitch = targetPitch;
        } else {
            finalYaw = LookHandler.getFreeYaw();
            finalPitch = LookHandler.getFreePitch();
        }

        RotationComponent.update(new Rotation(finalYaw, finalPitch), 255, 1, 6);
    }

    public static Rotation rotation(AttackAura aura) {
        AxisAlignedBB box = target.getBoundingBox();
        Vector3d eyes = mc.player.getEyePosition(1.0f);
        Vector3d centerPoint = box.getCenter().add(0, randomOffsetY, 0);

        Vector3d toTarget = centerPoint.subtract(eyes);
        Vector2f targetRot;
        int forwardTicks = 3;

        Vector3d xuesos = NewPredictRotations.predict(target, target.getBoundingBox().getCenter().subtract(eyes), forwardTicks);

        if (mc.player.isElytraFlying() && target.isElytraFlying()) {
            float centerYaw1 = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(xuesos.z, xuesos.x)) - 90);
            float centerPitch1 = (float) (-Math.toDegrees(Math.atan2(xuesos.y, Math.hypot(xuesos.x, xuesos.z))));

            targetRot = new Vector2f(centerYaw1, centerPitch1);

        } else {
            float centerYaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(toTarget.z, toTarget.x)) - 90);
            float centerPitch = (float) (-Math.toDegrees(Math.atan2(toTarget.y, Math.hypot(toTarget.x, toTarget.z))));
            targetRot = new Vector2f(centerYaw, centerPitch);
        }


        float deltaYaw;
        float deltaPitch;
        float smooth;
        float newYaw;
        Rotation smoothRot;
        float newPitch;

        if (mc.player.isElytraFlying() && target.isElytraFlying()) {
            if (isBack) {
                if (acceleration >= -0.02F) {
                    acceleration -= Math.abs(MathHelper.wrapDegrees(targetRot.x - lastYaw)) > 80.0F ? 0.1F : 0.01F;
                }

                if (acceleration <= -0.02F) {
                    isBack = false;
                    updateRandomOffset();
                }
            } else {
                acceleration += 0.0105F + (Math.random() * 0.02f - 0.001f);
                if (acceleration >= 0.305F || RayTraceUtil.rayTraceSingleEntity(mc.player.rotationYaw, mc.player.rotationPitch, 1488.0D, target)) {
                    isBack = true;
                }
            }
        } else if (isBack) {
            if (acceleration >= -0.15F) {
                float slowdownspeed = Math.abs(MathHelper.wrapDegrees(targetRot.x - lastYaw)) > 80.0F ? 0.1F : 0.01F;
                slowdownspeed *= (0.9 + Math.random() * 0.2f);
                acceleration -= slowdownspeed;
            }

            if (acceleration <= -0.15F) {
                isBack = false;
                updateRandomOffset();
            }
        } else {
            float accelSpeed = (float) (0.86f + (Math.random() * 0.02f - 0.001f));
            acceleration += accelSpeed;
            float threshold = 0.185f + ((float) Math.random() * 0.03f - 0.012f);
            if (acceleration >= threshold || RayTraceUtil.rayTraceSingleEntityWithCustomBox(mc.player.rotationYaw, mc.player.rotationPitch, 1488.0D, target, 0)) {
                isBack = true;
            }
        }

        deltaYaw = MathHelper.wrapDegrees(targetRot.x - lastYaw);
        deltaPitch = targetRot.y - lastPitch;
        smooth = Math.max(acceleration, 0.0F);
        newYaw = lastYaw + deltaYaw * Math.min(Math.max(smooth, 0.0F), 1.0F);
        newPitch = (float) (lastPitch + deltaPitch * Math.min(Math.max(smooth / (2 * (Math.random() * 0.02f)), 0.0F), 1.0F));
        newYaw -= (newYaw - lastYaw) % SensUtil.getGCDValue();
        newPitch -= (newPitch - lastPitch) % SensUtil.getGCDValue();
        smoothRot = new Rotation(newYaw, newPitch);


        lastYaw = smoothRot.getYaw();
        lastPitch = smoothRot.getPitch();
        return smoothRot;
    }

    public static void updateRandomOffset() {
        randomOffsetX = (float) (Math.random() * 0.2f - 0.1f);
        randomOffsetY = (float) (Math.random() * 0.6f - 0.1f);
        randomOffsetZ = (float) (Math.random() * 0.2f - 0.1f);
    }

    private void resetLonyGriefRotation(float currentYaw, float currentPitch) {
        lonyReverseSmooth = false;
        lonySmooth = 0.0F;
        lonyLastYaw = currentYaw;
        lonyLastPitch = currentPitch;
    }

    private void updateLonyGriefRotation(LivingEntity target) {
        if (mc.player == null || target == null) {
            return;
        }

        if (lonyLastTarget != target) {
            resetLonyGriefRotation(mc.player.rotationYaw, mc.player.rotationPitch);
            lonyLastTarget = target;
        }

        AxisAlignedBB box = target.getBoundingBox();
        Vector3d eyePos = mc.player.getEyePosition(1.0F);
        Vector3d center = box.getCenter();
        Vector3d toTarget = center.subtract(eyePos);

        float targetYaw = MathHelper.wrapDegrees(
                (float) Math.toDegrees(Math.atan2(toTarget.z, toTarget.x)) - 90.0F
        );
        float targetPitch = (float) -Math.toDegrees(
                Math.atan2(toTarget.y, Math.hypot(toTarget.x, toTarget.z))
        );

        Vector3d lookVec = mc.player.getVectorForRotation(
                mc.player.rotationPitch,
                mc.player.rotationYaw
        );
        Vector3d rayEnd = eyePos.add(lookVec.scale(999.0D));
        boolean lookingIntoBox = box.shrink(0.5D).rayTrace(eyePos, rayEnd).isPresent();

        if (lonyReverseSmooth) {
            if (lonySmooth >= -0.01F) {
                lonySmooth -= Math.abs(MathHelper.wrapDegrees(targetYaw - lonyLastYaw)) > 80.0F
                        ? 0.1F
                        : 0.01F;
            }
            if (lonySmooth <= -0.01F) {
                lonyReverseSmooth = false;
            }
        } else {
            lonySmooth += 0.005F;
            if (lonySmooth >= 0.22F || lookingIntoBox) {
                lonyReverseSmooth = true;
            }
        }

        float yawDelta = MathHelper.wrapDegrees(targetYaw - lonyLastYaw);
        float pitchDelta = targetPitch - lonyLastPitch;
        float positiveSmooth = Math.max(lonySmooth, 0.0F);

        float newYaw = lonyLastYaw + yawDelta * MathHelper.clamp(positiveSmooth * 10.3F, 0.0F, 1.0F);
        float newPitch = lonyLastPitch + pitchDelta * MathHelper.clamp(positiveSmooth / 10.7F, 0.0F, 1.0F);

        float gcd = getLonyMouseGcd();
        newYaw -= (newYaw - lonyLastYaw) % gcd;
        newPitch -= (newPitch - lonyLastPitch) % gcd;
        newPitch = MathHelper.clamp(newPitch, -89.0F, 89.0F);

        lonyLastYaw = newYaw;
        lonyLastPitch = newPitch;
        lerpRotation = new Vector2f(newYaw, newPitch);

        RotationComponent.update(new Rotation(newYaw, newPitch), 360.0F, 360.0F, 0.0F, 5.0F, 1, 5, false);
    }

    private float getLonyMouseGcd() {
        double sens = mc.gameSettings.mouseSensitivity;
        double value = sens * 0.6D + 0.2D;
        return (float) (Math.pow(value, 3.0D) * 0.8D) * 0.15F;
    }

    private boolean isTargetVisible(LivingEntity target) {
        if (target == null || mc.player == null) return false;

        Vector3d eyesPos = mc.player.getEyePosition(1.0F);
        Vector3d targetPos = target.getPositionVec().add(0, target.getHeight() / 2, 0);

        RayTraceContext context = new RayTraceContext(
                eyesPos,
                targetPos,
                RayTraceContext.BlockMode.OUTLINE,
                RayTraceContext.FluidMode.NONE,
                mc.player
        );

        BlockRayTraceResult result = mc.world.rayTraceBlocks(context);
        return result == null || result.getType() == RayTraceResult.Type.MISS;
    }


    private float hitShakeIntensity = 0f;
    private float shakeIncreaseSpeed = 0.15f;
    private float shakeDecreaseSpeed = 0.1f;
    private float deltaTime = 0.05f;
    private boolean isHitting = false;

    private void LegitRotation(LivingEntity target) {
        if (target == null || mc.player == null) return;


        if (!isTargetVisible(target)) {
            return;
        }

        Vector3d eyePos = mc.player.getEyePosition(1.0F);
        Vector3d lookVec = mc.player.getLook(1.0F);

        AxisAlignedBB innerBox = target.getBoundingBox().grow(-0.15);

        double dist = eyePos.distanceTo(target.getPositionVec().add(0, target.getHeight() / 2.0, 0));
        Vector3d projectedView = eyePos.add(lookVec.scale(dist));


        double shakeX = 0, shakeY = 0, shakeZ = 0;

        if (isHitting) {
            hitShakeIntensity = Math.min(1.0f, hitShakeIntensity + shakeIncreaseSpeed * deltaTime);
        } else {
            hitShakeIntensity = Math.max(0.0f, hitShakeIntensity - shakeDecreaseSpeed * deltaTime);
        }


        if (hitShakeIntensity > 0) {
            double time = System.currentTimeMillis() / 100.0;


            double bodyShakeX = Math.sin(time * 8) * 0.1 * hitShakeIntensity;
            double bodyShakeY = Math.cos(time * 7) * 0.05 * hitShakeIntensity;
            double bodyShakeZ = Math.sin(time * 9) * 0.1 * hitShakeIntensity;


            double headShakeX = (Math.sin(time * 15) * 0.3 + Math.cos(time * 12) * 0.2) * hitShakeIntensity;
            double headShakeY = (Math.sin(time * 20) * 0.2 + Math.cos(time * 18) * 0.15) * hitShakeIntensity;
            double headShakeZ = (Math.sin(time * 17) * 0.25 + Math.cos(time * 14) * 0.2) * hitShakeIntensity;


            shakeX = bodyShakeX + headShakeX * 0.5;
            shakeY = bodyShakeY + headShakeY * 0.5;
            shakeZ = bodyShakeZ + headShakeZ * 0.5;


            if (ThreadLocalRandom.current().nextFloat() < 0.1f * hitShakeIntensity) {
                shakeX *= 2.5;
                shakeY *= 2.5;
                shakeZ *= 2.5;
            }
        }

        double x = clamp(projectedView.x + shakeX, innerBox.minX, innerBox.maxX);
        double y = clamp(projectedView.y + shakeY, innerBox.minY, innerBox.maxY);
        double z = clamp(projectedView.z + shakeZ, innerBox.minZ, innerBox.maxZ);

        Vector3d targetPoint = new Vector3d(x, y, z);
        Vector3d diff = targetPoint.subtract(eyePos);

        float neededYaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0F;
        float neededPitch = (float) (-Math.toDegrees(Math.atan2(diff.y, Math.hypot(diff.x, diff.z))));

        float yawDiff = wrapDegrees(neededYaw - lerpRotation.x);
        float pitchDiff = neededPitch - lerpRotation.y;

        float minSpeed = 0.1f;
        float maxSpeed = 0.6f;
        float speed = minSpeed + ThreadLocalRandom.current().nextFloat() * (maxSpeed - minSpeed);

        lerpRotation.x += yawDiff * speed;
        lerpRotation.y += pitchDiff * speed;
        lerpRotation.y = clamp(lerpRotation.y, -90f, 90f);

        float gcd = SensUtil.getGCDValue();
        lerpRotation.x -= (lerpRotation.x - mc.player.rotationYaw) % gcd;
        lerpRotation.y -= (lerpRotation.y - mc.player.rotationPitch) % gcd;
        SmoothRotationComponent.update(new Rotation(lerpRotation.x, lerpRotation.y), 2, 3F, 2F, 4F, 1, 0, false);


    }

    private Vector2f calculateSpookyRotation(Vector2f currentRot, boolean attack) {
        if (target == null) return currentRot;

        float gcd = SensUtil.getGCDValue();

        float neckHeight = target.getEyeHeight() - 0.3f;
        Vector3d targetPos = target.getPositionVec().add(0, neckHeight, 0);

        if (selected == target) {
            float randomOffsetX = (new Random().nextFloat() - 0.5f) * 0.1f;
            float randomOffsetZ = (new Random().nextFloat() - 0.5f) * 0.1f;
            targetPos = targetPos.add(randomOffsetX, 0, randomOffsetZ);
        }

        Vector3d vecToNeck = targetPos.subtract(mc.player.getEyePosition(1.0F));
        float yawToTarget = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vecToNeck.z, vecToNeck.x)) - 90.0);
        float pitchToTarget = (float) (-Math.toDegrees(Math.atan2(vecToNeck.y, Math.hypot(vecToNeck.x, vecToNeck.z))));

        float yawDelta = MathHelper.wrapDegrees(yawToTarget - currentRot.x);
        float pitchDelta = MathHelper.wrapDegrees(pitchToTarget - currentRot.y);

        float clampedYaw = Math.min(Math.max(Math.abs(yawDelta), 1.0E-4F), 22.5F);
        float clampedPitch = Math.min(Math.max(Math.abs(pitchDelta), 1.0E-4F), 17.0F);

        float randomYawFactor = (float) (Math.random() * 2.5 - 1.5);
        float randomPitchFactor = (float) (Math.random() * 2.5 - 1.0);
        float randomThreshold = (float) (Math.random() * 2.5);
        float randomAddition = (float) (Math.random() * 3.5 + 2.5);

        if (selected != target) {
            clampedPitch = Math.max(Math.abs(pitchDelta), 1.0F);
        } else {
            clampedPitch /= 3.0F;
        }

        if (Math.abs(clampedYaw - lastSpookyYaw) <= randomThreshold) {
            clampedYaw = lastSpookyYaw + randomAddition;
        }

        clampedYaw += randomYawFactor;
        clampedPitch += randomPitchFactor;

        float yaw = currentRot.x + (yawDelta > 0.0F ? clampedYaw : -clampedYaw);
        float pitch = MathHelper.clamp(currentRot.y + (pitchDelta > 0.0F ? clampedPitch : -clampedPitch), -80.0F, 70.0F);

        yaw -= (yaw - currentRot.x) % gcd;
        pitch -= (pitch - currentRot.y) % gcd;

        lastSpookyYaw = clampedYaw;
        lastSpookyPitch = clampedPitch;
        selected = target;

        return new Vector2f(yaw, pitch);
    }

    public float getAICooldown() {
        if (mc.player.getHeldItemMainhand().getItem() == Items.AIR) return 0.9f;

        if (mc.player.getHeldItemMainhand().getItem() instanceof AxeItem || mc.player.getHeldItemMainhand().getItem() instanceof ShovelItem)
            return 0.95f;
        return 0.93f;
    }


    private void fastRotation() {

        if (target == null || mc.player == null || mc.world == null) {
            return;
        }

        float currentYaw = mc.player.rotationYaw;
        float currentPitch = mc.player.rotationPitch;

        float deltaYaw = MathHelper.wrapDegrees(lerpRotation.x - currentYaw);
        float deltaPitch = MathHelper.wrapDegrees(lerpRotation.y - currentPitch);

        float newYaw = currentYaw + deltaYaw;
        float newPitch = currentPitch + deltaPitch;

        RotationComponent.update(new Rotation(newYaw, newPitch), 180, 180, 0, 5);
    }

    private void sunwayRotation(Vector3d vector3d) {
        if (target == null || mc.player == null || mc.world == null) {
            return;
        }

        float targetYaw = (float) wrapDegrees(Math.toDegrees(Math.atan2(vector3d.z, vector3d.x)) - 90);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(vector3d.y, hypot(vector3d.x, vector3d.z))));

        SecureRandom random = new SecureRandom();

        float fallDistance = mc.player.fallDistance;

        float randomYawOffset = (random.nextFloat() - 0.5f) * (2.0f + fallDistance * 0.5f);
        float randomPitchOffset = (random.nextFloat() - 0.5f) * (1.5f + fallDistance * 0.3f);

        float yawSpeed = 412315.0f + random.nextFloat() * 41230.0f + fallDistance * 1234.0f;
        float pitchSpeed = 412310.0f + random.nextFloat() * 11232.0f + fallDistance * 1234.5f;

        if (fallDistance > 0.5f) {
            randomYawOffset += (random.nextFloat() - 0.5f) * 3.0f;
            randomPitchOffset += (random.nextFloat() - 0.5f) * 2.0f;
        }

        if (fallDistance > 1.0f) {
            yawSpeed += random.nextFloat() * 5.0f;
            pitchSpeed += random.nextFloat() * 3.0f;
        }

        float currentYaw = mc.player.rotationYaw;
        float currentPitch = mc.player.rotationPitch;

        float deltaYaw = MathHelper.wrapDegrees((targetYaw + randomYawOffset) - currentYaw);
        float deltaPitch = MathHelper.wrapDegrees((targetPitch + randomPitchOffset) - currentPitch);

        float gcd = SensUtil.getGCDValue();

        float newYaw = currentYaw + deltaYaw;
        float newPitch = MathHelper.clamp(currentPitch + deltaPitch, -90.0f, 90.0f);

        newYaw -= (newYaw - currentYaw) % gcd;
        newPitch -= (newPitch - currentPitch) % gcd;

        RotationComponent.update(new Rotation(newYaw, newPitch), yawSpeed, pitchSpeed, 0, 5);
    }


    private void Grim(Vector3d vector3d) {
        if (canAttack() && vector3d.length() <= attackRange.get()) tickssnap = 1;
        float finalyaw, finalpitch;
        float targetYaw = (float) wrapDegrees(Math.toDegrees(Math.atan2(vector3d.z, vector3d.x)) - 90);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(vector3d.y, hypot(vector3d.x, vector3d.z))));
        if (tickssnap > 0) {
            finalyaw = targetYaw;
            finalpitch = targetPitch;
        } else {
            finalyaw = LookHandler.getFreeYaw();
            finalpitch = LookHandler.getFreePitch();
        }
        RotationComponent.update(new Rotation(finalyaw, finalpitch), 255, 1, 6);
        if (tickssnap > 0) tickssnap--;
    }

    @Native
    private Rotation rotka(Rotation currentAngle, Rotation targetAngle) {
        int count = counter;
        float angleYaw = MathHelper.wrapDegrees(targetAngle.getYaw() - currentAngle.getYaw());
        float anglePitch = MathHelper.wrapDegrees(targetAngle.getPitch() - currentAngle.getPitch());
        Rotation angleDelta = new Rotation(angleYaw, anglePitch);
        float yawDelta = angleDelta.getYaw(), pitchDelta = angleDelta.getPitch();
        float rotationDifference = (float) hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

        if (canFTlerpRotation()) {
            float speed = canCrit ? 1 : new SecureRandom().nextBoolean() ? 0.2F : 0.1F;

            float lineYaw = (Math.abs(yawDelta / rotationDifference) * 180);
            float linePitch = (Math.abs(pitchDelta / rotationDifference) * 180);

            float moveYaw = clamp(yawDelta, -lineYaw, lineYaw);
            float movePitch = clamp(pitchDelta, -linePitch, linePitch);

            Rotation moveAngle = new Rotation(currentAngle.getYaw(), currentAngle.getPitch());
            moveAngle.setYaw(MathHelper.lerp(randomLerp(speed, speed + 0.2F), currentAngle.getYaw(), currentAngle.getYaw() + moveYaw));
            moveAngle.setPitch(MathHelper.lerp(randomLerp(speed, speed + 0.2F), currentAngle.getPitch(), currentAngle.getPitch() + movePitch));

            return moveAngle;
        } else {
            int suck = count % 3;
            float speed = stopWatch.finished(400) ? new SecureRandom().nextBoolean() ? 0.4F : 0.2F : -0.2F;
            float random = stopWatch.getElapsedTime() / 40F + (count % 6);

            Rotation randomAngle = switch (suck) {
                case 0 -> new Rotation((float) Math.cos(random), (float) Math.sin(random));
                case 1 -> new Rotation((float) Math.sin(random), (float) Math.cos(random));
                case 2 -> new Rotation((float) Math.sin(random), (float) -Math.cos(random));
                default -> new Rotation((float) -Math.cos(random), (float) Math.sin(random));
            };

            float yaw = randomLerp(5, 8) * randomAngle.getYaw();
            float pitch2 = randomLerp(0, 2) * (float) Math.cos((double) System.currentTimeMillis() / 5000);
            float pitch = randomLerp(2, 6) * randomAngle.getPitch() + pitch2;

            float lineYaw = (Math.abs(yawDelta / rotationDifference) * 180);
            float linePitch = (Math.abs(pitchDelta / rotationDifference) * 180);

            float moveYaw = clamp(yawDelta, -lineYaw, lineYaw);
            float movePitch = clamp(pitchDelta, -linePitch, linePitch);

            Rotation moveAngle = new Rotation(currentAngle.getYaw(), currentAngle.getPitch());
            moveAngle.setYaw(MathHelper.lerp(clamp(randomLerp(speed, speed + 0.2F), 0, 1), currentAngle.getYaw(), currentAngle.getYaw() + moveYaw) + yaw);
            moveAngle.setPitch(MathHelper.lerp(clamp(randomLerp(speed, speed + 0.2F), 0, 1), currentAngle.getPitch(), currentAngle.getPitch() + movePitch) + pitch);

            return moveAngle;
        }
    }


    private boolean canFTlerpRotation() {
        if (!perfectDelay.cooldownComplete() || !cooldownComplete()) return false;

        boolean isBlockAboveHead = PlayerUtil.isBlockAboveHead();

        boolean isInLiquid = mc.player.isActualySwimming() || mc.player.isSwimming() && mc.player.areEyesInFluid(FluidTags.WATER) || mc.player.areEyesInFluid(FluidTags.LAVA);

        if (isInLiquid) return true;
        boolean canDefaultCrit = (!mc.player.isOnGround() && stopWatch.finished(400)) || (mc.player.fallDistance >= 0 && canCrit && mc.player.getMotion().y == 0) || (mc.player.fallDistance > 0 && canCrit) || (isBlockAboveHead && mc.player.fallDistance >= 0 && !mc.player.isOnGround());

        if (onlycrit.get()) {
            return shouldCritical() && canDefaultCrit;
        }

        return AuraUtil.getStrictDistance(target) < attackDistance();
    }

    @java.lang.Override
    public void onDisable() {
        super.onDisable();
        target = null;
        ElytraComponent.pos = Vector3d.ZERO;
        if (componentMode.is("FunTime")) {
            RotationComponent.update(new Rotation(LookHandler.getFreeYaw(), LookHandler.getFreePitch()), 20, 20, 30, 30, 0, 30, false);
        }
        resetTargetRotationState();
        prev = null;
        counter = 9;
        lerpRotation = Vector2f.ZERO;
        lerpRot = Vector2f.ZERO;
    }

    //TYCAPIDOR1337
    @java.lang.Override
    public void onEnable() {
        super.onEnable();
        lerpRotation = new Vector2f(mc.player.rotationYaw, mc.player.rotationPitch);
        lerpRot = new Vector2f(mc.player.rotationYaw, mc.player.rotationPitch);
        lastYaw = mc.player.rotationYaw;
        lastPitch = mc.player.rotationPitch;
        acceleration = 0.0F;
        isBack = false;
        lonyLastTarget = null;
        resetLonyGriefRotation(mc.player.rotationYaw, mc.player.rotationPitch);
    }


    public float wrapLerp(float step, float input, float target) {
        return input + step * MathHelper.wrapDegrees(target - input);
    }

    public float randomLerp(float min, float max) {
        return MathHelper.lerp(new SecureRandom().nextFloat(), min, max);
    }

    public float cooldownFromLastSwing() {
        return clamp(mc.player.ticksSinceLastSwing / randomLerp(8, 12), 0.0F, 1.0F);
    }

    private void updateAttack() {
        Sprint autoSprint = (Sprint) SkyCore.getInstance().getModuleManager().getModule(Sprint.class);
        if (tickssnap > 0) return;
        if (canAttack() && AuraUtil.getStrictDistance(target) < attackDistance()) {
            boolean sprint = mc.player.serverSprintState && !mc.player.isInWater() && !mc.player.isInLava() && !mc.player.isSwimming();
            if (autoSprint != null && autoSprint.isEnabled() && autoSprint.getMode().is("Пакетный") && sprint) {
                legitSprintResetTicks = 1;
                if (mc.player.serverSprintState) {
                    mc.player.setServerSprintState(false);
                    mc.player.setSprinting(false);
                    mc.player.connection.sendPacket(new CEntityActionPacket(mc.player, CEntityActionPacket.Action.STOP_SPRINTING));
                }
            }


            if (bypassWalls.get() && !isBypassWallsBlockedByFluid()) {
                Vector3d startVec = mc.player.getEyePosition(mc.getRenderPartialTicks());
                Vector3d targetPos = target.getEyePosition(mc.getRenderPartialTicks());
                Vector3d direction = targetPos.subtract(startVec);
                double distance = direction.length();

                if (distance < 1.0E-3) return;

                Vector3d normalizedDir = direction.normalize();
                for (double i = 0; i < distance; i += 0.5) {
                    Vector3d point = startVec.add(normalizedDir.scale(i));
                    BlockPos pos = new BlockPos(point);
                    if (!mc.world.isAirBlock(pos)) {
                        mc.player.connection.sendPacket(new CPlayerDiggingPacket(CPlayerDiggingPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP));
                        mc.player.connection.sendPacket(new CPlayerDiggingPacket(CPlayerDiggingPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
                    }
                }
            }
            if (this.SHEIS.get() && mc.player.isBlocking()) {
                mc.playerController.onStoppedUsingItem(mc.player);
            }

            attackEntity(target);
            stopWatch.reset();

            count = (count + 1) % 2;
            counter++;
            canCrit = false;


        } else if (!shouldBlockRotationUpdate()
                && !mc.player.canEntityBeSeen(target)
                && ServerUtil.isConnectedToServer("holyworld")
                && mc.player.getServerBrand().contains("HolyWorld")) {
            RotationComponent.update(new Rotation(Rotation.cameraYaw(), 90), 360, 360, 0, 5);
        }
    }

    private boolean shouldBlockAttack() {
        boolean elytraPursuitActive = SkyCore.getInstance().getModuleManager().getElytraTarget().isEnabled()
                && target != null
                && mc.player.isElytraFlying()
                && target.isElytraFlying();

        boolean blockWhileEating = mc.player.isHandActive() && ignoreEat.get() && !elytraPursuitActive;
        boolean weaponCheckFailed = (!(mc.player.getHeldItemMainhand().getItem() instanceof AxeItem
                || mc.player.getHeldItemMainhand().getItem() instanceof SwordItem) && onlyWeapon.get());

        return blockWhileEating || weaponCheckFailed;
    }

    private boolean shouldBlockRotationUpdate() {
        if (TargetPearl.isRotationActive()) {
            return true;
        }
        return (!(mc.player.getHeldItemMainhand().getItem() instanceof AxeItem
                || mc.player.getHeldItemMainhand().getItem() instanceof SwordItem) && onlyWeapon.get());
    }

    private void attackEntity(Entity entity) {
        if (entity != target) {
            return;
        }
        mc.playerController.attackEntity(mc.player, entity);
        mc.player.swingArm(Hand.MAIN_HAND);
    }

    public boolean canAttack() {
        boolean ready = stopWatch.hasTimeElapsed(450) && mc.player.getCooledAttackStrength(tpsSync.get() ? TPSHandler.getAdjustTicks() : 1.5f) > getAICooldown();
        boolean air = mc.player.movementInput.jump || !mc.player.isOnGround();

        if (componentMode.is("SpookyTime")) {
            boolean spookyDelay = stopWatch.getElapsedTime() >= 350 + (counter % 10 == 0 ? 255 : 0);
            boolean spookyStrength = mc.player.getCooledAttackStrength(1.5F) >= 0.92F;
            if (!(spookyDelay && spookyStrength)) {
                return false;
            }
        }
        if (AuraUtil.isJumpBlockedByCeiling()) {
            return ready;
        } else if (SkyCore.getInstance().getModuleManager().getModule(PacketCriticals.class).isEnabled()) {
            return ready && (!onlycrit.get() || !mc.player.isValidAttackCondition() || (onlySpaceCritical.get() && !air) || mc.player.canCritical());
        } else {
            return ready && (!onlycrit.get() || !mc.player.isValidAttackCondition() || (onlySpaceCritical.get() && !air) || (mc.player.canCritical() && canCrit));
        }
    }

    private boolean shouldCritical() {
        boolean isDeBuffed = mc.player.isPotionActive(Effects.LEVITATION) || mc.player.isPotionActive(Effects.BLINDNESS) || mc.player.isPotionActive(Effects.SLOW_FALLING);
        boolean isInLiquid = mc.player.isActualySwimming() || mc.player.isSwimming() && mc.player.areEyesInFluid(FluidTags.WATER) || mc.player.areEyesInFluid(FluidTags.LAVA);
        boolean isFlying = mc.player.abilities.isFlying || mc.player.isElytraFlying();
        boolean isClimbing = mc.player.isOnLadder();
        boolean isCantJump = mc.player.isPassenger();
        boolean isOnWeb = PlayerUtil.isPlayerInWeb();

        return !(isInLiquid || isFlying || isClimbing || isCantJump || isOnWeb) || isDeBuffed;
    }

    private LivingEntity findTarget() {
        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        boolean scanPlayers = targets.is("Игроки") || targets.is("Голые") || targets.is("Друзья");
        if (scanPlayers) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player || !isValidTarget(player)) {
                    continue;
                }
                double distance = mc.player.getDistance(player);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = player;
                }
            }
        }

        boolean scanMobs = targets.is("Мобы") || targets.is("Животные");
        if (!scanMobs) {
            return nearest;
        }

        for (Entity entity : mc.world.getAllEntities()) {
            if (!(entity instanceof LivingEntity living) || living instanceof PlayerEntity) {
                continue;
            }
            if (!isValidTarget(living)) {
                continue;
            }
            double distance = mc.player.getDistance(living);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = living;
            }
        }

        return nearest;
    }

    public double getEntityArmor(PlayerEntity target) {
        double totalArmor = 0.0D;

        for (ItemStack armorStack : target.inventory.armorInventory) {
            if (armorStack != null && armorStack.getItem() instanceof ArmorItem) {
                totalArmor += this.getProtectionLvl(armorStack);
            }
        }

        return totalArmor;
    }

    public double getEntityHealth(Entity ent) {
        if (ent instanceof PlayerEntity player) {
            double armorValue = this.getEntityArmor(player) / 20.0D;
            return (double) (player.getHealth() + player.getAbsorptionAmount()) * armorValue;
        } else if (ent instanceof LivingEntity livingEntity) {
            return livingEntity.getHealth() + livingEntity.getAbsorptionAmount();
        } else {
            return 0.0D;
        }
    }

    private double getProtectionLvl(ItemStack stack) {
        ArmorItem armor = (ArmorItem) stack.getItem();
        double damageReduce = armor.getDamageReduceAmount();
        if (stack.isEnchanted()) {
            damageReduce += (double) EnchantmentHelper.getEnchantmentLevel(Enchantments.PROTECTION, stack) * 0.25D;
        }

        return damageReduce;
    }


    private boolean isValidTarget(LivingEntity entity) {
        if (entity instanceof ClientPlayerEntity) return false;

        if (entity.ticksExisted < 3) return false;
        if (mc.player.getDistanceEyePos(entity) >= getMaxAimRange()) return false;

        //   if (!attackThroughWalls.get() && !this.canSeeThroughWall(entity)) return false;
        if (entity instanceof PlayerEntity playerEntity) {
            if (SkyCore.getInstance().getModuleManager().getModule(AntiBot.class).isEnabled() && AntiBot.bot.contains(playerEntity)) {
                return false;
            }
            if (!targets.is("Друзья") && SkyCore.getInstance().getFriendManager().isFriend(playerEntity.getName().getString())) {
                return false;
            }
            if (playerEntity.getName().getString().equalsIgnoreCase(mc.player.getName().getString())) return false;
        }

        if (entity instanceof PlayerEntity && entity.getTotalArmorValue() == 0 && !targets.is("Голые")) return false;
        if (entity instanceof PlayerEntity && !targets.is("Игроки")) return false;
        if ((entity instanceof MonsterEntity || entity instanceof PhantomEntity || entity instanceof BatEntity || entity instanceof ShulkerEntity || entity instanceof VillagerEntity || entity instanceof WanderingTraderEntity || entity instanceof SlimeEntity || entity instanceof IronGolemEntity) && !targets.is("Мобы"))
            return false;
        if ((entity instanceof AnimalEntity || entity instanceof SalmonEntity || entity instanceof TropicalFishEntity || entity instanceof CodEntity || entity instanceof SquidEntity || entity instanceof DolphinEntity) && !targets.is("Животные"))
            return false;

        return !entity.isInvulnerable() && entity.isAlive() && !(entity instanceof ArmorStandEntity);
    }

    public boolean cooldownComplete() {
        float attackStrength = mc.player.getCooledAttackStrength(1.5F);
        if (componentMode.is("SpookyTime")) {
            if (stopWatch.getElapsedTime() >= 350 + (counter % 10 == 0 ? 255 : 0)) {
                return !(attackStrength < 0.92f);
            }
        } else if (stopWatch.getElapsedTime() >= 500) {
            return !(attackStrength < 0.92f);
        }
        return false;
    }

    public double getMaxAimRange() {
        float attackDist = (Float)this.attackRange.get();
        float lerpRotationDist = (Float)this.lerpRotationDistance.get();
        float originalAimDistance = this.componentMode.is("Легитная") ? 0.2F : 0.0F;
        boolean isGrim = this.componentMode.is("Grim");
        float var10000;
        if (SkyCore.getInstance().getModuleManager().getElytraTarget().isEnabled()) {
            SkyCore.getInstance().getModuleManager().getElytraTarget();
            var10000 = (Float)ElytraTarget.pursuitdistance.get();
        } else {
            var10000 = 0.0F;
        }

        float elytraAimDist = var10000;
        float test = isGrim ? attackDist : attackDist + lerpRotationDist - originalAimDistance;
        Minecraft var7 = mc;
        return Minecraft.player.isElytraFlying() ? (double)(attackDist + elytraAimDist) : (double)test;
    }

    public double attackDistance() {
        return Math.max(mc.playerController.extendedReach() ? 6.0D : 3.0D, attackRange.get());
    }



    private boolean isBypassWallsBlockedByFluid() {
        if (mc.player.isInWater() || mc.player.isInLava()
                || mc.player.areEyesInFluid(FluidTags.WATER)
                || mc.player.areEyesInFluid(FluidTags.LAVA)) {
            return true;
        }

        if (target == null) {
            return false;
        }

        if (target.isInWater() || target.isInLava()
                || target.areEyesInFluid(FluidTags.WATER)
                || target.areEyesInFluid(FluidTags.LAVA)) {
            return true;
        }

        Vector3d startVec = mc.player.getEyePosition(mc.getRenderPartialTicks());
        Vector3d targetPos = target.getEyePosition(mc.getRenderPartialTicks());
        Vector3d direction = targetPos.subtract(startVec);
        double distance = direction.length();
        if (distance < 1.0E-3) {
            return false;
        }

        Vector3d normalizedDir = direction.normalize();
        for (double i = 0; i < distance; i += 0.5) {
            BlockPos pos = new BlockPos(startVec.add(normalizedDir.scale(i)));
            FluidState fluid = mc.world.getFluidState(pos);
            if (fluid.isTagged(FluidTags.WATER) || fluid.isTagged(FluidTags.LAVA)) {
                return true;
            }
        }

        return false;
    }

    private void reset() {
        target = null;
        canCrit = false;
        resetTargetRotationState();

        ElytraComponent.resetState();

    }

    private void resetTargetRotationState() {
        lonyLastTarget = null;
        if (mc.player == null) {
            return;
        }

        resetLonyGriefRotation(mc.player.rotationYaw, mc.player.rotationPitch);
        if (!componentMode.is("LonyGrief")) {
            return;
        }

        LookHandler.setFreeYaw(mc.player.rotationYaw);
        LookHandler.setFreePitch(mc.player.rotationPitch);
        RotationComponent.getInstance().stopRotation();
        SmoothRotationComponent.getInstance().stopRotation();
    }
}