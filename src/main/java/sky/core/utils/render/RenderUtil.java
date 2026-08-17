package sky.core.utils.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Matrix4f;
import sky.core.utils.Wrapper;
import sky.core.utils.render.shader.KawaseBlur;
import sky.core.utils.render.shader.ShaderUtil;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector4f;
import org.lwjgl.opengl.GL11;
import sky.core.modules.impl.visuals.Interface;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;

public class RenderUtil implements Wrapper {

    private static int interfaceRoundScopeDepth = 0;
    private static int hudLiquidGlassScopeDepth = 0;
    private static int noLiquidGlassScopeDepth = 0;

    public static void beginInterfaceRounding() {
        interfaceRoundScopeDepth++;
    }

    public static void endInterfaceRounding() {
        interfaceRoundScopeDepth = Math.max(0, interfaceRoundScopeDepth - 1);
    }

    public static void beginHudLiquidGlass() {
        hudLiquidGlassScopeDepth++;
    }

    public static void endHudLiquidGlass() {
        hudLiquidGlassScopeDepth = Math.max(0, hudLiquidGlassScopeDepth - 1);
    }

    public static void beginNoLiquidGlass() {
        noLiquidGlassScopeDepth++;
    }

    public static void endNoLiquidGlass() {
        noLiquidGlassScopeDepth = Math.max(0, noLiquidGlassScopeDepth - 1);
    }

    private static float getInterfaceRoundType() {
        return interfaceRoundScopeDepth > 0 && Interface.isIosRounding() ? 1.0F : 0.0F;
    }

    private static boolean shouldUseNewHudBlur(float width, float height) {
        return interfaceRoundScopeDepth > 0 && Interface.isNewHudBlur() && width >= 40.0F && height >= 13.0F;
    }

    private static boolean shouldUseLiquidGlass(float width, float height) {
        return noLiquidGlassScopeDepth == 0
                && hudLiquidGlassScopeDepth > 0
                && Interface.isNewHud()
                && Interface.newHudMode.is("LiquidGlass")
                && width >= 40.0F
                && height >= 13.0F;
    }

    private static int getNewHudBlurColor() {
        return ThemeEditor.getColor(ThemeSettings.WINDOW_BG);
    }

    public static void drawRoundedRectangle(float x, float y, float width, float height, float radius, int color) {
        drawRoundedRectangle(x, y, width, height, new Vector4f(radius, radius, radius, radius), color);
    }
    public static void drawGradientRoundedRectangle(float x, float y, float width, float height, float radius, int color1, int color2) {
        ShaderUtil.rounded_rectangle_gradient.attach();

        ShaderUtil.rounded_rectangle_gradient.setUniformf("size", width, height);
        ShaderUtil.rounded_rectangle_gradient.setUniformf("radius", radius, radius, radius, radius);
        ShaderUtil.rounded_rectangle_gradient.setUniformf("roundType", getInterfaceRoundType());
        ShaderUtil.rounded_rectangle_gradient.setUniformf("color0", ((color1 >> 16) & 0xFF) / 255f, ((color1 >> 8) & 0xFF) / 255f, (color1 & 0xFF) / 255f);
        ShaderUtil.rounded_rectangle_gradient.setUniformf("color1", ((color1 >> 16) & 0xFF) / 255f, ((color1 >> 8) & 0xFF) / 255f, (color1 & 0xFF) / 255f);
        ShaderUtil.rounded_rectangle_gradient.setUniformf("color2", ((color2 >> 16) & 0xFF) / 255f, ((color2 >> 8) & 0xFF) / 255f, (color2 & 0xFF) / 255f);
        ShaderUtil.rounded_rectangle_gradient.setUniformf("color3", ((color2 >> 16) & 0xFF) / 255f, ((color2 >> 8) & 0xFF) / 255f, (color2 & 0xFF) / 255f);
        ShaderUtil.rounded_rectangle_gradient.setUniformf("alpha", 1.0f);

        beginRectBatch(false, false);
        drawQuads(x, y, width, height);
        endRectBatch();

        ShaderUtil.rounded_rectangle_gradient.detach();
    }

    public static void drawGradientRectangle(float x, float y, float width, float height, int color1, int color2) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.shadeModel(GL11.GL_SMOOTH);

        float r1 = ((color1 >> 16) & 0xFF) / 255f;
        float g1 = ((color1 >> 8) & 0xFF) / 255f;
        float b1 = (color1 & 0xFF) / 255f;
        float a1 = ((color1 >> 24) & 0xFF) / 255f;

        float r2 = ((color2 >> 16) & 0xFF) / 255f;
        float g2 = ((color2 >> 8) & 0xFF) / 255f;
        float b2 = (color2 & 0xFF) / 255f;
        float a2 = ((color2 >> 24) & 0xFF) / 255f;

        BUFFER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        BUFFER.pos(x, y + height, 0).color(r2, g2, b2, a2).endVertex();
        BUFFER.pos(x + width, y + height, 0).color(r2, g2, b2, a2).endVertex();
        BUFFER.pos(x + width, y, 0).color(r1, g1, b1, a1).endVertex();
        BUFFER.pos(x, y, 0).color(r1, g1, b1, a1).endVertex();
        TESSELLATOR.draw();

        RenderSystem.shadeModel(GL11.GL_FLAT);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    public static void drawRectangle(float x, float y, float width, float height, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        BUFFER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        BUFFER.pos(x, y + height, 0).color(r, g, b, a).endVertex();
        BUFFER.pos(x + width, y + height, 0).color(r, g, b, a).endVertex();
        BUFFER.pos(x + width, y, 0).color(r, g, b, a).endVertex();
        BUFFER.pos(x, y, 0).color(r, g, b, a).endVertex();
        TESSELLATOR.draw();

        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }
    public static void drawRound(float x, float y, float width, float height, float radius, int color) {
        drawRound(x, y, width, height, color);
    }


    public static void drawRoundedRectangleGradient(float x, float y, float width, float height, float radius, int color0, int color1, int color2, int color3, float alpha) {
        drawRoundedRectangleGradient(x, y, width, height, new Vector4f(radius, radius, radius, radius), color0, color1, color2, color3, alpha);
    }

    public static void drawRoundedRectangleGradientGlowed(float x, float y, float width, float height, float radius, int color0, int color1, int color2, int color3, float alpha, float glowRadius) {
        drawRoundedRectangleGradientGlowed(x, y, width, height, new Vector4f(radius, radius, radius, radius), color0, color1, color2, color3, alpha, glowRadius);
    }

    public static void drawBlurredRoundedRectangle(float x, float y, float width, float height, float radius, int color, float alpha) {
        drawBlurredRoundedRectangle(x, y, width, height, new Vector4f(radius, radius, radius, radius), color, alpha);
    }

    public static void drawOutlineRectangle(float x, float y, float width, float height, float radius, int color, float alpha) {
        drawRoundedOutline(x, y, width, height, radius, 0.12f, color, color, color, color, alpha / 255f, alpha / 255f, alpha / 255f, alpha / 255f);
    }
    public static void drawOutlineRectangleBold(float x, float y, float width, float height, float radius, int color, float alpha) {
        drawRoundedOutline(x, y, width, height, radius, 0.34f, color, color, color, color, alpha / 255f, alpha / 255f, alpha / 255f, alpha / 255f);
    }
    public static void box(AxisAlignedBB bb, int color) {
        GL11.glPushMatrix();

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_CULL_FACE);

        float[] rgb = ColorUtil.IntColor.rgb(color);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        GlStateManager.color4f(rgb[0], rgb[1], rgb[2], 0.15f);

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);

        buffer.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.minZ).endVertex();

        buffer.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();

        buffer.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        buffer.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();

        buffer.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        buffer.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();

        buffer.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();

        buffer.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        buffer.pos(bb.minX, bb.minY, bb.maxZ).endVertex();

        tessellator.draw();

        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        float[] alphas = new float[]{0.2f, 0.15f, 0.1f, 0.06f};
        float[] widths = new float[]{0.01f, 6.0f, 15.0f, 22.0f};


        GL11.glLineWidth(1.5f);
        GlStateManager.color4f(rgb[0], rgb[1], rgb[2], 1.0f);

        buffer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
        buffer.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        buffer.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        buffer.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        tessellator.draw();

        buffer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
        buffer.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        tessellator.draw();

        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        buffer.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.minZ).endVertex();

        buffer.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();

        buffer.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        buffer.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();

        buffer.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        buffer.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        tessellator.draw();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);

        GL11.glPopMatrix();
    }
    public static void drawRect(
            double left,
            double top,
            double right,
            double bottom,
            int color) {
        if (left < right) {
            double i = left;
            left = right;
            right = i;
        }

        if (top < bottom) {
            double j = top;
            top = bottom;
            bottom = j;
        }

        float f3 = (float) (color >> 24 & 255) / 255.0F;
        float f = (float) (color >> 16 & 255) / 255.0F;
        float f1 = (float) (color >> 8 & 255) / 255.0F;
        float f2 = (float) (color & 255) / 255.0F;
        BufferBuilder bufferbuilder = Tessellator.getInstance().getBuffer();
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(left, bottom, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.pos(right, bottom, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.pos(right, top, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.pos(left, top, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.finishDrawing();
        WorldVertexBufferUploader.draw(bufferbuilder);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    private static boolean image3DBatchActive = false;
    private static boolean rectBatchActive = false;
    private static boolean rectBatchWithColor = false;
    private static boolean rectBatchTextured = false;

    public static void beginRectBatch(boolean withColor, boolean textured) {
        if (rectBatchActive) {
            endRectBatch();
        }
        rectBatchActive = true;
        rectBatchWithColor = withColor;
        rectBatchTextured = textured;

        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.shadeModel(GL11.GL_SMOOTH);
        RenderSystem.alphaFunc(GL11.GL_GREATER, 0.01f);
        if (rectBatchTextured) {
            RenderSystem.enableTexture();
        } else {
            RenderSystem.disableTexture();
        }

        BUFFER.begin(GL11.GL_QUADS, withColor ? DefaultVertexFormats.POSITION_TEX_COLOR : DefaultVertexFormats.POSITION_TEX);
    }

    public static void drawCircle(float x, float y, float start, float end, float radius, float thickness, boolean filled, int color) {
        float diameter = radius * 2.0f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        ShaderUtil.circle.attach();

        ShaderUtil.circle.setUniformf("size", diameter, diameter);
        ShaderUtil.circle.setUniform("color", ColorUtil.getColor(color));
        ShaderUtil.circle.setUniformf("radius", radius);
        ShaderUtil.circle.setUniformf("startAngle", start);
        ShaderUtil.circle.setUniformf("endAngle", end);

        float drawThickness = filled ? radius : thickness;
        ShaderUtil.circle.setUniformf("thickness", drawThickness);

        ShaderUtil.circle.setUniformf("smoothness", 1.5f);

        beginRectBatch(false, false);
        drawQuads(x - radius, y - radius, diameter, diameter);
        endRectBatch();

        ShaderUtil.circle.detach();
    }
    public static void drawCircle(final float x, final float y, float start, float end, final float radius, final int color, final float linewidth) {
        RenderSystem.color4f(0.0f, 0.0f, 0.0f, 0.0f);
        if (start > end) {
            final float endOffset = end;
            end = start;
            start = endOffset;
        }
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        enableSmoothLine((float) linewidth);
        RenderSystem.blendFuncSeparate(770, 771, 1, 0);
        GL11.glBegin(3);
        for (float i = end; i >= start; i -= 4.0f) {
            ColorUtil.glHexColor(color, (int) (ColorUtil.getRGBAf(color)[3] * 255));
            final float cos = (float) (Math.cos(i * Math.PI / 180.0) * radius * 1.0);
            final float sin = (float) (Math.sin(i * Math.PI / 180.0) * radius * 1.0);
            GL11.glVertex2f(x + cos, y + sin);
        }
        GL11.glEnd();
        disableSmoothLine();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }
    public static void endRectBatch() {
        if (!rectBatchActive) return;
        TESSELLATOR.draw();
        if (!rectBatchTextured) {
            RenderSystem.enableTexture();
        }
        RenderSystem.shadeModel(GL11.GL_FLAT);
        RenderSystem.disableBlend();
        RenderSystem.popMatrix();
        rectBatchActive = false;
    }
    public static void disableSmoothLine() {
        GL11.glEnable(3553);
        GL11.glEnable(2929);
        GL11.glDisable(3042);
        GL11.glEnable(3008);
        GL11.glDepthMask(true);
        GL11.glCullFace(1029);
        GL11.glDisable(2848);
        GL11.glHint(3154, 4352);
        GL11.glHint(3155, 4352);
    }

    public static void enableSmoothLine(float width) {
        GL11.glDisable(3008);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glDisable(3553);
        GL11.glDisable(2929);
        GL11.glDepthMask(false);
        GL11.glEnable(2884);
        GL11.glEnable(2848);
        GL11.glHint(3154, 4354);
        GL11.glHint(3155, 4354);
        GL11.glLineWidth(width);
    }
    private static ResourceLocation currentBatchTexture = null;
    private static int batchQuadCount = 0;

    public static void drawImage3DQuadInternal(ResourceLocation texture, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, int color0, int color1, int color2, int color3, float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3) {
        boolean needNewBatch = !image3DBatchActive || !texture.equals(currentBatchTexture) || batchQuadCount >= 8192;

        if (needNewBatch) {
            if (image3DBatchActive) {
                TESSELLATOR.draw();
            }

            currentBatchTexture = texture;
            image3DBatchActive = true;
            batchQuadCount = 0;

            RenderSystem.enableBlend();
            RenderSystem.disableAlphaTest();
            RenderSystem.shadeModel(7425);
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.alphaFunc(GL11.GL_GREATER, 0.01f);

            mc.getTextureManager().bindTexture(texture);
            BUFFER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        }

        int r0 = (color0 >> 16) & 0xFF;
        int g0 = (color0 >> 8) & 0xFF;
        int b0 = color0 & 0xFF;
        int a0 = color0 >>> 24;

        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int a1 = color1 >>> 24;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int a2 = color2 >>> 24;

        int r3 = (color3 >> 16) & 0xFF;
        int g3 = (color3 >> 8) & 0xFF;
        int b3 = color3 & 0xFF;
        int a3 = color3 >>> 24;

        BUFFER.pos(x0, y0, z0).tex(u0, v0).color(r0, g0, b0, a0).endVertex();
        BUFFER.pos(x1, y1, z1).tex(u1, v1).color(r1, g1, b1, a1).endVertex();
        BUFFER.pos(x2, y2, z2).tex(u2, v2).color(r2, g2, b2, a2).endVertex();
        BUFFER.pos(x3, y3, z3).tex(u3, v3).color(r3, g3, b3, a3).endVertex();

        batchQuadCount++;
    }

    public static void drawImage3DQuad(ResourceLocation texture, boolean boost, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, int color) {
        int boostalpha = boost ? 30 : 0;
        int red = Math.min(255, ((color >> 16) & 0xFF) + boostalpha);
        int green = Math.min(255, ((color >> 8) & 0xFF) + boostalpha);
        int blue = Math.min(255, (color & 0xFF) + boostalpha);
        int alpha = color >>> 24;

        int boostedColor = (alpha << 24) | (red << 16) | (green << 8) | blue;
        drawImage3DQuadInternal(texture, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, boostedColor, boostedColor, boostedColor, boostedColor, 0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f);
    }

    public static void flushImage3DBatch() {
        if (image3DBatchActive) {
            TESSELLATOR.draw();
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            RenderSystem.enableAlphaTest();
            RenderSystem.shadeModel(7424);
            RenderSystem.depthMask(true);
            image3DBatchActive = false;
            currentBatchTexture = null;
            batchQuadCount = 0;
        }
    }
    public static void drawTexture(MatrixStack matrixStack, ResourceLocation resourceLocation, float x, float y, float width, float height, int color) {
        drawTexture(matrixStack, resourceLocation, x, y, width, height, color, color, color, color);
    }

    public static void drawTexture(MatrixStack matrixStack, ResourceLocation resourceLocation, float x, float y, float width, float height, int color1, int color2, int color3, int color4) {
        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(770, 1, 0, 1);
        RenderSystem.shadeModel(7425);
        RenderSystem.disableAlphaTest();
        RenderSystem.depthMask(false);
        Matrix4f matrix4f = matrixStack.getLast().getMatrix();
        mc.getTextureManager().bindTexture(resourceLocation);
        BUFFER.begin(7, DefaultVertexFormats.POSITION_COLOR_TEX);
        BUFFER.pos(matrix4f, x, y, 0).color(color1).tex(0, 0).endVertex();
        BUFFER.pos(matrix4f, x, y + height, 0).color(color2).tex(0, 1).endVertex();
        BUFFER.pos(matrix4f, x + width, y + height, 0).color(color3).tex(1, 1).endVertex();
        BUFFER.pos(matrix4f, x + width, y, 0).color(color4).tex(1, 0).endVertex();
        TESSELLATOR.draw();
        RenderSystem.depthMask(true);
        RenderSystem.enableAlphaTest();
        RenderSystem.shadeModel(7424);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.popMatrix();
    }
    public static void drawImage2D(ResourceLocation image, float x, float y, float width, float height, int color) {
        mc.getTextureManager().bindTexture(image);
        int filter;
        if (width > 128 || height > 128) {
            filter = GL11.GL_NEAREST;
        } else {
            filter = GL11.GL_LINEAR;
        }
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        float a = (color >> 24 & 255) / 255.0F;
        RenderSystem.color4f(r, g, b, a);
        beginRectBatch(true, true);
        drawQuads(x, y, width, height, color);
        endRectBatch();
    }

    public static void drawImage3D(ResourceLocation image, float x, float y, float z, float width, float height, int color, boolean boost) {
        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.alphaFunc(GL11.GL_GREATER, 0.01f);

        int boostalpha = boost ? 40 : 0;
        int red = Math.min(255, ((color >> 16) & 0xFF) + boostalpha);
        int green = Math.min(255, ((color >> 8) & 0xFF) + boostalpha);
        int blue = Math.min(255, (color & 0xFF) + boostalpha);
        int alpha = color >>> 24;

        mc.getTextureManager().bindTexture(image);
        BUFFER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

        BUFFER.pos(x, y + height, z).tex(0, 1).color(red, green, blue, alpha).endVertex();
        BUFFER.pos(x + width, y + height, z).tex(1, 1).color(red, green, blue, alpha).endVertex();
        BUFFER.pos(x + width, y, z).tex(1, 0f).color(red, green, blue, alpha).endVertex();
        BUFFER.pos(x, y, z).tex(0, 0f).color(red, green, blue, alpha).endVertex();

        TESSELLATOR.draw();

        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.popMatrix();
    }

    public static void drawRoundedRectangle(float x, float y, float width, float height, Vector4f radius, int color) {
        if (shouldUseLiquidGlass(width, height)) {
            float sourceAlpha = ColorUtil.alphaf(color);
            float backgroundRadiusScale = Interface.isIosRounding() ? 1.18F : 0.95F;
            Vector4f backgroundRadius = new Vector4f(
                    radius.getX() * backgroundRadiusScale,
                    radius.getY() * backgroundRadiusScale,
                    radius.getZ() * backgroundRadiusScale,
                    radius.getW() * backgroundRadiusScale
            );
            float glassRadiusScale = Interface.isIosRounding() ? 2.05F : 1.15F;
            Vector4f glassRadius = new Vector4f(
                    radius.getX() * glassRadiusScale,
                    radius.getY() * glassRadiusScale,
                    radius.getZ() * glassRadiusScale,
                    radius.getW() * glassRadiusScale
            );

            drawBlurredRoundedRectangle(
                    x, y, width, height,
                    backgroundRadius,
                    ColorUtil.rgba(0, 0, 0, 185),
                    sourceAlpha * 0.98F
            );

            drawLiquidGlassRoundedRectangle(x, y, width, height, glassRadius, sourceAlpha * 0.72F);
            return;
        }

        if (shouldUseNewHudBlur(width, height)) {
            drawBlurredRoundedRectangle(x, y, width, height, radius, getNewHudBlurColor(), ColorUtil.alphaf(color));
            return;
        }

        ShaderUtil.rounded_rectangle.attach();
        ShaderUtil.rounded_rectangle.setUniformf("size", width, height);
        ShaderUtil.rounded_rectangle.setUniform("radius", radius.getX(), radius.getY(), radius.getZ(), radius.getW());
        ShaderUtil.rounded_rectangle.setUniformf("roundType", getInterfaceRoundType());
        ShaderUtil.rounded_rectangle.setUniform("color", ColorUtil.getColor(color));

        beginRectBatch(false, false);
        drawQuads(x, y, width, height);
        endRectBatch();

        ShaderUtil.rounded_rectangle.detach();
    }

    public static void drawRound(float x, float y, float width, float height, int color) {
        ShaderUtil.round.attach();
        ShaderUtil.round.setUniformf("size", width, height);
        ShaderUtil.round.setUniform("color", ColorUtil.getColor(color));

        beginRectBatch(false, false);
        drawQuads(x, y, width, height);
        endRectBatch();

        ShaderUtil.round.detach();
    }



    public static void drawRoundedRectangleGradient(float x, float y, float width, float height, Vector4f radius, int color0, int color1, int color2, int color3, float alpha) {
        ShaderUtil.rounded_rectangle_gradient.attach();

        ShaderUtil.rounded_rectangle_gradient.setUniformf("size", width, height);
        ShaderUtil.rounded_rectangle_gradient.setUniformf("radius", radius.getX(), radius.getY(), radius.getZ(), radius.getW());
        ShaderUtil.rounded_rectangle_gradient.setUniformf("roundType", getInterfaceRoundType());
        ShaderUtil.rounded_rectangle_gradient.setUniformf("color0", ((color0 >> 16) & 0xFF) / 255f, ((color0 >> 8) & 0xFF) / 255f, (color0 & 0xFF) / 255f);
        ShaderUtil.rounded_rectangle_gradient.setUniformf("color1", ((color1 >> 16) & 0xFF) / 255f, ((color1 >> 8) & 0xFF) / 255f, (color1 & 0xFF) / 255f);
        ShaderUtil.rounded_rectangle_gradient.setUniformf("color2", ((color2 >> 16) & 0xFF) / 255f, ((color2 >> 8) & 0xFF) / 255f, (color2 & 0xFF) / 255f);
        ShaderUtil.rounded_rectangle_gradient.setUniformf("color3", ((color3 >> 16) & 0xFF) / 255f, ((color3 >> 8) & 0xFF) / 255f, (color3 & 0xFF) / 255f);
        ShaderUtil.rounded_rectangle_gradient.setUniformf("alpha", alpha);

        beginRectBatch(false, false);
        drawQuads(x, y, width, height);
        endRectBatch();

        ShaderUtil.rounded_rectangle_gradient.detach();
    }

    public static void drawRoundedRectangleGradientGlowed(float x, float y, float width, float height, Vector4f radius, int color0, int color1, int color2, int color3, float alpha, float glowRadius) {
        ShaderUtil.rounded_rectangle_gradient_glowed.attach();

        ShaderUtil.rounded_rectangle_gradient_glowed.setUniformf("size", width, height);
        ShaderUtil.rounded_rectangle_gradient_glowed.setUniformf("radius", radius.getX(), radius.getY(), radius.getZ(), radius.getW());
        ShaderUtil.rounded_rectangle_gradient_glowed.setUniformf("roundType", getInterfaceRoundType());
        ShaderUtil.rounded_rectangle_gradient_glowed.setUniformf("color0", ((color0 >> 16) & 0xFF) / 255f, ((color0 >> 8) & 0xFF) / 255f, (color0 & 0xFF) / 255f);
        ShaderUtil.rounded_rectangle_gradient_glowed.setUniformf("color1", ((color1 >> 16) & 0xFF) / 255f, ((color1 >> 8) & 0xFF) / 255f, (color1 & 0xFF) / 255f);
        ShaderUtil.rounded_rectangle_gradient_glowed.setUniformf("color2", ((color2 >> 16) & 0xFF) / 255f, ((color2 >> 8) & 0xFF) / 255f, (color2 & 0xFF) / 255f);
        ShaderUtil.rounded_rectangle_gradient_glowed.setUniformf("color3", ((color3 >> 16) & 0xFF) / 255f, ((color3 >> 8) & 0xFF) / 255f, (color3 & 0xFF) / 255f);
        ShaderUtil.rounded_rectangle_gradient_glowed.setUniformf("alpha", alpha);
        ShaderUtil.rounded_rectangle_gradient_glowed.setUniformf("glowRadius", glowRadius);

        beginRectBatch(false, false);
        drawQuads(x, y, width, height);
        endRectBatch();

        ShaderUtil.rounded_rectangle_gradient_glowed.detach();
    }

    public static void drawBlurredRoundedRectangle(float x, float y, float width, float height, Vector4f radius, int color, float alpha) {
        RenderSystem.bindTexture(KawaseBlur.blur.BLURRED.framebufferTexture);

        ShaderUtil.blurred_round_rectangle.attach();

        ShaderUtil.blurred_round_rectangle.setUniformf("resolution", mc.getMainWindow().getWidth(), mc.getMainWindow().getHeight());
        ShaderUtil.blurred_round_rectangle.setUniformf("start", x, y);
        ShaderUtil.blurred_round_rectangle.setUniformf("size", width, height);
        ShaderUtil.blurred_round_rectangle.setUniform("round", radius.getX(), radius.getY(), radius.getZ(), radius.getW());
        ShaderUtil.blurred_round_rectangle.setUniformf("roundType", getInterfaceRoundType());
        ShaderUtil.blurred_round_rectangle.setUniform("alpha", alpha);
        ShaderUtil.blurred_round_rectangle.setUniform("color", ColorUtil.getColor(color));

        beginRectBatch(false, true);
        drawQuads(x, y, width, height);
        endRectBatch();

        ShaderUtil.blurred_round_rectangle.detach();
    }

    public static void drawLiquidGlassRoundedRectangle(float x, float y, float width, float height, Vector4f radius, float alpha) {
        RenderSystem.bindTexture(KawaseBlur.blur.BLURRED.framebufferTexture);

        ShaderUtil.liquid_glass_rectangle.attach();

        ShaderUtil.liquid_glass_rectangle.setUniformf("resolution", mc.getMainWindow().getWidth(), mc.getMainWindow().getHeight());
        ShaderUtil.liquid_glass_rectangle.setUniformf("size", width, height);
        ShaderUtil.liquid_glass_rectangle.setUniform("round", radius.getX(), radius.getY(), radius.getZ(), radius.getW());
        ShaderUtil.liquid_glass_rectangle.setUniform("alpha", alpha);
        ShaderUtil.liquid_glass_rectangle.setUniform("color", ColorUtil.getColor(ColorUtil.rgba(0, 0, 0, 255)));

        beginRectBatch(false, true);
        drawQuads(x, y, width, height);
        endRectBatch();

        ShaderUtil.liquid_glass_rectangle.detach();
    }

    public static void drawRoundedHead(ResourceLocation skin, LivingEntity target, float x, float y, float width, float height, float radius, float alpha) {
        float hurt_time = target != null ? target.hurtTime : 0;
        hurt_time = hurt_time > 0 ? Math.min(0.25f, hurt_time / target.maxHurtTime) : 0;

        mc.getTextureManager().bindTexture(skin);

        ShaderUtil.rounded_head_texture.attach();

        ShaderUtil.rounded_head_texture.setUniformf("size", width, height);
        ShaderUtil.rounded_head_texture.setUniformf("radius", radius);
        ShaderUtil.rounded_head_texture.setUniformf("roundType", getInterfaceRoundType());
        ShaderUtil.rounded_head_texture.setUniformf("hurt_time", hurt_time);
        ShaderUtil.rounded_head_texture.setUniformf("alpha", alpha);
        ShaderUtil.rounded_head_texture.setUniformf("texXSize", 64);
        ShaderUtil.rounded_head_texture.setUniformf("texYSize", 64);

        beginRectBatch(false, true);
        drawQuads(x, y, width, height);
        endRectBatch();

        ShaderUtil.rounded_head_texture.detach();
    }

    public static void drawRoundedTexture(MatrixStack matrixStack, ResourceLocation texture, float x, float y, float width, float height, float radius, float alpha) {
        mc.getTextureManager().bindTexture(texture);

        ShaderUtil.rounded_texture.attach();
        ShaderUtil.rounded_texture.setUniformf("size", width, height);
        ShaderUtil.rounded_texture.setUniformf("radius", radius);
        ShaderUtil.rounded_texture.setUniformf("alpha", alpha);

        beginRectBatch(false, true);
        drawQuads(x, y, width, height);
        endRectBatch();

        ShaderUtil.rounded_texture.detach();
    }

    public static void drawRoundedOutline(float x, float y, float width, float height, float radius, float borderSize, int color0, int color1, int color2, int color3, float alpha1, float alpha2, float alpha3, float alpha4) {
        ShaderUtil.outline.attach();

        ShaderUtil.outline.setUniformf("u_size", width, height);
        ShaderUtil.outline.setUniformf("u_radius", radius);
        ShaderUtil.outline.setUniformf("u_round_type", getInterfaceRoundType());
        ShaderUtil.outline.setUniformf("u_border_size", borderSize);
        ShaderUtil.outline.setUniformf("u_alpha1", alpha1);
        ShaderUtil.outline.setUniformf("u_alpha2", alpha2);
        ShaderUtil.outline.setUniformf("u_alpha3", alpha3);
        ShaderUtil.outline.setUniformf("u_alpha4", alpha4);
        ShaderUtil.outline.setUniformf("u_color_1", ((color0 >> 16) & 0xFF) / 255f, ((color0 >> 8) & 0xFF) / 255f, (color0 & 0xFF) / 255f, 1);
        ShaderUtil.outline.setUniformf("u_color_2", ((color1 >> 16) & 0xFF) / 255f, ((color1 >> 8) & 0xFF) / 255f, (color1 & 0xFF) / 255f, 1);
        ShaderUtil.outline.setUniformf("u_color_3", ((color2 >> 16) & 0xFF) / 255f, ((color2 >> 8) & 0xFF) / 255f, (color2 & 0xFF) / 255f, 1);
        ShaderUtil.outline.setUniformf("u_color_4", ((color3 >> 16) & 0xFF) / 255f, ((color3 >> 8) & 0xFF) / 255f, (color3 & 0xFF) / 255f, 1);

        beginRectBatch(false, false);
        drawQuads(x, y, width, height);
        endRectBatch();

        ShaderUtil.outline.detach();
    }

    public static void drawStack(ItemStack itemStack, float x, float y, float size) {
        drawStack(itemStack, x, y, size, 1.0f);
    }

    public static void drawStack(ItemStack itemStack, float x, float y, float size, float alpha) {
        if (itemStack.isEmpty()) return;
        RenderSystem.pushMatrix();
        RenderSystem.translatef(x, y, 0);
        RenderSystem.scalef(size, size, size);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, alpha);

        mc.getItemRenderer().renderItemAndEffectIntoGUI(itemStack, 0, 0);
        mc.getItemRenderer().renderItemOverlays(mc.fontRenderer, itemStack, 0, 0);

        RenderSystem.disableBlend();
        RenderSystem.popMatrix();
    }

    public static void drawPotionLiquid(int color, float x, float y, float width, float height) {
        mc.getTextureManager().bindTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);
        AtlasTexture atlas = (AtlasTexture) mc.getTextureManager().getTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);
        TextureAtlasSprite fluidSprite = atlas.getSprite(new ResourceLocation("minecraft", "item/potion_overlay"));
        float a = (color >> 24 & 0xFF) / 255.0F;
        float r = (color >> 16 & 0xFF) / 255.0F;
        float g = (color >> 8 & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(r, g, b, a);
        BUFFER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        BUFFER.pos(x, y + height, 0).tex(fluidSprite.getMinU(), fluidSprite.getMaxV()).endVertex();
        BUFFER.pos(x + width, y + height, 0).tex(fluidSprite.getMaxU(), fluidSprite.getMaxV()).endVertex();
        BUFFER.pos(x + width, y, 0).tex(fluidSprite.getMaxU(), fluidSprite.getMinV()).endVertex();
        BUFFER.pos(x, y, 0).tex(fluidSprite.getMinU(), fluidSprite.getMinV()).endVertex();
        TESSELLATOR.draw();
        RenderSystem.disableBlend();
        RenderSystem.popMatrix();
    }

    public static void drawMinecraftRectangle(MatrixStack matrixStack, float x, float y, float width, float height, int color) {
        matrixStack.push();
        matrixStack.translate(x, y, 0);
        matrixStack.scale(width, height, 1);
        AbstractGui.fill(matrixStack, 0, 0, 1, 1, color);
        matrixStack.pop();
    }

    public static void drawMinecraftGradientRectangle(MatrixStack matrixStack, float x, float y, float width, float height, int color0, int color1) {
        matrixStack.push();
        matrixStack.translate(x, y, 0);
        matrixStack.scale(width, height, 1);
        AbstractGui.fillGradient(matrixStack, 0, 0, 1, 1, color0, color1);
        matrixStack.pop();
    }

    public static void drawQuads(double x, double y, double width, double height) {
        boolean batching = rectBatchActive && !rectBatchWithColor;
        if (!batching) {
            BUFFER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        }

        BUFFER.pos(x, y, 0).tex(0, 0).endVertex();
        BUFFER.pos(x, y + height, 0).tex(0, 1).endVertex();
        BUFFER.pos(x + width, y + height, 0).tex(1, 1).endVertex();
        BUFFER.pos(x + width, y, 0).tex(1, 0).endVertex();

        if (!batching) {
            TESSELLATOR.draw();
        }
    }

    public static void scaleStart(float x, float y, float scale) {
        GlStateManager.pushMatrix();
        GlStateManager.translated(x, y, 0);
        GlStateManager.scaled(scale, scale, 1);
        GlStateManager.translated(-x, -y, 0);
    }

    public static void scaleEnd() {
        GlStateManager.popMatrix();
    }

    public static void rotateStart(float x, float y, float degrees) {
        GlStateManager.pushMatrix();
        GlStateManager.translated(x, y, 0);
        GlStateManager.rotatef(degrees, 0f, 0f, 1f);
        GlStateManager.translated(-x, -y, 0);
    }

    public static void rotateEnd() {
        GlStateManager.popMatrix();
    }

    public static void drawQuads(float x, float y, float width, float height, int color) {
        boolean batching = rectBatchActive && rectBatchWithColor;
        if (!batching) {
            BUFFER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        }

        BUFFER.pos(x, y, 0).tex(0, 0).color(color).endVertex();
        BUFFER.pos(x, y + height, 0).tex(0, 1).color(color).endVertex();
        BUFFER.pos(x + width, y + height, 0).tex(1, 1).color(color).endVertex();
        BUFFER.pos(x + width, y, 0).tex(1, 0).color(color).endVertex();

        if (!batching) {
            TESSELLATOR.draw();
        }
    }
}
