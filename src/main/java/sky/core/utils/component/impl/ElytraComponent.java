package sky.core.utils.component.impl;


import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import org.lwjgl.opengl.GL11;
import sky.core.SkyCore;
import sky.core.utils.component.RotationAccess;
import sky.core.events.EventAttack;
import sky.core.events.EventRender3D;
import sky.core.handlers.impl.Rotation;
import sky.core.modules.impl.combat.AttackAura;
import sky.core.modules.impl.movement.ElytraTarget;
import sky.core.modules.impl.movement.FreeCamera;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.math.TimeUtil;
import sky.core.utils.render.ColorUtil;

import static java.lang.Math.*;
import static net.minecraft.util.math.MathHelper.clamp;

public class ElytraComponent extends RotationAccess {

    public static boolean defensiveActive = false;
    public static Vector3d leaveVec = Vector3d.ZERO;
    public static Vector3d lastVec = Vector3d.ZERO;
    public static boolean predictCondition = false;
    private static int movementTicks = 0;
    private static boolean prePredictCondition = false;
    private static final TimeUtil tickStopWatch = new TimeUtil();
    public static boolean blockPos;
    public static Vector3d pos = Vector3d.ZERO;
    public static double value2 = 0.0;
    public static Vector3d vector = Vector3d.ZERO;

    public static void renderPredictCube(EventRender3D event, ElytraTarget elytraTarget) {
        if (mc.player == null || mc.world == null) return;
        if (elytraTarget == null || !elytraTarget.isEnabled()) return;
        if (!elytraTarget.getTarget().get()) return;
        if (!elytraTarget.getPredictCube().get()) return;
        // Куб рендерится для ReallyWorld, Pedik и старых режимов
        if (!elytraTarget.supportsPredictCube()) return;
        if (!mc.player.isElytraFlying()) return;

        AttackAura aura = SkyCore.getInstance().getModuleManager().getAttackAura();
        if (aura == null) return;
        LivingEntity auraTarget = aura.getTarget();
        if (auraTarget == null) return;
        if (!auraTarget.isElytraFlying()) return;

        if (pos == null || pos.equals(Vector3d.ZERO)) return;

        Vector3d cam = mc.getRenderManager().info.getProjectedView();
        double x = pos.x - cam.x;
        double y = pos.y - cam.y;
        double z = pos.z - cam.z;

        double r = 0.35;
        AxisAlignedBB bb = new AxisAlignedBB(x - r, y - r, z - r, x + r, y + r, z + r);

        int baseColor = elytraTarget.getPredictFromTheme().get()
                ? ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL)
                : ColorUtil.rgb(255, 255, 255);
        int fillColor = ColorUtil.setAlpha(baseColor, elytraTarget.getPredictFillAlpha().get().intValue());
        int lineColor = ColorUtil.setAlpha(baseColor, 255);

        String boxMode = elytraTarget.getPredictBoxMode().get();
        if (boxMode.equals("Пунктир")) {
            drawPredictFill(bb, fillColor);
            drawPredictDashedOutline(bb, lineColor);
        } else if (boxMode.equals("Диагонали")) {
            drawPredictFill(bb, fillColor);
            drawPredictSolidOutline(bb, lineColor);
            drawPredictBodyDiagonals(bb, lineColor);
        } else {
            drawPredictFill(bb, fillColor);
            drawPredictSolidOutline(bb, lineColor);
        }
    }

    private static void drawPredictFill(AxisAlignedBB bb, int color) {
        if ((color >> 24 & 255) <= 0) {
            return;
        }
        setupPredictRender();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        float a = (color >> 24 & 255) / 255f;
        float r = (color >> 16 & 255) / 255f;
        float g = (color >> 8 & 255) / 255f;
        float b = (color & 255) / 255f;

        addPredictBoxFaces(buffer, bb, r, g, b, a);
        tessellator.draw();
        cleanupPredictRender();
    }

    private static void drawPredictSolidOutline(AxisAlignedBB bb, int color) {
        setupPredictRender();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(1.5f);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        float a = (color >> 24 & 255) / 255f;
        float r = (color >> 16 & 255) / 255f;
        float g = (color >> 8 & 255) / 255f;
        float b = (color & 255) / 255f;

        addPredictBoxEdges(buffer, bb, r, g, b, a);
        tessellator.draw();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        cleanupPredictRender();
    }

    private static void drawPredictDashedOutline(AxisAlignedBB bb, int color) {
        setupPredictRender();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(1.5f);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        float a = (color >> 24 & 255) / 255f;
        float r = (color >> 16 & 255) / 255f;
        float g = (color >> 8 & 255) / 255f;
        float b = (color & 255) / 255f;

        float dash = 0.12f;
        float gap = 0.1f;
        double[][] edges = {
                {bb.minX, bb.minY, bb.minZ, bb.maxX, bb.minY, bb.minZ},
                {bb.maxX, bb.minY, bb.minZ, bb.maxX, bb.minY, bb.maxZ},
                {bb.maxX, bb.minY, bb.maxZ, bb.minX, bb.minY, bb.maxZ},
                {bb.minX, bb.minY, bb.maxZ, bb.minX, bb.minY, bb.minZ},
                {bb.minX, bb.maxY, bb.minZ, bb.maxX, bb.maxY, bb.minZ},
                {bb.maxX, bb.maxY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ},
                {bb.maxX, bb.maxY, bb.maxZ, bb.minX, bb.maxY, bb.maxZ},
                {bb.minX, bb.maxY, bb.maxZ, bb.minX, bb.maxY, bb.minZ},
                {bb.minX, bb.minY, bb.minZ, bb.minX, bb.maxY, bb.minZ},
                {bb.maxX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.minZ},
                {bb.maxX, bb.minY, bb.maxZ, bb.maxX, bb.maxY, bb.maxZ},
                {bb.minX, bb.minY, bb.maxZ, bb.minX, bb.maxY, bb.maxZ},
        };
        for (double[] edge : edges) {
            drawPredictDashedLine(buffer, edge[0], edge[1], edge[2], edge[3], edge[4], edge[5], dash, gap, r, g, b, a);
        }
        tessellator.draw();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        cleanupPredictRender();
    }

    private static void drawPredictBodyDiagonals(AxisAlignedBB bb, int color) {
        setupPredictRender();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(1.2f);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        float a = (color >> 24 & 255) / 255f;
        float r = (color >> 16 & 255) / 255f;
        float g = (color >> 8 & 255) / 255f;
        float b = (color & 255) / 255f;

        double[][] diagonals = {
                {bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ},
                {bb.maxX, bb.minY, bb.minZ, bb.minX, bb.maxY, bb.maxZ},
                {bb.minX, bb.minY, bb.maxZ, bb.maxX, bb.maxY, bb.minZ},
                {bb.maxX, bb.minY, bb.maxZ, bb.minX, bb.maxY, bb.minZ},
        };
        for (double[] d : diagonals) {
            buffer.pos(d[0], d[1], d[2]).color(r, g, b, a).endVertex();
            buffer.pos(d[3], d[4], d[5]).color(r, g, b, a).endVertex();
        }
        tessellator.draw();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        cleanupPredictRender();
    }

    private static void drawPredictDashedLine(BufferBuilder buffer, double x1, double y1, double z1,
                                              double x2, double y2, double z2,
                                              float dashLen, float gapLen, float r, float g, float b, float a) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001) {
            return;
        }
        double nx = dx / len;
        double ny = dy / len;
        double nz = dz / len;
        double pos = 0;
        boolean draw = true;
        while (pos < len) {
            double end = Math.min(pos + (draw ? dashLen : gapLen), len);
            if (draw) {
                buffer.pos(x1 + nx * pos, y1 + ny * pos, z1 + nz * pos).color(r, g, b, a).endVertex();
                buffer.pos(x1 + nx * end, y1 + ny * end, z1 + nz * end).color(r, g, b, a).endVertex();
            }
            pos = end;
            draw = !draw;
        }
    }

    private static void addPredictBoxFaces(BufferBuilder buffer, AxisAlignedBB bb, float r, float g, float b, float a) {
        buffer.pos(bb.minX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();

        buffer.pos(bb.minX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();

        buffer.pos(bb.minX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.minX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();

        buffer.pos(bb.maxX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();

        buffer.pos(bb.minX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();

        buffer.pos(bb.minX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.minX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
    }

    private static void addPredictBoxEdges(BufferBuilder buffer, AxisAlignedBB bb, float r, float g, float b, float a) {
        buffer.pos(bb.minX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.minX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.minX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.minX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();

        buffer.pos(bb.minX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();

        buffer.pos(bb.minX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.minZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.minZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.minX, bb.minY, bb.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.maxZ).color(r, g, b, a).endVertex();
    }

    private static void setupPredictRender() {
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }

    private static void cleanupPredictRender() {
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableTexture();
    }


    public static void processTargetLogic(ElytraTarget elytraTarget) {
        float distanceValue = elytraTarget.distance.get();
        float forward = distanceValue < 4.0F ? 0.0F : distanceValue - 3.0F;
        Vector3d eyePos = target.getEyePosition(1);
        Vector3d resol = target.getForward();
        pos = eyePos.add(resol.normalize().scale((double) distanceValue));
        Vector3d var6 = eyePos.add(resol.normalize().scale(forward));
        updateDistances(var6);
    }

    public static void setVector(Vector3d vector1) {
        vector = vector1;
    }

    public ElytraComponent() {
        super("ElytraComponent");
    }

    public static void updateDistances(Vector3d var1) {
        float var2 = (float) var1.distanceTo(mc.player.getEyePosition(1.0F));
        float var3 = (float) mc.player.getDistance(target);
        value2 = blockPos ? (double) var2 : (double) var3;
    }

    public static void smartPredict() {
        if (tickStopWatch.isReached(50L)) {
            if (target != null) {
                float x = (float) (target.getPosX() - target.prevPosX);
                float y = (float) (target.getPosY() - target.prevPosY);
                float z = (float) (target.getPosZ() - target.prevPosZ);
                float dist = (float) Math.sqrt(x * x + y * y + z * z);
                float finalSpeed = (float) ((double) dist * (double) 20.0F);
                if (finalSpeed > 20.0F) {
                    movementTicks = 0;
                    prePredictCondition = false;
                    predictCondition = true;
                } else {
                    ++movementTicks;
                    if (predictCondition) {
                        prePredictCondition = true;
                    }

                    if (movementTicks >= 3) {
                        predictCondition = false;
                        movementTicks = 0;
                        prePredictCondition = false;
                    } else {
                        predictCondition = prePredictCondition;
                    }
                }
            }

            tickStopWatch.reset();
        }

    }


    public static void updateRotation(Vector2f current) {
        Vector3d vec3d = vector;

        float rawYaw = (float) MathHelper.wrapDegrees(toDegrees(atan2(vec3d.z, vec3d.x)) - 90.0);
        float rawPitch = (float) MathHelper.wrapDegrees(toDegrees(-atan2(vec3d.y, hypot(vec3d.x, vec3d.z))));

        float currentYaw = current.x;
        float currentPitch = current.y;

        float yawDelta = MathHelper.wrapDegrees(rawYaw - currentYaw);
        float pitchDelta = MathHelper.wrapDegrees(rawPitch - currentPitch);

        if (abs(yawDelta) > 180.0F && mc.player.isSwimming()) {
            yawDelta -= signum(yawDelta) * 360.0F;
        }
        float finalDeltaYaw = MathHelper.clamp(yawDelta, -180.0F * 1, 180.0F * 1);
        float finalDeltaPitch = MathHelper.clamp(pitchDelta, -90.0F * 1, 90.0F * 1);

        float newYaw = currentYaw + finalDeltaYaw;
        float newPitch = currentPitch + finalDeltaPitch;
        float finalNewPitch = MathHelper.clamp(newPitch, -89.9F, 89.9F);
        float speedYaw = 150;
        float speedPitch = 150;
        if (blockPos) {
            speedYaw = 9999;
            speedPitch = 9999;
        }

        RotationComponent.update(new Rotation(newYaw, finalNewPitch), speedYaw, speedPitch, 360, 360, 0, 1, false);
    }

    public static void attack(EventAttack e) {
    }

    public static Vector3d getVector3d(LivingEntity me, LivingEntity to) {
        ElytraTarget elytraTarget = SkyCore.getInstance().getModuleManager().getElytraTarget();
        FreeCamera freeCamera = (FreeCamera) SkyCore.getInstance().getModuleManager().getModule(FreeCamera.class);
        if (me == null || to == null || elytraTarget == null || !elytraTarget.isEnabled()) {
            return Vector3d.ZERO;
        }

        boolean reallyWorldMode = elytraTarget.predictMode.is("SDfsdf")
                || elytraTarget.predictMode.is("Ddsfsdf");
        boolean testMode = elytraTarget.predictMode.is("ReallyWorld");
        // Новый режим Pedik
        boolean pedikMode = elytraTarget.isPedikPredict();

        boolean baseChase = elytraTarget.target.get()
                && mc.player.isElytraFlying()
                && to.isElytraFlying()
                && (freeCamera == null || !freeCamera.isEnabled());

        Vector3d fenal;
        boolean useChasePredictPath;

        if (baseChase && reallyWorldMode) {
            useChasePredictPath = true;
            double predictDistance = elytraTarget.distance.get();
            Vector3d add = new Vector3d(0, MathHelper.clamp(to.getPosY() - to.getHeight(), 0, to.getHeight() / 2.0f), 0);
            Vector3d targetPos = to.getPositionVec().add(add);
            Vector3d playerPos = me.getEyePosition(mc.getRenderPartialTicks());
            Vector3d motionOffset = to.getMotion().mul(predictDistance, predictDistance, predictDistance);
            fenal = targetPos.subtract(playerPos).add(motionOffset);
        } else if (baseChase && testMode) {
            // Режим ReallyWorld — летим чуть впереди цели (по форварду цели)
            useChasePredictPath = true;
            Vector3d pedik = pos.add(to.getForward().normalize().scale(2));
            fenal = new Vector3d(pedik.getX() - me.getPosX(), pedik.getY() - me.getPosY(), pedik.getZ() - me.getPosZ());
        } else if (baseChase && pedikMode) {
            // Режим Pedik — летим по мотиону цели (как getReallyWorldChaseVector)
            useChasePredictPath = true;
            double predictDistance = elytraTarget.distance.get();
            Vector3d add = new Vector3d(0, MathHelper.clamp(to.getPosY() - to.getHeight(), 0, to.getHeight() / 2.0f), 0);
            Vector3d targetPos = to.getPositionVec().add(add);
            Vector3d playerPos = me.getEyePosition(mc.getRenderPartialTicks());
            Vector3d motionOffset = to.getMotion().mul(predictDistance, predictDistance, predictDistance);
            fenal = targetPos.subtract(playerPos).add(motionOffset);
        } else if (baseChase && predictCondition) {
            useChasePredictPath = true;
            Vector3d pedik = pos.add(to.getForward().normalize().scale(2));
            fenal = new Vector3d(pedik.getX() - me.getPosX(), pedik.getY() - me.getPosY(), pedik.getZ() - me.getPosZ());
        } else {
            useChasePredictPath = false;
            double wHalf = to.getWidth() / 2.0F;
            double yExpand = clamp(to.getPosYEye() - to.getPosY(), 0.0, to.getHeight() - (mc.player.isElytraFlying() ? 0 : 0.5f));
            double xExpand = clamp(mc.player.getPosX() - to.getPosX(), -wHalf, wHalf);
            double zExpand = clamp(mc.player.getPosZ() - to.getPosZ(), -wHalf, wHalf);
            fenal = new Vector3d(
                    to.getPosX() - mc.player.getPosX() + xExpand,
                    to.getPosY() - mc.player.getPosYEye() + yExpand,
                    to.getPosZ() - mc.player.getPosZ() + zExpand
            );
        }
        blockPos = useChasePredictPath;
        return fenal;
    }


    public static void updateDefensiveState(LivingEntity target) {
        defensiveActive = false;
    }

    public static void resetState() {
        defensiveActive = false;
        leaveVec = Vector3d.ZERO;
        lastVec = Vector3d.ZERO;
    }
}