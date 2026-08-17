package sky.core.ui.screens;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Session;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraft.util.text.StringTextComponent;
import org.lwjgl.glfw.GLFW;
import com.adl.nativeprotect.Native;
import sky.core.utils.managers.impl.AccountManager;
import sky.core.utils.animation.AnimationUtil;
import sky.core.utils.animation.Easings;
import sky.core.utils.net.MicrosoftLogin;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.ScissorUtil;
import sky.core.utils.render.font.Fonts;

import java.text.SimpleDateFormat;
import java.util.*;

import static sky.core.utils.Wrapper.mc;

public class AccountsScreen extends Screen {
    private boolean showAddDialog = false;
    private boolean showClearDialog = false;
    private float closeIconX;
    private float closeIconY;
    private final float closeIconWidth = 8;
    private final float closeIconHeight = 8;
    private String inputText = "";
    private String addDialogInputText = "";
    private boolean inputFocused = false;
    private boolean addDialogInputFocused = false;
    private boolean isTextSelected = false;
    private boolean isAddDialogTextSelected = false;
    private long cursorBlinkStart = 0;

    private final Map<String, Long> accountNames = new LinkedHashMap<>();
    private final Map<String, ResourceLocation> skinCache = new HashMap<>();
    private final Map<String, Boolean> xaveIconClicked = new HashMap<>();
    private String selectedAccount = null;
    private static final ResourceLocation DEFAULT_SKIN = new ResourceLocation("minecraft:textures/entity/steve.png");
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    private static final int MAX_INPUT_LENGTH = 16;
    private float scroll = 0;
    private float animatedScroll = 0;
    private float maxHeight = 0;
    private final AnimationUtil scrollAnimation = new AnimationUtil(0f, 7, Easings.QUINT_OUT);
    private static float savedScroll = 0f;

    private final AccountManager accountManager;

    public AccountsScreen() {

        super(new StringTextComponent("Аккаунты"));
        this.accountManager = new AccountManager();
        loadFromConfig();
        this.scroll = savedScroll;
        this.animatedScroll = savedScroll;
        this.scrollAnimation.setValue(savedScroll);
    }

    @Native
    private void scrollToBottom() {
        int accountCount = accountNames.size();
        int rows = (int) Math.ceil(accountCount / 2.0);
        float rectHeight = 64f;
        float rowPadding = 10f;
        float localMaxHeight = rows * (rectHeight + rowPadding);
        float contentHeight = 222f;
        float bottom = Math.min(0f, -localMaxHeight + contentHeight);
        this.scroll = MathHelper.clamp(bottom, -localMaxHeight + contentHeight, 0f);
        savedScroll = this.scroll;
    }

    private void loadFromConfig() {
        if (accountManager.getData() == null) {
            accountManager.init();
        }
        accountNames.clear();
        accountNames.putAll(accountManager.getAccountNames());
        xaveIconClicked.clear();
        xaveIconClicked.putAll(accountManager.getFavoriteAccounts());
        selectedAccount = accountManager.getSelectedAccount();
        updateAccountOrder();
    }

    private void saveToConfig() {
        for (Map.Entry<String, Long> entry : accountNames.entrySet()) {
            if (!accountManager.getAccountNames().containsKey(entry.getKey())) {
                accountManager.addAccount(entry.getKey());
            }
        }

        for (Map.Entry<String, Boolean> entry : xaveIconClicked.entrySet()) {
            accountManager.setFavorite(entry.getKey(), entry.getValue());
        }

        if (selectedAccount != null) {
            accountManager.selectAccount(selectedAccount);
        } else {
            accountManager.selectAccount(null);
        }
    }

    @Native
    private void updateAccountOrder() {
        LinkedHashMap<String, Long> sortedAccounts = new LinkedHashMap<>();
        accountNames.entrySet().stream().filter(entry -> xaveIconClicked.getOrDefault(entry.getKey(), false)).forEach(entry -> sortedAccounts.put(entry.getKey(), entry.getValue()));
        accountNames.entrySet().stream().filter(entry -> !xaveIconClicked.getOrDefault(entry.getKey(), false)).forEach(entry -> sortedAccounts.put(entry.getKey(), entry.getValue()));
        accountNames.clear();
        accountNames.putAll(sortedAccounts);
        saveToConfig();
    }

    @Native
    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        int baseScale = (int) mc.getMainWindow().getGuiScaleFactor();
        mc.gameRenderer.setupOverlayRendering(2);
        float factor = baseScale / 2.0f;
        int scaledMouseX = (int) (mouseX * factor);
        int scaledMouseY = (int) (mouseY * factor);

        renderBackgrounds(scaledMouseX, scaledMouseY);
        renderSearchBar(matrixStack, scaledMouseX, scaledMouseY);
        renderMainPanel(matrixStack, scaledMouseX, scaledMouseY);
        renderButtons(matrixStack, scaledMouseX, scaledMouseY);
        renderAddDialog(matrixStack, scaledMouseX, scaledMouseY);
        renderClearDialog(matrixStack, scaledMouseX, scaledMouseY);
        renderMicrosoft(matrixStack, scaledMouseX, scaledMouseY);

        super.render(matrixStack, scaledMouseX, scaledMouseY, partialTicks);
        mc.gameRenderer.setupOverlayRendering();
    }

    @Native
    private void renderBackgrounds(int mouseX, int mouseY) {
        int windowWidth = mc.getMainWindow().getScaledWidth();
        int windowHeight = mc.getMainWindow().getScaledHeight();
        int renderWidth = (int) (windowWidth * 1.05f);
        int renderHeight = (int) (windowHeight * 1.05f);

        float normMouseX = (mouseX / (float) windowWidth) * 2 - 1;
        float normMouseY = (mouseY / (float) windowHeight) * 2 - 1;

        float maxOffsetX = (renderWidth - windowWidth) / 2.0f;
        float maxOffsetY = (renderHeight - windowHeight) / 2.0f;

        float offsetX = MathHelper.clamp(normMouseX * 3, -maxOffsetX, maxOffsetX);
        float offsetY = MathHelper.clamp(normMouseY * 3, -maxOffsetY, maxOffsetY);
        RenderUtil.drawImage2D(new ResourceLocation("SkyCore/icons/mainmenu/background.png"), offsetX - (renderWidth - windowWidth) / 2.0f, offsetY - (renderHeight - windowHeight) / 2.0f, renderWidth, renderHeight, -1);

        float parallax = 2;
        float offsetXBlack = MathHelper.clamp(normMouseX * parallax, -maxOffsetX, maxOffsetX);
        float offsetYBlack = MathHelper.clamp(normMouseY * parallax, -maxOffsetY, maxOffsetY);
        RenderUtil.drawImage2D(new ResourceLocation("SkyCore/icons/mainmenu/black_background.png"), offsetXBlack - (renderWidth - windowWidth) / 2.0f, offsetYBlack - (renderHeight - windowHeight) / 2.0f, renderWidth, renderHeight, ColorUtil.applyOpacity(-1, 200));

        float offsetXBlue = MathHelper.clamp(normMouseX * parallax, -maxOffsetX, maxOffsetX);
        float offsetYBlue = MathHelper.clamp(normMouseY * parallax, -maxOffsetY, maxOffsetY);
        RenderUtil.drawImage2D(new ResourceLocation("SkyCore/icons/mainmenu/blue_background.png"), offsetXBlue - (renderWidth - windowWidth) / 2.0f, offsetYBlue - (renderHeight - windowHeight) / 2.0f, renderWidth, renderHeight, -1);
    }

    @Native
    private void renderSearchBar(MatrixStack matrixStack, int mouseX, int mouseY) {
        float centerSearcherX = (mc.getMainWindow().getScaledWidth() - 172) / 2.0f;
        float searcherY = 4;
        float searcherWidth = 172;
        float searcherHeight = 24;

        boolean searcherHovered = mouseX >= centerSearcherX && mouseX <= centerSearcherX + searcherWidth && mouseY >= searcherY && mouseY <= searcherY + searcherHeight;

        RenderUtil.drawRoundedRectangle(centerSearcherX, searcherY, searcherWidth, searcherHeight, 4, searcherHovered ? ColorUtil.getColor(33, 33, 33) : ColorUtil.getColor(22, 22, 22));

        String display = (inputText.isEmpty() && !inputFocused) ? "Search" : inputText;
        int textColor = (inputText.isEmpty() && !inputFocused) ? ColorUtil.getColor(120, 120, 120, 255) : -1;
        Fonts.sf_regular[17].drawString(matrixStack, display, centerSearcherX + 5, searcherY + (searcherHeight - Fonts.sf_regular[17].getHeight()) / 2.0f, textColor);

        if (inputFocused) {
            long time = System.currentTimeMillis();
            if (cursorBlinkStart == 0) cursorBlinkStart = time;
            boolean showCursor = (time - cursorBlinkStart) % 1000 < 500;
            if (showCursor) {
                float textWidth = Fonts.sf_regular[17].getWidth(inputText);
                RenderUtil.drawMinecraftRectangle(matrixStack, centerSearcherX + 5 + textWidth, searcherY + searcherHeight - 8, 5, 0.5f, -1);
            }
        } else {
            isTextSelected = false;
        }
    }

    @Native
    private void renderMainPanel(MatrixStack matrixStack, int mouseX, int mouseY) {
        float windowWidth = mc.getMainWindow().getScaledWidth();
        float windowHeight = mc.getMainWindow().getScaledHeight();
        float centerX = (windowWidth - 380) / 2.0f;
        float centerY = (windowHeight - 232) / 2.0f;

        RenderUtil.drawRoundedRectangleGradient(centerX, centerY + 35, 380, 232, 8, ColorUtil.getColor(9, 9, 9), ColorUtil.getColor(8, 8, 8), ColorUtil.getColor(31, 31, 31), ColorUtil.getColor(12, 12, 12), 1);
        Fonts.sf_semibold[28].drawString(matrixStack, "Аккаунты", centerX + 190 - Fonts.sf_semibold[28].getWidth("Аккаунты") / 2.0f, centerY, -1);

        String message = selectedAccount == null ? "Нажмите на любой, для входа в аккаунт" : "Нажмите на любой, для входа в аккаунт (Текущий: " + selectedAccount + ")";
        Fonts.sf_regular[18].drawString(matrixStack, message, centerX + 190 - Fonts.sf_regular[18].getWidth(message) / 2.0f, centerY + Fonts.sf_semibold[28].getHeight() + 9, ColorUtil.getColor(255, 255, 255, 120));

        scrollAnimation.setSpeed(1.2F);
        scrollAnimation.update(scroll);
        animatedScroll = scrollAnimation.getValue();

        List<Map.Entry<String, Long>> filteredAccounts = new ArrayList<>();
        for (Map.Entry<String, Long> entry : accountNames.entrySet()) {
            if (showAddDialog || inputText.isEmpty() || entry.getKey().toLowerCase().contains(inputText.toLowerCase())) {
                filteredAccounts.add(entry);
            }
        }

        int accountCount = filteredAccounts.size();
        int rows = (int) Math.ceil(accountCount / 2.0);
        float rectHeight = 64f;
        float rowPadding = 10f;
        maxHeight = rows * (rectHeight + rowPadding);

        float contentHeight = 232 - 10;
        if (maxHeight > contentHeight) {
            scroll = MathHelper.clamp(scroll, -maxHeight + contentHeight, 0);
            animatedScroll = MathHelper.clamp(animatedScroll, -maxHeight + contentHeight, 0);
        } else {
            scroll = animatedScroll = 0;
        }

        ScissorUtil.start(centerX, centerY + 35, 380, 232);

        float startX = centerX + 10;
        float startY = centerY + 45;
        int row = 0, col = 0;

        for (Map.Entry<String, Long> entry : filteredAccounts) {
            String name = entry.getKey();
            long timestamp = entry.getValue();

            float rectWidth = 170;
            float rectPadding = 20;
            float x = startX + col * (rectWidth + rectPadding);
            float y = startY + row * (64 + rectPadding - 10) + animatedScroll;
            String date = dateFormat.format(new Date(timestamp));

            RenderUtil.drawRoundedRectangle(x, y, rectWidth, 64, 10, ColorUtil.getColor(22, 22, 22));
            RenderUtil.drawRoundedRectangle(x + 124, y, 46, 18, new Vector4f(0, 7, 7, 0), ColorUtil.getColor(37, 138, 238));

            float xmarkIconX = x + 132.5f;
            float xmarkIconY = y + 5.5f;
            float xmarkIconWidth = 7;
            float xmarkIconHeight = 7;
            boolean xmarkHovered = (double) mouseX >= xmarkIconX && (double) mouseX <= xmarkIconX + xmarkIconWidth && (double) mouseY >= xmarkIconY && (double) mouseY <= xmarkIconY + xmarkIconHeight;
            int xmarkColor = xmarkHovered ? ColorUtil.getColor(134, 201, 244) : -1;
            RenderUtil.drawImage2D(new ResourceLocation("SkyCore/icons/alts/close.png"), xmarkIconX, xmarkIconY, xmarkIconWidth, xmarkIconHeight, xmarkColor);

            float copyIconX = x + 144;
            float copyIconY = y + 5;
            float copyIconWidth = 8;
            float copyIconHeight = 8;
            boolean copyHovered = (double) mouseX >= copyIconX && (double) mouseX <= copyIconX + copyIconWidth && (double) mouseY >= copyIconY && (double) mouseY <= copyIconY + copyIconHeight;
            int copyColor = copyHovered ? ColorUtil.getColor(134, 201, 244) : -1;
            RenderUtil.drawImage2D(new ResourceLocation("SkyCore/icons/alts/copy.png"), copyIconX, copyIconY, copyIconWidth, copyIconHeight, copyColor);

            float xaveIconX = x + 156;
            float xaveIconY = y + 5;
            float xaveIconWidth = 9;
            float xaveIconHeight = 9;
            boolean xaveIconHovered = (double) mouseX >= xaveIconX && (double) mouseX <= xaveIconX + xaveIconWidth && (double) mouseY >= xaveIconY - 3 && (double) mouseY <= xaveIconY - 3 + xaveIconHeight;
            boolean isClicked = xaveIconClicked.getOrDefault(name, false);
            int xaveIconColor = isClicked ? ColorUtil.getColor(255, 255, 0) : xaveIconHovered ? ColorUtil.getColor(134, 201, 244) : -1;
            RenderUtil.drawImage2D(new ResourceLocation("SkyCore/icons/alts/star.png"), xaveIconX + 1, xaveIconY, 7.5f, 7.5f, xaveIconColor);

            RenderUtil.drawRoundedHead(getSkinForName(name), null, x + 8, y + 8, 48, 48, 10, 1.0f);
            Fonts.sf_regular[16].drawString(matrixStack, name, x + 61, y + 23, ColorUtil.getColor(255, 255, 255, 200));
            Fonts.sf_regular[14].drawString(matrixStack, "Дата создания", x + 61, y + 32, ColorUtil.getColor(255, 255, 255, 120));
            Fonts.sf_regular[14].drawString(matrixStack, date, x + 61, y + 40, ColorUtil.getColor(255, 255, 255, 120));

            col++;
            if ((x + rectWidth + rectPadding) > (centerX + 380 - 10)) {
                col = 0;
                row++;
            }
        }

        maxHeight = (int) Math.ceil(filteredAccounts.size() / 2.0) * (74);

        ScissorUtil.end();
    }

    @Native
    private void renderButtons(MatrixStack matrixStack, int mouseX, int mouseY) {
        float centerX = (mc.getMainWindow().getScaledWidth() - 380) / 2.0f;
        float centerY = (mc.getMainWindow().getScaledHeight() - 230) / 2.0f;
        float spacing = 13;
        float buttonWidth = 84;
        float buttonHeight = 26;
        float totalWidth = buttonWidth * 4 + spacing * 3;
        float baseX = centerX + (380 - totalWidth) / 2.0f;
        float y = centerY + 35 + 230 + 10;

        String[] labels = {"Добавить", "Очистить", "Рандомный", "Закрыть"};

        for (int i = 0; i < 4; i++) {
            float x = baseX + i * (buttonWidth + spacing);
            boolean hovered = mouseX >= x && mouseX <= x + buttonWidth && mouseY >= y && mouseY <= y + buttonHeight;
            int textColor = hovered ? ColorUtil.getColor(255, 255, 255, 200) : ColorUtil.getColor(255, 255, 255, 120);
            RenderUtil.drawRoundedRectangleGradient(x, y, buttonWidth, buttonHeight, 6, ColorUtil.getColor(9, 9, 9), ColorUtil.getColor(8, 8, 8), ColorUtil.getColor(31, 31, 31), ColorUtil.getColor(12, 12, 12), 1);
            Fonts.sf_regular[26].drawString(matrixStack, labels[i], x + (buttonWidth - Fonts.sf_regular[26].getWidth(labels[i])) / 2, y + (buttonHeight - Fonts.sf_regular[26].getHeight()) / 2 - 1.5f, textColor);
        }
    }

    @Native
    private void renderAddDialog(MatrixStack matrixStack, int mouseX, int mouseY) {
        if (!showAddDialog) return;

        float dialogWidth = 180;
        float dialogHeight = 90;
        float dialogX = (mc.getMainWindow().getScaledWidth() - dialogWidth) / 2.0f;
        float dialogY = (mc.getMainWindow().getScaledHeight() - dialogHeight) / 2.0f;

        RenderUtil.drawImage2D(new ResourceLocation("SkyCore/icons/mainmenu/black_background.png"), 0, 0, mc.getMainWindow().getScaledWidth(), mc.getMainWindow().getScaledHeight(), ColorUtil.applyOpacity(-1, 240));
        RenderUtil.drawRoundedRectangleGradient(dialogX, dialogY + 35, dialogWidth, dialogHeight, 8, ColorUtil.getColor(11, 11, 11), ColorUtil.getColor(10, 10, 10), ColorUtil.getColor(31, 31, 32), ColorUtil.getColor(12, 12, 12), 1);

        Fonts.sf_medium[21].drawString(matrixStack, "Напишите желаемый ник", dialogX + (dialogWidth - Fonts.sf_medium[21].getWidth("Напишите желаемый ник")) / 2.0f, dialogY + 49.5f, -1);

        float inputX = dialogX + 4;
        float inputY = dialogY + 66.5f;
        float inputWidth = 172;
        float inputHeight = 24;
        boolean inputHovered = mouseX >= inputX && mouseX <= inputX + inputWidth && mouseY >= inputY && mouseY <= inputY + inputHeight;

        RenderUtil.drawRoundedRectangle(inputX, inputY, inputWidth, inputHeight, 4, inputHovered ? ColorUtil.getColor(33, 33, 33) : ColorUtil.getColor(22, 22, 22));

        String display = (addDialogInputText.isEmpty() && !addDialogInputFocused) ? "Никнейм" : addDialogInputText;

        int textColor = (addDialogInputText.isEmpty() && !addDialogInputFocused) ? ColorUtil.getColor(255, 255, 255, 120) : -1;
        Fonts.sf_regular[17].drawString(matrixStack, display, inputX + 5, inputY + 9.5f, textColor);

        if (addDialogInputFocused) {
            long time = System.currentTimeMillis();
            if (cursorBlinkStart == 0) cursorBlinkStart = time;
            boolean showCursor = (time - cursorBlinkStart) % 1000 < 500;
            if (showCursor) {
                float textWidth = Fonts.sf_regular[17].getWidth(addDialogInputText);
                RenderUtil.drawMinecraftRectangle(matrixStack, inputX + 5 + textWidth, inputY + 15, 5, 0.5f, -1);
            }
        } else {
            isAddDialogTextSelected = false;
        }

        closeIconX = dialogX + dialogWidth - closeIconWidth - 7;
        closeIconY = dialogY + 41;
        RenderUtil.drawImage2D(new ResourceLocation("SkyCore/icons/alts/close.png"), closeIconX, closeIconY, closeIconWidth, closeIconHeight, -1);

        float buttonWidth = 120;
        float buttonHeight = 22;
        float buttonX = dialogX + (dialogWidth - buttonWidth) / 2.0f;
        float buttonY = dialogY + 97.5f;
        boolean buttonHovered = mouseX >= buttonX && mouseX <= buttonX + buttonWidth && mouseY >= buttonY && mouseY <= buttonY + buttonHeight;

        RenderUtil.drawRoundedRectangleGradient(buttonX, buttonY, buttonWidth, buttonHeight, 5, ColorUtil.getColor(8, 8, 8), ColorUtil.getColor(7, 7, 7), ColorUtil.getColor(29, 29, 29), ColorUtil.getColor(10, 10, 10), 1);

        String text = "Добавить";
        textColor = buttonHovered ? ColorUtil.getColor(255, 255, 255, 200) : ColorUtil.getColor(255, 255, 255, 120);
        float textWidth = Fonts.sf_medium[24].getWidth(text);
        float textHeight = Fonts.sf_medium[24].getHeight();

        Fonts.sf_medium[24].drawString(matrixStack, text, buttonX + (buttonWidth - textWidth) / 2.0f, buttonY + (buttonHeight - textHeight) / 2.0f - 2, textColor);

        if (buttonHovered && GLFW.glfwGetMouseButton(mc.getMainWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
            if (!addDialogInputText.trim().isEmpty()) {
                String accountName = addDialogInputText.trim();
                accountNames.put(accountName, System.currentTimeMillis());
                accountManager.addAccount(accountName);
                updateAccountOrder();
                scrollToBottom();
                addDialogInputText = "";
                addDialogInputFocused = false;
                isAddDialogTextSelected = false;
                showAddDialog = false;
            }
        }
    }

    @Native
    private void renderMicrosoft(MatrixStack matrixStack, int mouseX, int mouseY) {
        float iconX = 4;
        float iconY = 4;
        float iconW = 32;
        float iconH = 32;
        boolean hovered = mouseX >= iconX && mouseX <= iconX + iconW && mouseY >= iconY && mouseY <= iconY + iconH;
        int color = hovered ? ColorUtil.getColor(134, 201, 244) : -1;
        RenderUtil.drawImage2D(new ResourceLocation("SkyCore/icons/alts/microsoft.png"), iconX, iconY, iconW, iconH, color);
    }

    @Native
    private void renderClearDialog(MatrixStack matrixStack, int mouseX, int mouseY) {
        if (!showClearDialog) return;

        float dialogWidth = 180;
        float dialogHeight = 60;
        float dialogX = (mc.getMainWindow().getScaledWidth() - dialogWidth) / 2.0f;
        float dialogY = (mc.getMainWindow().getScaledHeight() - dialogHeight) / 2.0f + 20;

        RenderUtil.drawImage2D(new ResourceLocation("SkyCore/icons/mainmenu/black_background.png"), 0, 0, mc.getMainWindow().getScaledWidth(), mc.getMainWindow().getScaledHeight(), ColorUtil.applyOpacity(-1, 240));

        RenderUtil.drawRoundedRectangleGradient(dialogX, dialogY, dialogWidth, dialogHeight, 8, ColorUtil.getColor(11, 11, 11), ColorUtil.getColor(10, 10, 10), ColorUtil.getColor(31, 31, 31), ColorUtil.getColor(12, 12, 12), 1);

        String text = "Подтвердить";
        Fonts.sf_medium[22].drawString(matrixStack, text, dialogX + (dialogWidth - Fonts.sf_medium[22].getWidth(text)) / 2.0f, dialogY + 14.5f, -1);

        closeIconX = dialogX + dialogWidth - closeIconWidth - 7;
        closeIconY = dialogY + 6;
        RenderUtil.drawImage2D(new ResourceLocation("SkyCore/icons/alts/close.png"), closeIconX, closeIconY, closeIconWidth, closeIconHeight, -1);

        float buttonWidth = 60;
        float buttonHeight = 23;
        float spacing = 20;
        float buttonY = dialogY + dialogHeight - buttonHeight - 4;
        float yesButtonX = dialogX + (dialogWidth - buttonWidth * 2 - spacing) / 2.0f;
        float noButtonX = yesButtonX + buttonWidth + spacing;

        boolean yesHovered = mouseX >= yesButtonX && mouseX <= yesButtonX + buttonWidth && mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
        boolean noHovered = mouseX >= noButtonX && mouseX <= noButtonX + buttonWidth && mouseY >= buttonY && mouseY <= buttonY + buttonHeight;

        RenderUtil.drawRoundedRectangleGradient(yesButtonX, buttonY, buttonWidth, buttonHeight, 5, ColorUtil.getColor(8, 8, 8), ColorUtil.getColor(7, 7, 7), ColorUtil.getColor(29, 29, 29), ColorUtil.getColor(10, 10, 10), 1);
        Fonts.sf_medium[24].drawString(matrixStack, "Ок", yesButtonX + (buttonWidth - Fonts.sf_medium[24].getWidth("Ок")) / 2.0f, buttonY + (buttonHeight - Fonts.sf_medium[24].getHeight()) / 2.0f - 1.5f, yesHovered ? ColorUtil.getColor(255, 255, 255, 200) : ColorUtil.getColor(255, 255, 255, 120));

        RenderUtil.drawRoundedRectangleGradient(noButtonX, buttonY, buttonWidth, buttonHeight, 5, ColorUtil.getColor(8, 8, 8), ColorUtil.getColor(7, 7, 7), ColorUtil.getColor(29, 29, 29), ColorUtil.getColor(10, 10, 10), 1);
        Fonts.sf_medium[24].drawString(matrixStack, "Отмена", noButtonX + (buttonWidth - Fonts.sf_medium[24].getWidth("Отмена")) / 2.0f, buttonY + (buttonHeight - Fonts.sf_medium[24].getHeight()) / 2.0f - 1.5f, noHovered ? ColorUtil.getColor(255, 255, 255, 200) : ColorUtil.getColor(255, 255, 255, 120));
    }

    private ResourceLocation getSkinForName(String name) {
        if (skinCache.containsKey(name)) {
            return skinCache.get(name);
        }

        try {
            ResourceLocation location = AbstractClientPlayerEntity.getLocationSkin(name);
            AbstractClientPlayerEntity.getDownloadImageSkin(location, name);
            skinCache.put(name, location);
            return location;
        } catch (Exception e) {
            skinCache.put(name, DEFAULT_SKIN);
            return DEFAULT_SKIN;
        }
    }

    @Native
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int baseScale = (int) mc.getMainWindow().getGuiScaleFactor();
        double factor = baseScale / 2.0;
        double sx = mouseX * factor;
        double sy = mouseY * factor;

        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(sx, sy, button);

        double w2 = mc.getMainWindow().getFramebufferWidth() / 2.0;
        double h2 = mc.getMainWindow().getFramebufferHeight() / 2.0;

        double searchX = (w2 - 172) / 2.0;
        double searchY = 4;
        double searchW = 172;
        double searchH = 24;
        if (!showAddDialog && !showClearDialog) {
            if (sx >= searchX && sx <= searchX + searchW && sy >= searchY && sy <= searchY + searchH) {
                inputFocused = true;
                addDialogInputFocused = false;
                cursorBlinkStart = System.currentTimeMillis();
                isTextSelected = false;
                return true;
            } else {
                inputFocused = false;
                isTextSelected = false;
            }
        }

        if (showClearDialog) {
            double dialogWidth = 180;
            double dialogHeight = 60;
            double dialogX = (w2 - dialogWidth) / 2.0;
            double dialogY = (h2 - dialogHeight) / 2.0 + 20;

            if (sx >= closeIconX * factor && sx <= closeIconX * factor + closeIconWidth * factor && sy >= closeIconY * factor && sy <= closeIconY * factor + closeIconHeight * factor) {
                showClearDialog = false;
                return true;
            }

            double buttonWidth = 60;
            double buttonHeight = 23;
            double spacing = 20;
            double buttonY = dialogY + dialogHeight - buttonHeight - 4;
            double yesButtonX = dialogX + (dialogWidth - buttonWidth * 2 - spacing) / 2.0;
            double noButtonX = yesButtonX + buttonWidth + spacing;

            if (sx >= yesButtonX && sx <= yesButtonX + buttonWidth && sy >= buttonY && sy <= buttonY + buttonHeight) {
                List<String> accountList = new ArrayList<>(accountNames.keySet());
                for (String accountName : accountList) accountManager.removeAccount(accountName);
                accountNames.clear();
                skinCache.clear();
                xaveIconClicked.clear();
                selectedAccount = null;
                showClearDialog = false;
                return true;
            }

            if (sx >= noButtonX && sx <= noButtonX + buttonWidth && sy >= buttonY && sy <= buttonY + buttonHeight) {
                showClearDialog = false;
                return true;
            }
            return true;
        }

        if (showAddDialog) {
            if (sx >= closeIconX * factor && sx <= closeIconX * factor + closeIconWidth * factor && sy >= closeIconY * factor && sy <= closeIconY * factor + closeIconHeight * factor) {
                showAddDialog = false;
                addDialogInputText = "";
                addDialogInputFocused = false;
                isAddDialogTextSelected = false;
                return true;
            }

            double dialogWidth = 180;
            double dialogX = (w2 - dialogWidth) / 2.0;
            double dialogY = (h2 - 90) / 2.0;
            double inputX = dialogX + 4;
            double inputY = dialogY + 66.5;
            if (sx >= inputX && sx <= inputX + 172 && sy >= inputY && sy <= inputY + 24) {
                addDialogInputFocused = true;
                inputFocused = false;
                cursorBlinkStart = System.currentTimeMillis();
                isAddDialogTextSelected = false;
                return true;
            } else {
                addDialogInputFocused = false;
                isAddDialogTextSelected = false;
            }
            return true;
        }

        double centerX2 = (w2 - 380) / 2.0;
        double centerY2 = (h2 - 230) / 2.0;
        double scissorY2 = centerY2 + 35;
        double scissorW2 = 380;
        double scissorH2 = 232;
        boolean isInScissorArea = sx >= centerX2 && sx <= centerX2 + scissorW2 && sy >= scissorY2 && sy <= scissorY2 + scissorH2;

        double startX2 = centerX2 + 10;
        double startY2 = centerY2 + 45;
        int row = 0, col = 0;
        Iterator<Map.Entry<String, Long>> iterator = accountNames.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            String name = entry.getKey();

            double rectWidth = 170;
            double rectPadding = 20;
            double x = startX2 + col * (rectWidth + rectPadding);
            double rectHeight = 64;
            double y = startY2 + row * (rectHeight + (rectPadding - 10)) + animatedScroll;

            if (isInScissorArea) {
                double xmarkIconX = x + 132.5;
                double xmarkIconY = y + 5.5;
                double xmarkIconWidth = 7;
                double xmarkIconHeight = 7;
                if (sx >= xmarkIconX && sx <= xmarkIconX + xmarkIconWidth && sy >= xmarkIconY && sy <= xmarkIconY + xmarkIconHeight) {
                    iterator.remove();
                    skinCache.remove(name);
                    xaveIconClicked.remove(name);
                    accountManager.removeAccount(name);
                    if (Objects.equals(selectedAccount, name)) selectedAccount = null;
                    updateAccountOrder();
                    return true;
                }

                double copyIconX = x + 144;
                double copyIconY = y + 5;
                double copyIconWidth = 8;
                double copyIconHeight = 8;
                if (sx >= copyIconX && sx <= copyIconX + copyIconWidth && sy >= copyIconY && sy <= copyIconY + copyIconHeight) {
                    mc.keyboardListener.setClipboardString(name);
                    return true;
                }

                double favX = x + 156;
                double favY = y + 7;
                double favW = 9;
                double favH = 9;
                if (sx >= favX && sx <= favX + favW && sy >= favY - 3 && sy <= favY - 3 + favH) {
                    boolean newFavoriteState = !xaveIconClicked.getOrDefault(name, false);
                    xaveIconClicked.put(name, newFavoriteState);
                    accountManager.setFavorite(name, newFavoriteState);
                    updateAccountOrder();
                    return true;
                }

                if (sx >= x && sx <= x + rectWidth && sy >= y && sy <= y + rectHeight) {
                    selectedAccount = name;
                    mc.session.setUsername(selectedAccount);
                    accountManager.selectAccount(selectedAccount);
                    return true;
                }
            }

            col++;
            if ((x + rectWidth + rectPadding) > (centerX2 + 380 - 10)) {
                col = 0;
                row++;
            }
        }

        double spacing = 13;
        double buttonWidth = 84;
        double buttonHeight = 26;
        double totalWidth = buttonWidth * 4 + spacing * 3;
        double baseX = centerX2 + (380 - totalWidth) / 2.0;
        double yButtons = centerY2 + 35 + 230 + 10;

        if (sx >= baseX && sx <= baseX + buttonWidth && sy >= yButtons && sy <= yButtons + buttonHeight) {
            showAddDialog = true;
            addDialogInputFocused = true;
            cursorBlinkStart = System.currentTimeMillis();
            addDialogInputText = "";
            isAddDialogTextSelected = false;
            return true;
        }

        double msIconX = 4;
        double msIconY = 4;
        double msIconW = 32;
        double msIconH = 32;
        if (sx >= msIconX && sx <= msIconX + msIconW && sy >= msIconY && sy <= msIconY + msIconH) {
            MicrosoftLogin.getRefreshToken(refreshToken -> {
                if (refreshToken == null) return;
                MicrosoftLogin.LoginData data = MicrosoftLogin.login(refreshToken);
                if (data.isGood()) {
                    mc.execute(() -> {
                        if (data.username != null && !data.username.isEmpty()) {
                            if (!accountNames.containsKey(data.username)) {
                                accountNames.put(data.username, System.currentTimeMillis());
                                accountManager.addAccount(data.username);
                                updateAccountOrder();
                            }
                            try {
                                mc.session = new Session(data.username, data.uuid, data.mcToken, "mojang");
                                selectedAccount = data.username;
                                accountManager.selectAccount(data.username);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            });
            return true;
        }

        double x2 = baseX + (buttonWidth + spacing);
        if (sx >= x2 && sx <= x2 + buttonWidth && sy >= yButtons && sy <= yButtons + buttonHeight) {
            if (!accountNames.isEmpty()) showClearDialog = true;
            return true;
        }

        double x3 = baseX + 2 * (buttonWidth + spacing);
        if (sx >= x3 && sx <= x3 + buttonWidth && sy >= yButtons && sy <= yButtons + buttonHeight) {
//            String randomNickname = OtherUtil.generateRandomNickname();
//            if (!accountNames.containsKey(randomNickname)) {
//                accountNames.put(randomNickname, System.currentTimeMillis());
//                accountManager.addAccount(randomNickname);
//                updateAccountOrder();
//                scrollToBottom();
//            }
            return true;
        }

        double x4 = baseX + 3 * (buttonWidth + spacing);
        if (sx >= x4 && sx <= x4 + buttonWidth && sy >= yButtons && sy <= yButtons + buttonHeight) {
            savedScroll = this.scroll;
            mc.displayGuiScreen(new MainMenuScreen());
            return true;
        }

        return super.mouseClicked(sx, sy, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (showAddDialog) return super.mouseScrolled(mouseX, mouseY, delta);

        float windowWidth = mc.getMainWindow().getScaledWidth();
        float windowHeight = mc.getMainWindow().getScaledHeight();
        float centerX = (windowWidth - 380) / 2.0f;
        float centerY = (windowHeight - 232) / 2.0f;

        boolean hovered = mouseX >= centerX && mouseX <= centerX + 380 && mouseY >= centerY + 35 && mouseY <= centerY + 35 + 232;

        if (hovered) {
            float contentHeight = 232 - 10;
            boolean canScroll = maxHeight > contentHeight;
            float previousScroll = scroll;

            scroll += (float) (delta * 60);
            scroll = MathHelper.clamp(scroll, -maxHeight + contentHeight, 0);

            return canScroll && scroll != previousScroll;
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!Character.toString(codePoint).matches("[a-zA-Z0-9_]")) {
            return true;
        }
        if (showAddDialog && addDialogInputFocused) {
            if (isAddDialogTextSelected) {
                addDialogInputText = String.valueOf(codePoint);
                isAddDialogTextSelected = false;
            } else if (addDialogInputText.length() < MAX_INPUT_LENGTH) {
                addDialogInputText += codePoint;
            }
            return true;
        } else if (inputFocused && !showClearDialog) {
            if (isTextSelected) {
                inputText = String.valueOf(codePoint);
                isTextSelected = false;
            } else if (inputText.length() < MAX_INPUT_LENGTH) {
                inputText += codePoint;
            }
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        float windowWidth = mc.getMainWindow().getScaledWidth();
        float windowHeight = mc.getMainWindow().getScaledHeight();
        float centerX = (windowWidth - 380) / 2.0f;
        float centerY = (windowHeight - 232) / 2.0f;
        double mouseX = mc.mouseHelper.getMouseX() * mc.getMainWindow().getScaledWidth() / mc.getMainWindow().getWidth();
        double mouseY = mc.mouseHelper.getMouseY() * mc.getMainWindow().getScaledHeight() / mc.getMainWindow().getHeight();
        boolean panelHovered = mouseX >= centerX && mouseX <= centerX + 380 && mouseY >= centerY + 35 && mouseY <= centerY + 35 + 232;

        if (panelHovered && keyCode == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && !showAddDialog && !showClearDialog) {
            String clipboardText = mc.keyboardListener.getClipboardString();
            if (!clipboardText.isEmpty()) {
                String trimmedText = clipboardText.trim();
                trimmedText = trimmedText.substring(0, Math.min(trimmedText.length(), MAX_INPUT_LENGTH));
                if (!trimmedText.isEmpty() && !accountNames.containsKey(trimmedText)) {
                    accountNames.put(trimmedText, System.currentTimeMillis());
                    accountManager.addAccount(trimmedText);
                    updateAccountOrder();
                    scrollToBottom();
                }
            }
            return true;
        }

        if (showAddDialog && addDialogInputFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (isAddDialogTextSelected) {
                    addDialogInputText = "";
                    isAddDialogTextSelected = false;
                } else if (!addDialogInputText.isEmpty()) {
                    addDialogInputText = addDialogInputText.substring(0, addDialogInputText.length() - 1);
                }
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_ENTER && !addDialogInputText.trim().isEmpty()) {
                String accountName = addDialogInputText.trim();
                accountNames.put(accountName, System.currentTimeMillis());
                accountManager.addAccount(accountName);
                updateAccountOrder();
                addDialogInputText = "";
                addDialogInputFocused = false;
                isAddDialogTextSelected = false;
                showAddDialog = false;
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_A && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                if (!addDialogInputText.isEmpty()) {
                    isAddDialogTextSelected = true;
                }
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_C && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                if (isAddDialogTextSelected && !addDialogInputText.isEmpty()) {
                    mc.keyboardListener.setClipboardString(addDialogInputText);
                }
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                String clipboardText = mc.keyboardListener.getClipboardString();
                if (!clipboardText.isEmpty()) {
                    clipboardText = clipboardText.substring(0, Math.min(clipboardText.length(), MAX_INPUT_LENGTH));
                    if (isAddDialogTextSelected) {
                        addDialogInputText = clipboardText;
                        isAddDialogTextSelected = false;
                    } else {
                        int remainingChars = MAX_INPUT_LENGTH - addDialogInputText.length();
                        if (remainingChars > 0) {
                            addDialogInputText += clipboardText.substring(0, Math.min(clipboardText.length(), remainingChars));
                        }
                    }
                }
                return true;
            }
        } else if (inputFocused && !showClearDialog) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (isTextSelected) {
                    inputText = "";
                    isTextSelected = false;
                } else if (!inputText.isEmpty()) {
                    inputText = inputText.substring(0, inputText.length() - 1);
                }
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_A && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                if (!inputText.isEmpty()) {
                    isTextSelected = true;
                }
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_C && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                if (isTextSelected && !inputText.isEmpty()) {
                    mc.keyboardListener.setClipboardString(inputText);
                }
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                String clipboardText = mc.keyboardListener.getClipboardString();
                if (!clipboardText.isEmpty()) {
                    clipboardText = clipboardText.substring(0, Math.min(clipboardText.length(), MAX_INPUT_LENGTH));
                    if (isTextSelected) {
                        inputText = clipboardText;
                        isTextSelected = false;
                    } else {
                        int remainingChars = MAX_INPUT_LENGTH - inputText.length();
                        if (remainingChars > 0) {
                            inputText += clipboardText.substring(0, Math.min(clipboardText.length(), remainingChars));
                        }
                    }
                }
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                inputText = "";
                inputFocused = false;
                isTextSelected = false;
                return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (showClearDialog) {
                showClearDialog = false;
                return true;
            } else if (showAddDialog) {
                showAddDialog = false;
                addDialogInputText = "";
                addDialogInputFocused = false;
                isAddDialogTextSelected = false;
                return true;
            } else {
                mc.displayGuiScreen(new MainMenuScreen());
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}