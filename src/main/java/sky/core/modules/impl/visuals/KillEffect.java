package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix3f;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import org.lwjgl.opengl.GL11;
import sky.core.events.EventAttack;
import sky.core.events.EventRender3D;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.ColorSetting;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class KillEffect extends Module {
    private static final ResourceLocation GLOW_TEXTURE = new ResourceLocation("SkyCore/icons/world_render/glow.png");

    private static final String MODE_PARTICLES = "Частицы";
    private static final String MODE_LIGHTNING = "Молния";
    private static final String MODE_GLOW = "Сияние";
    private static final String COLOR_RAINBOW = "Радуга";
    private static final String COLOR_CLIENT = "Клиент";
    private static final String COLOR_CUSTOM = "Свой";

    private static final long TRACK_TARGET_MS = 2500L;
    private static final long SAME_ENTITY_COOLDOWN_MS = 900L;
    private static final int MAX_CAPTURED_VERTICES = 24000;
    private static final int FULL_BRIGHT = 0x00F000F0;

    private static final float GLOW_DURATION = 2.8f;
    private static final float GLOW_Y_OFFSET = -0.42f;
    private static final float GLOW_DISC_RADIUS = 2.15f;
    private static final float GLOW_SLICE_STEP = 0.035f;
    private static final float GLOW_APPEAR_END = 0.30f;
    private static final float GLOW_BEAM_DISAPPEAR_START = 0.34f;
    private static final float GLOW_BEAM_DISAPPEAR_END = 0.54f;
    private static final float GLOW_DISC_FADE_START = 0.40f;

    private static int nextClientEffectEntityId = -100000;

    private final Random random = new Random();

    private final ModeSetting effectMode = new ModeSetting("Режим", MODE_PARTICLES, MODE_PARTICLES, MODE_LIGHTNING, MODE_GLOW);
    private final SliderSetting particles = new SliderSetting("Частицы", 280.0f, 80.0f, 650.0f, 10.0f,
            () -> effectMode.is(MODE_PARTICLES));
    private final SliderSetting particleSize = new SliderSetting("Размер частиц", 0.115f, 0.045f, 0.22f, 0.005f,
            () -> effectMode.is(MODE_PARTICLES));
    private final SliderSetting deathHold = new SliderSetting("Задержка", 200.0f, 0.0f, 500.0f, 10.0f,
            () -> effectMode.is(MODE_PARTICLES));
    private final SliderSetting evaporation = new SliderSetting("Испарение", 1050.0f, 250.0f, 2400.0f, 25.0f,
            () -> effectMode.is(MODE_PARTICLES));
    private final SliderSetting riseHeight = new SliderSetting("Подъем", 1.75f, 0.35f, 4.0f, 0.05f,
            () -> effectMode.is(MODE_PARTICLES));
    private final SliderSetting chaos = new SliderSetting("Хаос", 0.75f, 0.0f, 1.75f, 0.05f,
            () -> effectMode.is(MODE_PARTICLES));
    private final BooleanSetting throughWalls = new BooleanSetting("Через стены", false,
            () -> effectMode.is(MODE_PARTICLES));
    private final ModeSetting colorMode = new ModeSetting("Цвет", COLOR_CLIENT,
            () -> effectMode.is(MODE_PARTICLES), COLOR_RAINBOW, COLOR_CLIENT, COLOR_CUSTOM);
    private final BooleanSetting secondColor = new BooleanSetting("Второй цвет", false,
            () -> effectMode.is(MODE_PARTICLES) && colorMode.is(COLOR_CUSTOM));
    private final ColorSetting color = new ColorSetting("Цвет 1", true, new Color(255, 255, 255, 255).getRGB(),
            () -> effectMode.is(MODE_PARTICLES) && colorMode.is(COLOR_CUSTOM));
    private final ColorSetting color2 = new ColorSetting("Цвет 2", true, new Color(70, 70, 70, 170).getRGB(),
            () -> effectMode.is(MODE_PARTICLES) && colorMode.is(COLOR_CUSTOM) && secondColor.get());
    private final SliderSetting glowWidth = new SliderSetting("Ширина глоу", 0.13f, 0.05f, 0.35f, 0.01f,
            () -> effectMode.is(MODE_GLOW));
    private final SliderSetting glowHeight = new SliderSetting("Высота глоу", 4.0f, 1.5f, 8.0f, 0.25f,
            () -> effectMode.is(MODE_GLOW));

    private final CopyOnWriteArrayList<KillParticle> activeParticles = new CopyOnWriteArrayList<>();
    private final List<RecentKill> recentKills = new ArrayList<>();
    private final List<GlowEffect> glowEffects = new ArrayList<>();

    private int trackedEntityId = Integer.MIN_VALUE;
    private long trackedAtMs;
    private Vector3d lastTrackedPos;
    private float lastTrackedWidth;
    private float lastTrackedHeight;
    private float lastTrackedYaw;
    private long lastStaticEffectMs;

    public KillEffect() {
        super("KillEffect", "Визуальный эффект при убийстве цели", Category.Visuals);
        addSettings(
                effectMode,
                particles,
                particleSize,
                deathHold,
                evaporation,
                riseHeight,
                chaos,
                throughWalls,
                colorMode,
                secondColor,
                color,
                color2,
                glowWidth,
                glowHeight
        );
    }

    @EventTarget
    private void onAttack(EventAttack event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        Entity entity = event.getTarget();
        if (!(entity instanceof LivingEntity) || entity == mc.player) {
            return;
        }

        trackedEntityId = entity.getEntityId();
        trackedAtMs = System.currentTimeMillis();
        updateLastTracked(entity);
    }

    @EventTarget
    private void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) {
            clearState();
            return;
        }

        if (trackedEntityId == Integer.MIN_VALUE) {
            cleanupRecentKills(System.currentTimeMillis());
            return;
        }

        long nowMs = System.currentTimeMillis();
        if (nowMs - trackedAtMs > TRACK_TARGET_MS) {
            resetTrackedEntity();
            return;
        }

        Entity tracked = mc.world.getEntityByID(trackedEntityId);
        if (tracked == null) {
            spawnFromLastKnownPosition(nowMs);
            resetTrackedEntity();
            return;
        }

        if (!(tracked instanceof LivingEntity) || tracked == mc.player) {
            resetTrackedEntity();
            return;
        }

        LivingEntity living = (LivingEntity) tracked;
        if (isDeadOrRemoved(living)) {
            if (!isRecentlySpawned(tracked.getEntityId(), nowMs)) {
                recentKills.add(new RecentKill(tracked.getEntityId(), nowMs));
                spawnFromEntity(living, nowMs);
            }
            resetTrackedEntity();
            return;
        }

        updateLastTracked(tracked);
    }

    @EventTarget
    private void onRender3D(EventRender3D event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        renderParticles();
        renderGlowEffects();
    }

    private void updateLastTracked(Entity entity) {
        lastTrackedPos = entity.getPositionVec();
        lastTrackedWidth = entity.getWidth();
        lastTrackedHeight = entity.getHeight();
        lastTrackedYaw = entity.rotationYaw;
    }

    private void spawnFromLastKnownPosition(long nowMs) {
        if (lastTrackedPos == null || isRecentlySpawned(trackedEntityId, nowMs)) {
            return;
        }

        recentKills.add(new RecentKill(trackedEntityId, nowMs));
        if (effectMode.is(MODE_PARTICLES)) {
            boolean playerLike = lastTrackedWidth <= 0.7f && lastTrackedHeight > 1.4f;
            ParticleModel model = makeFallbackModel(
                    lastTrackedPos,
                    lastTrackedWidth,
                    lastTrackedHeight,
                    lastTrackedYaw,
                    playerLike
            );
            spawnParticles(model, nowMs);
        } else {
            spawnStaticEffect(lastTrackedPos);
        }
    }

    private void spawnFromEntity(LivingEntity entity, long nowMs) {
        if (effectMode.is(MODE_PARTICLES)) {
            ParticleModel model = captureModel(entity, mc.getRenderPartialTicks());
            if (model.isEmpty()) {
                model = makeFallbackModel(
                        entity.getPositionVec(),
                        entity.getWidth(),
                        entity.getHeight(),
                        entity.rotationYaw,
                        entity instanceof PlayerEntity
                );
            }
            spawnParticles(model, nowMs);
            return;
        }

        spawnStaticEffect(entity.getPositionVec());
    }

    private ParticleModel captureModel(LivingEntity entity, float partialTicks) {
        List<Vector3d> capturedVertices = EntityModelCapture.capture(entity, partialTicks, MAX_CAPTURED_VERTICES);
        return ParticleModel.fromVertices(capturedVertices);
    }

    private void spawnParticles(ParticleModel model, long nowMs) {
        if (model.isEmpty()) {
            return;
        }

        int count = Math.max(1, Math.round(particles.get()));
        float chaosValue = chaos.get();

        for (int i = 0; i < count; ++i) {
            Vector3d worldPoint = model.randomPoint(random);
            Vector3d away = worldPoint.subtract(model.center);
            if (away.length() < 1.0E-5) {
                away = new Vector3d(
                        random.nextDouble() - 0.5,
                        random.nextDouble() * 0.4,
                        random.nextDouble() - 0.5
                );
            }

            Vector3d drift = away.normalize()
                    .scale((0.32 + random.nextDouble() * 0.58) * chaosValue)
                    .add(
                            (random.nextDouble() - 0.5) * 0.34 * chaosValue,
                            random.nextDouble() * 0.38 * chaosValue,
                            (random.nextDouble() - 0.5) * 0.34 * chaosValue
                    );

            activeParticles.add(new KillParticle(
                    worldPoint,
                    drift,
                    nowMs,
                    random.nextInt(1440) * (random.nextBoolean() ? 1 : -1),
                    random.nextFloat() * 360.0f,
                    (random.nextFloat() - 0.5f) * 210.0f,
                    random.nextFloat() * (float) Math.PI * 2.0f,
                    0.72f + random.nextFloat() * 0.65f
            ));
        }
    }

    private void renderParticles() {
        if (activeParticles.isEmpty()) {
            return;
        }

        long nowMs = System.currentTimeMillis();
        long holdMs = Math.max(0L, Math.round(deathHold.get()));
        long fadeMs = Math.max(1L, Math.round(evaporation.get()));
        long maxLifeMs = holdMs + fadeMs;

        activeParticles.removeIf(particle -> particle.isDead(nowMs, maxLifeMs));
        if (activeParticles.isEmpty()) {
            return;
        }

        ActiveRenderInfo camera = mc.getRenderManager().info;
        if (camera == null) {
            return;
        }

        Vector3d cameraPos = camera.getProjectedView();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.disableAlphaTest();
        if (throughWalls.get()) {
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
        }
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);

        mc.getTextureManager().bindTexture(GLOW_TEXTURE);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

        for (KillParticle particle : activeParticles) {
            float alpha = particle.getAlpha(nowMs, holdMs, fadeMs);
            if (alpha <= 1.0f / 255.0f) {
                continue;
            }

            Vector3d pos = particle.getPosition(nowMs, holdMs, fadeMs, chaos.get(), riseHeight.get());
            float size = particle.getSize(nowMs, holdMs, fadeMs, particleSize.get());
            int argb = ColorUtil.setAlpha(getParticleColor(particle.getColorSeed()), Math.round(alpha * 255.0f));

            drawBillboard(
                    buffer,
                    pos.x - cameraPos.x,
                    pos.y - cameraPos.y,
                    pos.z - cameraPos.z,
                    size * 0.5f,
                    particle.getRotation(nowMs),
                    camera,
                    argb
            );
        }

        tessellator.draw();

        RenderSystem.enableAlphaTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private int getParticleColor(int seed) {
        if (colorMode.is(COLOR_RAINBOW)) {
            float hue = (float) Math.floorMod(System.currentTimeMillis() / 12L + seed, 360L) / 360.0f;
            return Color.HSBtoRGB(hue, 1.0f, 1.0f) | 0xFF000000;
        }

        int firstColor;
        int secondColorValue;
        if (colorMode.is(COLOR_CLIENT)) {
            firstColor = ColorUtil.setAlpha(ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL), 255);
            secondColorValue = firstColor;
        } else {
            firstColor = color.get();
            secondColorValue = secondColor.get() ? color2.get() : color.get();
        }

        int angle = (int) Math.floorMod(System.currentTimeMillis() / 8L + seed, 360L);
        float progress = angle > 180 ? (float) (360 - angle) / 180.0f : (float) angle / 180.0f;
        return ColorUtil.interpolate(firstColor, secondColorValue, progress);
    }

    private void drawBillboard(BufferBuilder buffer, double x, double y, double z, float halfSize,
                               float angleDeg, ActiveRenderInfo camera, int argb) {
        MatrixStack matrices = new MatrixStack();
        matrices.push();
        matrices.translate(x, y, z);
        matrices.rotate(camera.getRotation().copy());
        if (angleDeg != 0.0f) {
            matrices.rotate(Vector3f.ZP.rotationDegrees(angleDeg));
        }

        Matrix4f matrix = matrices.getLast().getMatrix();
        int a = argb >>> 24 & 0xFF;
        int r = argb >>> 16 & 0xFF;
        int g = argb >>> 8 & 0xFF;
        int b = argb & 0xFF;

        buffer.pos(matrix, -halfSize, -halfSize, 0.0f).tex(1.0f, 1.0f).color(r, g, b, a).endVertex();
        buffer.pos(matrix, -halfSize, halfSize, 0.0f).tex(1.0f, 0.0f).color(r, g, b, a).endVertex();
        buffer.pos(matrix, halfSize, halfSize, 0.0f).tex(0.0f, 0.0f).color(r, g, b, a).endVertex();
        buffer.pos(matrix, halfSize, -halfSize, 0.0f).tex(0.0f, 1.0f).color(r, g, b, a).endVertex();

        matrices.pop();
    }

    private ParticleModel makeFallbackModel(Vector3d origin, float width, float height, float yaw, boolean playerLike) {
        if (!(width > 0.0f) || !(height > 0.0f)) {
            return ParticleModel.empty();
        }

        ArrayList<Vector3d> vertices = new ArrayList<>();
        if (playerLike) {
            addBox(vertices, origin, yaw, -0.25, 1.22, -0.25, 0.25, 1.72, 0.25);
            addBox(vertices, origin, yaw, -0.25, 0.72, -0.125, 0.25, 1.22, 0.125);
            addBox(vertices, origin, yaw, -0.43, 0.72, -0.105, -0.25, 1.22, 0.105);
            addBox(vertices, origin, yaw, 0.25, 0.72, -0.105, 0.43, 1.22, 0.105);
            addBox(vertices, origin, yaw, -0.24, 0.0, -0.105, -0.02, 0.72, 0.105);
            addBox(vertices, origin, yaw, 0.02, 0.0, -0.105, 0.24, 0.72, 0.105);
        } else {
            double half = Math.max(0.05, width * 0.5);
            double entityHeight = Math.max(0.1, height);
            addBox(vertices, origin, yaw, -half, 0.0, -half, half, entityHeight, half);
        }

        return ParticleModel.fromVertices(vertices);
    }

    private void addBox(List<Vector3d> vertices, Vector3d origin, float yaw,
                        double minX, double minY, double minZ,
                        double maxX, double maxY, double maxZ) {
        Vector3d nnn = transform(origin, yaw, minX, minY, minZ);
        Vector3d pnn = transform(origin, yaw, maxX, minY, minZ);
        Vector3d ppn = transform(origin, yaw, maxX, maxY, minZ);
        Vector3d npn = transform(origin, yaw, minX, maxY, minZ);
        Vector3d nnp = transform(origin, yaw, minX, minY, maxZ);
        Vector3d pnp = transform(origin, yaw, maxX, minY, maxZ);
        Vector3d ppp = transform(origin, yaw, maxX, maxY, maxZ);
        Vector3d npp = transform(origin, yaw, minX, maxY, maxZ);

        addQuad(vertices, nnn, pnn, ppn, npn);
        addQuad(vertices, pnp, nnp, npp, ppp);
        addQuad(vertices, nnp, nnn, npn, npp);
        addQuad(vertices, pnn, pnp, ppp, ppn);
        addQuad(vertices, npn, ppn, ppp, npp);
        addQuad(vertices, nnp, pnp, pnn, nnn);
    }

    private void addQuad(List<Vector3d> vertices, Vector3d a, Vector3d b, Vector3d c, Vector3d d) {
        vertices.add(a);
        vertices.add(b);
        vertices.add(c);
        vertices.add(d);
    }

    private Vector3d transform(Vector3d origin, float yaw, double x, double y, double z) {
        double radians = Math.toRadians(-yaw);
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        double rotatedX = x * cos - z * sin;
        double rotatedZ = x * sin + z * cos;
        return origin.add(rotatedX, y, rotatedZ);
    }

    private void spawnStaticEffect(Vector3d pos) {
        if (pos == null || !Double.isFinite(pos.x) || !Double.isFinite(pos.y) || !Double.isFinite(pos.z)) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastStaticEffectMs < 250L) {
            return;
        }
        lastStaticEffectMs = now;

        if (effectMode.is(MODE_LIGHTNING)) {
            spawnLightning(pos.x, pos.y, pos.z);
        } else if (effectMode.is(MODE_GLOW)) {
            glowEffects.add(new GlowEffect(pos, now));
        }
    }

    private void renderGlowEffects() {
        if (glowEffects.isEmpty()) {
            return;
        }

        ActiveRenderInfo camera = mc.getRenderManager().info;
        if (camera == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        Vector3d cameraPos = camera.getProjectedView();
        int themeColor = ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL);
        float themeAlpha = ThemeEditor.getAlpha(ThemeSettings.MODULE_VISUAL);

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.disableAlphaTest();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ZERO,
                GlStateManager.DestFactor.ONE
        );
        mc.getTextureManager().bindTexture(GLOW_TEXTURE);

        Iterator<GlowEffect> effectIterator = glowEffects.iterator();
        while (effectIterator.hasNext()) {
            GlowEffect effect = effectIterator.next();
            float progress = (currentTime - effect.startTime) / (GLOW_DURATION * 1000f);
            if (progress >= 1.0f) {
                effectIterator.remove();
                continue;
            }
            renderGlowPillar(camera, cameraPos, effect.position, progress, themeColor, themeAlpha);
        }

        RenderSystem.enableCull();
        RenderSystem.enableAlphaTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void renderGlowPillar(ActiveRenderInfo camera, Vector3d cameraPos, Vector3d basePos,
                                  float progress, int themeColor, float themeAlpha) {
        Vector3d effectPos = basePos.add(0, GLOW_Y_OFFSET, 0);

        float appear = getAppearProgress(progress);
        float beamDisappear = getBeamDisappear(progress);
        float beamFactor = appear * (1f - beamDisappear);
        float discFade = getDiscGlowFade(progress);

        drawFlatGlowDisc(cameraPos, effectPos, GLOW_DISC_RADIUS, themeColor, themeAlpha, appear * discFade);

        if (beamFactor <= 0.005f) {
            return;
        }

        float glowH = glowHeight.get();
        float beamHeight = glowH * beamFactor;
        float maxHeight = glowH * appear;
        float baseRadius = glowWidth.get();
        float softEdge = GLOW_SLICE_STEP * 0.55f;
        float tipZone = 0.14f;

        for (float y = 0f; y <= maxHeight; y += GLOW_SLICE_STEP) {
            float over = y - beamHeight;
            if (over > softEdge) {
                continue;
            }
            float edgeFade = over > 0f
                    ? (float) Math.pow(1f - MathHelper.clamp(over / softEdge, 0f, 1f), 2.2f)
                    : 1f;

            float heightT = beamHeight > 0.01f ? y / beamHeight : 0f;
            float tipFade = 1f;
            float tipRadius = 1f;
            if (heightT > 1f - tipZone) {
                float tipT = (heightT - (1f - tipZone)) / tipZone;
                tipFade = (float) Math.pow(1f - tipT, 2.6f);
                tipRadius = 0.28f + 0.72f * tipFade;
            }

            float sliceAlpha = appear * (1f - beamDisappear) * edgeFade * tipFade;
            if (sliceAlpha <= 0.005f) {
                continue;
            }

            float radius = baseRadius * tipRadius;
            Vector3d pos = effectPos.add(0, y, 0);
            drawGlowBillboard(camera, cameraPos, pos, radius * 1.95f, themeColor, themeAlpha, sliceAlpha * 0.2f);
            drawGlowBillboard(camera, cameraPos, pos, radius * 1.05f, themeColor, themeAlpha, sliceAlpha * 0.44f);
            drawGlowBillboard(camera, cameraPos, pos, radius * 0.5f, themeColor, themeAlpha, sliceAlpha * 0.8f);
        }
    }

    private float easeSmooth(float t) {
        t = MathHelper.clamp(t, 0f, 1f);
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    private float smootherstep(float t) {
        t = MathHelper.clamp(t, 0f, 1f);
        return t * t * t * (t * (t * 6f - 15f) + 10f);
    }

    private float getDiscGlowFade(float progress) {
        if (progress < GLOW_DISC_FADE_START) {
            return 1f;
        }
        float gone = MathHelper.clamp(
                (progress - GLOW_DISC_FADE_START) / (1f - GLOW_DISC_FADE_START),
                0f,
                1f
        );
        float remain = 1f - gone;
        return remain * remain * remain * remain * remain;
    }

    private float getAppearProgress(float progress) {
        if (progress >= GLOW_APPEAR_END) {
            return 1f;
        }
        return easeSmooth(progress / GLOW_APPEAR_END);
    }

    private float getBeamDisappear(float progress) {
        if (progress <= GLOW_BEAM_DISAPPEAR_START) {
            return 0f;
        }
        if (progress >= GLOW_BEAM_DISAPPEAR_END) {
            return 1f;
        }
        float t = (progress - GLOW_BEAM_DISAPPEAR_START)
                / (GLOW_BEAM_DISAPPEAR_END - GLOW_BEAM_DISAPPEAR_START);
        return smootherstep(t);
    }

    private void drawFlatGlowDisc(Vector3d cameraPos, Vector3d basePos,
                                  float radius, int themeColor, float themeAlpha, float alpha) {
        if (alpha <= 0.003f) {
            return;
        }

        MatrixStack stack = new MatrixStack();
        stack.translate(basePos.x - cameraPos.x, basePos.y - cameraPos.y + 0.03, basePos.z - cameraPos.z);
        stack.rotate(Vector3f.XP.rotationDegrees(90f));

        float baseAlpha = (themeAlpha / 255f) * alpha;
        int color = ColorUtil.applyOpacity(themeColor, baseAlpha);

        RenderSystem.pushMatrix();
        RenderSystem.multMatrix(stack.getLast().getMatrix());
        float size = radius * 2f;
        float outerMul = 0.28f * alpha;
        float midMul = 0.55f * alpha;
        RenderUtil.drawImage3D(GLOW_TEXTURE, -radius * 1.35f, -radius * 1.35f, 0f, size * 1.35f, size * 1.35f,
                ColorUtil.applyOpacity(themeColor, baseAlpha * outerMul), true);
        RenderUtil.drawImage3D(GLOW_TEXTURE, -radius, -radius, 0f, size, size,
                ColorUtil.applyOpacity(themeColor, baseAlpha * midMul), true);
        RenderUtil.drawImage3D(GLOW_TEXTURE, -radius * 0.55f, -radius * 0.55f, 0f, size * 0.55f, size * 0.55f, color, true);
        RenderSystem.popMatrix();
    }

    private void drawGlowBillboard(ActiveRenderInfo camera, Vector3d cameraPos, Vector3d worldPos,
                                   float size, int themeColor, float themeAlpha, float alpha) {
        if (alpha <= 0.01f) {
            return;
        }

        MatrixStack stack = new MatrixStack();
        stack.translate(worldPos.x - cameraPos.x, worldPos.y - cameraPos.y, worldPos.z - cameraPos.z);
        stack.rotate(camera.getRotation().copy());

        int color = ColorUtil.applyOpacity(themeColor, (themeAlpha / 255f) * alpha);

        RenderSystem.pushMatrix();
        RenderSystem.multMatrix(stack.getLast().getMatrix());
        RenderUtil.drawImage3D(GLOW_TEXTURE, -size * 0.5f, -size * 0.5f, 0f, size, size, color, true);
        RenderSystem.popMatrix();
    }

    private void spawnLightning(double x, double y, double z) {
        if (mc.world == null) {
            return;
        }

        final ClientWorld spawnWorld = mc.world instanceof ClientWorld ? (ClientWorld) mc.world : null;
        final double lx = x;
        final double ly = y;
        final double lz = z;

        mc.execute(() -> {
            if (mc.world == null || (spawnWorld != null && mc.world != spawnWorld)) {
                return;
            }
            LightningBoltEntity bolt = EntityType.LIGHTNING_BOLT.create(mc.world);
            if (bolt == null) {
                return;
            }
            bolt.setEffectOnly(false);
            bolt.setPosition(lx, ly, lz);

            try {
                if (mc.world instanceof ClientWorld) {
                    int id = allocateUniqueClientEntityId((ClientWorld) mc.world);
                    ((ClientWorld) mc.world).addEntity(id, bolt);
                } else {
                    mc.world.addEntity(bolt);
                }
            } catch (Throwable ignored) {
            }
        });

        mc.getSoundHandler().play(SimpleSound.master(
                SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
                0.8F + mc.world.rand.nextFloat() * 0.2F,
                0.5F
        ));

        mc.getSoundHandler().play(SimpleSound.master(
                SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT,
                0.8F + mc.world.rand.nextFloat() * 0.2F,
                0.5F
        ));
    }

    private int allocateUniqueClientEntityId(ClientWorld world) {
        int attempts = 0;
        int candidate = nextClientEffectEntityId;
        while (attempts++ < 100000) {
            if (world.getEntityByID(candidate) == null) {
                nextClientEffectEntityId = candidate - 1;
                if (nextClientEffectEntityId >= -1) {
                    nextClientEffectEntityId = -100000;
                }
                return candidate;
            }
            candidate--;
            if (candidate >= -1) {
                candidate = -100000;
            }
        }
        return -100000 - (int) (System.nanoTime() & 0x7FFF);
    }

    private boolean isDeadOrRemoved(LivingEntity entity) {
        return entity == null || !entity.isAlive() || entity.removed || entity.getHealth() <= 0.0f || entity.deathTime > 0;
    }

    private boolean isRecentlySpawned(int entityId, long nowMs) {
        Iterator<RecentKill> iterator = recentKills.iterator();
        while (iterator.hasNext()) {
            RecentKill stamp = iterator.next();
            if (nowMs - stamp.timeMs > SAME_ENTITY_COOLDOWN_MS) {
                iterator.remove();
                continue;
            }
            if (stamp.entityId == entityId) {
                return true;
            }
        }
        return false;
    }

    private void cleanupRecentKills(long nowMs) {
        recentKills.removeIf(stamp -> nowMs - stamp.timeMs > SAME_ENTITY_COOLDOWN_MS);
    }

    private float smoothStep(float value) {
        float t = MathHelper.clamp(value, 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    private float easeOutCubic(float value) {
        float inv = 1.0f - MathHelper.clamp(value, 0.0f, 1.0f);
        return 1.0f - inv * inv * inv;
    }

    private void resetTrackedEntity() {
        trackedEntityId = Integer.MIN_VALUE;
        trackedAtMs = 0L;
        lastTrackedPos = null;
        lastTrackedWidth = 0.0f;
        lastTrackedHeight = 0.0f;
        lastTrackedYaw = 0.0f;
    }

    private void clearState() {
        activeParticles.clear();
        recentKills.clear();
        glowEffects.clear();
        resetTrackedEntity();
    }

    @Override
    public void onDisable() {
        clearState();
        super.onDisable();
    }

    private static double triangleArea(Vector3d a, Vector3d b, Vector3d c) {
        Vector3d ab = b.subtract(a);
        Vector3d ac = c.subtract(a);
        double crossX = ab.y * ac.z - ab.z * ac.y;
        double crossY = ab.z * ac.x - ab.x * ac.z;
        double crossZ = ab.x * ac.y - ab.y * ac.x;
        return Math.sqrt(crossX * crossX + crossY * crossY + crossZ * crossZ) * 0.5;
    }

    private static Vector3d randomPointInTriangle(Vector3d a, Vector3d b, Vector3d c, Random random) {
        double u = random.nextDouble();
        double v = random.nextDouble();
        if (u + v > 1.0) {
            u = 1.0 - u;
            v = 1.0 - v;
        }
        return a.add(b.subtract(a).scale(u)).add(c.subtract(a).scale(v));
    }

    private static boolean isSaneCoordinate(double value) {
        return Double.isFinite(value) && Math.abs(value) < 3.0E7;
    }

    private static double lerp(double partialTicks, double previous, double current) {
        return previous + (current - previous) * partialTicks;
    }

    private static float lerpAngle(float partialTicks, float previous, float current) {
        return previous + partialTicks * MathHelper.wrapDegrees(current - previous);
    }

    private final class KillParticle {
        private final Vector3d anchor;
        private final Vector3d drift;
        private final long bornMs;
        private final int colorSeed;
        private final float startAngle;
        private final float spin;
        private final float phase;
        private final float sizeMultiplier;

        private KillParticle(Vector3d anchor, Vector3d drift, long bornMs, int colorSeed,
                             float startAngle, float spin, float phase, float sizeMultiplier) {
            this.anchor = anchor;
            this.drift = drift;
            this.bornMs = bornMs;
            this.colorSeed = colorSeed;
            this.startAngle = startAngle;
            this.spin = spin;
            this.phase = phase;
            this.sizeMultiplier = sizeMultiplier;
        }

        private boolean isDead(long nowMs, long maxLifeMs) {
            return nowMs - bornMs >= maxLifeMs;
        }

        private Vector3d getPosition(long nowMs, long holdMs, long fadeMs, float chaosValue, float riseValue) {
            float evaporationProgress = getEvaporation(nowMs, holdMs, fadeMs);
            if (evaporationProgress <= 0.0f) {
                return anchor;
            }

            float eased = easeOutCubic(evaporationProgress);
            double wave = Math.sin((nowMs - bornMs) * 0.012 + phase) * 0.075 * chaosValue * evaporationProgress;
            double sideWave = Math.cos((nowMs - bornMs) * 0.009 + phase * 1.37) * 0.055 * chaosValue * evaporationProgress;
            double rise = riseValue * eased * (0.75 + sizeMultiplier * 0.35);
            return anchor.add(drift.scale(eased)).add(wave, rise, sideWave);
        }

        private float getAlpha(long nowMs, long holdMs, long fadeMs) {
            long ageMs = nowMs - bornMs;
            float fadeIn = MathHelper.clamp((float) ageMs / 80.0f, 0.0f, 1.0f);
            float evaporationProgress = getEvaporation(nowMs, holdMs, fadeMs);
            if (evaporationProgress <= 0.0f) {
                return fadeIn;
            }
            return fadeIn * (1.0f - smoothStep(evaporationProgress));
        }

        private float getSize(long nowMs, long holdMs, long fadeMs, float baseSize) {
            float evaporationProgress = getEvaporation(nowMs, holdMs, fadeMs);
            float pulse = 1.0f + MathHelper.sin((float) (nowMs - bornMs) * 0.018f + phase) * 0.08f;
            float dissolveScale = 1.0f - smoothStep(evaporationProgress) * 0.45f;
            return baseSize * sizeMultiplier * pulse * dissolveScale;
        }

        private float getRotation(long nowMs) {
            return startAngle + (float) (nowMs - bornMs) / 1000.0f * spin;
        }

        private float getEvaporation(long nowMs, long holdMs, long fadeMs) {
            return MathHelper.clamp((float) (nowMs - bornMs - holdMs) / (float) fadeMs, 0.0f, 1.0f);
        }

        private int getColorSeed() {
            return colorSeed;
        }
    }

    private static final class RecentKill {
        private final int entityId;
        private final long timeMs;

        private RecentKill(int entityId, long timeMs) {
            this.entityId = entityId;
            this.timeMs = timeMs;
        }
    }

    private static final class GlowEffect {
        private final Vector3d position;
        private final long startTime;

        private GlowEffect(Vector3d position, long startTime) {
            this.position = position;
            this.startTime = startTime;
        }
    }

    private static final class ParticleModel {
        private final List<Vector3d> vertices;
        private final List<Surface> surfaces;
        private final double totalArea;
        private final Vector3d center;

        private ParticleModel(List<Vector3d> vertices, List<Surface> surfaces, double totalArea, Vector3d center) {
            this.vertices = vertices;
            this.surfaces = surfaces;
            this.totalArea = totalArea;
            this.center = center;
        }

        private static ParticleModel empty() {
            return new ParticleModel(new ArrayList<>(), new ArrayList<>(), 0.0, Vector3d.ZERO);
        }

        private static ParticleModel fromVertices(List<Vector3d> capturedVertices) {
            if (capturedVertices == null || capturedVertices.size() < 8) {
                return empty();
            }

            ArrayList<Vector3d> vertices = new ArrayList<>(capturedVertices.size());
            for (Vector3d vertex : capturedVertices) {
                if (vertex == null
                        || !isSaneCoordinate(vertex.x)
                        || !isSaneCoordinate(vertex.y)
                        || !isSaneCoordinate(vertex.z)) {
                    continue;
                }
                vertices.add(vertex);
            }
            if (vertices.size() < 8) {
                return empty();
            }

            Vector3d min = vertices.get(0);
            Vector3d max = vertices.get(0);
            for (Vector3d vertex : vertices) {
                min = new Vector3d(Math.min(min.x, vertex.x), Math.min(min.y, vertex.y), Math.min(min.z, vertex.z));
                max = new Vector3d(Math.max(max.x, vertex.x), Math.max(max.y, vertex.y), Math.max(max.z, vertex.z));
            }

            Vector3d center = new Vector3d(
                    (min.x + max.x) * 0.5,
                    (min.y + max.y) * 0.5,
                    (min.z + max.z) * 0.5
            );

            ArrayList<Surface> surfaces = new ArrayList<>();
            double totalArea = 0.0;
            for (int i = 0; i + 3 < vertices.size(); i += 4) {
                Surface surface = new Surface(vertices.get(i), vertices.get(i + 1), vertices.get(i + 2), vertices.get(i + 3));
                if (surface.area <= 1.0E-7) {
                    continue;
                }
                surfaces.add(surface);
                totalArea += surface.area;
            }

            return new ParticleModel(vertices, surfaces, totalArea, center);
        }

        private boolean isEmpty() {
            return vertices.isEmpty();
        }

        private Vector3d randomPoint(Random random) {
            if (!surfaces.isEmpty() && totalArea > 1.0E-7) {
                double cursor = random.nextDouble() * totalArea;
                for (Surface surface : surfaces) {
                    cursor -= surface.area;
                    if (cursor <= 0.0) {
                        return surface.randomPoint(random);
                    }
                }
                return surfaces.get(surfaces.size() - 1).randomPoint(random);
            }

            Vector3d vertex = vertices.get(random.nextInt(vertices.size()));
            return vertex.add(
                    (random.nextDouble() - 0.5) * 0.025,
                    (random.nextDouble() - 0.5) * 0.025,
                    (random.nextDouble() - 0.5) * 0.025
            );
        }
    }

    private static final class Surface {
        private final Vector3d a;
        private final Vector3d b;
        private final Vector3d c;
        private final Vector3d d;
        private final double firstTriangleArea;
        private final double area;

        private Surface(Vector3d a, Vector3d b, Vector3d c, Vector3d d) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
            this.firstTriangleArea = triangleArea(a, b, c);
            this.area = firstTriangleArea + triangleArea(a, c, d);
        }

        private Vector3d randomPoint(Random random) {
            if (random.nextDouble() * area <= firstTriangleArea) {
                return randomPointInTriangle(a, b, c, random);
            }
            return randomPointInTriangle(a, c, d, random);
        }
    }

    private static final class EntityModelCapture {
        private static List<Vector3d> capture(LivingEntity entity, float partialTicks, int maxVertices) {
            ArrayList<Vector3d> empty = new ArrayList<>();
            if (entity == null || maxVertices <= 0) {
                return empty;
            }

            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null || minecraft.getRenderManager() == null) {
                return empty;
            }

            try {
                @SuppressWarnings("unchecked")
                EntityRenderer<? super LivingEntity> renderer =
                        (EntityRenderer<? super LivingEntity>) minecraft.getRenderManager().getRenderer(entity);
                if (renderer == null) {
                    return empty;
                }

                CapturingRenderTypeBuffer buffer = new CapturingRenderTypeBuffer(maxVertices);
                MatrixStack matrices = new MatrixStack();

                double x = lerp(partialTicks, entity.lastTickPosX, entity.getPosX());
                double y = lerp(partialTicks, entity.lastTickPosY, entity.getPosY());
                double z = lerp(partialTicks, entity.lastTickPosZ, entity.getPosZ());
                float yaw = lerpAngle(partialTicks, entity.prevRotationYaw, entity.rotationYaw);

                boolean oldRenderName = renderer.isRenderName();
                try {
                    renderer.setRenderName(false);
                    matrices.push();
                    matrices.translate(x, y, z);
                    renderer.render(entity, yaw, partialTicks, matrices, buffer, FULL_BRIGHT);
                    matrices.pop();
                } finally {
                    renderer.setRenderName(oldRenderName);
                }

                return buffer.getVertices();
            } catch (Throwable ignored) {
                return empty;
            }
        }
    }

    private static final class CapturingRenderTypeBuffer implements IRenderTypeBuffer {
        private final CapturingVertexBuilder vertexBuilder;

        private CapturingRenderTypeBuffer(int maxVertices) {
            this.vertexBuilder = new CapturingVertexBuilder(maxVertices);
        }

        @Override
        public IVertexBuilder getBuffer(RenderType renderType) {
            return vertexBuilder;
        }

        private List<Vector3d> getVertices() {
            return vertexBuilder.getVertices();
        }
    }

    private static final class CapturingVertexBuilder implements IVertexBuilder {
        private final List<Vector3d> vertices = new ArrayList<>();
        private final int maxVertices;

        private double x;
        private double y;
        private double z;

        private CapturingVertexBuilder(int maxVertices) {
            this.maxVertices = maxVertices;
        }

        @Override
        public IVertexBuilder pos(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        @Override
        public IVertexBuilder pos(Matrix4f matrix, float x, float y, float z) {
            return pos(
                    matrix.getTransformX(x, y, z, 1.0f),
                    matrix.getTransformY(x, y, z, 1.0f),
                    matrix.getTransformZ(x, y, z, 1.0f)
            );
        }

        @Override
        public IVertexBuilder color(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public IVertexBuilder tex(float u, float v) {
            return this;
        }

        @Override
        public IVertexBuilder overlay(int u, int v) {
            return this;
        }

        @Override
        public IVertexBuilder lightmap(int u, int v) {
            return this;
        }

        @Override
        public IVertexBuilder normal(float x, float y, float z) {
            return this;
        }

        @Override
        public IVertexBuilder normal(Matrix3f matrix, float x, float y, float z) {
            return normal(x, y, z);
        }

        @Override
        public void endVertex() {
            addVertex(x, y, z);
        }

        private void addVertex(double x, double y, double z) {
            if (vertices.size() >= maxVertices
                    || !isSaneCoordinate(x)
                    || !isSaneCoordinate(y)
                    || !isSaneCoordinate(z)) {
                return;
            }
            vertices.add(new Vector3d(x, y, z));
        }

        private List<Vector3d> getVertices() {
            return vertices;
        }
    }
}
