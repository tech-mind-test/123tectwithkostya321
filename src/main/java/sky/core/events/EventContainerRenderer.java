package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import com.mojang.blaze3d.matrix.MatrixStack;

public class EventContainerRenderer implements Event {

    private final MatrixStack matrixStack;
    private final int mouseX;
    private final int mouseY;

    public EventContainerRenderer(MatrixStack matrixStack, int mouseX, int mouseY) {
        this.matrixStack = matrixStack;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    public MatrixStack getMatrixStack() {
        return matrixStack;
    }

    public int getMouseX() {
        return mouseX;
    }

    public int getMouseY() {
        return mouseY;
    }

}