package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.border.WorldBorder;
import org.lwjgl.opengl.GL11;
import sky.core.events.EventRender3D;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class Cubes extends Module {
    private static final ResourceLocation GLOW_TEXTURE = new ResourceLocation("SkyCore/icons/world_render/glow.png");
    private static final float SPAWN_RADIUS = 12.0f, PARTICLE_SPEED = 0.25f, PARTICLE_SIZE = 0.18f;
    private final List<float[]> cubes = new ArrayList<>();
    private final Random random = new Random();
    public final SliderSetting count = new SliderSetting("Количество", 30.0f, 5.0f, 100.0f, 1.0f);
    private boolean lastAttackPressed;

    public Cubes() {
        super("Cubes", "", Category.Visuals);
        addSettings(count);
    }

    @java.lang.Override
    public void onEnable() {
        super.onEnable();
        cubes.clear();
    }

    @java.lang.Override
    public void onDisable() {
        super.onDisable();
        cubes.clear();
    }

    private Vector3d cameraLookDir() {
        Vector3f look = mc.gameRenderer.getActiveRenderInfo().getViewVector();
        return new Vector3d(look.getX(), look.getY(), look.getZ());
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.player == null || mc.world == null) return;
        boolean atk = mc.gameSettings.keyBindAttack.isKeyDown();
        if (atk && !lastAttackPressed) applyHitImpulse();
        lastAttackPressed = atk;
        updateCubes();
        renderCubes(event);
    }

    private void applyHitImpulse() {
        if (cubes.isEmpty()) return;
        Vector3d origin = mc.gameRenderer.getActiveRenderInfo().getProjectedView(), dir = cameraLookDir().normalize();
        double bestT = Double.MAX_VALUE;
        float[] best = null;
        for (float[] p : cubes) {
            Vector3d op = new Vector3d(p[0], p[1], p[2]).subtract(origin);
            double t = op.dotProduct(dir);
            if (t >= 0 && t <= 128.0) {
                double dist = new Vector3d(p[0], p[1], p[2]).distanceTo(origin.add(dir.scale(t)));
                if (dist <= 1.15 && t < bestT) {
                    bestT = t;
                    best = p;
                }
            }
        }
        if (best != null) {
            best[3] += (float) (dir.x * 0.08);
            best[4] += (float) (dir.y * 0.08 + 0.02);
            best[5] += (float) (dir.z * 0.08);
        }
    }

    private boolean isBehindCamera(double px, double py, double pz) {
        Vector3d cam = mc.gameRenderer.getActiveRenderInfo().getProjectedView(), d = cameraLookDir();
        return (px - cam.x) * d.x + (py - cam.y) * d.y + (pz - cam.z) * d.z < -1.0;
    }

    private void updateCubes() {
        int target = count.get().intValue();
        while (cubes.size() < target) cubes.add(spawnCube());
        while (cubes.size() > target) cubes.remove(cubes.size() - 1);
        Iterator<float[]> it = cubes.iterator();
        while (it.hasNext()) {
            float[] p = it.next();
            float spd = PARTICLE_SPEED;
            p[0] += p[3] * spd;
            p[1] += p[4] * spd;
            p[2] += p[5] * spd;
            for (int i = 6; i <= 8; i++) p[i] += p[i + 3] * spd;
            p[3] *= 0.995f;
            p[4] *= 0.995f;
            p[5] *= 0.995f;
            p[12]--;
            if (p[12] <= 0) it.remove();
        }
    }

    private void renderCubes(EventRender3D e) {
        ActiveRenderInfo info = mc.gameRenderer.getActiveRenderInfo();
        Vector3d cam = info.getProjectedView();
        MatrixStack ms = new MatrixStack();
        float s = PARTICLE_SIZE;
        int c = ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL);
        float cr = ((c >> 16) & 0xFF) / 255f, cg = ((c >> 8) & 0xFF) / 255f, cb = (c & 0xFF) / 255f;
        List<float[]> visible = new ArrayList<>();
        for (float[] p : cubes) if (!isBehindCamera(p[0], p[1], p[2])) visible.add(p);
        if (visible.isEmpty()) return;

        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
        mc.getTextureManager().bindTexture(GLOW_TEXTURE);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder bb = tess.getBuffer();
        bb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (float[] p : visible) {
            float a = getAlpha(p);
            Vector3d pos = new Vector3d(p[0], p[1], p[2]);
            for (float[] gl : new float[][]{{17f, 0.06f}, {10f, 0.14f}, {6f, 0.25f}})
                drawGlow(bb, ms, info, pos, cam, s * gl[0], cr, cg, cb, gl[1] * a);
        }
        tess.draw();

        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableTexture();
        bb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (float[] p : visible) {
            float a = getAlpha(p) * 0.4f;
            ms.push();
            ms.translate(p[0] - cam.x, p[1] - cam.y, p[2] - cam.z);
            ms.rotate(Vector3f.XP.rotationDegrees(p[6]));
            ms.rotate(Vector3f.YP.rotationDegrees(p[7]));
            ms.rotate(Vector3f.ZP.rotationDegrees(p[8]));
            drawCubeFaces(bb, ms.getLast().getMatrix(), s, cr, cg, cb, a);
            ms.pop();
        }
        tess.draw();

        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        bb.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        float er = Math.min(1f, cr * 1.5f), eg = Math.min(1f, cg * 1.5f), eb = Math.min(1f, cb * 1.5f);
        for (float[] p : visible) {
            float a = getAlpha(p);
            ms.push();
            ms.translate(p[0] - cam.x, p[1] - cam.y, p[2] - cam.z);
            ms.rotate(Vector3f.XP.rotationDegrees(p[6]));
            ms.rotate(Vector3f.YP.rotationDegrees(p[7]));
            ms.rotate(Vector3f.ZP.rotationDegrees(p[8]));
            drawDashedEdges(bb, ms.getLast().getMatrix(), s, er, eg, eb, a);
            ms.pop();
        }
        tess.draw();

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableTexture();
        RenderSystem.popMatrix();
    }

    private float getAlpha(float[] p) {
        return MathHelper.clamp(p[12] / p[13], 0, 1) * Math.min(1f, (p[13] - p[12]) / 20f);
    }

    private void drawGlow(BufferBuilder bb, MatrixStack ms, ActiveRenderInfo info, Vector3d pos, Vector3d cam, float scale, float r, float g, float b, float a) {
        if (a <= 0.001f) return;
        ms.push();
        ms.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
        ms.rotate(Vector3f.YP.rotationDegrees(-info.getYaw()));
        ms.rotate(Vector3f.XP.rotationDegrees(info.getPitch()));
        ms.scale(scale, scale, scale);
        Matrix4f m = ms.getLast().getMatrix();
        bb.pos(m, -0.5f, 0.5f, 0).tex(0, 1).color(r, g, b, a).endVertex();
        bb.pos(m, 0.5f, 0.5f, 0).tex(1, 1).color(r, g, b, a).endVertex();
        bb.pos(m, 0.5f, -0.5f, 0).tex(1, 0).color(r, g, b, a).endVertex();
        bb.pos(m, -0.5f, -0.5f, 0).tex(0, 0).color(r, g, b, a).endVertex();
        ms.pop();
    }

    private void drawCubeFaces(BufferBuilder bb, Matrix4f m, float s, float r, float g, float b, float a) {
        float[][] faces = {{-s, -s, s, s, -s, s, s, s, s, -s, s, s}, {s, -s, -s, -s, -s, -s, -s, s, -s, s, s, -s},
                {-s, s, s, s, s, s, s, s, -s, -s, s, -s}, {-s, -s, -s, s, -s, -s, s, -s, s, -s, -s, s},
                {s, -s, s, s, -s, -s, s, s, -s, s, s, s}, {-s, -s, -s, -s, -s, s, -s, s, s, -s, s, -s}};
        for (float[] f : faces)
            for (int i = 0; i < 12; i += 3) bb.pos(m, f[i], f[i + 1], f[i + 2]).color(r, g, b, a).endVertex();
    }

    private void drawDashedEdges(BufferBuilder bb, Matrix4f m, float s, float r, float g, float b, float a) {
        float[][] edges = {{-s, -s, -s, s, -s, -s}, {s, -s, -s, s, -s, s}, {s, -s, s, -s, -s, s}, {-s, -s, s, -s, -s, -s},
                {-s, s, -s, s, s, -s}, {s, s, -s, s, s, s}, {s, s, s, -s, s, s}, {-s, s, s, -s, s, -s},
                {-s, -s, -s, -s, s, -s}, {s, -s, -s, s, s, -s}, {s, -s, s, s, s, s}, {-s, -s, s, -s, s, s}};
        float d = s * 0.3f, gp = s * 0.25f;
        for (float[] e : edges) dLine(bb, m, e[0], e[1], e[2], e[3], e[4], e[5], d, gp, r, g, b, a);
    }

    private void dLine(BufferBuilder bb, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, float dl, float gl, float r, float g, float b, float a) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1, len = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001f) return;
        float nx = dx / len, ny = dy / len, nz = dz / len, p = 0;
        boolean dr = true;
        while (p < len) {
            float e = Math.min(p + (dr ? dl : gl), len);
            if (dr) {
                bb.pos(m, x1 + nx * p, y1 + ny * p, z1 + nz * p).color(r, g, b, a).endVertex();
                bb.pos(m, x1 + nx * e, y1 + ny * e, z1 + nz * e).color(r, g, b, a).endVertex();
            }
            p = e;
            dr = !dr;
        }
    }

    private float[] spawnCube() {
        int life = 420 + random.nextInt(420);
        WorldBorder border = mc.world.getWorldBorder();
        double minX = border.minX() + 16.0;
        double maxX = border.maxX() - 16.0;
        double minZ = border.minZ() + 16.0;
        double maxZ = border.maxZ() - 16.0;
        double x = MathHelper.lerp(random.nextDouble(), minX, maxX);
        double z = MathHelper.lerp(random.nextDouble(), minZ, maxZ);
        double y = mc.world.getHeight() * 0.25 + random.nextDouble() * (mc.world.getHeight() * 0.7);
        float yaw = random.nextFloat() * 360f;
        float v = 0.01f + random.nextFloat() * 0.02f;
        float vx = -MathHelper.sin((float) Math.toRadians(yaw)) * v;
        float vz = MathHelper.cos((float) Math.toRadians(yaw)) * v;
        float vy = (random.nextFloat() - 0.5f) * 0.01f;
        return new float[]{(float) x, (float) y, (float) z, vx, vy, vz, random.nextFloat() * 360, random.nextFloat() * 360, random.nextFloat() * 360,
                (random.nextFloat() - 0.5f) * 1.5f, (random.nextFloat() - 0.5f) * 1.5f, (random.nextFloat() - 0.5f) * 1.5f, life, life,
                (float) (random.nextDouble() * Math.PI * 2), random.nextFloat() * 10f};
    }
}