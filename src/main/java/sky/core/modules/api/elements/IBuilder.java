package sky.core.modules.api.elements;

import com.mojang.blaze3d.matrix.MatrixStack;

public interface IBuilder {
    default void render(MatrixStack stack, float mouseX, float mouseY, float alpha) {
    }
    default void mouseClicked(float mouseX, float mouseY, int button) {
    }
    default void charTyped(char codePoint, int modifiers) {
    }
    default void mouseReleased(float mouseX, float mouseY, int button) {
    }
    default void keyPressed(int keyCode, int scanCode, int modifiers) {
    }
    default boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return false;
    }
    default boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false;
    }
}
