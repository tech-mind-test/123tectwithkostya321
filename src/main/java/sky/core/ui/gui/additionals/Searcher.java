package sky.core.ui.gui.additionals;

import com.mojang.blaze3d.matrix.MatrixStack;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;
import lombok.Getter;
import lombok.Setter;
import org.lwjgl.glfw.GLFW;


@Getter
@Setter
public class Searcher {

    private float x, y, width, height;
    private static final float VISUAL_Y_OFFSET = -16.0f;
    private String text = "";
    private boolean focused = false;
    private boolean activated = false;
    private boolean isTextSelected = false;
    private long cursorAnimationStart = 0;

    public Searcher(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void render(MatrixStack stack, float alphaRender) {
        float fieldX = x;
        float fieldY = y + VISUAL_Y_OFFSET;
        float fieldW = width;
        float fieldH = height;
        int logoColor = ThemeEditor.getColor(ThemeSettings.LOGO);
        int bgColor = ColorUtil.darken(logoColor, 0.1f);
        int bgWithAlpha = ColorUtil.applyOpacity(bgColor, ThemeEditor.getAlpha(ThemeSettings.LOGO) / 255f * alphaRender);


        RenderUtil.drawRoundedRectangle(fieldX, fieldY, fieldW, fieldH - 3, 6, bgWithAlpha);

        String displayText = text.isEmpty() && !focused ? "Искать.." : text;
        float textX = fieldX + 5;
        float maxWidth = width - 10;

        if (!text.isEmpty() && Fonts.sf_medium[15].getWidth(displayText) > maxWidth) {
            while (Fonts.sf_medium[15].getWidth(displayText) > maxWidth && displayText.length() > 1) {
                displayText = displayText.substring(1);
            }
        }

        float cursorX;
        if (text.isEmpty() && !focused) {
            cursorX = textX + Fonts.sf_medium[15].getWidth("Поиск");
        } else {
            cursorX = textX + Fonts.sf_medium[15].getWidth(displayText);
        }

        float rightBound = fieldX + fieldW - 5;
        if (cursorX > rightBound) {
            cursorX = rightBound;
        }

        if (activated) {
            long now = System.currentTimeMillis();
            if (cursorAnimationStart == 0) cursorAnimationStart = now;

            float alpha = (float) Math.sin((now - cursorAnimationStart) % 1000 / 500f * Math.PI) * 0.5f + 0.5f;
            float cursorWidth = 4;
            float cursorY = fieldY + fieldH - 8;
            RenderUtil.drawMinecraftRectangle(stack, cursorX + 2, cursorY - 2, cursorWidth, 0.5f, ColorUtil.applyOpacity(-1, (int) (alpha * 255 * alphaRender)));
        } else {
            cursorAnimationStart = 0;
        }
        float textY = fieldY + (fieldH / 2f - 4f) - 2.2f;
        Fonts.sfregular[14].drawString(stack, displayText, textX + 1f, textY + 3f, text.isEmpty() && !focused ? ColorUtil.getColorWithAlpha(ThemeEditor.getColor(ThemeSettings.TEXT_INACTIVE), ThemeEditor.getAlpha(ThemeSettings.TEXT_INACTIVE) * alphaRender) : ColorUtil.getColorWithAlpha(ThemeEditor.getColor(ThemeSettings.TEXT), ThemeEditor.getAlpha(ThemeSettings.TEXT) * alphaRender));

    }


    public boolean mouseClicked(float mouseX, float mouseY) {
        float fieldX = x;
        float fieldY = y + VISUAL_Y_OFFSET;
        float fieldW = width;
        float fieldH = height;
        boolean wasFocused = focused;
        focused = mouseX >= fieldX && mouseX <= fieldX + fieldW - 1 && mouseY >= fieldY && mouseY <= fieldY + fieldH - 1;

        if (focused && !wasFocused) {
            activated = true;
            cursorAnimationStart = System.currentTimeMillis();
            isTextSelected = false;
        } else if (!focused) {
            activated = false;
            isTextSelected = false;
        }

        return focused;
    }


    public void charTyped(char codePoint) {
        if (focused && activated) {
            if (isTextSelected) {
                text = String.valueOf(codePoint);
                isTextSelected = false;
            } else if (codePoint >= 32 && codePoint <= 126 && text.length() < 30) {
                text += codePoint;
            }
        }
    }


    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (focused && activated) {
            if (keyCode == GLFW.GLFW_KEY_A && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                if (!text.isEmpty()) {
                    isTextSelected = true;
                }
                return;
            }

            if (keyCode == GLFW.GLFW_KEY_C && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                if (isTextSelected && !text.isEmpty()) {
                    GLFW.glfwSetClipboardString(GLFW.glfwGetCurrentContext(), text);
                }
                return;
            }

            if (keyCode == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                String clipboardText = GLFW.glfwGetClipboardString(GLFW.glfwGetCurrentContext());
                if (clipboardText != null && !clipboardText.isEmpty()) {
                    if (isTextSelected) {
                        text = "";
                        isTextSelected = false;
                    }
                    int remainingChars = 30 - text.length();
                    if (remainingChars > 0) {
                        text += clipboardText.substring(0, Math.min(remainingChars, clipboardText.length()));
                    }
                }
                return;
            }

            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !text.isEmpty()) {
                if (isTextSelected) {
                    text = "";
                    isTextSelected = false;
                } else {
                    text = text.substring(0, text.length() - 1);
                }
                return;
            }

            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) {
                activated = false;
                focused = false;
                isTextSelected = false;
            }
        }
    }
}