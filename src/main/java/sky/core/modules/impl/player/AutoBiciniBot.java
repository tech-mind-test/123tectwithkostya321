package sky.core.modules.impl.player;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import mods.baritone.api.api.java.baritone.api.BaritoneAPI;
import mods.baritone.api.api.java.baritone.api.pathing.goals.GoalNear;
import mods.baritone.api.api.java.baritone.api.pathing.goals.GoalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CClickWindowPacket;
import net.minecraft.network.play.client.CCloseWindowPacket;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Registry;
import sky.core.SkyCore;
import sky.core.events.EventUpdate;
import sky.core.events.EventPacket;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.impl.combat.AntiBot;
import sky.core.utils.math.TimeUtil;
import sky.core.utils.player.InventoryUtil;
import net.minecraft.network.play.server.SChatPacket;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoBiciniBot extends Module {

    private static final int PORTAL_X = 131;
    private static final int PORTAL_Y = 64;
    private static final int PORTAL_Z = 47;

    private static final int ZONE_X1 = 54;
    private static final int ZONE_Z1 = 161;
    private static final int ZONE_X2 = 12;
    private static final int ZONE_Z2 = -62;

    private static final List<String> PICKUP_ITEMS = List.of(
            "Формула крабсбургера",
            "Крабсбургер",
            "Голова"
    );

    private final Random random = new Random();
    private LivingEntity target = null;
    private final TimeUtil attackTimer = new TimeUtil();
    private final TimeUtil stateTimer = new TimeUtil();
    private final TimeUtil walkTimer = new TimeUtil();
    private final TimeUtil waitTimer = new TimeUtil();
    private final TimeUtil eventCheckTimer = new TimeUtil();

    private static final float ATTACK_DISTANCE = 3.0f;
    private static final float ATTACK_SEARCH_RANGE = 15.0f;
    private static final float APPROACH_DISTANCE = 2.5f;
    private static final long EVENT_CHECK_INTERVAL = 30000;

    private float lastYaw = 0f;
    private float lastPitch = 0f;
    private float lastClampedYaw = 0f;
    private LivingEntity selectedTarget = null;

    private static final float JITTER_YAW_AMPLITUDE = 0.6f;
    private static final float JITTER_PITCH_AMPLITUDE = 0.3f;

    private static final List<String> ALL_ANARCHIES = new ArrayList<>();
    static {
        for (int i = 103; i <= 108; i++) ALL_ANARCHIES.add("an" + i);
        for (int i = 202; i <= 216; i++) ALL_ANARCHIES.add("an" + i);
        for (int i = 301; i <= 306; i++) ALL_ANARCHIES.add("an" + i);
        for (int i = 501; i <= 505; i++) ALL_ANARCHIES.add("an" + i);
        for (int i = 601; i <= 605; i++) ALL_ANARCHIES.add("an" + i);
    }

    private int currentAnarchyIndex = 0;
    private String currentAnarchy = null;
    private boolean waitingForResponse = false;
    private boolean waitingForEventCheck = false;
    private boolean teleported = false;
    private double lastX, lastY, lastZ;
    private boolean walkingToPortal = false;
    private int waitSeconds = 0;

    private boolean eating = false;
    private int lastSlot = -1;

    private enum BotState {
        INIT_CHECK,
        CONNECTING,
        CHECKING_EVENT,
        WAITING_EVENT_START,
        WARPING,
        WAITING_TELEPORT,
        GOING_TO_PORTAL,
        ENTERING_PORTAL,
        FARMING
    }

    private BotState state = BotState.INIT_CHECK;

    public AutoBiciniBot() {
        super("AutoBiciniBot", "Автоматически лутает ивенты бикинибот на спукитайме", Category.Miscellaneous);
    }

    @java.lang.Override
    public void onEnable() {
        selectedTarget = null;
        currentAnarchyIndex = 0;
        waitingForResponse = false;
        waitingForEventCheck = false;
        teleported = false;
        walkingToPortal = false;
        waitSeconds = 0;
        currentAnarchy = ALL_ANARCHIES.get(0);
        state = BotState.INIT_CHECK;
        stateTimer.reset();
        eventCheckTimer.reset();
        eating = false;
        lastSlot = -1;

        if (mc.player != null) {
            lastX = mc.player.getPosX();
            lastY = mc.player.getPosY();
            lastZ = mc.player.getPosZ();
        }

        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();

        super.onEnable();
    }

    @java.lang.Override
    public void onDisable() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
        mc.gameSettings.keyBindForward.setPressed(false);
        mc.gameSettings.keyBindUseItem.setPressed(false);
        target = null;
        selectedTarget = null;
        eating = false;
        lastSlot = -1;
        super.onDisable();
    }

    private boolean checkAndEat() {
        if (mc.player == null) return false;

        float health = mc.player.getHealth();
        float foodLevel = mc.player.getFoodStats().getFoodLevel();

        boolean needHeal = health < 14.0f;
        boolean needFood = foodLevel < 18.0f;

        if (!needHeal && !needFood) {
            if (eating) {
                stopEating();
            }
            return false;
        }

        if (eating) {
            if (!mc.player.isHandActive()) {
                mc.gameSettings.keyBindUseItem.setPressed(true);
                mc.playerController.processRightClick(mc.player, mc.world, Hand.MAIN_HAND);
            }
            return true;
        }

        int foodSlot = findFoodSlot(needHeal);
        if (foodSlot != -1) {
            lastSlot = mc.player.inventory.currentItem;
            mc.player.inventory.currentItem = foodSlot;
            mc.gameSettings.keyBindUseItem.setPressed(true);
            mc.playerController.processRightClick(mc.player, mc.world, Hand.MAIN_HAND);
            eating = true;
            return true;
        }

        return false;
    }

    private void stopEating() {
        mc.gameSettings.keyBindUseItem.setPressed(false);
        if (lastSlot != -1 && lastSlot >= 0 && lastSlot < 9) {
            mc.player.inventory.currentItem = lastSlot;
        }
        eating = false;
        lastSlot = -1;
    }

    private int findFoodSlot(boolean preferGoldenApple) {
        if (preferGoldenApple) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.inventory.getStackInSlot(i);
                if (stack.getItem() == Items.GOLDEN_APPLE || stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
                    return i;
                }
            }
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack.getItem().isFood() && stack.getItem() != Items.SPIDER_EYE && stack.getItem() != Items.ROTTEN_FLESH && stack.getItem() != Items.PUFFERFISH) {
                return i;
            }
        }

        return -1;
    }

    private boolean isInFarmZone() {
        if (mc.player == null) return false;

        int minX = Math.min(ZONE_X1, ZONE_X2);
        int maxX = Math.max(ZONE_X1, ZONE_X2);
        int minZ = Math.min(ZONE_Z1, ZONE_Z2);
        int maxZ = Math.max(ZONE_Z1, ZONE_Z2);

        double x = mc.player.getPosX();
        double z = mc.player.getPosZ();

        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    private boolean isNearPortal() {
        if (mc.player == null) return false;
        double dx = mc.player.getPosX() - PORTAL_X;
        double dz = mc.player.getPosZ() - PORTAL_Z;
        return Math.sqrt(dx * dx + dz * dz) < 30;
    }

    private boolean isValidItem(ItemEntity item) {
        if (item == null) return false;

        ItemStack stack = item.getItem();
        net.minecraft.item.Item itemType = stack.getItem();

        if (itemType == Items.PLAYER_HEAD ||
                itemType == Items.ZOMBIE_HEAD ||
                itemType == Items.SKELETON_SKULL ||
                itemType == Items.CREEPER_HEAD ||
                itemType == Items.WITHER_SKELETON_SKULL ||
                itemType == Items.DRAGON_HEAD) {
            return true;
        }

        ResourceLocation res = Registry.ITEM.getKey(itemType);
        String registryName = res != null ? res.toString().toLowerCase() : "";

        if (registryName.contains("head") || registryName.contains("skull")) {
            return true;
        }

        String displayName = stack.getDisplayName().getString().toLowerCase();

        if (displayName.contains("формула крабсбургера") ||
                displayName.contains("крабсбургер") ||
                displayName.contains("голова") ||
                displayName.contains("череп") ||
                displayName.contains("head") ||
                displayName.contains("skull") ||
                displayName.contains("★")) {
            return true;
        }

        return false;
    }

    private int parseWaitTime(String message) {
        Pattern secPattern = Pattern.compile("Начнётся через (\\d+) сек");
        Matcher secMatcher = secPattern.matcher(message);
        if (secMatcher.find()) {
            return Integer.parseInt(secMatcher.group(1));
        }

        Pattern minSecPattern = Pattern.compile("Начнётся через (\\d+) мин(?:\\s+(\\d+) сек)?");
        Matcher minSecMatcher = minSecPattern.matcher(message);
        if (minSecMatcher.find()) {
            int minutes = Integer.parseInt(minSecMatcher.group(1));
            int seconds = minSecMatcher.group(2) != null ? Integer.parseInt(minSecMatcher.group(2)) : 0;
            return minutes * 60 + seconds;
        }

        return 0;
    }

    private boolean isEventEnded(String message) {
        return message.contains("Удаление ивента") ||
                (message.contains("До следующего ивента") && !message.contains("Бикини Боттом")) ||
                (message.contains("[Ивенты]") && !message.contains("Бикини Боттом") && !message.contains("Начнётся через"));
    }

    private boolean isEventActive(String message) {
        return message.contains("Бикини Боттом") &&
                (message.contains("Идёт страшный бой") ||
                        (message.contains("Статус:") && !message.contains("Начнётся через") && !message.contains("Удаление")));
    }

    @EventTarget
    private void onPacket(EventPacket event) {
        if (event.getPacket() instanceof SChatPacket) {
            SChatPacket packet = (SChatPacket) event.getPacket();
            String message = packet.getChatComponent().getString();

            if (message.contains("[Ивенты]")) {
                if (waitingForEventCheck) {
                    waitingForEventCheck = false;

                    if (isEventEnded(message)) {
                        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
                        state = BotState.CONNECTING;
                        stateTimer.reset();
                        currentAnarchyIndex = 0;
                        currentAnarchy = ALL_ANARCHIES.get(0);
                    }
                    return;
                }

                if (waitingForResponse) {
                    waitingForResponse = false;

                    if (message.contains("Бикини Боттом")) {
                        if (isEventActive(message)) {
                            state = BotState.WARPING;
                            stateTimer.reset();
                        } else if (message.contains("Начнётся через")) {
                            waitSeconds = parseWaitTime(message);
                            if (waitSeconds > 0) {
                                state = BotState.WAITING_EVENT_START;
                                waitTimer.reset();
                                stateTimer.reset();
                            } else {
                                state = BotState.WARPING;
                                stateTimer.reset();
                            }
                        } else if (message.contains("Удаление ивента")) {
                            nextAnarchy();
                        } else {
                            state = BotState.WARPING;
                            stateTimer.reset();
                        }
                    } else {
                        nextAnarchy();
                    }
                }
            }
        }
    }

    @EventTarget
    private void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        if (checkAndEat()) return;

        switch (state) {
            case INIT_CHECK:
                handleInitCheck();
                break;
            case CONNECTING:
                handleConnecting();
                break;
            case CHECKING_EVENT:
                handleCheckingEvent();
                break;
            case WAITING_EVENT_START:
                handleWaitingEventStart();
                break;
            case WARPING:
                handleWarping();
                break;
            case WAITING_TELEPORT:
                handleWaitingTeleport();
                break;
            case GOING_TO_PORTAL:
                handleGoingToPortal();
                break;
            case ENTERING_PORTAL:
                handleEnteringPortal();
                break;
            case FARMING:
                handleFarming();
                break;
        }
    }

    private void handleInitCheck() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();

        if (isInFarmZone()) {
            state = BotState.FARMING;
            stateTimer.reset();
            eventCheckTimer.reset();
        } else {
            state = BotState.CHECKING_EVENT;
            stateTimer.reset();
        }
    }

    private void handleConnecting() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();

        if (stateTimer.hasTimeElapsed(100)) {
            mc.player.sendChatMessage("/" + currentAnarchy);
            state = BotState.CHECKING_EVENT;
            stateTimer.reset();
        }
    }

    private void handleCheckingEvent() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();

        if (!waitingForResponse && stateTimer.hasTimeElapsed(1500)) {
            mc.player.sendChatMessage("/event delay");
            waitingForResponse = true;
            stateTimer.reset();
        }

        if (waitingForResponse && stateTimer.hasTimeElapsed(3000)) {
            waitingForResponse = false;
            nextAnarchy();
        }
    }

    private void handleWaitingEventStart() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();

        long waitedMs = waitTimer.getElapsedTime();
        long needToWaitMs = (waitSeconds + 2) * 1000L;

        if (waitedMs >= needToWaitMs) {
            state = BotState.WARPING;
            stateTimer.reset();
        }
    }

    private void handleWarping() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();

        if (isNearPortal()) {
            state = BotState.GOING_TO_PORTAL;
            stateTimer.reset();
            return;
        }

        if (stateTimer.hasTimeElapsed(1000)) {
            lastX = mc.player.getPosX();
            lastY = mc.player.getPosY();
            lastZ = mc.player.getPosZ();
            teleported = false;

            mc.player.sendChatMessage("/warp BikiniBottom");
            state = BotState.WAITING_TELEPORT;
            stateTimer.reset();
        }
    }

    private void handleWaitingTeleport() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();

        double dx = mc.player.getPosX() - lastX;
        double dy = mc.player.getPosY() - lastY;
        double dz = mc.player.getPosZ() - lastZ;
        double moved = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (moved > 10) {
            teleported = true;
            walkingToPortal = false;
            state = BotState.GOING_TO_PORTAL;
            stateTimer.reset();
        }

        if (stateTimer.hasTimeElapsed(5000) && !teleported) {
            mc.player.sendChatMessage("/warp BikiniBottom");
            lastX = mc.player.getPosX();
            lastY = mc.player.getPosY();
            lastZ = mc.player.getPosZ();
            stateTimer.reset();
        }
    }

    private void handleGoingToPortal() {
        if (!stateTimer.hasTimeElapsed(1000)) return;

        double distToPortal = getDistanceToPortal();

        if (distToPortal < 2) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
            state = BotState.ENTERING_PORTAL;
            stateTimer.reset();
            walkTimer.reset();
        } else {
            if (!BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing() || !walkingToPortal) {
                walkingToPortal = true;
                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess()
                        .setGoalAndPath(new GoalBlock(PORTAL_X, PORTAL_Y, PORTAL_Z));
            }
        }
    }

    private void handleEnteringPortal() {
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();

        double dx = PORTAL_X + 0.5 - mc.player.getPosX();
        double dz = PORTAL_Z + 0.5 - mc.player.getPosZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (dist < 0.5 || isInFarmZone()) {
            mc.gameSettings.keyBindForward.setPressed(false);
            state = BotState.FARMING;
            stateTimer.reset();
            eventCheckTimer.reset();
            return;
        }

        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90);
        mc.player.rotationYaw = targetYaw;

        mc.gameSettings.keyBindForward.setPressed(true);

        if (walkTimer.hasTimeElapsed(5000)) {
            mc.gameSettings.keyBindForward.setPressed(false);
            state = BotState.FARMING;
            stateTimer.reset();
            eventCheckTimer.reset();
        }
    }

    private double getDistanceToPortal() {
        double dx = mc.player.getPosX() - PORTAL_X;
        double dz = mc.player.getPosZ() - PORTAL_Z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private void nextAnarchy() {
        currentAnarchyIndex++;
        if (currentAnarchyIndex >= ALL_ANARCHIES.size()) {
            currentAnarchyIndex = 0;
        }
        currentAnarchy = ALL_ANARCHIES.get(currentAnarchyIndex);
        state = BotState.CONNECTING;
        stateTimer.reset();
    }

    private void handleFarming() {
        mc.gameSettings.keyBindForward.setPressed(false);

        if (eventCheckTimer.hasTimeElapsed(EVENT_CHECK_INTERVAL) && !waitingForEventCheck) {
            mc.player.sendChatMessage("/event delay");
            waitingForEventCheck = true;
            eventCheckTimer.reset();
        }

        target = findTargetForAttack();

        if (target != null) {
            float distanceToTarget = mc.player.getDistance(target);

            float[] rots = calculateSpookyRotation(
                    mc.player.rotationYaw,
                    mc.player.rotationPitch,
                    target
            );

            float prevYaw = mc.player.rotationYaw;
            float prevPitch = mc.player.rotationPitch;
            mc.player.rotationYaw = rots[0];
            mc.player.rotationPitch = rots[1];
            mc.player.prevRotationYaw = prevYaw;
            mc.player.prevRotationPitch = prevPitch;

            if (distanceToTarget > ATTACK_DISTANCE) {
                goToTarget(target);
            } else {
                if (isLookingAtTarget()) {
                    attack();
                }
            }
            return;
        }

        ItemEntity bestItem = mc.world.getEntitiesWithinAABB(ItemEntity.class, mc.player.getBoundingBox().grow(50))
                .stream()
                .filter(this::isInZone)
                .filter(this::isValidItem)
                .min(Comparator.comparingDouble(item -> mc.player.getDistanceSq(item)))
                .orElse(null);

        ZombieEntity bestZombie = mc.world.getEntitiesWithinAABB(ZombieEntity.class, mc.player.getBoundingBox().grow(50))
                .stream()
                .filter(this::isInZone)
                .filter(Entity::isAlive)
                .min(Comparator.comparingDouble(zombie -> mc.player.getDistanceSq(zombie)))
                .orElse(null);

        if (bestItem != null) {
            goToEntity(bestItem);
            return;
        }

        if (bestZombie != null) {
            goToEntity(bestZombie);
            return;
        }

        if (!BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {
            walkInRectangle();
        }
    }

    private void walkInRectangle() {
        int minX = Math.min(ZONE_X1, ZONE_X2);
        int maxX = Math.max(ZONE_X1, ZONE_X2);
        int minZ = Math.min(ZONE_Z1, ZONE_Z2);
        int maxZ = Math.max(ZONE_Z1, ZONE_Z2);

        int destX = minX + random.nextInt(maxX - minX + 1);
        int destZ = minZ + random.nextInt(maxZ - minZ + 1);
        int destY = (int) mc.player.getPosY();

        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess()
                .setGoalAndPath(new GoalBlock(destX, destY, destZ));
    }

    private void goToTarget(LivingEntity target) {
        if (target == null) return;

        BlockPos targetPos = target.getPosition();
        var currentGoal = BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getGoal();

        boolean needNewPath = currentGoal == null ||
                !BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing();

        if (!needNewPath && currentGoal instanceof GoalNear) {
            GoalNear goalNear = (GoalNear) currentGoal;
            double distToOldGoal = Math.sqrt(
                    Math.pow(goalNear.getGoalPos().getX() - targetPos.getX(), 2) +
                            Math.pow(goalNear.getGoalPos().getZ() - targetPos.getZ(), 2)
            );
            if (distToOldGoal > 3.0) {
                needNewPath = true;
            }
        }

        if (needNewPath) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess()
                    .setGoalAndPath(new GoalNear(targetPos, (int) APPROACH_DISTANCE));
        }
    }

    private void goToEntity(Entity entity) {
        if (entity == null) return;

        BlockPos pos = entity.getPosition();
        var currentGoal = BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getGoal();
        boolean needNewPath = currentGoal == null ||
                !BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing();

        int radius = (entity instanceof ItemEntity) ? 0 : 1;

        if (!needNewPath && currentGoal instanceof GoalNear) {
            GoalNear goalNear = (GoalNear) currentGoal;
            double dist = Math.sqrt(
                    Math.pow(goalNear.getGoalPos().getX() - pos.getX(), 2) +
                            Math.pow(goalNear.getGoalPos().getZ() - pos.getZ(), 2)
            );

            if (entity instanceof ItemEntity && dist > 1.0) {
                needNewPath = true;
            } else if (dist > 5.0) {
                needNewPath = true;
            }
        }

        if (needNewPath) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess()
                    .setGoalAndPath(new GoalNear(pos, radius));
        }
    }

    private float[] calculateSpookyRotation(float currentYaw, float currentPitch, LivingEntity target) {
        if (target == null) return new float[]{currentYaw, currentPitch};

        float gcd = getGCDValue();
        float neckHeight = target.getEyeHeight() - 0.3f;
        Vector3d targetPos = target.getPositionVec().add(0, neckHeight, 0);

        if (selectedTarget == target) {
            float randomOffsetX = (random.nextFloat() - 0.5f) * 0.1f;
            float randomOffsetZ = (random.nextFloat() - 0.5f) * 0.1f;
            targetPos = targetPos.add(randomOffsetX, 0, randomOffsetZ);
        }

        Vector3d vecToNeck = targetPos.subtract(mc.player.getEyePosition(1.0F));
        float yawToTarget = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vecToNeck.z, vecToNeck.x)) - 90.0);
        float pitchToTarget = (float) (-Math.toDegrees(Math.atan2(vecToNeck.y, Math.hypot(vecToNeck.x, vecToNeck.z))));

        float yawDelta = MathHelper.wrapDegrees(yawToTarget - currentYaw);
        float pitchDelta = MathHelper.wrapDegrees(pitchToTarget - currentPitch);

        float clampedYaw = Math.min(Math.max(Math.abs(yawDelta), 1.0E-4F), 22.5F);
        float clampedPitch = Math.min(Math.max(Math.abs(pitchDelta), 1.0E-4F), 17.0F);

        float randomYawFactor = random.nextFloat() * 2.5f - 1.5f;
        float randomPitchFactor = random.nextFloat() * 2.5f - 1.0f;
        float randomThreshold = random.nextFloat() * 2.5f;
        float randomAddition = random.nextFloat() * 3.5f + 2.5f;

        if (selectedTarget != target) {
            clampedPitch = Math.max(Math.abs(pitchDelta), 1.0F);
        } else {
            clampedPitch /= 3.0F;
        }

        if (Math.abs(clampedYaw - lastClampedYaw) <= randomThreshold) {
            clampedYaw = lastClampedYaw + randomAddition;
        }

        clampedYaw += randomYawFactor;
        clampedPitch += randomPitchFactor;

        float yaw = currentYaw + (yawDelta > 0.0F ? clampedYaw : -clampedYaw);
        float pitch = MathHelper.clamp(currentPitch + (pitchDelta > 0.0F ? clampedPitch : -clampedPitch), -80.0F, 70.0F);

        float t = mc.player.ticksExisted;
        float jitterYaw = (float) (Math.sin(t * 0.50F) * JITTER_YAW_AMPLITUDE + Math.sin(t * 2.3F) * 0.1);
        float jitterPitch = (float) (Math.sin(t * 0.65F) * JITTER_PITCH_AMPLITUDE + Math.sin(t * 2.8F) * 0.05);

        yaw += jitterYaw;
        pitch += jitterPitch;

        yaw -= (yaw - currentYaw) % gcd;
        pitch -= (pitch - currentPitch) % gcd;

        lastClampedYaw = clampedYaw;
        lastYaw = yaw;
        lastPitch = pitch;
        selectedTarget = target;

        return new float[]{yaw, pitch};
    }

    private float getGCDValue() {
        float sensitivity = (float) (mc.gameSettings.mouseSensitivity * 0.6f + 0.2f);
        return sensitivity * sensitivity * sensitivity * 1.2f;
    }

    private boolean isLookingAtTarget() {
        if (target == null) return false;

        Vector3d targetPos = target.getPositionVec().add(0, target.getEyeHeight() - 0.3f, 0);
        Vector3d vecToTarget = targetPos.subtract(mc.player.getEyePosition(1.0F));

        float targetYaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vecToTarget.z, vecToTarget.x)) - 90.0);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(vecToTarget.y, Math.hypot(vecToTarget.x, vecToTarget.z))));

        float yawDiff = Math.abs(MathHelper.wrapDegrees(targetYaw - mc.player.rotationYaw));
        float pitchDiff = Math.abs(targetPitch - mc.player.rotationPitch);

        return yawDiff <= 15.0f && pitchDiff <= 15.0f;
    }

    private void attack() {
        if (canAttack() && target != null && mc.player.getDistance(target) <= ATTACK_DISTANCE) {
            mc.playerController.attackEntity(mc.player, target);
            mc.player.swingArm(Hand.MAIN_HAND);
            attackTimer.reset();
            shieldBreaker();
        }
    }

    public boolean canAttack() {
        return attackTimer.hasTimeElapsed(400) && mc.player.getCooledAttackStrength(0.5F) > 0.92F;
    }

    private boolean shieldBreaker() {
        int axeSlot = InventoryUtil.findAxeSlot();
        if (target != null && target.getActiveItemStack().getItem() == Items.SHIELD && axeSlot != -1) {
            if (axeSlot < 9) {
                mc.getConnection().sendPacket(new CHeldItemChangePacket(axeSlot));
                mc.playerController.attackEntity(mc.player, target);
                mc.player.swingArm(Hand.MAIN_HAND);
                mc.getConnection().sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem));
            } else {
                mc.getConnection().sendPacket(new CClickWindowPacket(mc.player.container.windowId, axeSlot, mc.player.inventory.currentItem, ClickType.SWAP, ItemStack.EMPTY, mc.player.openContainer.getNextTransactionID(mc.player.inventory)));
                mc.getConnection().sendPacket(new CCloseWindowPacket(mc.player.container.windowId));
                mc.playerController.attackEntity(mc.player, target);
                mc.player.swingArm(Hand.MAIN_HAND);
                mc.getConnection().sendPacket(new CClickWindowPacket(mc.player.container.windowId, axeSlot, mc.player.inventory.currentItem, ClickType.SWAP, ItemStack.EMPTY, mc.player.openContainer.getNextTransactionID(mc.player.inventory)));
                mc.getConnection().sendPacket(new CCloseWindowPacket(mc.player.container.windowId));
            }
            return true;
        }
        return false;
    }

    private LivingEntity findTargetForAttack() {
        return mc.world.getEntitiesWithinAABB(ZombieEntity.class, mc.player.getBoundingBox().grow(ATTACK_SEARCH_RANGE))
                .stream()
                .filter(this::isTarget)
                .filter(this::isInZone)
                .min(Comparator.comparingDouble(e -> e.getDistance(mc.player)))
                .orElse(null);
    }

    private boolean isTarget(LivingEntity entity) {
        if (entity == null || !entity.isAlive() || entity == mc.player || entity instanceof ArmorStandEntity) {
            return false;
        }
        if (entity instanceof PlayerEntity player) {
            if (SkyCore.getInstance().getModuleManager().getModule(AntiBot.class).isEnabled() && AntiBot.bot.contains(player)) {
                return false;
            }
        }
        return true;
    }

    private boolean isInZone(Entity entity) {
        int minX = Math.min(ZONE_X1, ZONE_X2);
        int maxX = Math.max(ZONE_X1, ZONE_X2);
        int minZ = Math.min(ZONE_Z1, ZONE_Z2);
        int maxZ = Math.max(ZONE_Z1, ZONE_Z2);

        return entity.getPosX() >= minX && entity.getPosX() <= maxX &&
                entity.getPosZ() >= minZ && entity.getPosZ() <= maxZ;
    }
}