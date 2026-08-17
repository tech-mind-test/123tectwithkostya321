package sky.core.ui.screens;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.StringTextComponent;

import java.util.Random;

public class AddAccount extends Screen {
    private TextFieldWidget fieldWidget;
    private Button loginButton;
    private Button randomButton;
    private int textWidth = 200;
    private int buttonHeight = 20;
    private int randomButtonWidth = 50;
    private int loginButtonWidth = 150;
    String currentUsername = Minecraft.getInstance().getSession().getUsername();
    AccountManager accountManagement = new AccountManager();

    public AddAccount() {
        super(new StringTextComponent("Add New Account"));
    }

    @Override
    public void init(Minecraft minecraft, int width, int height) {
        super.init(minecraft, width, height);

        int textFieldX = this.width / 2 - textWidth / 2;
        int textFieldY = this.height / 2 - 60;
        int buttonY = textFieldY + 25;

        fieldWidget = new TextFieldWidget(font, textFieldX, textFieldY, textWidth, 20, new StringTextComponent("Account")) {
            @Override
            public boolean charTyped(char codePoint, int modifiers) {
                if (Character.toString(codePoint).matches("[a-zA-Z0-9_]") || codePoint == '\b') {
                    return super.charTyped(codePoint, modifiers);
                }
                return false;
            }
        };
        fieldWidget.setMaxStringLength(16);
        addButton(fieldWidget);

        loginButton = new Button(textFieldX, buttonY, loginButtonWidth, buttonHeight, new StringTextComponent("Login"), button -> {
            String username = fieldWidget.getText().trim();
            if (!username.isEmpty()) {
                accountManagement.addAccount(username);
                Minecraft.getInstance().displayGuiScreen(new AccountManager());
            }
        });
        addButton(loginButton);

        randomButton = new Button(textFieldX + loginButtonWidth, buttonY, randomButtonWidth, buttonHeight, new StringTextComponent("Random"), button -> {
            Random random = new Random();
            String randomNick = "SkyCore" + (100 + random.nextInt(899));
            fieldWidget.setText(randomNick);
        });
        addButton(randomButton);

        Button cancelButton = new Button(textFieldX, buttonY + 25, textWidth, buttonHeight, new StringTextComponent("Отмена"), button -> {
            Minecraft.getInstance().displayGuiScreen(new AccountManager());
        });
        addButton(cancelButton);
    }

    @Override
    public void tick() {
        super.tick();
        fieldWidget.tick();
        loginButton.active = fieldWidget.getText().trim().length() >= 3;
    }
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (fieldWidget.isFocused() && Screen.hasControlDown() && keyCode == 86) {
            String clipboard = Minecraft.getInstance().keyboardListener.getClipboardString();
            if (clipboard != null) {
                clipboard = clipboard.replaceAll("[^a-zA-Z0-9_]", "");
                fieldWidget.writeText(clipboard);
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderDirtBackground(0);
        super.render(matrixStack, mouseX, mouseY, partialTicks);

        fieldWidget.render(matrixStack, mouseX, mouseY, partialTicks);
        loginButton.render(matrixStack, mouseX, mouseY, partialTicks);
        randomButton.render(matrixStack, mouseX, mouseY, partialTicks);

        if (fieldWidget.getText().isEmpty()) {
            drawString(matrixStack, font, "Enter name...", width / 2 - textWidth / 2, height / 2 - 75, 0xCCCCCC);
        }

        if (!fieldWidget.getText().isEmpty()) {
            drawString(matrixStack, font, "Enter name", width / 2 - textWidth / 2, height / 2 - 75, 0xCCCCCC);
        }

        if (mouseOverButton(loginButton, mouseX, mouseY)) {
            if (!loginButton.active) {
                renderTooltip(matrixStack, new StringTextComponent("Enter at least 3 characters"), mouseX, mouseY);
            } else {
                renderTooltip(matrixStack, new StringTextComponent("Click to login"), mouseX, mouseY);
            }
        }

        if (mouseOverButton(randomButton, mouseX, mouseY)) {
            renderTooltip(matrixStack, new StringTextComponent("Click to generate a random username"), mouseX, mouseY);
        }

        drawCenteredString(matrixStack, font, "Login account info", this.width / 2, 10, 0xFFFFFF);

    }

    private boolean mouseOverButton(Button button, int mouseX, int mouseY) {

        int buttonX1 = button.x;
        int buttonY1 = button.y;
        int buttonX2 = button.x + button.getWidth();
        int buttonY2 = button.y + button.getHeightRealms();

        return mouseX >= buttonX1 && mouseY >= buttonY1 && mouseX < buttonX2 && mouseY < buttonY2;
    }
}