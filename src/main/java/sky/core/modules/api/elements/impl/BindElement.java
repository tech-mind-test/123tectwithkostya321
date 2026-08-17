package sky.core.modules.api.elements.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import sky.core.modules.api.constructors.impl.BindSetting;
import sky.core.modules.api.elements.Element;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.math.MathUtil;
import sky.core.utils.misc.KeyMapper;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;
import lombok.Setter;
import org.lwjgl.glfw.GLFW;

@Setter
public class BindElement extends Element {
    final BindSetting setting;
    public boolean activated;

    public BindElement(BindSetting setting) {
        this.setting = setting;
    }

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY, float alpha) {
        super.render(stack, mouseX, mouseY, alpha);

        String bind = activated ? "..." : (setting.get() != -1 ? KeyMapper.getKey(setting.get()) : "None");
        float boxWidth = Fonts.sf_regular[13].getWidth(bind) + 5;
        float boxHeight = 11;
        float textWidth = Math.max(20, getWidth() - boxWidth - 11);

        Fonts.sf_regular[13].drawScrolledString(stack, setting.getName(), getX() + 6, getY() + 3, textWidth, ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.TEXT), (ThemeEditor.getAlpha(ThemeSettings.TEXT) / 255F) * alpha), MathUtil.isHovered(mouseX, mouseY, getX() + 6, getY(), textWidth, Fonts.sf_regular[14].getHeight() + 2), getScrollState());
        int currentBg = ColorUtil.interpolate(ThemeEditor.getColor(ThemeSettings.BUTTON), ThemeEditor.getColor(ThemeSettings.BUTTON_INACTIVE), alpha);

        float x = getX() + getWidth() - boxWidth - 5;
        float y = getY() + getHeight() - boxHeight;
        float bgAlpha = (ThemeEditor.getAlpha(ThemeSettings.BUTTON_INACTIVE)) / 255F * alpha;

        int finalBg = ColorUtil.applyOpacity(currentBg, bgAlpha);
        RenderUtil.drawRoundedRectangle(x, y - 4.5F, boxWidth, boxHeight, 2, finalBg);

        float rectCenterX = x + boxWidth / 2;
        float rectCenterY = y - 4 + boxHeight / 2;

        float textX = rectCenterX - Fonts.sf_medium[13].getWidth(bind) / 2;
        int textY = (int) (rectCenterY - Fonts.sf_medium[13].getHeight() / 2 + 1);

        Fonts.sf_medium[13].drawString(stack, bind, textX, textY, ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.TEXT), (ThemeEditor.getAlpha(ThemeSettings.TEXT) / 255F) * alpha));

        setHeight(15);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (activated) {
            if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_ESCAPE) {
                setting.set(-1);
                activated = false;
                return;
            }
            setting.set(keyCode);
            activated = false;
        }
        super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void mouseClicked(float mouseX, float mouseY, int button) {
        float boxWidth = Fonts.sf_regular[13].getWidth(setting.get() != -1 ? KeyMapper.getKey(setting.get()) : "None") + 5;
        float boxHeight = 11;

        float boxX = getX() + getWidth() - boxWidth - 5;
        float boxY = getY() + getHeight() - boxHeight - 6;

        if (activated) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_1 || button == GLFW.GLFW_MOUSE_BUTTON_2) {
                return;
            }
            if (button >= 1) {
                setting.set(button);
                activated = false;
                return;
            }
        }

        if (MathUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight())) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
                if (mouseX >= boxX && mouseX <= boxX + boxWidth && mouseY >= boxY && mouseY <= boxY + boxHeight) {
                    activated = true;
                }
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_3) {
                setting.set(-1);
                activated = false;
            }
        }

        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isVisible() {
        return setting.isVisible();
    }
}