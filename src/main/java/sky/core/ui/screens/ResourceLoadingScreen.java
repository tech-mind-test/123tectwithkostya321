package sky.core.ui.screens;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;

public class ResourceLoadingScreen {

    private static final ResourceLocation MENU_BACKGROUND =
            new ResourceLocation("minecraft", "SkyCore/mainmenu/background.png");
    private static final ResourceLocation MENU_LOGO =
            new ResourceLocation("");

    public static void render(Minecraft mc, MatrixStack matrixStack, int screenWidth, int screenHeight,
                              float alpha, float progress, float displayProgress) {
        int alpha255 = MathHelper.ceil(alpha * 255.0F);

        GlStateManager.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        fillBackground(mc, matrixStack, screenWidth, screenHeight, alpha);

        float barMarginX = screenWidth * 0.03f;
        float barW = screenWidth - barMarginX * 2f;
        float barH = 3.5f;
        float barX = barMarginX;
        float barY = screenHeight * 0.98f;

        String loadingMessage = getLoadingMessage(progress);
        String progText = MathHelper.floor(displayProgress * 100f) + "%";
        int textGray = ColorUtil.getColor(103, 106, 114, (int) (0.85f * alpha255));

        try {
            if (Fonts.sf_regular != null && Fonts.sf_regular[13] != null) {
                float textY = barY - Fonts.sf_regular[13].getHeight() - 6f;
                Fonts.sf_regular[13].drawString(matrixStack, loadingMessage, barX, textY, textGray);
                float progW = Fonts.sf_regular[13].getWidth(progText);
                Fonts.sf_regular[13].drawString(matrixStack, progText, barX + barW - progW, textY, textGray);
            }
        } catch (Throwable ignored) {
        }

        drawProgressBar(barX, barY, barW, barH, progress, alpha);
    }

    private static void fillBackground(Minecraft mc, MatrixStack ms, int w, int h, float alpha) {
        int bgFill = ((int) (255f * alpha) << 24) | 0x151418;
        net.minecraft.client.gui.AbstractGui.fill(ms, 0, 0, w, h, bgFill);
        RenderSystem.color4f(1f, 1f, 1f, alpha);
        mc.getTextureManager().bindTexture(MENU_BACKGROUND);
        net.minecraft.client.gui.AbstractGui.blit(ms, 0, 0, 0, 0, w, h, w, h);
        RenderSystem.color4f(1f, 1f, 1f, 1f);
    }



    private static void drawProgressBar(float barX, float barY, float barW, float barH, float progress, float alpha) {
        float clamped = MathHelper.clamp(progress, 0f, 1f);
        float radius = barH * 0.5f;

        int trackColor = ColorUtil.getColor(255, 255, 255, (int) (10f * alpha));
        RenderUtil.drawRoundedRectangle(barX, barY, barW, barH, radius, trackColor);

        float fillW = barW * clamped;
        if (fillW <= 0.5f) return;

        long time = Util.milliTime();
        float wave = (float) (Math.sin(time / 380.0) * 0.5 + 0.5);
        float wave2 = (float) (Math.sin(time / 520.0 + 1.4) * 0.5 + 0.5);

        int blueDark = ColorUtil.getColor(
                (int) (40 + 30 * wave),
                (int) (100 + 40 * wave2),
                (int) (220 + 25 * wave),
                255);
        int blueMid = ColorUtil.getColor(
                (int) (90 + 40 * wave2),
                (int) (150 + 35 * wave),
                (int) (255),
                255);
        int blueBright = ColorUtil.getColor(
                (int) (154 + 50 * wave),
                (int) (192 + 30 * wave2),
                255,
                255);

        float barAlpha = alpha * (0.75f + 0.25f * wave);
        RenderUtil.drawRoundedRectangleGradient(barX, barY, fillW, barH, radius,
                blueDark, blueMid, blueBright, blueMid, barAlpha);

        float shineW = Math.min(fillW * 0.35f, 80f);
        float shinePhase = (time % 2400L) / 2400f;
        float shineX = barX + (fillW + shineW) * shinePhase - shineW;
        shineX = MathHelper.clamp(shineX, barX, barX + fillW - shineW);
        if (shineW > 2f && shineX + shineW <= barX + fillW) {
            int shineColor = ColorUtil.getColor(255, 255, 255, (int) (90 * alpha * (0.35f + 0.65f * wave)));
            RenderUtil.drawRoundedRectangleGradient(shineX, barY, shineW, barH, radius,
                    ColorUtil.applyOpacity(shineColor, 0),
                    shineColor,
                    ColorUtil.applyOpacity(shineColor, 0),
                    shineColor,
                    barAlpha * 0.85f);
        }
    }

    private static String getLoadingMessage(float progress) {
        if (progress < 0.2f) return "Loading libraries...";
        if (progress < 0.6f) return "Loading native...";
        if (progress < 0.8f) return "Loading assets...";
        if (progress < 0.96f) return "Loading configurations...";
        return "Finalizing resources...";
    }
}