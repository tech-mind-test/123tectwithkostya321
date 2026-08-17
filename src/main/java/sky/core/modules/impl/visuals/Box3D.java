package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import org.lwjgl.opengl.GL11;
import sky.core.SkyCore;
import sky.core.events.EventRender3D;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.StopWatch;
import sky.core.utils.render.ColorUtil;

public class Box3D extends Module {

    private final BooleanSetting pulse = new BooleanSetting("Пульсация", false);
    private final StopWatch stopWatch = new StopWatch();
    private float alpha = 50f;
    private boolean decrease = true;

    private final BufferBuilder buffer = Tessellator.getInstance().getBuffer();
    private final Tessellator tessellator = Tessellator.getInstance();

    public Box3D() {
        super("3DBox", "Отображает контуры игроков", Category.Visuals);
        addSettings(pulse);
    }

    @EventTarget
    private void onRender(EventRender3D event) {
        if (mc.world == null || mc.player == null) return;

        Vector3d cam = mc.getRenderManager().info.getProjectedView();
        RenderSystem.pushMatrix();
        RenderSystem.translated(-cam.x, -cam.y, -cam.z);

        for (Entity entity : mc.world.getAllEntities()) {
            if (shouldSkipPlayer(entity)) continue;
            renderPlayerBox((LivingEntity) entity, mc.getRenderPartialTicks());
        }

        RenderSystem.popMatrix();
    }

    private boolean shouldSkipPlayer(Entity entity) {
        return entity == null
                || !entity.isAlive()
                || !(entity instanceof PlayerEntity)
                || (entity == mc.player && mc.gameSettings.getPointOfView().firstPerson());
    }

    private void renderPlayerBox(LivingEntity player, float tickDelta) {
        double x = MathHelper.lerp(tickDelta, player.lastTickPosX, player.getPosX());
        double y = MathHelper.lerp(tickDelta, player.lastTickPosY, player.getPosY());
        double z = MathHelper.lerp(tickDelta, player.lastTickPosZ, player.getPosZ());

        AxisAlignedBB bb = player.getBoundingBox()
                .offset(-player.getPosX() + x, -player.getPosY() + y, -player.getPosZ() + z);

        float hurtPercent = (float) Math.sin(player.hurtTime * Math.PI / 10f);
        boolean friend = isFriend(player);

        int baseColor = friend ? ColorUtil.getColor(0, 255, 0) : ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL);
        int hurtColor = ColorUtil.getColor(255, 0, 0);
        int color = overCol(baseColor, hurtColor, hurtPercent);

        if (pulse.get()) {
            if (stopWatch.finished(25)) {
                if (decrease) {
                    alpha -= 2;
                    if (alpha <= 30) {
                        alpha = 30;
                        decrease = false;
                    }
                } else {
                    alpha += 2;
                    if (alpha >= 50) {
                        alpha = 60;
                        decrease = true;
                    }
                }
                stopWatch.reset();
            }
        } else {
            alpha = 40;
        }

        int fillColor = ColorUtil.setAlpha(color, (int) alpha);
        int outlineColor = ColorUtil.setAlpha(color, 255);

        drawFill(bb, fillColor);
        drawOutline(bb, outlineColor, 1f);
    }

    private void drawFill(AxisAlignedBB bb, int color) {
        setupRender();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableAlphaTest();
        RenderSystem.shadeModel(7425);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        float a = (color >> 24 & 255) / 255f;
        float r = (color >> 16 & 255) / 255f;
        float g = (color >> 8 & 255) / 255f;
        float b = (color & 255) / 255f;

        addBoxVertices(bb, r, g, b, a);

        tessellator.draw();
        RenderSystem.enableAlphaTest();
        RenderSystem.shadeModel(7424);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        cleanupRender();
    }

    private void drawOutline(AxisAlignedBB bb, int color, float width) {
        setupRender();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableAlphaTest();
        RenderSystem.shadeModel(7425);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glLineWidth(width);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);

        float a = (color >> 24 & 255) / 255f;
        float r = (color >> 16 & 255) / 255f;
        float g = (color >> 8 & 255) / 255f;
        float b = (color & 255) / 255f;

        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        addBoxOutlineVertices(bb, r, g, b, a);
        tessellator.draw();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.enableAlphaTest();
        RenderSystem.shadeModel(7424);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        cleanupRender();
    }

    private void addBoxVertices(AxisAlignedBB bb, float r, float g, float b, float a) {
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

    private void addBoxOutlineVertices(AxisAlignedBB bb, float r, float g, float b, float a) {
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

    private void setupRender() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.disableLighting();
        RenderSystem.disableCull();
    }

    private void cleanupRender() {
        RenderSystem.enableCull();
        RenderSystem.enableTexture();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private boolean isFriend(Entity entity) {
        return entity instanceof PlayerEntity
                && SkyCore.getInstance().getFriendManager().isFriend(entity.getName().getString());
    }

    public int overCol(int color1, int color2, float percent) {
        percent = MathHelper.clamp(percent, 0f, 1f);
        return ColorUtil.getColor(
                (int) MathHelper.lerp(percent, ColorUtil.red(color1), ColorUtil.red(color2)),
                (int) MathHelper.lerp(percent, ColorUtil.green(color1), ColorUtil.green(color2)),
                (int) MathHelper.lerp(percent, ColorUtil.blue(color1), ColorUtil.blue(color2)),
                ColorUtil.alpha(color1)
        );
    }
}

