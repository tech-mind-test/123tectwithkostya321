package sky.core.modules.api.elements.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import sky.core.modules.api.constructors.impl.StringSetting;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.modules.api.elements.Element;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.animation.AnimationUtil;
import sky.core.utils.animation.Easings;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;
import lombok.Getter;
import org.lwjgl.glfw.GLFW;

@Getter
public class StringElement extends Element {
    private final StringSetting setting;
    private String inputText;
    public boolean isFocused = false;
    private boolean isTextSelected = false;
    private long cursorAnimationStart = 0;
    private final AnimationUtil caretFadeAnimation = new AnimationUtil(0f, 12f, Easings.SINE_OUT);

    public StringElement(StringSetting setting) {
        this.setting = setting;
        this.inputText = setting.get();
    }

    public void setInputText(String text) {
        this.inputText = text == null ? "" : text;
        this.setting.setValue(this.inputText);
    }

    @Override
    public void render(MatrixStack matrixStack, float mouseX, float mouseY, float alpha) {
        super.render(matrixStack, mouseX, mouseY, alpha);
        setHeight(16);

        String displayText = inputText.isEmpty() ? setting.getName() : inputText;
        boolean isEmpty = inputText.isEmpty();

        float inputWidth = getWidth() - 36;
        RenderUtil.drawRoundedRectangle(getX() + 5, getY(), inputWidth, 11, 3F, ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.BUTTON_INACTIVE), (ThemeEditor.getAlpha(ThemeSettings.BUTTON_INACTIVE) / 255F) * alpha));

        if (isTextSelected && !isEmpty) {
            int textWidth = (int) Math.min(Fonts.sf_medium[13].getWidth(displayText), 71);
            RenderUtil.drawMinecraftRectangle(matrixStack, getX() + 7, getY() + 2, textWidth + 1, 8, ColorUtil.applyOpacity(ColorUtil.getColor(26, 53, 255), 255 * alpha));
        }

        int cursorX;
        if (isEmpty) {
            cursorX = (int) (getX() + 7);
        } else {
            int textWidth = (int) Math.min(Fonts.sf_medium[13].getWidth(displayText), 70);
            cursorX = (int) (getX() + 8 + textWidth);

            int rightBound = (int) (getX() + 5 + inputWidth - 2);
            if (cursorX > rightBound) {
                cursorX = rightBound;
            }
        }

        long now = System.currentTimeMillis();
        if (cursorAnimationStart == 0) cursorAnimationStart = now;
        float blinkAlpha = (float) Math.sin((now - cursorAnimationStart) % 1000 / 500f * Math.PI) * 0.5f + 0.5f;
        caretFadeAnimation.update(isFocused ? 1f : 0f);
        if (!isFocused) {
            isTextSelected = false;
        }
        float caretFade = caretFadeAnimation.getValue();
        if (caretFade > 0.01f) {
            float finalAlpha = blinkAlpha * caretFade;
            RenderUtil.drawMinecraftRectangle(matrixStack, cursorX, getY() + 2, 0.5f, 8, ColorUtil.applyOpacity(-1, (int) (finalAlpha * 255)));
        }

        while (Fonts.sf_regular[13].getWidth(displayText) > 71 && !displayText.isEmpty()) {
            displayText = displayText.substring(1);
        }

        Fonts.sf_regular[13].drawString(matrixStack, displayText, getX() + 8, getY() + 4.5f, inputText.isEmpty() ? ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.TEXT_INACTIVE), (ThemeEditor.getAlpha(ThemeSettings.TEXT_INACTIVE) / 255F) * alpha) : ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.TEXT), (ThemeEditor.getAlpha(ThemeSettings.TEXT) / 255F) * alpha));

        setting.set(inputText);
    }

    @Override
    public void mouseClicked(float mouseX, float mouseY, int button) {
        boolean isHovered = isHovered(mouseX, mouseY);

        if (isHovered) {
            isFocused = true;
            isTextSelected = false;
            cursorAnimationStart = System.currentTimeMillis();
        } else if (isFocused) {
            isFocused = false;
            isTextSelected = false;
            setting.setValue(inputText);
        }

        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseReleased(float mouseX, float mouseY, int button) {
        if (isFocused && !isHovered(mouseX, mouseY)) {
            isFocused = false;
            isTextSelected = false;
            setting.setValue(inputText);
        }

        super.mouseReleased(mouseX, mouseY, button);
    }

    public boolean isHovered(float mouseX, float mouseY) {
        return mouseX >= getX() + 5 && mouseX <= getX() + getWidth() - 5 && mouseY >= getY() && mouseY <= getY() + 11;
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isFocused) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) {
                isFocused = false;
                isTextSelected = false;
                setting.setValue(inputText);
                return;
            }

            if (keyCode == GLFW.GLFW_KEY_A && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                isTextSelected = true;
                return;
            }

            if (keyCode == GLFW.GLFW_KEY_C && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                if (isTextSelected && !inputText.isEmpty()) {
                    GLFW.glfwSetClipboardString(GLFW.glfwGetCurrentContext(), inputText);
                }
                return;
            }

            if (keyCode == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                String clipboardText = GLFW.glfwGetClipboardString(GLFW.glfwGetCurrentContext());
                if (clipboardText != null && !clipboardText.isEmpty()) {
                    if (isTextSelected) {
                        inputText = "";
                        isTextSelected = false;
                    }
                    int maxLen = setting.getName().equals("Название") ? 16 : 100;
                    int remainingChars = maxLen - inputText.length();
                    if (remainingChars > 0) {
                        inputText += clipboardText.substring(0, Math.min(remainingChars, clipboardText.length()));
                    }
                }
                return;
            }

            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (isTextSelected) {
                    inputText = "";
                    isTextSelected = false;
                } else if (!inputText.isEmpty()) {
                    inputText = inputText.substring(0, inputText.length() - 1);
                }
                return;
            }
        }
        super.keyPressed(keyCode, scanCode, modifiers);
    }

    public void clearText() {
        this.inputText = "";
        this.setting.setValue("");
        this.isFocused = false;
        this.isTextSelected = false;
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        if (isFocused) {
            if (isTextSelected) {
                inputText = String.valueOf(codePoint);
                isTextSelected = false;
            } else {
                int maxLen = setting.getName().equals("Название") ? 16 : 100;
                if (inputText.length() < maxLen) {
                    inputText += codePoint;
                }
            }
        }
        super.charTyped(codePoint, modifiers);
    }
}
