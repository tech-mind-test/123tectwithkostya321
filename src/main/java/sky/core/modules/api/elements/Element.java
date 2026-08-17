package sky.core.modules.api.elements;

import sky.core.ui.gui.Panel;
import sky.core.utils.animation.AnimationUtil;
import sky.core.utils.render.font.StyledFont;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Element implements IBuilder {
    private float x, y, width, height;
    private Panel panel;
    private final AnimationUtil visibilityAnimation = new AnimationUtil(0.0f, 15.0f);
    private final StyledFont.TextScrollState scrollState = new StyledFont.TextScrollState();

    public boolean isHovered(float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean isHovered(float mouseX, float mouseY, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean isVisible() {
        return true;
    }

    public float getVisibilityAnimation() {
        visibilityAnimation.update(isVisible() ? 1.0f : 0.0f);
        return visibilityAnimation.getValue();
    }
}