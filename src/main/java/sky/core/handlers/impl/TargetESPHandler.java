package sky.core.handlers.impl;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import org.lwjgl.opengl.GL11;
import sky.core.SkyCore;
import sky.core.events.EventRender3D;
import sky.core.events.EventWorld3D;
import sky.core.modules.impl.combat.AimBot;
import sky.core.modules.impl.combat.AttackAura;
import sky.core.modules.impl.visuals.TargetESP;
import sky.core.utils.animation.AnimationUtil;
import sky.core.utils.animation.Easings;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.math.AnimationTest;
import sky.core.utils.math.Easing;
import sky.core.utils.math.MathUtil;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.GhostRenderer3D;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.TargetCrystalRender;

import static sky.core.utils.Wrapper.mc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TargetESPHandler {
    private static final AnimationUtil szAnim = new AnimationUtil(1f, 6f, Easings.LINEAR);
    private static final AnimationUtil alpAnim = new AnimationUtil(0f, 6f, Easings.LINEAR);
    private static Vector3d lstSqPos;
    private static float lstSqHH;
    private static boolean lstHT;
    private static long sqRotMs;
    private static double sqRotPh;
    private static Entity lstSqEnt;
    private static final AnimationUtil htScAnim = new AnimationUtil(1f, 6f, Easings.LINEAR);
    private static final AnimationUtil htClAnim = new AnimationUtil(0f, 6f, Easings.LINEAR);
    private static LivingEntity target;
    private LivingEntity crystalsLastTarget = null;
    private final AnimationTest crystalsAnim = new AnimationTest(Easing.LINEAR, 300);

    private static final AnimationUtil nigerAnim = new AnimationUtil(0f, 6f, Easings.CUBIC_IN_OUT);
    private static float nigerPlavnost = 0f;
    private static LivingEntity nigerTarget;

    private static final ResourceLocation BLOOM_TEX = new ResourceLocation("SkyCore/icons/world_render/bloom.png");
    private static final ResourceLocation BLOOM_TEX4 = new ResourceLocation("SkyCore/icons/world_render/firepart2.png");
    private static final int MAX_FOLLOW_GHOSTS = 3;
    private static final float FOLLOW_ALPHA_STEP = 0.005f;
    private static final float FOLLOW_MOTION_MUL = 0.05f;
    private static final float FOLLOW_BASE_SIZE = 0.3f;
    private final CopyOnWriteArrayList<GlowPoint> glowPoints = new CopyOnWriteArrayList<>();
    private LivingEntity ghostTarget;
    private static final int PIG_FULL_BRIGHT = 15728880;
    private final AnimationTest ghostAnim = new AnimationTest(Easing.EASE_OUT_CIRC, 400);
    private final List<GhostRenderer3D> followGhosts = new ArrayList<>();
    private LivingEntity followGhostTarget;
    private float followSpin;
    private float jelloMoving;
    private long lastDamageTime;
    private int lastTargetHurtTime;
    private float damageFlashIntensity;
    private static final long DAMAGE_FLASH_DURATION = 350L;

    private TargetESP getTargetESPModule() {
        return SkyCore.getInstance().getModuleManager().getTargetESP();
    }

    private boolean isTargetESPMode(String mode) {
        TargetESP targetESP = getTargetESPModule();
        return targetESP != null && targetESP.isEnabled() && targetESP.getMode().is(mode);
    }

    private boolean isCrystalMode() {
        TargetESP targetESP = getTargetESPModule();
        if (targetESP == null || !targetESP.isEnabled()) return false;
        return targetESP.getMode().is("Кристаллы")
                || targetESP.getMode().is("Кристалики")
                || targetESP.getMode().is("Кристалы");
    }

    private AttackAura getAura() {
        return SkyCore.getInstance().getModuleManager().getAttackAura();
    }

    private AimBot getAimBot() {
        return (AimBot) SkyCore.getInstance().getModuleManager().getModule(AimBot.class);
    }

    /** Цели для ESP: Projectile Helper и AttackAura (обе сразу, без дубликатов). */
    private List<LivingEntity> getEspTargets() {
        List<LivingEntity> list = new ArrayList<>();
        AimBot aimBot = getAimBot();
        if (aimBot != null && aimBot.isEnabled() && aimBot.isAiming()) {
            LivingEntity aimTarget = aimBot.getTarget();
            if (aimTarget != null && aimTarget.isAlive()) {
                list.add(aimTarget);
            }
        }
        AttackAura aura = getAura();
        if (aura != null && aura.isEnabled()) {
            LivingEntity auraTarget = aura.getTarget();
            if (auraTarget != null && auraTarget.isAlive() && !list.contains(auraTarget)) {
                list.add(auraTarget);
            }
        }
        return list;
    }

    private boolean hasEspTargets() {
        return !getEspTargets().isEmpty();
    }

    @EventTarget
    public void onRender3D(EventRender3D e) {
        renderSquare(e);
        renderCircle(e);
    }

    @EventTarget
    public void onWorld3D(EventWorld3D e) {
        sada(e);
        renderJello(e);
        handleGhost(e);
        renderetoROKSTAR(e);
        renderGhostRider(e);
        spawnGhostPoints(e);
        renderPigs(e);
    }

    @EventTarget
    private void renderSquare(EventRender3D e) {
        boolean rombMode = isTargetESPMode("Ромб");
        boolean romb2Mode = isTargetESPMode("Ромб-2");
        if (!rombMode && !romb2Mode) {
            return;
        }

        List<LivingEntity> targets = getEspTargets();
        if (!targets.isEmpty()) {
            alpAnim.update(1f);
            szAnim.update(1f);
            for (LivingEntity t : targets) {
                renderSquareAt(e, t, rombMode, romb2Mode, true);
            }
            lstHT = true;
            lstSqEnt = targets.get(targets.size() - 1);
            return;
        }

        ActiveRenderInfo cm = mc.getRenderManager().info;
        Vector3d cp = cm.getProjectedView();
        alpAnim.update(0f);
        szAnim.update(2f);
        if (alpAnim.getValue() <= 0.01f) {
            lstHT = false;
            lstSqEnt = null;
            return;
        }

        Vector3d bp;
        float hh;
        Entity f = lstSqEnt;
        if (f != null && f.isAlive()) {
            bp = MathUtil.interpolate(f, e.getPartialTicks());
            hh = f.getHeight() / 2f;
            lstSqPos = bp;
            lstSqHH = hh;
        } else {
            if (lstSqPos == null || !lstHT) return;
            bp = lstSqPos;
            hh = lstSqHH;
        }

        renderSquareAt(e, (LivingEntity) f, rombMode, romb2Mode, false, bp, hh, cp, cm);
    }

    private void renderSquareAt(EventRender3D e, LivingEntity t, boolean rombMode, boolean romb2Mode, boolean active) {
        ActiveRenderInfo cm = mc.getRenderManager().info;
        Vector3d cp = cm.getProjectedView();
        Vector3d bp = MathUtil.interpolate(t, e.getPartialTicks());
        float hh = t.getHeight() / 2f;
        renderSquareAt(e, t, rombMode, romb2Mode, active, bp, hh, cp, cm);
    }

    private void renderSquareAt(EventRender3D e, LivingEntity t, boolean rombMode, boolean romb2Mode, boolean active,
                                Vector3d bp, float hh, Vector3d cp, ActiveRenderInfo cm) {
        MatrixStack m = new MatrixStack();
        m.translate(bp.x - cp.x, bp.y + hh - cp.y, bp.z - cp.z);
        m.rotate(cm.getRotation().copy());
        float hf = t != null && t.maxHurtTime > 0
                ? MathHelper.clamp((float) t.hurtTime / t.maxHurtTime, 0f, 1f) : 0f;
        htScAnim.update(MathHelper.lerp(hf, 1f, 0.5f));
        htClAnim.update(hf);
        float sz = szAnim.getValue() * htScAnim.getValue();
        long n = System.currentTimeMillis();
        double dt = sqRotMs == 0 ? 0 : (n - sqRotMs) / 1000.0;
        sqRotMs = n;
        double spin = romb2Mode ? -2.15 : 1.667;
        sqRotPh += spin * (active ? 1 : 1.5) * dt;
        RenderSystem.pushMatrix();
        RenderSystem.multMatrix(m.getLast().getMatrix());
        float rotMul = romb2Mode ? 260f : 180f;
        float sizeMul = romb2Mode ? 1.18f : 1.0f;
        RenderSystem.rotatef((float) (Math.sin(sqRotPh) * rotMul), 0, 0, 1);
        RenderSystem.disableDepthTest();
        RenderSystem.disableAlphaTest();
        RenderSystem.disableCull();
        int bc = ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL);
        float mx = htClAnim.getValue();
        int cl = ColorUtil.getColor(MathHelper.clamp((int) (ColorUtil.red(bc) * (1f - mx) + 255 * mx), 0, 255), MathHelper.clamp((int) (ColorUtil.green(bc) * (1f - mx)), 0, 255), MathHelper.clamp((int) (ColorUtil.blue(bc) * (1f - mx)), 0, 255), (int) (ThemeEditor.getAlpha(ThemeSettings.MODULE_VISUAL) * MathHelper.clamp(alpAnim.getValue(), 0f, 1f)));
        float finalSize = sz * sizeMul;
        RenderUtil.drawImage3D(new ResourceLocation("SkyCore/icons/world_render/target.png"), -finalSize / 2f, -finalSize / 2f, 0, finalSize, finalSize, cl, true);
        RenderSystem.enableCull();
        RenderSystem.enableAlphaTest();
        RenderSystem.enableDepthTest();
        RenderSystem.popMatrix();
    }

    @EventTarget
    private void renderCircle(EventRender3D e) {
        if (!isTargetESPMode("Кольцо")) return;
        for (LivingEntity t : getEspTargets()) {
            renderCircleAt(e, t);
        }
    }

    private void renderCircleAt(EventRender3D e, LivingEntity t) {
        float hf = t.maxHurtTime > 0 ? MathHelper.clamp((float) t.hurtTime / t.maxHurtTime, 0f, 1f) : 0f;
        float r = t.getWidth() * 0.8F;
        Vector3d v = MathUtil.interpolate(t, e.getPartialTicks());
        double d = 3000, el = System.currentTimeMillis() % d;
        boolean s = el > d / 2;
        double p = el / (d / 2);
        p = s ? p - 1 : 1 - p;
        p = p < 0.5 ? 2 * p * p : 1 - Math.pow(-2 * p + 2, 2) / 2;
        double ea = (t.getHeight() / 2) * (p > 0.5 ? 1 - p : p) * (s ? -1 : 1);
        BufferBuilder b = Tessellator.getInstance().getBuffer();
        MatrixStack st = new MatrixStack();
        Vector3d o = mc.getRenderManager().info.getProjectedView();
        st.push();
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.disableAlphaTest();
        RenderSystem.shadeModel(7425);
        RenderSystem.disableCull();
        GlStateManager.depthMask(false);
        RenderSystem.lineWidth(2f);
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        RenderSystem.disableDepthTest();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        b.begin(8, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= 360; ++i) {
            int bcc = ColorUtil.gradientModule(i * 5);
            int cll = ColorUtil.getColor(MathHelper.clamp((int) (ColorUtil.red(bcc) * (1f - hf) + 60 * hf), 0, 255), MathHelper.clamp((int) (ColorUtil.green(bcc) * (1f - hf)), 0, 255), MathHelper.clamp((int) (ColorUtil.blue(bcc) * (1f - hf)), 0, 255), 255);
            double c = Math.cos(Math.toRadians(i)), si = Math.sin(Math.toRadians(i));
            b.pos(st.getLast().getMatrix(), (float) (v.x + c * r - o.x), (float) (v.y + t.getHeight() * p - o.y), (float) (v.z + si * r - o.z)).color(cll).endVertex();
            b.pos(st.getLast().getMatrix(), (float) (v.x + c * r - o.x), (float) (v.y + t.getHeight() * p + ea - o.y), (float) (v.z + si * r - o.z)).color(ColorUtil.applyOpacity(cll, 0)).endVertex();
        }
        Tessellator.getInstance().draw();
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        b.begin(2, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= 360; ++i) {
            int bcc = ColorUtil.gradientModule(i * 5);
            double c = Math.cos(Math.toRadians(i)), si = Math.sin(Math.toRadians(i));
            b.pos(st.getLast().getMatrix(), (float) (v.x + c * r - o.x), (float) (v.y + t.getHeight() * p - o.y), (float) (v.z + si * r - o.z)).color(ColorUtil.getColor(MathHelper.clamp((int) (ColorUtil.red(bcc) * (1f - hf) + 60 * hf), 0, 255), MathHelper.clamp((int) (ColorUtil.green(bcc) * (1f - hf)), 0, 255), MathHelper.clamp((int) (ColorUtil.blue(bcc) * (1f - hf)), 0, 255), 255)).endVertex();
        }
        Tessellator.getInstance().draw();
        RenderSystem.enableDepthTest();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.enableTexture();
        RenderSystem.enableAlphaTest();
        RenderSystem.shadeModel(7424);
        GlStateManager.depthMask(true);
        st.pop();
    }

    @EventTarget
    private void sada(EventWorld3D eventRender3D) {
        if (!isCrystalMode()) {
            return;
        }
        TargetESP targetESP = getTargetESPModule();
        List<LivingEntity> targets = getEspTargets();
        if (!targets.isEmpty()) {
            target = targets.get(0);
            crystalsLastTarget = targets.get(0);
        }
        crystalsAnim.run(targets.isEmpty() ? 0 : 1);
        if (crystalsLastTarget == null || crystalsAnim.getValue() <= 0.001) {
            if (targets.isEmpty()) {
                crystalsLastTarget = null;
            }
            return;
        }
        float anim = (float) crystalsAnim.getValue();
        float scatter = 1.0f - anim;
        float size = targetESP != null ? targetESP.getCrystalSize().get() : 2.0f;
        for (LivingEntity crystalTarget : targets) {
            TargetCrystalRender.render(eventRender3D, crystalTarget, anim, scatter, size);
        }
        if (anim <= 0.01f && targets.isEmpty()) {
            crystalsLastTarget = null;
        }
    }

    @EventTarget
    private void renderJello(EventWorld3D event) {
        if (!isTargetESPMode("Кружочек")) return;

        List<LivingEntity> targets = getEspTargets();
        if (!targets.isEmpty()) {
            target = targets.get(0);
            for (LivingEntity current : targets) {
                if (current.hurtTime > lastTargetHurtTime) {
                    lastDamageTime = System.currentTimeMillis();
                }
                lastTargetHurtTime = Math.max(lastTargetHurtTime, current.hurtTime);
            }
        } else {
            lastTargetHurtTime = 0;
        }

        timeAnimation.update(targets.isEmpty() ? 0f : 1f);
        float alpha = MathHelper.clamp(timeAnimation.getValue(), 0.0F, 1.0F);
        if (alpha <= 0.0F) return;

        TargetESP targetESP = getTargetESPModule();
        if (targetESP == null) return;

        List<LivingEntity> toRender = new ArrayList<>(targets);
        if (toRender.isEmpty() && target != null && target.isAlive()) {
            toRender.add(target);
        }
        if (toRender.isEmpty()) return;

        jelloMoving += 2.5F;
        for (LivingEntity entityToRender : toRender) {
            renderJelloAt(event, entityToRender, targetESP, alpha);
        }
    }

    private void renderJelloAt(EventWorld3D event, LivingEntity entityToRender, TargetESP targetESP, float alpha) {

        Vector3d cameraPos = mc.getRenderManager().info.getProjectedView();
        Vector3d targetPos = MathUtil.interpolate(entityToRender, event.partialTicks);
        Vector3d renderPos = targetPos.subtract(cameraPos);

        float entityWidth = entityToRender.getWidth() * 1.65F;
        float entityHeight = entityToRender.getHeight() - 0.15F;
        int color = applyDamageFlash(ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL), targetESP.getDamageRed().get());
        int r = ColorUtil.red(color);
        int g = ColorUtil.green(color);
        int b = ColorUtil.blue(color);

        MatrixStack matrices = event.getMatrixStack();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();

        if (targetESP.getThroughWalls().get()) {
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
        }

        matrices.push();
        matrices.translate(renderPos.x, renderPos.y, renderPos.z);
        mc.getTextureManager().bindTexture(BLOOM_TEX);

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

        for (int i = 0; i < 360; i += 2) {
            float scale = Math.max(0.5F, 0.7F - 0.2F * alpha);
            double rad = Math.toRadians(i + jelloMoving);
            float xOffset = (float) (Math.cos(rad) * entityWidth * scale);
            float zOffset = (float) (Math.sin(rad) * entityWidth * scale);
            float sizeBase = 0.2F;

            for (int jLayer = 0; jLayer < 15; ++jLayer) {
                float yOffsetLayer = entityHeight / 1.7F + (entityHeight / 2.0F) * (float) Math.cos(Math.toRadians(jelloMoving / 1.5F + jLayer * 2.0F));
                matrices.push();
                matrices.translate(xOffset, yOffsetLayer, zOffset);
                matrices.rotate(mc.getRenderManager().getCameraOrientation());

                Matrix4f mat = matrices.getLast().getMatrix();
                int finalAlpha = (int) (255 * alpha * ((float) jLayer / 15.0F) * 0.05F);
                buffer.pos(mat, -sizeBase / 2.0F, -sizeBase / 2.0F, 0).tex(0, 0).color(r, g, b, finalAlpha).endVertex();
                buffer.pos(mat, sizeBase / 2.0F, -sizeBase / 2.0F, 0).tex(1, 0).color(r, g, b, finalAlpha).endVertex();
                buffer.pos(mat, sizeBase / 2.0F, sizeBase / 2.0F, 0).tex(1, 1).color(r, g, b, finalAlpha).endVertex();
                buffer.pos(mat, -sizeBase / 2.0F, sizeBase / 2.0F, 0).tex(0, 1).color(r, g, b, finalAlpha).endVertex();
                matrices.pop();
            }
        }
        Tessellator.getInstance().draw();
        matrices.pop();

        if (!targetESP.getThroughWalls().get()) {
            RenderSystem.depthMask(true);
        }
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private int applyDamageFlash(int color, boolean enabled) {
        if (!enabled) return color;

        float targetIntensity = 0.0F;
        long timeSinceDamage = System.currentTimeMillis() - this.lastDamageTime;
        if (timeSinceDamage < DAMAGE_FLASH_DURATION) {
            float progress = (float) timeSinceDamage / (float) DAMAGE_FLASH_DURATION;
            targetIntensity = 1.0F - (1.0F - MathHelper.clamp(progress, 0.0F, 1.0F)) * (1.0F - MathHelper.clamp(progress, 0.0F, 1.0F));
        }

        this.damageFlashIntensity = MathHelper.lerp(0.1F, this.damageFlashIntensity, targetIntensity);
        if (this.damageFlashIntensity < 0.05F) return color;

        int rr = ColorUtil.red(color);
        int gg = ColorUtil.green(color);
        int bb = ColorUtil.blue(color);
        int aa = ColorUtil.alpha(color);

        int finalR = (int) MathHelper.lerp(this.damageFlashIntensity, rr, 255);
        int finalG = (int) MathHelper.lerp(this.damageFlashIntensity, gg, 50);
        int finalB = (int) MathHelper.lerp(this.damageFlashIntensity, bb, 50);
        return ColorUtil.getColor(finalR, finalG, finalB, aa);
    }

    private static final AnimationUtil timeAnimation = new AnimationUtil(1f, 6f, Easings.LINEAR);
    private long startTime = System.currentTimeMillis();
    private float animationSpeed = 0.0f;

    @EventTarget
    private void handleGhost(EventWorld3D eventRender) {
        boolean enabled = isTargetESPMode("Призраки");

        if (!enabled) return;

        List<LivingEntity> targets = getEspTargets();
        if (!targets.isEmpty()) {
            target = targets.get(0);
        }

        if (targets.isEmpty() && target == null) {
            timeAnimation.update(1);
            startTime = System.currentTimeMillis();
            return;
        }

        animationSpeed = (animationSpeed + (9 * (System.currentTimeMillis() - startTime) / 1000.0f));
        startTime = System.currentTimeMillis();
        timeAnimation.update(hasEspTargets() ? 1 : 0);

        List<LivingEntity> toRender = new ArrayList<>(targets);
        if (toRender.isEmpty() && target != null && target.isAlive()) {
            toRender.add(target);
        }
        for (LivingEntity ghostEntity : toRender) {
            renderGhostAt(eventRender, ghostEntity);
        }
    }

    private void renderGhostAt(EventWorld3D eventRender, LivingEntity ghostEntity) {
        float hf = ghostEntity.maxHurtTime > 0 ? MathHelper.clamp((float) ghostEntity.hurtTime / ghostEntity.maxHurtTime, 0f, 1f) : 0f;
        htClAnim.update(hf);

        MatrixStack e = eventRender.getMatrixStack();
        Vector3d vector3d = MathUtil.interpolate(ghostEntity, eventRender.partialTicks);

        final double x = vector3d.x - mc.getRenderManager().info.getProjectedView().x;
        final double y = vector3d.y - mc.getRenderManager().info.getProjectedView().y;
        final double z = vector3d.z - mc.getRenderManager().info.getProjectedView().z;

        int rings = 3;
        int n3 = 12;
        int n4 = 3 * rings;
        float size = 50;

        for (int i = 0; i < n4; i += rings) {
            for (int j = 0; j < n3; ++j) {
                int moduleVisual = ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL);
                float mx = htClAnim.getValue();
                int cl = ColorUtil.getColor(
                        MathHelper.clamp((int) (ColorUtil.red(moduleVisual) * (1f - mx) + 255 * mx), 0, 255),
                        MathHelper.clamp((int) (ColorUtil.green(moduleVisual) * (1f - mx)), 0, 255),
                        MathHelper.clamp((int) (ColorUtil.blue(moduleVisual) * (1f - mx)), 0, 255),
                        (int) (ThemeEditor.getAlpha(ThemeSettings.MODULE_VISUAL) * timeAnimation.getValue())
                );

                float f2 = animationSpeed + j * 0.1f;
                float f3 = 0.8f;
                float f4 = 0.5f;
                int n5 = (int) Math.pow(i, 2.0);

                e.push();
                e.translate((x + (f3 * MathHelper.sin(f2 + n5))), y + f4 + (0.3f * MathHelper.sin(animationSpeed + j * 0.2f)) + (0.2f * i), (z + (f3 * MathHelper.cos(f2 - n5))));
                e.scale(timeAnimation.getValue() * (0.005f + j / 1800.0f));
                e.rotate(mc.getRenderManager().getCameraOrientation());

                RenderSystem.disableDepthTest();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                RenderSystem.disableAlphaTest();
                RenderSystem.shadeModel(7425);

                RenderUtil.drawTexture(e, BLOOM_TEX4, -size / 2, -size / 2, size, size, cl);

                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                RenderSystem.enableDepthTest();
                RenderSystem.enableAlphaTest();
                RenderSystem.shadeModel(7424);
                e.pop();
            }
        }
    }

    private void renderetoROKSTAR(EventWorld3D eventRender) {
        boolean enabled = isTargetESPMode("Души");

        if (!enabled) return;

        List<LivingEntity> targets = getEspTargets();
        if (!targets.isEmpty()) {
            nigerTarget = targets.get(0);
        }

        nigerAnim.update(targets.isEmpty() ? 0 : 1);
        nigerPlavnost = (System.currentTimeMillis() % 100000) / 1.5f;

        if (nigerAnim.getValue() <= 0.01f) {
            if (targets.isEmpty()) {
                nigerTarget = null;
            }
            return;
        }

        List<LivingEntity> toRender = new ArrayList<>(targets);
        if (toRender.isEmpty() && nigerTarget != null && nigerTarget.isAlive()) {
            toRender.add(nigerTarget);
        }
        for (LivingEntity soulTarget : toRender) {
            renderSoulsAt(eventRender, soulTarget);
        }

        if (targets.isEmpty() && nigerAnim.getValue() <= 0.01f) {
            nigerTarget = null;
        }
    }

    private void renderSoulsAt(EventWorld3D eventRender, LivingEntity nigerTarget) {
        MatrixStack ms = eventRender.getMatrixStack();
        Vector3d pos = MathUtil.interpolate(nigerTarget, eventRender.partialTicks);

        double x = pos.x - mc.getRenderManager().info.getProjectedView().x;
        double y = pos.y - mc.getRenderManager().info.getProjectedView().y;
        double z = pos.z - mc.getRenderManager().info.getProjectedView().z;

        float width = nigerTarget.getWidth() * 1.5f;
        int color = ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL);
        float alpha = (float) (ThemeEditor.getAlpha(ThemeSettings.MODULE_VISUAL) * nigerAnim.getValue() / 255.0);
        ms.push();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.disableAlphaTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();
        mc.getTextureManager().bindTexture(BLOOM_TEX);
        RenderSystem.shadeModel(7425);
        builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        int r = ColorUtil.red(color);
        int g = ColorUtil.green(color);
        int b = ColorUtil.blue(color);

        int step = 2;
        int wormTick = 0;
        int wormCD = 0;

        for (int i = 0; i < 360; i += step) {
            float size = 0.13f + 0.005f * wormTick;
            float bigSize = 0.7f + 0.005f * wormTick;
            if (wormCD > 0) {
                wormCD -= step;
                continue;
            }
            if ((wormTick += step) > 50) {
                wormCD = 100;
                wormTick = 0;
                continue;
            }
            float val = Math.max(0.5f, 1.2f - 0.5f * nigerAnim.getValue());
            float angleRad = (float) Math.toRadians(i + nigerPlavnost);
            float sin = (float) (Math.sin(angleRad) * width * val);
            float cos = (float) (Math.cos(angleRad) * width * val);
            float waveY = (float) Math.sin(Math.toRadians(i / 2.0f + nigerPlavnost / 5.0f));
            ms.push();
            ms.translate(
                    x + sin,
                    y + (nigerTarget.getHeight() / 1.5f) + (nigerTarget.getHeight() / 3.0f) * waveY,
                    z + cos
            );

            ms.rotate(mc.getRenderManager().getCameraOrientation());
            putBloomQuad(builder, ms, -bigSize / 2.0f, -bigSize / 2.0f, bigSize, r, g, b, alpha * 0.05f);
            putBloomQuad(builder, ms, -size / 2.0f, -size / 2.0f, size, r, g, b, alpha);
            ms.pop();
        }

        tessellator.draw();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableAlphaTest();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        ms.pop();
    }

    private void spawnGhostPoints(EventWorld3D e) {
        if (!isTargetESPMode("Преследующие призраки")) {
            glowPoints.clear();
            return;
        }

        List<LivingEntity> targets = getEspTargets();
        if (targets.isEmpty()) return;

        TargetESP targetESP = getTargetESPModule();
        if (targetESP == null) return;

        int count = Math.max(1, Math.round(targetESP.getGhostCount().get()));
        int maxTime = Math.max(50, Math.round(targetESP.getGhostLifeTime().get()));
        int color = ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL);
        for (LivingEntity current : targets) {
            if (!current.isAlive()) continue;
            addGlowPoints(current, count, e.partialTicks, maxTime, color, targetESP.getStrengthXZ().get(), targetESP.getStrengthY().get());
        }
        removeOldGlowPoints();
        drawGlowPoints(e);
    }

    private void addGlowPoints(LivingEntity entity, int count, float partialTicks, int maxTime, int colorBase, float strengthXZ, float strengthY) {
        final float x = (float) (entity.lastTickPosX + (entity.getPosX() - entity.lastTickPosX) * partialTicks);
        final float y = (float) (entity.lastTickPosY + (entity.getPosY() - entity.lastTickPosY) * partialTicks);
        final float z = (float) (entity.lastTickPosZ + (entity.getPosZ() - entity.lastTickPosZ) * partialTicks);
        final float xzRange = entity.getWidth();
        final float yRange = entity.getHeight();

        int delayXZ = Math.max(1, (int) strengthXZ);
        int delayY = Math.max(1, (int) strengthY);
        long time = System.currentTimeMillis();

        for (int corner = 0; corner < count; corner++) {
            float cornerProgress = corner / (float) count;
            float xzRotate = ((time + (int) (delayXZ * cornerProgress)) % delayXZ) / (float) delayXZ * 360F;
            float yProgress = ((time + (int) (delayY * cornerProgress)) % delayY) / (float) delayY;
            yProgress = (yProgress > 0.5F ? 1.0F - yProgress : yProgress) * 2.0F;
            yProgress = (float) Math.pow(yProgress, 2);
            double yawRad = Math.toRadians(MathHelper.wrapDegrees(cornerProgress * 360.0F + xzRotate));
            final float xPos = x - (float) Math.sin(yawRad) * xzRange;
            final float yPos = y + yRange * yProgress;
            final float zPos = z + (float) Math.cos(yawRad) * xzRange;
            glowPoints.add(new GlowPoint(xPos, yPos, zPos, maxTime, colorBase));
        }
    }

    private void removeOldGlowPoints() {
        glowPoints.removeIf(GlowPoint::shouldRemove);
    }

    private void drawGlowPoints(EventWorld3D event) {
        if (glowPoints.isEmpty()) return;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        MatrixStack matrixStack = event.getMatrixStack();
        for (GlowPoint point : glowPoints) {
            point.update();
            float timeProgress = point.getTimeProgress();
            float scale = 0.5f * (1.0f - timeProgress);
            int color = point.getColor(timeProgress);

            matrixStack.push();
            Vector3d view = mc.getRenderManager().info.getProjectedView();
            matrixStack.translate(point.x - view.x, point.y - view.y, point.z - view.z);
            matrixStack.rotate(mc.getRenderManager().getCameraOrientation());
            matrixStack.scale(scale, scale, scale);
            RenderUtil.drawTexture(matrixStack, BLOOM_TEX4, -0.5f, -0.5f, 1.0f, 1.0f, color);
            matrixStack.pop();
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void renderGhostRider(EventWorld3D eventRender) {
        if (!isTargetESPMode("Души 2")) return;

        List<LivingEntity> targets = getEspTargets();
        if (!targets.isEmpty()) {
            ghostTarget = targets.get(0);
        }
        ghostAnim.run(targets.isEmpty() ? 0.0 : 1.0);
        if (ghostAnim.getValue() < 0.01) {
            if (targets.isEmpty()) {
                ghostTarget = null;
            }
            return;
        }

        List<LivingEntity> toRender = new ArrayList<>(targets);
        if (toRender.isEmpty() && ghostTarget != null && ghostTarget.isAlive()) {
            toRender.add(ghostTarget);
        }
        for (LivingEntity riderTarget : toRender) {
            renderGhostRiderAt(eventRender, riderTarget);
        }
    }

    private void renderGhostRiderAt(EventWorld3D eventRender, LivingEntity ghostTarget) {
        Vector3d vec = MathUtil.interpolate(ghostTarget, eventRender.partialTicks);
        double baseX = vec.x - mc.getRenderManager().info.getProjectedView().x;
        double baseY = vec.y - mc.getRenderManager().info.getProjectedView().y + (ghostTarget.getHeight() / 2.0);
        double baseZ = vec.z - mc.getRenderManager().info.getProjectedView().z;

        float alphaPC = (float) ghostAnim.getValue();
        int baseColor = ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL);
        long time = System.currentTimeMillis();

        MatrixStack ms = eventRender.getMatrixStack();
        ms.push();

        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.disableAlphaTest();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.shadeModel(7425);
        mc.getTextureManager().bindTexture(BLOOM_TEX4);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        ActiveRenderInfo camera = mc.getRenderManager().info;

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

        for (int i = 0; i < 20; i++) {
            float trailFactor = 1.0F - (float) i / 20.0F * 0.7F;
            double angle = 0.15F * ((double) time - (double) i * 10.0F) / 25.0F;
            drawOrbitalParticle(ms, buffer, camera, baseX, baseY, baseZ, angle, 1, 1, -1, (float) (9.5F * 0.03 * trailFactor), alphaPC, baseColor);
            drawOrbitalParticle(ms, buffer, camera, baseX, baseY, baseZ, angle, -1, 1, -1, (float) (9.5F * 0.03 * trailFactor), alphaPC, baseColor);
            drawOrbitalParticle(ms, buffer, camera, baseX, baseY, baseZ, angle, -1, -1, 1, (float) (9.5F * 0.03 * trailFactor), alphaPC, baseColor);
        }

        tessellator.draw();
        RenderSystem.shadeModel(7424);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.enableAlphaTest();
        RenderSystem.depthMask(true);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableBlend();
        ms.pop();
    }

    private void drawOrbitalParticle(MatrixStack ms, BufferBuilder buffer, ActiveRenderInfo camera, double bx, double by, double bz, double angle, double sMult, double cMult, double cZMult, float size, float alphaPC, int baseColor) {
        double finalX, finalY, finalZ;
        double s = Math.sin(angle) * 0.7;
        double c = Math.cos(angle) * 0.7;
        if (sMult == 1 && cMult == 1 && cZMult == -1) {
            finalX = s; finalY = c; finalZ = -c;
        } else if (sMult == -1 && cMult == 1 && cZMult == -1) {
            finalX = -s; finalY = s; finalZ = -c;
        } else {
            finalX = -s; finalY = -s; finalZ = c;
        }
        ms.push();
        ms.translate(bx + finalX, by + finalY, bz + finalZ);
        ms.rotate(camera.getRotation());
        Matrix4f mat = ms.getLast().getMatrix();
        int r = ColorUtil.red(baseColor);
        int g = ColorUtil.green(baseColor);
        int b = ColorUtil.blue(baseColor);
        float glowSize = size * 2.5f;
        buffer.pos(mat, -glowSize / 2, glowSize / 2, 0).tex(0, 1).color(r, g, b, (int)(255 * alphaPC * 0.3f)).endVertex();
        buffer.pos(mat, glowSize / 2, glowSize / 2, 0).tex(1, 1).color(r, g, b, (int)(255 * alphaPC * 0.3f)).endVertex();
        buffer.pos(mat, glowSize / 2, -glowSize / 2, 0).tex(1, 0).color(r, g, b, (int)(255 * alphaPC * 0.3f)).endVertex();
        buffer.pos(mat, -glowSize / 2, -glowSize / 2, 0).tex(0, 0).color(r, g, b, (int)(255 * alphaPC * 0.3f)).endVertex();
        int coreAlpha = (int)(255 * alphaPC);
        buffer.pos(mat, -size / 2, size / 2, 0).tex(0, 1).color(r, g, b, coreAlpha).endVertex();
        buffer.pos(mat, size / 2, size / 2, 0).tex(1, 1).color(r, g, b, coreAlpha).endVertex();
        buffer.pos(mat, size / 2, -size / 2, 0).tex(1, 0).color(r, g, b, coreAlpha).endVertex();
        buffer.pos(mat, -size / 2, -size / 2, 0).tex(0, 0).color(r, g, b, coreAlpha).endVertex();

        ms.pop();
    }
    private void putBloomQuad(BufferBuilder builder, MatrixStack ms, float x, float y, float size, int r, int g, int b, float a) {
        Matrix4f mat = ms.getLast().getMatrix();
        builder.pos(mat, x, y + size, 0).tex(0, 1).color(r / 255f, g / 255f, b / 255f, a).endVertex();
        builder.pos(mat, x + size, y + size, 0).tex(1, 1).color(r / 255f, g / 255f, b / 255f, a).endVertex();
        builder.pos(mat, x + size, y, 0).tex(1, 0).color(r / 255f, g / 255f, b / 255f, a).endVertex();
        builder.pos(mat, x, y, 0).tex(0, 0).color(r / 255f, g / 255f, b / 255f, a).endVertex();
    }

    private void renderFollowGhosts(EventWorld3D event) {
        if (!isTargetESPMode("Преследующие призраки")) {
            followGhosts.clear();
            followGhostTarget = null;
            followSpin = 0.0f;
            return;
        }

        List<LivingEntity> espTargets = getEspTargets();
        followGhostTarget = espTargets.isEmpty() ? null : espTargets.get(0);
        if (followGhostTarget == null || !followGhostTarget.isAlive()) {
            followGhosts.clear();
            return;
        }

        if (followGhosts.size() < MAX_FOLLOW_GHOSTS) {
            int color = ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL);
            for (int i = followGhosts.size(); i < MAX_FOLLOW_GHOSTS; i++) {
                followGhosts.add(new GhostRenderer3D(followGhostTarget.getPositionVec(), Vector3d.ZERO, FOLLOW_BASE_SIZE, color));
            }
        }

        float fpsFactor = 500f / Math.max(net.minecraft.client.Minecraft.getDebugFPS(), 5);
        followSpin += 29.0f;

        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        List<GhostRenderer3D> toRemove = new ArrayList<>();
        int color = ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL);

        for (int i = 0; i < followGhosts.size(); i++) {
            GhostRenderer3D ghost = followGhosts.get(i);
            ghost.setColor(color);
            updateFollowGhostPosition(ghost, i, fpsFactor);

            if (ghost.getAlpha() < 1f) {
                ghost.setAlpha(Math.max(0f, ghost.getAlpha() - FOLLOW_ALPHA_STEP * fpsFactor));
            }

            if (ghost.getAlpha() <= 0f) {
                toRemove.add(ghost);
                followGhosts.add(new GhostRenderer3D(followGhostTarget.getPositionVec(), Vector3d.ZERO, FOLLOW_BASE_SIZE, color));
                continue;
            }
            ghost.render(event.matrixStack, BLOOM_TEX);
        }

        followGhosts.removeAll(toRemove);

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void updateFollowGhostPosition(GhostRenderer3D ghost, int index, float fpsFactor) {
        if (followGhostTarget == null) return;

        Vector3d base = followGhostTarget.getPositionVec();
        float angleOffset = index * (360f / MAX_FOLLOW_GHOSTS);
        double radian = Math.toRadians(followSpin + angleOffset);

        float hurtFactor = followGhostTarget.hurtTime > 7 ? 1.0f : 0.0f;
        float radius = 0.3F - hurtFactor * 0.3F;
        double x = Math.sin(radian) * radius;
        double z = Math.cos(radian) * radius;
        double y = 0.2 + followGhostTarget.getHeight() / 2.0 * Math.sin(Math.toRadians(followSpin / (index + 1.0f)));

        Vector3d targetPos = base.add(x, y, z);
        Vector3d motion = targetPos.subtract(ghost.getPosition()).scale(FOLLOW_MOTION_MUL * fpsFactor);
        ghost.setMotion(motion);
    }

    private PigEntity createPigForRender(LivingEntity target, Minecraft mc) {
        PigEntity pig = new PigEntity(EntityType.PIG, mc.world);
        pig.setPosition(target.getPosX(), target.getPosY(), target.getPosZ());
        pig.prevPosX = pig.getPosX();
        pig.prevPosY = pig.getPosY();
        pig.prevPosZ = pig.getPosZ();
        pig.lastTickPosX = pig.getPosX();
        pig.lastTickPosY = pig.getPosY();
        pig.lastTickPosZ = pig.getPosZ();
        pig.rotationYaw = 0.0F;
        pig.prevRotationYaw = 0.0F;
        pig.rotationPitch = 0.0F;
        pig.prevRotationPitch = 0.0F;
        pig.renderYawOffset = 0.0F;
        pig.prevRenderYawOffset = 0.0F;
        pig.rotationYawHead = 0.0F;
        pig.prevRotationYawHead = 0.0F;
        pig.limbSwing = 0.0F;
        pig.limbSwingAmount = 0.0F;
        pig.prevLimbSwingAmount = 0.0F;
        pig.hurtTime = 0;
        pig.deathTime = 0;
        pig.ticksExisted = mc.player != null ? mc.player.ticksExisted : 0;
        return pig;
    }

    private void renderPigs(EventWorld3D e) {
        if (!isTargetESPMode("Свинки")) return;

        List<LivingEntity> targets = getEspTargets();
        if (targets.isEmpty() || mc.world == null) {
            return;
        }

        TargetESP targetESP = getTargetESPModule();
        if (targetESP == null) return;

        for (LivingEntity target : targets) {
            if (target == null || !target.isAlive()) continue;
            renderPigsAt(e, target, targetESP);
        }
    }

    private void renderPigsAt(EventWorld3D e, LivingEntity target, TargetESP targetESP) {
        Minecraft mc = Minecraft.getInstance();
        float pt = e.getPartialTicks();
        Vector3d cameraPosition = mc.getRenderManager().info.getProjectedView();
        double targetX = target.lastTickPosX + (target.getPosX() - target.lastTickPosX) * pt;
        double targetY = target.lastTickPosY + (target.getPosY() - target.lastTickPosY) * pt;
        double targetZ = target.lastTickPosZ + (target.getPosZ() - target.lastTickPosZ) * pt;
        double x = targetX - cameraPosition.x;
        double y = targetY - cameraPosition.y;
        double z = targetZ - cameraPosition.z;

        PigEntity pigForRender = createPigForRender(target, mc);

        IRenderTypeBuffer.Impl buffer = mc.getRenderTypeBuffers().getBufferSource();
        MatrixStack ms = e.getMatrixStack();
        EntityRendererManager rm = mc.getRenderManager();

        float progress = 1.0F;
        float radius = 0.7F;
        float spinStep = 0.00025F * targetESP.getPigSpeed().get();
        float halfHeight = target.getHeight() / 2.0F;
        float timeRing = -(System.currentTimeMillis() % 1000000L) * spinStep;

        double[] px = new double[8];
        double[] py = new double[8];
        double[] pz = new double[8];
        float aoe = timeRing * 360.0F;
        for (int i = 0; i < 8; i++) {
            float ta = aoe + (i / 8.0F) * 360.0F;
            double rad = Math.toRadians(ta);
            float yOff = (i % 2 == 0) ? 0.1F : -0.1F;
            double oX = Math.cos(rad) * radius;
            double oZ = Math.sin(rad) * radius;
            px[i] = x + oX;
            py[i] = y + halfHeight + yOff - 0.2F;
            pz[i] = z + oZ;
        }

        double centerX = x;
        double centerZ = z;
        double topY = y + target.getHeight() * 0.95D;
        float timeSpin = (System.currentTimeMillis() % 1000000L) * targetESP.getPigSpeed().get() * 0.001F;
        float yawSpin = timeSpin * 180.0F;
        float pitchWobble = (float) (Math.sin(timeSpin * 1.5D) * 120.0D);
        float rollWobble = (float) (Math.cos(timeSpin * 1.2D) * 90.0D);

        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();

        try {
            for (int i = 0; i < 9; i++) {
                double mx, my, mz, nx, ny, nz;
                if (i < 8) {
                    mx = px[i];
                    my = py[i];
                    mz = pz[i];
                    int next = (i + 1) % 8;
                    nx = px[next];
                    ny = py[next];
                    nz = pz[next];
                } else {
                    mx = centerX;
                    my = topY;
                    mz = centerZ;
                    nx = px[0];
                    ny = py[0];
                    nz = pz[0];
                }

                ms.push();
                ms.translate(mx, my, mz);

                if (i == 8) {
                    ms.rotate(Vector3f.YP.rotationDegrees(yawSpin));
                    ms.rotate(Vector3f.XP.rotationDegrees(pitchWobble));
                    ms.rotate(Vector3f.ZP.rotationDegrees(rollWobble));
                } else {
                    double lX = nx - mx;
                    double lZ = nz - mz;
                    float yaw = (float) Math.toDegrees(Math.atan2(-lZ, lX)) - 95.0F;
                    ms.rotate(Vector3f.YP.rotationDegrees(yaw));
                }

                float scale = (i == 8) ? 0.4F * progress : 0.3F * progress;
                ms.scale(scale, scale, scale);

                try {
                    rm.renderEntityStatic(pigForRender, 0.0D, 0.0D, 0.0D, 0.0F, pt, ms, buffer, PIG_FULL_BRIGHT);
                } catch (Exception ignored) {
                }

                ms.pop();
            }
        } finally {
            buffer.finish();
            RenderSystem.enableCull();
        }
    }

    private static class GlowPoint {
        private final float x;
        private final float y;
        private final float z;
        private final long creationTime;
        private final int maxTime;
        private final int baseColor;
        private float currentTimeProgress;

        private GlowPoint(float x, float y, float z, int maxTime, int color) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.creationTime = System.currentTimeMillis();
            this.maxTime = maxTime;
            this.baseColor = color;
            this.currentTimeProgress = 0.0f;
        }

        private void update() {
            currentTimeProgress = Math.min((System.currentTimeMillis() - creationTime) / (float) maxTime, 1.0f);
        }

        private float getTimeProgress() {
            return currentTimeProgress;
        }

        private int getColor(float timeProgress) {
            float alphaMultiplier = (timeProgress > 0.5f ? 1.0f - timeProgress : timeProgress) * 2.0f;
            alphaMultiplier = Math.max(0f, Math.min(1f, alphaMultiplier));
            int alpha = (int) (alphaMultiplier * 255);
            int rgb = baseColor & 0x00FFFFFF;
            return (alpha << 24) | rgb;
        }

        private boolean shouldRemove() {
            update();
            return currentTimeProgress >= 1.0f;
        }
    }
}

