package sky.core.utils.misc;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import sky.core.SkyCore;
import sky.core.modules.api.constructors.impl.MultiBooleanSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import sky.core.modules.impl.combat.AntiBot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static sky.core.utils.Wrapper.mc;

public class TargetUtil {

    public static boolean isPlayerTarget(Entity entity, MultiBooleanSetting settings, boolean considerNaked) {
        if (!(entity instanceof PlayerEntity player) || entity == mc.player) return false;
        boolean isFriend = SkyCore.getInstance().getFriendManager().isFriend(player.getGameProfile().getName());
        boolean isNaked = player.getTotalArmorValue() == 0;
        if (isFriend) {
            return settings.is("Друзей");
        }
        if (considerNaked && isNaked) {
            return settings.is("Голых");
        }
        return settings.is("Игроков");
    }

    public static boolean isPlayerTarget(Entity entity, MultiBooleanSetting settings) {
        return isPlayerTarget(entity, settings, true);
    }

    public static boolean isVillagerTarget(LivingEntity entity, MultiBooleanSetting settings) {
        return entity instanceof VillagerEntity && settings.is("Жителей");
    }

    public LivingEntity sortEntitiesByFov(LivingEntity target, float distance, float preDistance, MultiBooleanSetting targets) {


        List<LivingEntity> validTargets = validEntities(distance, preDistance, targets);

        LivingEntity selectedTarget = validTargets.stream()
                .filter(e -> mc.player.canEntityBeSeen(e) || !SkyCore.getInstance().getModuleManager().getAttackAura().getThroughWalls().get())
                .min(Comparator.comparing((LivingEntity e) -> e != target)
                        .thenComparingDouble(this::getAimDelta)
                        .thenComparingDouble(e -> e.getDistanceSq(mc.player))
                        .thenComparingInt(Entity::getEntityId))
                .orElse(null);


        return selectedTarget;
    }

    private float getCameraYaw() {
        return MathHelper.wrapDegrees(mc.gameRenderer.getActiveRenderInfo().getYaw() + (mc.gameRenderer.getActiveRenderInfo().isThirdPersonReverse() ? 180 : 0));
    }

    private double getAimDelta(LivingEntity entity) {
        Vector2f rotation = rotationEntityAngles(entity);
        float yaw = getCameraYaw();
        float pitch = getCameraPitch();
        double yawDelta = MathHelper.wrapDegrees(rotation.x - yaw);
        double pitchDelta = MathHelper.wrapDegrees(rotation.y - pitch);
        return Math.hypot(yawDelta, pitchDelta);
    }

    public Vector2f rotationEntityAngles(Entity entity) {
        Vector3d playerEyes = mc.player.getEyePosition(1);

        Vector3d entityPos = entity.getPositionVec().add(0, entity.getHeight() / 2.0, 0);

        Vector3d direction = entityPos.subtract(playerEyes).normalize();

        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float pitch = (float) Math.toDegrees(Math.asin(direction.y));

        return new Vector2f(yaw, -pitch);
    }

    private float getCameraPitch() {
        return (mc.gameRenderer.getActiveRenderInfo().isThirdPersonReverse() ? -1 : 1) * mc.gameRenderer.getActiveRenderInfo().getPitch();
    }

    private List<LivingEntity> validEntities(float distance, float preDistance, MultiBooleanSetting targets) {
        List<LivingEntity> validTargets = new ArrayList<>();
        for (Entity entity : mc.world.getAllEntities()) {

            boolean player = targets.is("Players");
            boolean mobs = targets.is("Mobs");
            boolean naked = targets.is("Naked");
            boolean friends = targets.is("Friends");
            boolean creative = targets.is("Creative");

            if (entity instanceof LivingEntity living && living.getHealth() > 0.0) {

                if (entity instanceof PlayerEntity playerEntity && entity != mc.world.getEntityByID(1337)) {

                    boolean isFriend = !SkyCore.getInstance().getFriendManager().isFriend(playerEntity.getName().getString()) || friends;

                    boolean isNaked = playerEntity.getTotalArmorValue() > 0 || naked;

                    boolean isCreative = !playerEntity.isCreative() || creative;

                    if (isValidEntities(playerEntity, distance, preDistance) && isFriend && isNaked && isCreative && player) {
                        validTargets.add(playerEntity);
                    }

                }

                if (entity instanceof PlayerEntity playerEntity && friends && SkyCore.getInstance().getFriendManager().isFriend(playerEntity.getName().getString())) {
                    if (isValidEntities(playerEntity, distance, preDistance)) {
                        validTargets.add(playerEntity);
                    }
                }

                if (entity instanceof MobEntity mobEntity && mobs) {
                    if (isValidEntities(mobEntity, distance, preDistance)) {
                        validTargets.add(mobEntity);
                    }
                }

            }

        }

        return validTargets;
    }

    private boolean isValidEntities(LivingEntity entity, float distance, float preDistance) {

        if (AntiBot.bot.contains(entity))
            return false;

        if (entity == mc.player) {
            return false;
        }

        return mc.player.getDistance(entity) <= distance + preDistance;
    }

    public static boolean isAnimalTarget(LivingEntity entity, MultiBooleanSetting settings) {
        return entity instanceof AnimalEntity && settings.is("Животных");
    }

    public static boolean isMobTarget(LivingEntity entity, MultiBooleanSetting settings) {
        return entity instanceof MobEntity && settings.is("Мобов");
    }

    public static boolean isMonsterTarget(LivingEntity entity, MultiBooleanSetting settings) {
        return entity instanceof MobEntity && !(entity instanceof AnimalEntity) && !(entity instanceof VillagerEntity) && settings.is("Монстров");
    }

    public static boolean isSelfTarget(Entity entity, MultiBooleanSetting settings) {
        return entity == mc.player && settings.is("Себя") && !mc.gameSettings.getPointOfView().firstPerson();
    }

    public static boolean isItemTarget(Entity entity, MultiBooleanSetting settings) {
        return entity instanceof ItemEntity && settings.is("Предметы");
    }


    public static boolean isEntityTarget(Entity entity, MultiBooleanSetting settings) {
        if (entity instanceof LivingEntity livingEntity) {
            return isSelfTarget(entity, settings) ||
                    isPlayerTarget(entity, settings, true) ||
                    isVillagerTarget(livingEntity, settings) ||
                    isAnimalTarget(livingEntity, settings) ||
                    isMonsterTarget(livingEntity, settings);
        }
        return isItemTarget(entity, settings);
    }
}