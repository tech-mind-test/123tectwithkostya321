package sky.core.ui.screens;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.GradientUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import lombok.Getter;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

import static sky.core.utils.Wrapper.mc;

public class MainScreen extends Screen {

    private final List<MainMenuButton> menuButtons = new ArrayList<>();
    private static final float BUTTON_WIDTH = 140;
    private static final float BUTTON_HEIGHT = 23.5f;
    private static final float SPACING = 4.5f;

    public MainScreen() {
        super(NarratorChatListener.EMPTY);
    }

    
    @Override
    protected void init() {
        super.init();
        menuButtons.clear();
        Vector2f center = new Vector2f((int) (mc.getMainWindow().getScaledWidth() / 2f), (int) (mc.getMainWindow().getScaledHeight() / 2f));
        float startY = center.getY() - (BUTTON_HEIGHT * 2 + SPACING * 1.5f);

        addButton("Одиночная игра", center.getX(), startY, BUTTON_WIDTH, true, () -> {
            assert this.minecraft != null;
            this.minecraft.displayGuiScreen(new WorldSelectionScreen(this));
        });
        addButton("Сетевая игра", center.getX(), startY + BUTTON_HEIGHT + SPACING, BUTTON_WIDTH, false, () -> {
            assert this.minecraft != null;
            Screen screen = this.minecraft.gameSettings.skipMultiplayerWarning ? new MultiplayerScreen(this) : new MultiplayerWarningScreen(this);
            this.minecraft.displayGuiScreen(screen);
        });
        addButton("Аккаунты", center.getX(), startY + (BUTTON_HEIGHT + SPACING) * 2, BUTTON_WIDTH, false, () -> {
            assert this.minecraft != null;
            this.minecraft.displayGuiScreen(new AccountsScreen());
        });
        addButton("Настройки", center.getX(), startY + (BUTTON_HEIGHT + SPACING) * 3, BUTTON_WIDTH, false, () -> {
            assert this.minecraft != null;
            this.minecraft.displayGuiScreen(new OptionsScreen(this, this.minecraft.gameSettings));
        });
        assert this.minecraft != null;
        addButton("Выйти", center.getX(), startY + (BUTTON_HEIGHT + SPACING + 5.5f) * 4, BUTTON_WIDTH, false, this.minecraft::shutdown);
    }

    private void addButton(String text, float x, float y, float width, boolean showTitle, Runnable action) {
        MainMenuButton button = new MainMenuButton((int) (x - width / 2), (int) y, (int) width, (int) BUTTON_HEIGHT, new StringTextComponent(text), showTitle, btn -> action.run());
        menuButtons.add(button);
        buttons.add(button);
    }

    
    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        int baseScale = (int) mc.getMainWindow().getGuiScaleFactor();
        mc.gameRenderer.setupOverlayRendering(2);
        float factor = baseScale / 2.0f;
        mouseX = (int) (mouseX * factor);
        mouseY = (int) (mouseY * factor);
        int windowWidth = mc.getMainWindow().getScaledWidth();
        int windowHeight = mc.getMainWindow().getScaledHeight();

        float centerY = windowHeight / 2f;
        float startY = centerY - (BUTTON_HEIGHT * 2 + SPACING * 1.5f);
        for (int i = 0; i < menuButtons.size(); i++) {
            MainMenuButton b = menuButtons.get(i);
            float by = startY + i * (BUTTON_HEIGHT + SPACING);
            if (i == 4) {
                by = startY + (BUTTON_HEIGHT + SPACING + 5.5f) * 4;
            }
            b.y = (int) by;
        }

        // --- Программный фон ---
        // 1. Тёмная подложка
        RenderUtil.drawRectangle(0, 0, windowWidth, windowHeight, ColorUtil.getColor(13, 17, 23, 255));

        // 2. Dot grid
        drawDotGrid(windowWidth, windowHeight);

        // 3. Радиальный градиент от цвета темы сверху-центра
        int themeColor = ThemeEditor.getColor(ThemeSettings.LOGO);
        drawRadialGlow(windowWidth / 2f, windowHeight * 0.1f, windowWidth * 0.55f, themeColor);

        float normMouseX = (mouseX / (float) windowWidth) * 2 - 1;
        float normMouseY = (mouseY / (float) windowHeight) * 2 - 1;
        float offsetYBackground = normMouseY * 3;
        float maxParalax = 2;
        int renderWidth = (int) (windowWidth * 1.05f);
        int renderHeight = (int) (windowHeight * 1.05f);
        float maxOffsetX = (renderWidth - windowWidth) / 2.0f;
        float maxOffsetY = (renderHeight - windowHeight) / 2.0f;

        if (windowWidth >= 800) {
            float offsetXOutline = normMouseX * maxParalax;
            float outlineWidth = 336;
            float outlineHeight = 480;
            offsetXOutline = MathHelper.clamp(offsetXOutline, -maxOffsetX, maxOffsetX);
            float outlineY = centerY - outlineHeight / 2 - 13.5f;
            RenderUtil.drawImage2D(new ResourceLocation("SkyCore/icons/mainmenu/icon_outline.png"), offsetXOutline - 45, outlineY + offsetYBackground, outlineWidth, outlineHeight, -1);

            float offsetXIcon = normMouseX * maxParalax;
            float iconWidth = 457;
            float iconHeight = 515;
            offsetXIcon = MathHelper.clamp(offsetXIcon, -maxOffsetX, maxOffsetX);
            float iconY = centerY - iconHeight / 2 + 24;
            RenderUtil.drawImage2D(new ResourceLocation("SkyCore/icons/mainmenu/icon.png"), offsetXIcon - 10, iconY + offsetYBackground, iconWidth, iconHeight, -1);

            Fonts.sf_regular[13].drawString(matrices, "Авторское право ©Peduj 2025", (mc.getMainWindow().getScaledWidth() - Fonts.sf_regular[13].getWidth("Авторское право @g 2025")) / 2.0f, mc.getMainWindow().getScaledHeight() - Fonts.sf_regular[13].getHeight() - 3, ColorUtil.getColor(255, 255, 255, 80));
        }

        super.render(matrices, mouseX, mouseY, delta);
        mc.gameRenderer.setupOverlayRendering();
    }

    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int baseScale = (int) mc.getMainWindow().getGuiScaleFactor();
        double factor = baseScale / 2.0;
        double sx = mouseX * factor;
        double sy = mouseY * factor;

        boolean handled = false;
        for (MainMenuButton widget : menuButtons) {
            if (widget.mouseClicked(sx, sy, button)) {
                handled = true;
            }
        }
        return handled || super.mouseClicked(sx, sy, button);
    }

    private void drawDotGrid(int width, int height) {
        int dotSpacing = 18;
        int dotRadius = 1;
        int dotColor = ColorUtil.getColor(255, 255, 255, 28);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        float r = ((dotColor >> 16) & 0xFF) / 255f;
        float g = ((dotColor >> 8) & 0xFF) / 255f;
        float b = (dotColor & 0xFF) / 255f;
        float a = ((dotColor >> 24) & 0xFF) / 255f;

        for (int x = dotSpacing; x < width; x += dotSpacing) {
            for (int y = dotSpacing; y < height; y += dotSpacing) {
                buffer.pos(x - dotRadius, y - dotRadius, 0).color(r, g, b, a).endVertex();
                buffer.pos(x - dotRadius, y + dotRadius, 0).color(r, g, b, a).endVertex();
                buffer.pos(x + dotRadius, y + dotRadius, 0).color(r, g, b, a).endVertex();
                buffer.pos(x + dotRadius, y - dotRadius, 0).color(r, g, b, a).endVertex();
            }
        }
        tessellator.draw();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    private void drawRadialGlow(float cx, float cy, float radius, int themeColor) {
        int tr = (themeColor >> 16) & 0xFF;
        int tg = (themeColor >> 8) & 0xFF;
        int tb = themeColor & 0xFF;

        int steps = 64;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);

        buffer.pos(cx, cy, 0).color(tr / 255f, tg / 255f, tb / 255f, 0.18f).endVertex();
        for (int i = 0; i <= steps; i++) {
            double angle = (2 * Math.PI * i) / steps;
            float ex = cx + (float) Math.cos(angle) * radius;
            float ey = cy + (float) Math.sin(angle) * radius * 0.55f;
            buffer.pos(ex, ey, 0).color(tr / 255f, tg / 255f, tb / 255f, 0.0f).endVertex();
        }
        tessellator.draw();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    @Getter
    public static class MainMenuButton extends Button {
        private final boolean showTitle;

        public MainMenuButton(int x, int y, int width, int height, StringTextComponent title, boolean showTitle, IPressable pressedAction) {
            super(x, y, width, height, title, pressedAction);
            this.showTitle = showTitle;
        }

        
        @Override
        public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            float buttonWidth = 139;
            float buttonX;
            float buttonY = y + 5.5f;

            if (mc.getMainWindow().getScaledWidth() < 800) {
                buttonX = (mc.getMainWindow().getScaledWidth() - buttonWidth) / 2;
            } else {
                float rightMargin = 74;
                buttonX = mc.getMainWindow().getScaledWidth() - buttonWidth - rightMargin;
            }

            boolean hovered = mouseX >= buttonX && mouseX <= buttonX + buttonWidth && mouseY >= buttonY && mouseY <= buttonY + 25;

            RenderUtil.drawRoundedRectangleGradient(buttonX, y + 5.5f, buttonWidth, 25, 5, ColorUtil.getColor(9, 9, 9), ColorUtil.getColor(8, 8, 8), ColorUtil.getColor(31, 31, 31), ColorUtil.getColor(12, 12, 12), 1);

            String text = getMessage().getString();
            float textWidth = Fonts.sf_regular[26].getWidth(text);
            float textHeight = Fonts.sf_regular[26].getHeight();
            float textX = buttonX + (buttonWidth - textWidth) / 2;
            float textY = y + 6 + (25 - textHeight) / 2;

            int textColor = hovered ? ColorUtil.getColor(255, 255, 255, 200) : ColorUtil.getColor(255, 255, 255, 130);
            Fonts.sf_regular[26].drawString(matrixStack, text, textX, textY - 2, textColor);

            if (showTitle) {

                ITextComponent gradientLogo = GradientUtil.gradient("SkyCore", ColorUtil.getColor(0, 0, 255), ColorUtil.getColor(0, 200, 200), 5, 45);
                ITextComponent gradientClient = GradientUtil.gradient("Client", ColorUtil.getColor(0, 0, 255), ColorUtil.getColor(0, 200, 200), 5, 45);

                float titleX = buttonX + (buttonWidth - Fonts.sf_semibold[36].getWidth("SkyCore")) / 2;
                Fonts.sf_semibold[36].drawText(matrixStack, gradientLogo, titleX, y - 40, -1);
                Fonts.sf_semibold[36].drawText(matrixStack, gradientClient, titleX + 4.5f, y - 24.5f, -1);
            }
        }

        
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            float buttonWidth = 139;
            float buttonX;

            float rightMargin = 74;
            float scaledWidthAt2 = (float) (mc.getMainWindow().getFramebufferWidth() / 2.0);
            if (scaledWidthAt2 < 800) {
                buttonX = (scaledWidthAt2 - buttonWidth) / 2f;
            } else {
                buttonX = scaledWidthAt2 - buttonWidth - rightMargin;
            }


            boolean isWithinBounds = mouseX >= buttonX && mouseX < buttonX + buttonWidth && mouseY >= y + 5.5f && mouseY < y + 5.5f + 25;

            if (isWithinBounds && this.active && this.visible) {
                this.onClick(mouseX, mouseY);
                return true;
            }
            return false;
        }
    }
}