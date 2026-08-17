package mods.proxy;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.MultiplayerScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.CheckboxButton;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.lang3.StringUtils;

public class GuiProxy extends Screen {
    private boolean isSocks4 = false;

    private TextFieldWidget ipPort;
    private TextFieldWidget username;
    private TextFieldWidget password;
    private CheckboxButton enabledCheck;

    private final Screen parentScreen;

    private String msg = "";

    private int[] positionY;
    private int positionX;

    private TestPing testPing = new TestPing();

    public GuiProxy(Screen parentScreen) {
        super(new StringTextComponent("Proxy"));
        this.parentScreen = parentScreen;
    }

    private boolean checkProxy() {
        if (!isValidIpPort(ipPort.getText())) {
            msg = TextFormatting.RED + "Invalid IP:PORT";
            this.ipPort.setFocused2(true);
            return false;
        }
        return true;
    }

    private static boolean isValidIpPort(String ipP) {
        String[] split = ipP.split(":");
        if (split.length > 1) {
            if (!StringUtils.isNumeric(split[1])) return false;
            int port = Integer.parseInt(split[1]);
            return port >= 0 && port <= 0xFFFF;
        } else {
            return false;
        }
    }

    private void centerButtons(int amount, int buttonLength, int gap) {
        positionX = (this.width / 2) - (buttonLength / 2);
        positionY = new int[amount];
        int center = (this.height + amount * gap) / 2;
        int buttonStarts = center - (amount * gap);
        for (int i = 0; i != amount; i++) {
            positionY[i] = buttonStarts + (gap * i);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        super.keyPressed(keyCode, scanCode, modifiers);
        msg = "";
        testPing.state = "";
        return true;
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);

        if (enabledCheck.isChecked() && !isValidIpPort(ipPort.getText())) {
            enabledCheck.onPress();
        }

        drawString(matrixStack, this.font, "Proxy Type:", this.width / 2 - 149, positionY[1] + 5, 10526880);
        drawString(matrixStack, this.font, "Proxy Auth", this.width / 2, positionY[3] + 8, TextFormatting.WHITE.getColor());
        drawString(matrixStack, this.font, "IP:PORT: ", this.width / 2 - 125, positionY[2] + 5, 10526880);

        this.ipPort.render(matrixStack, mouseX, mouseY, partialTicks);
        if (isSocks4) {
            drawString(matrixStack, this.font, "User ID: ", this.width / 2 - 140, positionY[4] + 5, 10526880);
            this.username.render(matrixStack, mouseX, mouseY, partialTicks);
        } else {
            drawString(matrixStack, this.font, "Username: ", this.width / 2 - 140, positionY[4] + 5, 10526880);
            drawString(matrixStack, this.font, "Password: ", this.width / 2 - 140, positionY[5] + 5, 10526880);
            this.username.render(matrixStack, mouseX, mouseY, partialTicks);
            this.password.render(matrixStack, mouseX, mouseY, partialTicks);
        }

        drawString(matrixStack, this.font, !msg.isEmpty() ? msg : testPing.state, this.width / 2, positionY[6] + 5, 10526880);

        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public void tick() {
        testPing.pingPendingNetworks();

        this.ipPort.tick();
        this.username.tick();
        this.password.tick();
    }

    @Override
    public void init() {
        Minecraft.getInstance().keyboardListener.enableRepeatEvents(true);
        int buttonLength = 160;
        centerButtons(10, buttonLength, 26);

        isSocks4 = ProxyServer.proxy.type == Proxy.ProxyType.SOCKS4;

        Button proxyType = new Button(positionX, positionY[1], buttonLength, 20, new StringTextComponent(isSocks4 ? "Socks 4" : "Socks 5"), (button) -> {
            isSocks4 = !isSocks4;
            button.setMessage(new StringTextComponent(isSocks4 ? "Socks 4" : "Socks 5"));
        });
        this.addButton(proxyType);

        this.ipPort = new TextFieldWidget(this.font, positionX, positionY[2], buttonLength, 20, new StringTextComponent(""));
        this.ipPort.setText(ProxyServer.proxy.ipPort);
        this.ipPort.setMaxStringLength(1024);
        this.ipPort.setFocused2(true);
        this.children.add(this.ipPort);

        this.username = new TextFieldWidget(this.font, positionX, positionY[4], buttonLength, 20, new StringTextComponent(""));
        this.username.setMaxStringLength(255);
        this.username.setText(ProxyServer.proxy.username);
        this.children.add(this.username);

        this.password = new TextFieldWidget(this.font, positionX, positionY[5], buttonLength, 20, new StringTextComponent(""));
        this.password.setMaxStringLength(255);
        this.password.setText(ProxyServer.proxy.password);
        this.children.add(this.password);

        int posXButtons = (this.width / 2) - (((buttonLength / 2) * 3) / 2);

        Button apply = new Button(posXButtons, positionY[8], buttonLength / 2 - 3, 20, new StringTextComponent("Confirm"), (button) -> {
            if (checkProxy()) {
                ProxyServer.proxy = new Proxy(isSocks4, ipPort.getText(), username.getText(), password.getText());
                ProxyServer.proxyEnabled = enabledCheck.isChecked();
                Config.setDefaultProxy(ProxyServer.proxy);
                Config.saveConfig();
                Minecraft.getInstance().displayGuiScreen(new MultiplayerScreen(new MainMenuScreen()));
            }
        });
        this.addButton(apply);

        Button test = new Button(posXButtons + buttonLength / 2 + 3, positionY[8], buttonLength / 2 - 3, 20, new StringTextComponent("Ping"), (button) -> {
            if (ipPort.getText().isEmpty() || ipPort.getText().equalsIgnoreCase("none")) {
                msg = TextFormatting.RED + "Specify proxy to test";
                return;
            }
            if (checkProxy()) {
                testPing = new TestPing();
                testPing.run("mc.bravohvh.space", 25565, new Proxy(isSocks4, ipPort.getText(), username.getText(), password.getText()));
            }
        });
        this.addButton(test);

        this.enabledCheck = new CheckboxButton((this.width / 2) - (15 + font.getStringWidth("Proxy Enabled")) / 2, positionY[7], buttonLength, 20, new StringTextComponent("Proxy Enabled"), ProxyServer.proxyEnabled);
        this.addButton(this.enabledCheck);

        Button cancel = new Button(posXButtons + (buttonLength / 2 + 3) * 2, positionY[8], buttonLength / 2 - 3, 20, new StringTextComponent("Cancel"), (button) -> {
            Minecraft.getInstance().displayGuiScreen(parentScreen);
        });
        this.addButton(cancel);
    }

    @Override
    public void onClose() {
        msg = "";
        Minecraft.getInstance().keyboardListener.enableRepeatEvents(false);
    }
}