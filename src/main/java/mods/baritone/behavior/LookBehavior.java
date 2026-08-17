/*
 * This file is part of Baritone.
 */

package mods.baritone.behavior;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.eventapinew.EventTarget;
import mods.baritone.Baritone;
import mods.baritone.api.api.java.baritone.api.Settings;
import mods.baritone.api.api.java.baritone.api.behavior.ILookBehavior;
import mods.baritone.api.api.java.baritone.api.behavior.look.IAimProcessor;
import mods.baritone.api.api.java.baritone.api.behavior.look.ITickableAimProcessor;
import mods.baritone.api.api.java.baritone.api.event.events.PacketEvent;
import mods.baritone.api.api.java.baritone.api.event.events.TickEvent;
import mods.baritone.api.api.java.baritone.api.event.events.WorldEvent;
import mods.baritone.api.api.java.baritone.api.utils.IPlayerContext;
import mods.baritone.api.api.java.baritone.api.utils.Rotation;
import mods.baritone.behavior.look.ForkableRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.MathHelper;
import sky.core.events.EventUpdate;

import java.util.Optional;
import java.util.Random;

public final class LookBehavior extends Behavior implements ILookBehavior {

    private Target target;
    private Rotation prevRotation;
    private final AimProcessor processor;

    private float lastYaw = 0f;
    private float lastPitch = 0f;
    private float lastClampedYaw = 0f;
    private float lastClampedPitch = 0f;
    private Rotation lastTargetRotation = null;
    private final Random random = new Random();

    private static final float JITTER_YAW_AMPLITUDE = 8f;
    private static final float JITTER_PITCH_AMPLITUDE = 0.4f;
    private static final float MICRO_JITTER_YAW = 0.12f;
    private static final float MICRO_JITTER_PITCH = 0.06f;

    public LookBehavior(Baritone baritone) {
        super(baritone);
        this.processor = new AimProcessor(baritone.getPlayerContext());
        EventManager.register(this);
    }

    @java.lang.Override
    public void updateTarget(Rotation rotation, boolean blockInteract) {
        this.target = new Target(rotation, blockInteract);
    }

    @java.lang.Override
    public IAimProcessor getAimProcessor() {
        return this.processor;
    }

    @java.lang.Override
    public void onTick(TickEvent event) {
        if (event.getType() == TickEvent.Type.IN) {
            this.processor.tick();
        }
    }

    @EventTarget
    public void onPlayerUpdate(EventUpdate event) {
        if (this.target == null) {
            return;
        }

        if (ctx.player() == null) {
            return;
        }

        float currentYaw = ctx.player().rotationYaw;
        float currentPitch = ctx.player().rotationPitch;

        Rotation targetRotation = this.target.rotation;

        float[] smoothRots = calculateSmoothRotation(
                currentYaw, currentPitch,
                targetRotation.getYaw(), targetRotation.getPitch()
        );

        float t = ctx.player().ticksExisted + ctx.minecraft().getRenderPartialTicks();

        float jitterYaw = (float) (
                Math.sin(t * 0.50F) * JITTER_YAW_AMPLITUDE +
                        Math.sin(t * 0.04F + 17.2) * (JITTER_YAW_AMPLITUDE * 0.2)
        );
        float jitterPitch = (float) (
                Math.sin(t * 0.65F) * JITTER_PITCH_AMPLITUDE +
                        Math.sin(t * 0.03F + 54.1) * (JITTER_PITCH_AMPLITUDE * 0.25)
        );

        float microJitterYaw = (float) (
                Math.sin(t * 2.3F) * MICRO_JITTER_YAW +
                        Math.sin(t * 3.7F) * (MICRO_JITTER_YAW * 0.7)
        );
        float microJitterPitch = (float) (
                Math.sin(t * 2.8F) * MICRO_JITTER_PITCH +
                        Math.sin(t * 4.1F) * (MICRO_JITTER_PITCH * 0.6)
        );

        float noiseYaw = (random.nextFloat() - 0.5f) * 0.08f;
        float noisePitch = (random.nextFloat() - 0.5f) * 0.04f;

        float finalYaw = smoothRots[0] + jitterYaw + microJitterYaw + noiseYaw;
        float finalPitch = smoothRots[1] + jitterPitch + microJitterPitch + noisePitch;

        finalPitch = MathHelper.clamp(finalPitch, -90f, 90f);

        float gcd = getGCDValue();
        finalYaw -= (finalYaw - currentYaw) % gcd;
        finalPitch -= (finalPitch - currentPitch) % gcd;

        ctx.player().rotationYaw = finalYaw;
        ctx.player().rotationPitch = finalPitch;

        ctx.player().prevRotationYaw = currentYaw;
        ctx.player().prevRotationPitch = currentPitch;

        this.target = null;
    }


    private float[] calculateSmoothRotation(float currentYaw, float currentPitch,
                                            float targetYaw, float targetPitch) {

        float gcd = getGCDValue();

        float yawDelta = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float pitchDelta = MathHelper.wrapDegrees(targetPitch - currentPitch);

        boolean isNewTarget = lastTargetRotation == null ||
                Math.abs(MathHelper.wrapDegrees(targetYaw - lastTargetRotation.getYaw())) > 5.0f ||
                Math.abs(targetPitch - lastTargetRotation.getPitch()) > 5.0f;

        float maxYawSpeed = 18.0f;
        float maxPitchSpeed = 12.0f;

        float clampedYaw = Math.min(Math.max(Math.abs(yawDelta), 0.0001f), maxYawSpeed);
        float clampedPitch = Math.min(Math.max(Math.abs(pitchDelta), 0.0001f), maxPitchSpeed);

        float randomYawFactor = (random.nextFloat() - 0.5f) * 2.0f;
        float randomPitchFactor = (random.nextFloat() - 0.5f) * 1.5f;
        float randomThreshold = random.nextFloat() * 2.0f;
        float randomAddition = random.nextFloat() * 2.5f + 1.5f;

        if (isNewTarget) {
            clampedYaw = Math.min(Math.abs(yawDelta), maxYawSpeed * 1.2f);
            clampedPitch = Math.min(Math.abs(pitchDelta), maxPitchSpeed * 1.2f);
        } else {
            clampedYaw *= 0.7f;
            clampedPitch *= 0.6f;
        }

        if (Math.abs(clampedYaw - lastClampedYaw) <= randomThreshold && clampedYaw < maxYawSpeed) {
            clampedYaw += randomAddition;
        }

        clampedYaw += randomYawFactor;
        clampedPitch += randomPitchFactor;

        clampedYaw = MathHelper.clamp(clampedYaw, 0.1f, maxYawSpeed);
        clampedPitch = MathHelper.clamp(clampedPitch, 0.1f, maxPitchSpeed);

        float newYaw = currentYaw + (yawDelta > 0 ? clampedYaw : -clampedYaw);
        float newPitch = currentPitch + (pitchDelta > 0 ? clampedPitch : -clampedPitch);

        newPitch = MathHelper.clamp(newPitch, -90.0f, 90.0f);

        newYaw -= (newYaw - currentYaw) % gcd;
        newPitch -= (newPitch - currentPitch) % gcd;

        if (Math.abs(yawDelta) < clampedYaw * 1.1f) {
            newYaw = targetYaw - (targetYaw % gcd);
        }
        if (Math.abs(pitchDelta) < clampedPitch * 1.1f) {
            newPitch = targetPitch - (targetPitch % gcd);
        }

        lastClampedYaw = clampedYaw;
        lastClampedPitch = clampedPitch;
        lastYaw = newYaw;
        lastPitch = newPitch;
        lastTargetRotation = new Rotation(targetYaw, targetPitch);

        return new float[]{newYaw, newPitch};
    }

    private float getGCDValue() {
        Minecraft mc = Minecraft.getInstance();
        float sensitivity = (float) (mc.gameSettings.mouseSensitivity * 0.6f + 0.2f);
        return sensitivity * sensitivity * sensitivity * 1.2f;
    }

    @java.lang.Override
    public void onSendPacket(PacketEvent event) {
    }

    @java.lang.Override
    public void onWorldEvent(WorldEvent event) {
        this.target = null;
        this.lastTargetRotation = null;
    }

    public void pig() {
        if (this.target != null && ctx.player() != null) {
            float t = ctx.player().ticksExisted + ctx.minecraft().getRenderPartialTicks();
            float jitterYaw = (float) (Math.sin(t * 0.50F) * JITTER_YAW_AMPLITUDE);

            float[] smoothRots = calculateSmoothRotation(
                    ctx.player().rotationYaw, ctx.player().rotationPitch,
                    this.target.rotation.getYaw(), ctx.player().rotationPitch
            );

            ctx.player().rotationYaw = smoothRots[0] + jitterYaw;
        }
    }

    public Optional<Rotation> getEffectiveRotation() {
        return Optional.empty();
    }


    private static final class AimProcessor extends AbstractAimProcessor {
        public AimProcessor(final IPlayerContext ctx) {
            super(ctx);
        }

        @java.lang.Override
        protected Rotation getPrevRotation() {
            return ctx.playerRotations();
        }
    }

    private static abstract class AbstractAimProcessor implements ITickableAimProcessor {

        protected final IPlayerContext ctx;
        private final ForkableRandom rand;
        private double randomYawOffset;
        private double randomPitchOffset;

        private float smoothYaw = 0;
        private float smoothPitch = 0;
        private boolean initialized = false;
        private final Random random = new Random();

        public AbstractAimProcessor(IPlayerContext ctx) {
            this.ctx = ctx;
            this.rand = new ForkableRandom();
        }

        private AbstractAimProcessor(final AbstractAimProcessor source) {
            this.ctx = source.ctx;
            this.rand = source.rand.fork();
            this.randomYawOffset = source.randomYawOffset;
            this.randomPitchOffset = source.randomPitchOffset;
            this.smoothYaw = source.smoothYaw;
            this.smoothPitch = source.smoothPitch;
            this.initialized = source.initialized;
        }

        @java.lang.Override
        public final Rotation peekRotation(final Rotation rotation) {
            final Rotation prev = this.getPrevRotation();

            if (!initialized) {
                smoothYaw = prev.getYaw();
                smoothPitch = prev.getPitch();
                initialized = true;
            }

            float desiredYaw = rotation.getYaw();
            float desiredPitch = rotation.getPitch();

            if (desiredPitch == prev.getPitch()) {
                desiredPitch = nudgeToLevel(desiredPitch);
            }

            desiredYaw += this.randomYawOffset;
            desiredPitch += this.randomPitchOffset;

            float[] result = calculateSmoothMouseMoveWithJitter(
                    prev.getYaw(), prev.getPitch(),
                    desiredYaw, desiredPitch
            );

            return new Rotation(result[0], result[1]).clamp();
        }

        private float[] calculateSmoothMouseMoveWithJitter(float currentYaw, float currentPitch,
                                                           float targetYaw, float targetPitch) {

            float gcd = getGCDValue();

            float yawDiff = MathHelper.wrapDegrees(targetYaw - currentYaw);
            float pitchDiff = targetPitch - currentPitch;

            float maxYawStep = 15.0f + random.nextFloat() * 5.0f;
            float maxPitchStep = 10.0f + random.nextFloat() * 3.0f;

            float yawSpeed = Math.min(Math.abs(yawDiff) * 0.4f, maxYawStep);
            float pitchSpeed = Math.min(Math.abs(pitchDiff) * 0.35f, maxPitchStep);

            yawSpeed = Math.max(yawSpeed, 0.5f);
            pitchSpeed = Math.max(pitchSpeed, 0.3f);

            yawSpeed += (random.nextFloat() - 0.5f) * 1.5f;
            pitchSpeed += (random.nextFloat() - 0.5f) * 1.0f;

            float yawStep = yawDiff > 0 ? Math.min(yawSpeed, yawDiff) : Math.max(-yawSpeed, yawDiff);
            float pitchStep = pitchDiff > 0 ? Math.min(pitchSpeed, pitchDiff) : Math.max(-pitchSpeed, pitchDiff);

            float newYaw = currentYaw + yawStep;
            float newPitch = MathHelper.clamp(currentPitch + pitchStep, -90f, 90f);

            if (ctx.player() != null) {
                float t = ctx.player().ticksExisted;
                float jitterYaw = (float) (Math.sin(t * 0.50F) * 0.6 + Math.sin(t * 2.3F) * 0.1);
                float jitterPitch = (float) (Math.sin(t * 0.65F) * 0.3 + Math.sin(t * 2.8F) * 0.05);

                newYaw += jitterYaw;
                newPitch += jitterPitch;
            }

            newYaw -= (newYaw - currentYaw) % gcd;
            newPitch -= (newPitch - currentPitch) % gcd;

            if (Math.abs(yawDiff) < 1.0f) {
                newYaw = targetYaw;
            }
            if (Math.abs(pitchDiff) < 1.0f) {
                newPitch = targetPitch;
            }

            return new float[]{newYaw, newPitch};
        }

        private float getGCDValue() {
            Minecraft mc = Minecraft.getInstance();
            float sensitivity = (float) (mc.gameSettings.mouseSensitivity * 0.6f + 0.2f);
            return sensitivity * sensitivity * sensitivity * 1.2f;
        }

        @java.lang.Override
        public final void tick() {
            this.randomYawOffset = (this.rand.nextDouble() - 0.5) * Baritone.settings().randomLooking.value;
            this.randomPitchOffset = (this.rand.nextDouble() - 0.5) * Baritone.settings().randomLooking.value;

            double random = this.rand.nextDouble() - 0.5;
            if (Math.abs(random) < 0.1) {
                random *= 4;
            }
            this.randomYawOffset += random * Baritone.settings().randomLooking113.value;
        }

        @java.lang.Override
        public final void advance(int ticks) {
            for (int i = 0; i < ticks; i++) {
                this.tick();
            }
        }

        @java.lang.Override
        public Rotation nextRotation(final Rotation rotation) {
            final Rotation actual = this.peekRotation(rotation);
            this.tick();
            return actual;
        }

        @java.lang.Override
        public final ITickableAimProcessor fork() {
            return new AbstractAimProcessor(this) {
                private Rotation prev = AbstractAimProcessor.this.getPrevRotation();

                @java.lang.Override
                public Rotation nextRotation(final Rotation rotation) {
                    return (this.prev = super.nextRotation(rotation));
                }

                @java.lang.Override
                protected Rotation getPrevRotation() {
                    return this.prev;
                }
            };
        }

        protected abstract Rotation getPrevRotation();

        private float nudgeToLevel(float pitch) {
            if (pitch < -20) {
                return pitch + 1;
            } else if (pitch > 10) {
                return pitch - 1;
            }
            return pitch;
        }
    }

    private static class Target {
        public final Rotation rotation;
        public final Mode mode;

        public Target(Rotation rotation, boolean blockInteract) {
            this.rotation = rotation;
            this.mode = Mode.resolve(blockInteract);
        }

        enum Mode {
            CLIENT, SERVER, NONE;

            static Mode resolve(boolean blockInteract) {
                final Settings settings = Baritone.settings();
                final boolean antiCheat = settings.antiCheatCompatibility.value;
                final boolean blockFreeLook = settings.blockFreeLook.value;
                final boolean freeLook = settings.freeLook.value;

                if (!freeLook) return CLIENT;
                if (!blockFreeLook && blockInteract) return CLIENT;
                if (antiCheat || blockInteract) return SERVER;
                return NONE;
            }
        }
    }
}