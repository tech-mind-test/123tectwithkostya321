package sky.core.utils.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import org.lwjgl.opengl.GL11;
import sky.core.events.EventWorld3D;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class TargetCrystalRender {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final ResourceLocation GLOW_TEXTURE = new ResourceLocation("SkyCore/icons/world_render/bloom.png");
    private static final Tessellator TESSELLATOR = Tessellator.getInstance();
    private static final float[] CRYSTAL_FACE_MUL = new float[]{1.0f, 0.8f, 0.6f, 0.9f, 0.7f, 0.5f, 0.4f, 0.6f};
    private static List<Vector3f> cachedCrystalPoints = new ArrayList<>();
    private static int cachedCrystalTargetId = Integer.MIN_VALUE;
    private static float cachedCrystalWidth = -1f;
    private static float cachedCrystalHeight = -1f;
    private static long cachedCrystalPointsAt = 0L;
    private static final long CRYSTAL_POINTS_TTL_MS = 120L;

    private TargetCrystalRender() {
    }

    public static void render(EventWorld3D e, LivingEntity target, float anim, float scatter, float sizeSetting) {
        if (target == null || mc.player == null || mc.world == null) return;
        float alpha = MathHelper.clamp(anim, 0.0f, 1.0f);
        if (alpha <= 0.01f) return;

        float partialTicks = e.getPartialTicks();
        Vector3d cameraPosition = mc.getRenderManager().info.getProjectedView();
        double targetX = target.lastTickPosX + (target.getPosX() - target.lastTickPosX) * partialTicks;
        double targetY = target.lastTickPosY + (target.getPosY() - target.lastTickPosY) * partialTicks;
        double targetZ = target.lastTickPosZ + (target.getPosZ() - target.lastTickPosZ) * partialTicks;
        double x = targetX - cameraPosition.x;
        double y = targetY - cameraPosition.y;
        double z = targetZ - cameraPosition.z;

        float width = target.getWidth() * 1.5f;
        float height = target.getHeight();
        int baseColor = setAlpha(ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL), (int) (255 * alpha));
        float rotate = (System.currentTimeMillis() % 360000L) / 7.25f;
        float timeSec = System.currentTimeMillis() * 0.001f;
        float meshScale = 0.10f * MathHelper.clamp(sizeSetting, 0.5f, 3.0f);
        float radiusAnimFactor = 1.25f - 0.5f * alpha + (MathHelper.clamp(scatter, 0.0f, 1.0f) * 0.35f);
        float rotationRad = (float) Math.toRadians(rotate);
        float sinRot = (float) Math.sin(rotationRad);
        float cosRot = (float) Math.cos(rotationRad);

        List<Vector3f> points = getCrystalPoints(target, width, height);
        MatrixStack ms = e.getMatrixStack();
        double cx = x;
        double cy = y + height * 0.5;
        double cz = z;

        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableAlphaTest();
        RenderSystem.shadeModel(7425);

        BufferBuilder buffer = TESSELLATOR.getBuffer();
        RenderSystem.disableTexture();
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);

        for (int i = 0; i < points.size(); i++) {
            Vector3f p = points.get(i);
            float rotX = p.getX() * cosRot - p.getZ() * sinRot;
            float rotZ = p.getX() * sinRot + p.getZ() * cosRot;
            float rotY = p.getY();
            float finalX = rotX * radiusAnimFactor;
            float finalZ = rotZ * radiusAnimFactor;
            float bob = (float) (0.05f * Math.sin(timeSec * 2.0f + i * 1337.0f));
            float finalY = rotY + bob;

            ms.push();
            ms.translate(x + finalX, y + finalY, z + finalZ);

            Vector3f dir = new Vector3f((float) (cx - (x + finalX)), (float) (cy - (y + finalY)), (float) (cz - (z + finalZ)));
            float len = (float) Math.sqrt(dir.getX() * dir.getX() + dir.getY() * dir.getY() + dir.getZ() * dir.getZ());
            if (len > 0.001f) {
                dir.set(dir.getX() / len, dir.getY() / len, dir.getZ() / len);
                ms.rotate(quatFromTo(Vector3f.YP, dir));
            }

            ms.scale(meshScale, meshScale, meshScale);
            putCrystalMesh(buffer, ms.getLast().getMatrix(), baseColor);
            ms.pop();
        }

        TESSELLATOR.draw();
        RenderSystem.enableTexture();

        mc.getTextureManager().bindTexture(GLOW_TEXTURE);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

        for (int i = 0; i < points.size(); i++) {
            Vector3f p = points.get(i);
            float rotX = p.getX() * cosRot - p.getZ() * sinRot;
            float rotZ = p.getX() * sinRot + p.getZ() * cosRot;
            float rotY = p.getY();
            float finalX = rotX * radiusAnimFactor;
            float finalZ = rotZ * radiusAnimFactor;
            float bob = (float) (0.05f * Math.sin(timeSec * 2.0f + i * 1337.0f));
            float finalY = rotY + bob;

            ms.push();
            ms.translate(x + finalX, y + finalY, z + finalZ);
            ms.rotate(mc.getRenderManager().getCameraOrientation());
            Matrix4f mat = ms.getLast().getMatrix();

            int bigCol = setAlpha(baseColor, (int) (255 * alpha * 0.20f));
            int smallCol = setAlpha(baseColor, (int) (255 * alpha));
            float glowScale = MathHelper.clamp(sizeSetting, 0.5f, 3.0f);
            drawTextureQuad(buffer, mat, 0.55f * 0.85f * glowScale, bigCol);
            drawTextureQuad(buffer, mat, 0.18f * 0.85f * glowScale, smallCol);
            ms.pop();
        }

        TESSELLATOR.draw();
        RenderSystem.shadeModel(7424);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.enableAlphaTest();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.popMatrix();
    }

    private static void putCrystalMesh(BufferBuilder bb, Matrix4f mat, int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;

        float[] vertices = {
                0, 0.5f, 0,
                0, -0.5f, 0,
                0.3f, 0, 0.3f,
                -0.3f, 0, -0.3f,
                0.3f, 0, -0.3f,
                -0.3f, 0, 0.3f
        };

        for (int face = 0; face < 8; face++) {
            float mul = CRYSTAL_FACE_MUL[face % CRYSTAL_FACE_MUL.length];
            int faceR = (int) (r * mul);
            int faceG = (int) (g * mul);
            int faceB = (int) (b * mul);

            if (face < 4) {
                int v1 = 0;
                int v2 = 2 + (face % 4);
                int v3 = 2 + ((face + 1) % 4);
                bb.pos(mat, vertices[v1 * 3], vertices[v1 * 3 + 1], vertices[v1 * 3 + 2]).color(faceR, faceG, faceB, a).endVertex();
                bb.pos(mat, vertices[v2 * 3], vertices[v2 * 3 + 1], vertices[v2 * 3 + 2]).color(faceR, faceG, faceB, a).endVertex();
                bb.pos(mat, vertices[v3 * 3], vertices[v3 * 3 + 1], vertices[v3 * 3 + 2]).color(faceR, faceG, faceB, a).endVertex();
            } else {
                int v1 = 1;
                int v2 = 2 + ((face - 4) % 4);
                int v3 = 2 + ((face - 3) % 4);
                bb.pos(mat, vertices[v1 * 3], vertices[v1 * 3 + 1], vertices[v1 * 3 + 2]).color(faceR, faceG, faceB, a).endVertex();
                bb.pos(mat, vertices[v3 * 3], vertices[v3 * 3 + 1], vertices[v3 * 3 + 2]).color(faceR, faceG, faceB, a).endVertex();
                bb.pos(mat, vertices[v2 * 3], vertices[v2 * 3 + 1], vertices[v2 * 3 + 2]).color(faceR, faceG, faceB, a).endVertex();
            }
        }
    }

    private static void drawTextureQuad(BufferBuilder bb, Matrix4f mat, float size, int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;
        float halfSize = size / 2f;

        bb.pos(mat, -halfSize, halfSize, 0).tex(0, 1).color(r, g, b, a).endVertex();
        bb.pos(mat, halfSize, halfSize, 0).tex(1, 1).color(r, g, b, a).endVertex();
        bb.pos(mat, halfSize, -halfSize, 0).tex(1, 0).color(r, g, b, a).endVertex();
        bb.pos(mat, -halfSize, -halfSize, 0).tex(0, 0).color(r, g, b, a).endVertex();
    }

    private static Quaternion quatFromTo(Vector3f from, Vector3f to) {
        Vector3f cross = new Vector3f(
                from.getY() * to.getZ() - from.getZ() * to.getY(),
                from.getZ() * to.getX() - from.getX() * to.getZ(),
                from.getX() * to.getY() - from.getY() * to.getX()
        );
        float dot = from.getX() * to.getX() + from.getY() * to.getY() + from.getZ() * to.getZ();
        float len = (float) Math.sqrt(cross.getX() * cross.getX() + cross.getY() * cross.getY() + cross.getZ() * cross.getZ());

        if (len < 0.001f) {
            if (dot > 0) return Quaternion.ONE;
            Vector3f axis = Math.abs(from.getX()) < 0.9f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
            return new Quaternion(axis, 180f, true);
        }

        float angle = (float) Math.acos(MathHelper.clamp(dot, -1f, 1f));
        cross.set(cross.getX() / len, cross.getY() / len, cross.getZ() / len);
        return new Quaternion(cross, (float) Math.toDegrees(angle), true);
    }

    private static int setAlpha(int color, int alpha) {
        alpha = MathHelper.clamp(alpha, 0, 255);
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static List<Vector3f> getCrystalPoints(LivingEntity target, float width, float height) {
        long now = System.currentTimeMillis();
        int targetId = target.getEntityId();
        if (targetId == cachedCrystalTargetId
                && Math.abs(width - cachedCrystalWidth) < 0.001f
                && Math.abs(height - cachedCrystalHeight) < 0.001f
                && (now - cachedCrystalPointsAt) <= CRYSTAL_POINTS_TTL_MS
                && !cachedCrystalPoints.isEmpty()) {
            return cachedCrystalPoints;
        }

        List<Vector3f> points = new ArrayList<>();
        Random rng = new Random((long) targetId * 133769420L);
        int count = (int) (width + height * 12);
        int candidatesPerPoint = 15;

        for (int i = 0; i < count; i++) {
            Vector3f bestCandidate = null;
            float bestDistSq = -1.0f;

            for (int attempt = 0; attempt < candidatesPerPoint; attempt++) {
                float angle = rng.nextFloat() * 360f;
                float h = rng.nextFloat() * height;
                float rad = (float) Math.toRadians(angle);
                float px = (float) (Math.sin(rad) * width);
                float pz = (float) (Math.cos(rad) * width);
                float py = h;

                if (points.isEmpty()) {
                    bestCandidate = new Vector3f(px, py, pz);
                    break;
                }

                float nearestDistSq = Float.MAX_VALUE;
                for (Vector3f existing : points) {
                    float dSq = (existing.getX() - px) * (existing.getX() - px)
                            + (existing.getY() - py) * (existing.getY() - py)
                            + (existing.getZ() - pz) * (existing.getZ() - pz);
                    if (dSq < nearestDistSq) nearestDistSq = dSq;
                }

                if (nearestDistSq > bestDistSq) {
                    bestDistSq = nearestDistSq;
                    bestCandidate = new Vector3f(px, py, pz);
                }
            }

            if (bestCandidate != null) points.add(bestCandidate);
        }

        cachedCrystalPoints = points;
        cachedCrystalTargetId = targetId;
        cachedCrystalWidth = width;
        cachedCrystalHeight = height;
        cachedCrystalPointsAt = now;
        return cachedCrystalPoints;
    }
}