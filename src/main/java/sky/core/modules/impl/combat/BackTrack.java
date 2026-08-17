package sky.core.modules.impl.combat;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import org.lwjgl.opengl.GL11;
import com.adl.nativeprotect.Native;
import sky.core.events.EventMotion;
import sky.core.events.EventRender3D;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_LINE_SMOOTH;

public class BackTrack extends Module {

    private final SliderSetting time = new SliderSetting("Время (мс)", 600, 100, 1000, 50);
    public final BooleanSetting render = new BooleanSetting("Отображать", true);

    private final Map<PlayerEntity, List<Position>> backtrackPositions = new HashMap<>();


    public BackTrack() {
        super("BackTrack", "Задерживает хитбокс игроков для увеличения дальности удара", Category.Combat);
        this.addSettings(time, render);
    }

    @Native
    @EventTarget
    public void gandon(EventMotion eventMotion) {
        handleMotion();
    }

    @Native
    @EventTarget
    public void render(EventRender3D eventRender3D) {
        handleRender(eventRender3D);
    }

    @Native
    private void handleMotion() {
        final long maxDelay = time.get().longValue();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;

            List<Position> positions = backtrackPositions.computeIfAbsent(player, k -> new ArrayList<>());

            positions.add(new Position(
                    new Vector3d(player.getPosX(), player.getPosY(), player.getPosZ()),
                    System.currentTimeMillis()
            ));

            positions.removeIf(pos -> (System.currentTimeMillis() - pos.getTime()) > maxDelay);
        }
    }


    @Native
    public Position getOldestPosition(PlayerEntity player) {
        if (!isEnabled()) return null;
        List<Position> positions = backtrackPositions.get(player);
        if (positions == null || positions.isEmpty()) return null;

        return positions.get(0);
    }

    /**
     * Создает AABB игрока, смещенный к его сохраненной BackTrack позиции.
     * Это можно использовать для изменения хитбокса во время удара.
     */
    @Native
    public AxisAlignedBB getBackTrackBox(PlayerEntity player, Position backtrackPos) {
        if (backtrackPos == null) return null;

        float width = player.getWidth();
        float height = player.getHeight();

        Vector3d pos = backtrackPos.getPos();


        return new AxisAlignedBB(
                pos.x - width / 2.0,
                pos.y,
                pos.z - width / 2.0,
                pos.x + width / 2.0,
                pos.y + height,
                pos.z + width / 2.0
        );
    }

    @Native
    private void handleRender(EventRender3D e) {
        if (!render.get()) return;

        EntityRendererManager renderManager = mc.getRenderManager();
        if (renderManager == null || renderManager.info == null) return;

        final long maxDelay = time.get().longValue();

        GL11.glPushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.depthMask(false);
        GlStateManager.disableTexture();
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GlStateManager.disableCull();
        GlStateManager.enableDepthTest();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_DST_ALPHA);

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;

            List<Position> positions = backtrackPositions.getOrDefault(player, new ArrayList<>());
            for (Position position : positions) {
                try {
                    Vector3d pos = position.getPos();
                    Vector3d cameraPos = renderManager.info.getProjectedView();

                    double x = pos.x - cameraPos.x;
                    double y = pos.y - cameraPos.y;
                    double z = pos.z - cameraPos.z;

                    float timePassed = (float) (System.currentTimeMillis() - position.getTime());
                    float alpha = 1.0f - (timePassed / maxDelay);
                    alpha = Math.max(0, Math.min(alpha, 0.5f));


                    GL11.glPushMatrix();
                    GL11.glEnable(GL11.GL_BLEND);
                    GL11.glLineWidth(2);
                    GL11.glDisable(GL11.GL_TEXTURE_2D);
                    GL11.glDisable(GL11.GL_DEPTH_TEST);

                    float width = player.getWidth();
                    float height = player.getHeight();
                    AxisAlignedBB box = new AxisAlignedBB(
                            x - width / 2.0,
                            y,
                            z - width / 2.0,
                            x + width / 2.0,
                            y + height,
                            z + width / 2.0
                    );

                    int color = new Color(1.0f, 1.0f, 1.0f, alpha).getRGB();
                    drawBox(box, color);

                    GL11.glLineWidth(1);
                    GL11.glEnable(GL11.GL_TEXTURE_2D);
                    GL11.glEnable(GL11.GL_DEPTH_TEST);
                    GL11.glDisable(GL11.GL_BLEND);
                    GL11.glPopMatrix();

                } catch (Exception ex) {
                }
            }
        }

        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glShadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture();
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GL11.glPopMatrix();
    }

    public static void drawBox(AxisAlignedBB bb, int color) {
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL_DEPTH_TEST);
        GL11.glEnable(GL_LINE_SMOOTH);
        GL11.glLineWidth(1);
        float[] rgb = IntColor.rgb(color);
        GlStateManager.color4f(rgb[0], rgb[1], rgb[2], rgb[3]);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder vertexbuffer = tessellator.getBuffer();
        vertexbuffer.begin(3, DefaultVertexFormats.POSITION);
        vertexbuffer.pos(bb.minX, bb.minY, bb.minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
        vertexbuffer.pos(bb.maxX, bb.minY, bb.minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
        vertexbuffer.pos(bb.maxX, bb.minY, bb.maxZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
        vertexbuffer.pos(bb.minX, bb.minY, bb.maxZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
        vertexbuffer.pos(bb.minX, bb.minY, bb.minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
        tessellator.draw();
        vertexbuffer.begin(3, DefaultVertexFormats.POSITION);
        vertexbuffer.pos(bb.minX, bb.maxY, bb.minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
        vertexbuffer.pos(bb.maxX, bb.maxY, bb.minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
        vertexbuffer.pos(bb.maxX, bb.maxY, bb.maxZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
        vertexbuffer.pos(bb.minX, bb.maxY, bb.maxZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
        vertexbuffer.pos(bb.minX, bb.maxY, bb.minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
        tessellator.draw();
        vertexbuffer.begin(1, DefaultVertexFormats.POSITION);
        vertexbuffer.pos(bb.minX, bb.minY, bb.minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
        vertexbuffer.pos(bb.minX, bb.maxY, bb.minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
        vertexbuffer.pos(bb.maxX, bb.minY, bb.minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
        vertexbuffer.pos(bb.maxX, bb.maxY, bb.minZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
        vertexbuffer.pos(bb.maxX, bb.minY, bb.maxZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
        vertexbuffer.pos(bb.maxX, bb.maxY, bb.maxZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
        vertexbuffer.pos(bb.minX, bb.minY, bb.maxZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
        vertexbuffer.pos(bb.minX, bb.maxY, bb.maxZ).color(rgb[0], rgb[1], rgb[2], rgb[3]).endVertex();
        tessellator.draw();
        GlStateManager.color4f(rgb[0], rgb[1], rgb[2], rgb[3]);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL_DEPTH_TEST);
        GL11.glDisable(GL_LINE_SMOOTH);
        GL11.glPopMatrix();

    }

    public static class IntColor {
        public static float[] rgb(final int color) {
            return new float[]{
                    (color >> 16 & 0xFF) / 255f,
                    (color >> 8 & 0xFF) / 255f,
                    (color & 0xFF) / 255f,
                    (color >> 24 & 0xFF) / 255f
            };
        }

        public static int getRed(final int hex) {
            return hex >> 16 & 255;
        }

        public static int getGreen(final int hex) {
            return hex >> 8 & 255;
        }

        public static int getBlue(final int hex) {
            return hex & 255;
        }

        public static int getAlpha(final int hex) {
            return hex >> 24 & 255;
        }
    }

    @Override
    public void onEnable() {
        backtrackPositions.clear();
    }

    @Override
    public void onDisable() {
        backtrackPositions.clear();
    }


    static class Position {
        private final Vector3d pos;
        private final long time; // Время сохранения позиции

        public Position(Vector3d pos, long time) {
            this.pos = pos;
            this.time = time;
        }

        public Vector3d getPos() {
            return pos;
        }

        public long getTime() {
            return time;
        }
    }
}