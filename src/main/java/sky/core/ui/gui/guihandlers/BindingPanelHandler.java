package sky.core.ui.gui.guihandlers;

import com.mojang.blaze3d.matrix.MatrixStack;
import sky.core.modules.api.elements.Element;
import sky.core.modules.api.elements.ModuleElement;
import sky.core.modules.api.elements.impl.BooleanElement;
import sky.core.ui.gui.additionals.BindingPanel;
import sky.core.utils.Wrapper;

public class BindingPanelHandler implements Wrapper {
    private BindingPanel activePanel;
    private Element activeElement;

    public BindingPanelHandler() {
    }

    public void render(MatrixStack matrixStack, int mouseX, int mouseY) {
        if (activePanel != null && activeElement != null) {
            activePanel.updatePosition(activeElement.getX() + activeElement.getWidth(), activeElement.getY() + 4);
            activePanel.render(matrixStack);
            if (activePanel.shouldClose && activePanel.isClosed()) {
                activePanel = null;
                activeElement = null;
            }
        }
    }

    public boolean mouseClicked(float mouseX, float mouseY, int button) {
        if (activePanel != null) {
            return activePanel.mouseClicked(mouseX, mouseY, button);
        }
        return false;
    }

    public boolean keyPressed(int keyCode) {
        if (activePanel != null) {
            activePanel.keyPressed(keyCode);
            return true;
        }
        return false;
    }

    public void close() {
        if (activePanel != null) {
            activePanel.shouldClose = true;
        }
    }

    public boolean isActive() {
        return activePanel != null && activeElement != null && !activePanel.isClosed();
    }

    public void openForModule(ModuleElement moduleElement) {
        if (moduleElement == null) return;
        moduleElement.setBind(true);
        if (activePanel != null && activeElement == moduleElement) {
            activePanel.shouldClose = false;
            return;
        }
        activeElement = moduleElement;
        activePanel = new BindingPanel(activeElement);
    }

    public void openForBoolean(BooleanElement booleanElement) {
        if (booleanElement == null) return;
        booleanElement.setBind(true);
        if (activePanel != null && activeElement == booleanElement) {
            activePanel.shouldClose = false;
            return;
        }
        activeElement = booleanElement;
        activePanel = new BindingPanel(activeElement);
    }
}