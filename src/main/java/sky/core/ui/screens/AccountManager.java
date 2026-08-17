package sky.core.ui.screens;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Session;
import net.minecraft.util.text.StringTextComponent;
import com.adl.nativeprotect.Native;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

public class AccountManager extends Screen {
    private long lastClickTime = 0;
    private static final long DOUBLE_CLICK_INTERVAL = 250;
    private boolean isScrolling = false;
    private int lastMouseY = 0;
    private int scrollOffset = 0;
    private final int maxListHeight = 200;
    private Button loginButton;
    private Button deleteButton;
    private static List<Account> filteredAccounts = new ArrayList<>();
    private static final List<Account> accounts = new ArrayList<>();
    private TextFieldWidget fieldWidget;
    private Account selectedAccount = null;
    private static TextFieldWidget searchField;
    private final int accountListStartY = 45;
    private final int accountListEntryHeight = 20;
    private int selectedAccountColor = 0xFFFFFF;
    private static final ResourceLocation STEVE_SKIN = new ResourceLocation("textures/entity/steve.png");

    public AccountManager() {
        super(new StringTextComponent("Account Manager"));
        loadAccounts();
        readLastAlt();
    }

    private static class Account {
        String username;

        Account(String username) {
            this.username = username;
        }
    }
    @Native
    @Override
    public void init() {
        searchField = new TextFieldWidget(font, this.width / 2 - 100, 20, 200, 20, new StringTextComponent(""));
        searchField.setResponder(this::filterAccounts);
        this.children.add(searchField);

        int buttonWidth = 100;
        int buttonHeight = 20;
        int startY = this.height - 35;

        loginButton = addButton(new Button(this.width / 2 - (buttonWidth + 5) * 2, startY, buttonWidth, buttonHeight, new StringTextComponent("Login"), button -> loginSelectedAccount()));
        deleteButton = addButton(new Button(this.width / 2 - (buttonWidth + 5), startY, buttonWidth, buttonHeight, new StringTextComponent("Delete"), button -> deleteSelectedAccount()));
        addButton(new Button(this.width / 2, startY, buttonWidth, buttonHeight, new StringTextComponent("Add Account"), button -> Minecraft.getInstance().displayGuiScreen(new AddAccount())));
        addButton(new Button(this.width / 2 + (buttonWidth + 5), startY, buttonWidth, buttonHeight, new StringTextComponent("Отмена"), button -> Minecraft.getInstance().displayGuiScreen(new MainMenuScreen())));

        fieldWidget = new TextFieldWidget(this.font, this.width / 2 - 150, 100, 300, 20, new StringTextComponent("Username"));
        fieldWidget.setVisible(false);
        fieldWidget.setMaxStringLength(16);
        this.children.add(fieldWidget);

        filteredAccounts = new ArrayList<>(accounts);
    }

    public void addAccount(String username) {
        if (!isValidUsername(username)) {
            return;
        }
        for (Account account : accounts) {
            if (account.username.equalsIgnoreCase(username)) {
                return;
            }
        }
        accounts.add(new Account(username));
        saveAccounts();
    }

    @Native
    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);

        loginButton.active = selectedAccount != null;
        deleteButton.active = selectedAccount != null;

        String currentUsername = Minecraft.getInstance().getSession().getUsername();
        String screenTitle = "Account Management (" + currentUsername + ")";
        this.font.drawString(matrixStack, screenTitle, (float) this.width / 2 - (float) this.font.getStringWidth(screenTitle) / 2, 8, 0xFFFFFF);

        int listWidth = 300;
        int listX = this.width / 2 - listWidth / 2;
        int listY = accountListStartY;
        int visibleAccounts = maxListHeight / accountListEntryHeight;
        int startIndex = scrollOffset / accountListEntryHeight;
        int endIndex = Math.min(filteredAccounts.size(), startIndex + visibleAccounts);

        int maxRenderY = this.height - 35 - accountListEntryHeight;

        fill(matrixStack, listX, listY, listX + listWidth, listY + maxListHeight, 0x80000000);
        if (!filteredAccounts.isEmpty()) {
            for (int i = startIndex; i < endIndex; i++) {
                int entryY = listY + (i - startIndex) * accountListEntryHeight - (scrollOffset % accountListEntryHeight);
                if (entryY + accountListEntryHeight <= maxRenderY) {
                    Account account = filteredAccounts.get(i);
                    String accountName = account.username;

                    if (selectedAccount != null && accountName.equals(selectedAccount.username)) {
                        fill(matrixStack, listX, entryY, listX + listWidth, entryY + accountListEntryHeight, new Color(124, 124, 125, 255).getRGB());
                        fill(matrixStack, listX + 1, entryY + 1, listX + listWidth - 1, entryY + accountListEntryHeight - 1, new Color(0, 0, 0, 255).getRGB());
                    }

                    Minecraft.getInstance().getTextureManager().bindTexture(STEVE_SKIN);
                    GlStateManager.enableBlend();
                    drawScaledCustomSizeModalRect(listX + 2, entryY + 2, 8, 8, 8, 8, 16, 16, 64, 64);
                    GlStateManager.disableBlend();

                    int textColor = accountName.equals(currentUsername) ? 0x00FF00 : 0xFFFFFF;
                    drawString(matrixStack, font, accountName, listX + 21, entryY + 6, textColor);
                }

                if (filteredAccounts.size() > visibleAccounts) {
                    int scrollBarHeight = maxListHeight * visibleAccounts / filteredAccounts.size();
                    int scrollBarY = listY + (scrollOffset * (maxListHeight - scrollBarHeight)) / (filteredAccounts.size() * accountListEntryHeight - maxListHeight);
                    fill(matrixStack, listX + listWidth + 2, scrollBarY, listX + listWidth + 6, scrollBarY + scrollBarHeight, 0xFFFFFFFF);
                }

                String searchFieldText = "Find Account...";
                int searchFieldX = this.width / 2 - this.font.getStringWidth(searchFieldText) / 2 - 60;
                int searchFieldY = 26;
                if (!searchField.isFocused()) {
                    this.font.drawString(matrixStack, searchFieldText, searchFieldX, searchFieldY, 0xCCCCCC);
                }

                int scrollBarHeight = maxListHeight * maxListHeight / (filteredAccounts.size() * accountListEntryHeight);
                if (scrollBarHeight < maxListHeight) {
                    fill(matrixStack, listX + listWidth + 2, listY, listX + listWidth + 6, listY + maxListHeight, 0xFF000000);
                    fill(matrixStack, listX + listWidth + 2, listY + scrollOffset * maxListHeight / (filteredAccounts.size() * accountListEntryHeight), listX + listWidth + 6, listY + scrollOffset * maxListHeight / (filteredAccounts.size() * accountListEntryHeight) + scrollBarHeight, 0xFFFFFFFF);
                }

                if (mouseX >= listX && mouseX < listX + listWidth && mouseY >= entryY && mouseY < entryY + accountListEntryHeight) {
                    fill(matrixStack, listX + 2, entryY + 2, listX + 18, entryY + 18, new Color(0, 0, 0, 100).getRGB());
                }
            }

        } else {
            String noAccountsText = "Accounts not found!";
            this.font.drawString(matrixStack, noAccountsText, ((float) this.width / 2) - ((float) this.font.getStringWidth(noAccountsText) / 2), listY + 20, 0xFFFFFF);
        }

        searchField.render(matrixStack, mouseX, mouseY, partialTicks);
        if (fieldWidget.getVisible()) {
            fieldWidget.render(matrixStack, mouseX, mouseY, partialTicks);
        }
    }
    @Native
    public static void drawScaledCustomSizeModalRect(float x, float y, float u, float v, float uWidth, float vHeight, float width, float height, float tileWidth, float tileHeight) {
        float f = 1.0F / tileWidth;
        float f1 = 1.0F / tileHeight;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(x, y + height, 0.0D).tex(u * f, (v + vHeight) * f1).endVertex();
        bufferbuilder.pos(x + width, y + height, 0.0D).tex((u + uWidth) * f, (v + vHeight) * f1).endVertex();
        bufferbuilder.pos(x + width, y, 0.0D).tex((u + uWidth) * f, v * f1).endVertex();
        bufferbuilder.pos(x, y, 0.0D).tex(u * f, v * f1).endVertex();
        tessellator.draw();
    }
    @Native
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listWidth = 300;
        int listX = this.width / 2 - listWidth / 2;

        int visibleAccounts = maxListHeight / accountListEntryHeight;
        int startIndex = scrollOffset / accountListEntryHeight;
        int endIndex = Math.min(filteredAccounts.size(), startIndex + visibleAccounts);

        int maxRenderY = this.height - 35 - accountListEntryHeight;

        if (button == 0) {
            long currentTime = System.currentTimeMillis();
            for (int i = startIndex; i < endIndex; i++) {
                int entryY = accountListStartY + (i - startIndex) * accountListEntryHeight - (scrollOffset % accountListEntryHeight);

                if (entryY + accountListEntryHeight <= maxRenderY && mouseX >= listX && mouseX < listX + listWidth && mouseY >= entryY && mouseY < entryY + accountListEntryHeight) {
                    selectedAccount = filteredAccounts.get(i);
                    selectedAccountColor = 0xFFFF00;

                    if (currentTime - lastClickTime < DOUBLE_CLICK_INTERVAL) {
                        loginSelectedAccount();
                    }
                    lastClickTime = currentTime;
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
    @Native
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.isScrolling) {
            this.isScrolling = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.isScrolling) {
            int deltaYInt = (int) mouseY - this.lastMouseY;
            this.scrollOffset -= deltaYInt;
            this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, filteredAccounts.size() * accountListEntryHeight - maxListHeight));
            this.lastMouseY = (int) mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    private void filterAccounts(String query) {
        if (query.isEmpty()) {
            filteredAccounts = new ArrayList<>(accounts);
        } else {
            filteredAccounts.clear();
            for (Account account : accounts) {
                if (account.username.toLowerCase().contains(query.toLowerCase())) {
                    filteredAccounts.add(account);
                }
            }
        }
    }

    private void loginSelectedAccount() {
        if (selectedAccount != null) {
            Minecraft.getInstance().session = new Session(selectedAccount.username, "", "", "mojang");
            selectedAccountColor = 0x00FF00;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (isMouseOverScreen(mouseX, mouseY)) {
            scrollOffset -= (int) delta * accountListEntryHeight;
            int maxScroll = Math.max(0, filteredAccounts.size() * accountListEntryHeight - maxListHeight);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private boolean isMouseOverScreen(double mouseX, double mouseY) {
        int listWidth = 300;
        int listX = this.width / 2 - listWidth / 2;
        int listY = accountListStartY;
        return mouseX >= listX && mouseX < listX + listWidth && mouseY >= listY && mouseY < listY + maxListHeight;
    }

    private void deleteSelectedAccount() {
        if (selectedAccount != null) {
            accounts.remove(selectedAccount);
            saveAccounts();
            selectedAccount = null;
        }
    }

    private boolean isValidUsername(String username) {
        if (username.length() < 3 || username.length() > 16) {
            return false;
        }
        return username.matches("[a-zA-Z0-9_]+");
    }

    private void saveAccounts() {
        try (PrintWriter out = new PrintWriter("accounts.txt")) {
            Set<String> uniqueUsernames = new HashSet<>();
            for (Account account : accounts) {
                if (uniqueUsernames.add(account.username.toLowerCase()) && isValidUsername(account.username)) {
                    out.println(account.username);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveLastAccount() {
        if (accounts == null || accounts.isEmpty()) {
            return;
        }

        Account lastAccount = accounts.get(accounts.size() - 1);

        if (!isValidUsername(lastAccount.username)) {
            return;
        }

        try (PrintWriter out = new PrintWriter("last_account.txt")) {
            out.println(lastAccount.username);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static final File LAST_ACCOUNT_FILE = new File("last_account.txt");

    public void readLastAlt() {
        try (BufferedReader reader = Files.newBufferedReader(LAST_ACCOUNT_FILE.toPath(), StandardCharsets.UTF_8)) {
            String last = null;
            String line;

            while ((line = reader.readLine()) != null) {
                last = line.trim();
            }

            if (last != null) {
                Minecraft.getInstance().session = new Session(last, "", "", "mojang");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadAccounts() {
        File file = new File("accounts.txt");
        if (file.exists()) {
            try (Scanner scanner = new Scanner(file)) {
                accounts.clear();
                Set<String> uniqueUsernames = new HashSet<>();
                while (scanner.hasNextLine()) {
                    String username = scanner.nextLine();
                    if (uniqueUsernames.add(username.toLowerCase()) && isValidUsername(username)) {
                        accounts.add(new Account(username));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (searchField != null) {
            filterAccounts(searchField.getText());
        }
    }

    @Override
    public void onClose() {
        saveAccounts();
        super.onClose();
    }
}
