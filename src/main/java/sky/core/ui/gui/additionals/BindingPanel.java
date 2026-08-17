package sky.core.ui.gui.additionals;

import com.mojang.blaze3d.matrix.MatrixStack;
import sky.core.modules.Module;
import sky.core.modules.api.elements.Element;
import sky.core.modules.api.elements.ModuleElement;
import sky.core.modules.api.elements.impl.BooleanElement;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.Wrapper;
import sky.core.utils.animation.AnimationUtil;
import sky.core.utils.animation.Easings;
import sky.core.utils.math.MathUtil;
import sky.core.utils.misc.KeyMapper;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;
import lombok.Getter;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.glfw.GLFW;


public class BindingPanel implements Wrapper {
    @Getter
    private final Element element;
    private final AnimationUtil animation = new AnimationUtil(0f, 6f, Easings.QUAD_IN_OUT);
    private final AnimationUtil strikeAnimation = new AnimationUtil(0f, 6f, Easings.LINEAR);
    private final AnimationUtil toggleModeAnimation = new AnimationUtil(0f, 5f, Easings.LINEAR);
    private final AnimationUtil hideTooltipAnimation = new AnimationUtil(0f, 10f, Easings.LINEAR);
    private final AnimationUtil resetTooltipAnimation = new AnimationUtil(0f, 10f, Easings.LINEAR);
    private float x, y;
    private final float width = 70;
    private final float height = 18;
    private boolean waitingForKey = false;
    public boolean shouldClose = false;

    public BindingPanel(Element element) {
        this.element = element;
        this.x = element.getX() + element.getWidth();
        this.y = element.getY() + 4;
        boolean isVisible = element instanceof ModuleElement ? ((ModuleElement) element).getModule().isKeybindvisible() : ((BooleanElement) element).getSetting().isKeybindvisible();
        strikeAnimation.setValue(isVisible ? 0f : 1f);
    }

    public void render(MatrixStack matrixStack) {
        Module.ToggleMode currentMode = element instanceof ModuleElement ? ((ModuleElement) element).getModule().getToggleMode() : ((BooleanElement) element).getSetting().getToggleMode();
        toggleModeAnimation.setSpeed(5f);
        toggleModeAnimation.setEasing(Easings.LINEAR);
        toggleModeAnimation.update(currentMode == Module.ToggleMode.HOLD ? 1f : 0f);
        float tmValue = toggleModeAnimation.getValue();
        if (shouldClose && animation.getValue() <= 0.01) {
            if (element instanceof ModuleElement) {
                ((ModuleElement) element).setBind(false);
            } else {
                ((BooleanElement) element).setBind(false);
            }
            shouldClose = false;
            waitingForKey = false;
        }
        animation.setSpeed(6f);
        animation.setEasing(Easings.QUAD_IN_OUT);
        animation.update(shouldClose ? 0f : (element instanceof ModuleElement ? ((ModuleElement) element).isBind() : ((BooleanElement) element).isBind()) ? 1f : 0f);

        if (animation.getValue() <= 0) {
            return;
        }

        RenderUtil.scaleStart(x + width / 2f, y + height / 2f, animation.getValue());
        int logoColor = ThemeEditor.getColor(ThemeSettings.LOGO);
        int bgColor = ColorUtil.darken(logoColor, 0.1f);
        int bgWithAlpha = ColorUtil.applyOpacity(bgColor, ThemeEditor.getAlpha(ThemeSettings.LOGO) / 255f * animation.getValue());

        RenderUtil.drawRoundedRectangle(x, y, width, height, 5, bgWithAlpha);
        Fonts.sf_regular[14].drawString(matrixStack, "Бинд", x + 5, y + 7, ThemeEditor.getColor(ThemeSettings.TEXT));

        float mouseX = (float) mc.mouseHelper.getMouseX() * mc.getMainWindow().getScaledWidth() / mc.getMainWindow().getWidth();
        float mouseY = (float) mc.mouseHelper.getMouseY() * mc.getMainWindow().getScaledHeight() / mc.getMainWindow().getHeight();

        boolean isVisibleHovered = MathUtil.isHovered(mouseX, mouseY, x + 23.5f, y + 4, 9, 8);
        hideTooltipAnimation.setSpeed(10f);
        hideTooltipAnimation.setEasing(Easings.LINEAR);
        float prevResetAlpha = resetTooltipAnimation.getValue();
        if (isVisibleHovered && prevResetAlpha <= 0f) {
            hideTooltipAnimation.update(1f);
        } else {
            hideTooltipAnimation.update(0f);
        }
        float hideAlpha = hideTooltipAnimation.getValue();

        boolean isVisible = element instanceof ModuleElement ? ((ModuleElement) element).getModule().isKeybindvisible() : ((BooleanElement) element).getSetting().isKeybindvisible();
        strikeAnimation.setSpeed(6f);
        strikeAnimation.setEasing(Easings.LINEAR);
        strikeAnimation.update(isVisible ? 0f : 1f);
        float prog = strikeAnimation.getValue();

        int iconColor = ColorUtil.interpolate(ThemeEditor.getColor(ThemeSettings.INDICATOR_INACTIVE), ThemeEditor.getColor(ThemeSettings.INDICATOR), prog);

        boolean isBinHovered = MathUtil.isHovered(mouseX, mouseY, x + 34, y + 5, 8, 8);
        resetTooltipAnimation.setSpeed(10f);
        resetTooltipAnimation.setEasing(Easings.LINEAR);
        if (isBinHovered && hideAlpha <= 0f) {
            resetTooltipAnimation.update(1f);
        } else {
            resetTooltipAnimation.update(0f);
        }
        float resetAlpha = resetTooltipAnimation.getValue();
        if (resetAlpha > 0f) {
            float widthReset = Fonts.sf_regular[12].getWidth("Сбросить") + 4;
            RenderUtil.drawBlurredRoundedRectangle(x + 33, y - 4.5f, widthReset, 9, 1, ThemeEditor.getColor(ThemeSettings.TOOLTIP_BG), resetAlpha);
            int textColor = ThemeEditor.getColor(ThemeSettings.TEXT);
            int textColorWithAlpha = ColorUtil.applyOpacity(textColor, (int) (resetAlpha * 255));
            Fonts.sf_regular[12].drawString(matrixStack, "Сбросить", x + 35, y - 1, textColorWithAlpha);
        }
        RenderUtil.drawImage2D(new ResourceLocation("SkyCore/icons/gui/bin.png"), x + 30, y + 5, 8, 8, ThemeEditor.getColor(ThemeSettings.INDICATOR));

        int currentBind = element instanceof ModuleElement ? ((ModuleElement) element).getModule().getBind() : ((BooleanElement) element).getSetting().getBind();
        RenderUtil.drawRoundedRectangle(x + 43, y + 4, 23, 10, 3, ThemeEditor.getColor(ThemeSettings.BUTTON));
        float textX = x + 42.5F + (23 - Fonts.sf_regular[12].getWidth(waitingForKey ? "..." : (currentBind == -100 ? "Нету" : KeyMapper.getKey(currentBind)))) / 2f;
        float textY = y + 4 + (10 - Fonts.sf_regular[12].getHeight()) / 2f + 1;
        Fonts.sfregular[12].drawString(matrixStack, waitingForKey ? "..." : (currentBind == -100 ? "Нету" : KeyMapper.getKey(currentBind)), textX + 0.5f, textY, ThemeEditor.getColor(ThemeSettings.TEXT));

        int activeBg = ThemeEditor.getColor(ThemeSettings.BUTTON_INACTIVE);
        int inactiveBg = ThemeEditor.getColor(ThemeSettings.BUTTON);
        int toggleBg = ColorUtil.interpolate(activeBg, inactiveBg, tmValue);
        float toggleTextX = x + 3.5f + (30 - Fonts.sf_regular[12].getWidth("Toggle")) / 2f;
        float toggleTextY = y + 16 + (10 - Fonts.sf_regular[12].getHeight()) / 2f + 1;
        int activeText = ThemeEditor.getColor(ThemeSettings.TEXT_INACTIVE);
        int inactiveText = ThemeEditor.getColor(ThemeSettings.TEXT);
        int toggleTextColor = ColorUtil.interpolate(activeText, inactiveText, tmValue);

        int holdBg = ColorUtil.interpolate(inactiveBg, activeBg, tmValue);
        float holdTextX = x + 35.5f + (30 - Fonts.sf_regular[12].getWidth("Hold")) / 2f;
        float holdTextY = y + 16 + (10 - Fonts.sf_regular[12].getHeight()) / 2f + 1;
        int holdTextColor = ColorUtil.interpolate(inactiveText, activeText, tmValue);

        RenderUtil.scaleEnd();

        if (waitingForKey) {
            RenderUtil.drawImage2D(new ResourceLocation("SkyCore/icons/mainmenu/black_background.png"), 0, 0, mc.getMainWindow().getScaledWidth(), mc.getMainWindow().getScaledHeight(), ColorUtil.applyOpacity(-1, 160));
            Fonts.sf_regular[30].drawString(matrixStack, "Нажмите на любую кнопку", (mc.getMainWindow().getScaledWidth() - Fonts.sf_regular[32].getWidth("Нажмите на любую кнопку")) / 2f, (mc.getMainWindow().getScaledHeight() - Fonts.sf_regular[32].getHeight()) / 2f - 4, -1);
        }
    }

    public boolean mouseClicked(float mouseX, float mouseY, int button) {
        boolean isBinding = element instanceof ModuleElement ? ((ModuleElement) element).isBind() : ((BooleanElement) element).isBind();
        if (!isBinding || animation.getValue() < 0.1) {
            return false;
        }

        if (waitingForKey) {
            if (MathUtil.isHovered(mouseX, mouseY, x + 34, y + 5, 8, 8) && button == 0) {
                if (element instanceof ModuleElement) {
                    ((ModuleElement) element).getModule().setBind(-100);
                } else {
                    ((BooleanElement) element).getSetting().setBind(-100);
                }
                waitingForKey = false;
                return true;
            }
            if (MathUtil.isHovered(mouseX, mouseY, x + 43, y + 4, 23, 10) && button == 0) {
                waitingForKey = false;
                return true;
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_1 || button == GLFW.GLFW_MOUSE_BUTTON_2) {
                return true;
            }
            if (button >= 1) {
                if (element instanceof ModuleElement) {
                    ((ModuleElement) element).getModule().setBind(button);
                } else {
                    ((BooleanElement) element).getSetting().setBind(button);
                }
                waitingForKey = false;
                return true;
            }
            return true;
        }

        if (MathUtil.isHovered(mouseX, mouseY, x, y, width, height)) {
            if (MathUtil.isHovered(mouseX, mouseY, x + 43, y + 4, 23, 10) && button == 0) {
                waitingForKey = true;
                return true;
            }

            if (MathUtil.isHovered(mouseX, mouseY, x + 34, y + 5, 8, 8) && button == 0) {
                if (element instanceof ModuleElement) {
                    ((ModuleElement) element).getModule().setBind(-100);
                } else {
                    ((BooleanElement) element).getSetting().setBind(-100);
                }
                return true;
            }

            return true;
        }

        shouldClose = true;
        return true;
    }

    public void keyPressed(int keyCode) {
        if (waitingForKey) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) {
                if (element instanceof ModuleElement) {
                    ((ModuleElement) element).getModule().setBind(-100);
                } else if (element instanceof BooleanElement) {
                    ((BooleanElement) element).getSetting().setBind(-100);
                }
            } else if (keyCode != GLFW.GLFW_KEY_UNKNOWN && keyCode != GLFW.GLFW_MOUSE_BUTTON_1 && keyCode != GLFW.GLFW_MOUSE_BUTTON_2) {
                if (element instanceof ModuleElement) {
                    ((ModuleElement) element).getModule().setBind(keyCode);
                } else if (element instanceof BooleanElement) {
                    ((BooleanElement) element).getSetting().setBind(keyCode);
                }
            }
            waitingForKey = false;
        } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            shouldClose = true;
        }
    }

    public void updatePosition(float x, float y) {
        float yOffset = (element instanceof BooleanElement) ? -4 : 0;
        this.x = x;
        this.y = y + yOffset;
    }

    public boolean isClosed() {
        return animation.getValue() <= 0.01f;
    }
}