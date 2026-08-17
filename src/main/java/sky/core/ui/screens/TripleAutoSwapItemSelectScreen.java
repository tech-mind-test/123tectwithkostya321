package sky.core.ui.screens;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;
import sky.core.utils.Wrapper;

public class TripleAutoSwapItemSelectScreen extends Screen implements Wrapper {

    private final Screen back;

    public TripleAutoSwapItemSelectScreen(Screen back) {
        super(new StringTextComponent("Select item"));
        this.back = back;
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            mc.displayGuiScreen(back);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
