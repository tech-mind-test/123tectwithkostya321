package net.minecraft.client.gui.screen;

import com.darkmagician6.eventapi.EventManager;
import com.mojang.blaze3d.matrix.MatrixStack;
import sky.core.SkyCore;
import sky.core.events.EventDragging;
import sky.core.events.EventMouseClicked;
import sky.core.events.EventMouseReleased;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.CommandSuggestionHelper;
import net.minecraft.client.gui.NewChatGui;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TranslationTextComponent;

import java.io.*;

public class ChatScreen extends Screen {
    private String historyBuffer = "";
    private int sentHistoryCursor = -1;
    protected TextFieldWidget inputField;
    private String defaultInputFieldText = "";
    private CommandSuggestionHelper commandSuggestionHelper;
    private static boolean isHVisible = false;
    private static final File CONFIG_FILE = new File(Minecraft.getInstance().gameDir, "chat_config.txt");

    public ChatScreen(String defaultText) {
        super(NarratorChatListener.EMPTY);
        this.defaultInputFieldText = defaultText;
        loadHVisibleState();
    }

    private void loadHVisibleState() {
        if (CONFIG_FILE.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(CONFIG_FILE))) {
                String line = reader.readLine();
                if (line != null) {
                    isHVisible = Boolean.parseBoolean(line.trim());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveHVisibleState() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CONFIG_FILE))) {
            writer.write(String.valueOf(isHVisible));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void init() {
        this.minecraft.keyboardListener.enableRepeatEvents(true);
        this.sentHistoryCursor = this.minecraft.ingameGUI.getChatGUI().getSentMessages().size();
        this.inputField = new TextFieldWidget(this.font, 4, this.height - 12, this.width - 4, 12, new TranslationTextComponent("chat.editBox")) {
            protected IFormattableTextComponent getNarrationMessage() {
                return super.getNarrationMessage().appendString(ChatScreen.this.commandSuggestionHelper.getSuggestionMessage());
            }
        };
        this.inputField.setMaxStringLength(256);
        this.inputField.setEnableBackgroundDrawing(false);
        this.inputField.setText(this.defaultInputFieldText);
        this.inputField.setResponder(this::func_212997_a);
        this.children.add(this.inputField);
        this.commandSuggestionHelper = new CommandSuggestionHelper(this.minecraft, this, this.inputField, this.font, false, false, 1, 10, true, -805306368);
        this.commandSuggestionHelper.init();
        this.setFocusedDefault(this.inputField);
    }

    public void resize(Minecraft minecraft, int width, int height) {
        String s = this.inputField.getText();
        this.init(minecraft, width, height);
        this.setChatLine(s);
        this.commandSuggestionHelper.init();
    }

    public void onClose() {
        this.minecraft.keyboardListener.enableRepeatEvents(false);
        this.minecraft.ingameGUI.getChatGUI().resetScroll();
        saveHVisibleState();
    }

    public void tick() {
        this.inputField.tick();
    }

    private void func_212997_a(String p_2129971) {
        String s = this.inputField.getText();
        this.commandSuggestionHelper.shouldAutoSuggest(!s.equals(this.defaultInputFieldText));
        this.commandSuggestionHelper.init();
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.commandSuggestionHelper.onKeyPressed(keyCode, scanCode, modifiers)) {
            return true;
        } else if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        } else if (keyCode == 256) {
            this.minecraft.displayGuiScreen(null);
            return true;
        } else if (keyCode != 257 && keyCode != 335) {
            if (keyCode == 265) {
                this.getSentHistory(-1);
                return true;
            } else if (keyCode == 264) {
                this.getSentHistory(1);
                return true;
            } else if (keyCode == 266) {
                this.minecraft.ingameGUI.getChatGUI().addScrollPos(this.minecraft.ingameGUI.getChatGUI().getLineCount() - 1);
                return true;
            } else if (keyCode == 267) {
                this.minecraft.ingameGUI.getChatGUI().addScrollPos(-this.minecraft.ingameGUI.getChatGUI().getLineCount() + 1);
                return true;
            } else {
                return false;
            }
        } else {
            String s = this.inputField.getText().trim();
            if (!s.isEmpty()) {
                this.sendMessage(s);
            }
            this.minecraft.displayGuiScreen(null);
            return true;
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 1.0D) {
            delta = 1.0D;
        }
        if (delta < -1.0D) {
            delta = -1.0D;
        }
        if (this.commandSuggestionHelper.onScroll(delta)) {
            return true;
        } else {
            if (!hasShiftDown()) {
                delta *= 7.0D;
            }
            this.minecraft.ingameGUI.getChatGUI().addScrollPos(delta);
            return true;
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int yTop = this.height - 14;
        int yBottom = this.height - 2;
        int secondRectWidth = 12;
        int thirdRectWidth = 12;
        int gap = 2;
        int firstRectEndX = this.width - 2 - (secondRectWidth + thirdRectWidth + gap * 2);
        int secondRectStartX = firstRectEndX + gap;
        int secondRectEndX = secondRectStartX + secondRectWidth;
        int thirdRectStartX = secondRectEndX + gap;
        int thirdRectEndX = thirdRectStartX + thirdRectWidth;

        if (button == 0) {
            if (mouseX >= secondRectStartX && mouseX <= secondRectEndX && mouseY >= yTop && mouseY <= yBottom) {
                this.minecraft.world.playSound(this.minecraft.player, this.minecraft.player.getPosX(), this.minecraft.player.getPosY(), this.minecraft.player.getPosZ(), SoundEvents.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0F, 1.0F);
                SkyCore.getInstance().getDraggingManager().resetAllToInitialPositions();
                return true;
            }
            if (mouseX >= thirdRectStartX && mouseX <= thirdRectEndX && mouseY >= yTop && mouseY <= yBottom) {
                this.minecraft.world.playSound(this.minecraft.player, this.minecraft.player.getPosX(), this.minecraft.player.getPosY(), this.minecraft.player.getPosZ(), SoundEvents.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0F, 1.0F);
                isHVisible = !isHVisible;
                saveHVisibleState();
                return true;
            }
        }

        if (this.commandSuggestionHelper.onClick(mouseX, mouseY, button)) {
            return true;
        } else {
            if (button == 0) {
                NewChatGui newchatgui = this.minecraft.ingameGUI.getChatGUI();
                if (newchatgui.func_238491_a_(mouseX, mouseY)) {
                    return true;
                }
                Style style = newchatgui.func_238494_b_(mouseX, mouseY);
                if (style != null && this.handleComponentClicked(style)) {
                    return true;
                }
            }
            EventManager.call(new EventMouseClicked(button, (float) mouseX, (float) mouseY));
            return this.inputField.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
        }
    }


    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        EventManager.call(new EventMouseReleased(button, mouseX, mouseY));
        return super.mouseReleased(mouseX, mouseY, button);
    }

    protected void insertText(String text, boolean overwrite) {
        if (overwrite) {
            this.inputField.setText(text);
        } else {
            this.inputField.writeText(text);
        }
    }

    public void getSentHistory(int msgPos) {
        int i = this.sentHistoryCursor + msgPos;
        int j = this.minecraft.ingameGUI.getChatGUI().getSentMessages().size();
        i = MathHelper.clamp(i, 0, j);
        if (i != this.sentHistoryCursor) {
            if (i == j) {
                this.sentHistoryCursor = j;
                this.inputField.setText(this.historyBuffer);
            } else {
                if (this.sentHistoryCursor == j) {
                    this.historyBuffer = this.inputField.getText();
                }
                this.inputField.setText(this.minecraft.ingameGUI.getChatGUI().getSentMessages().get(i));
                this.commandSuggestionHelper.shouldAutoSuggest(false);
                this.sentHistoryCursor = i;
            }
        }
    }

    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.setListener(this.inputField);
        this.inputField.setFocused2(true);

        int chatBackgroundColor = this.minecraft.gameSettings.getChatBackgroundColor(Integer.MIN_VALUE);
        int rectHeight = 12;
        int yTop = this.height - 14;
        int yBottom = this.height - 2;
        int secondRectWidth = 12;
        int thirdRectWidth = 12;
        int gap = 2;
        int firstRectEndX = this.width - 2 - (secondRectWidth + thirdRectWidth + gap * 2);
        fill(matrixStack, 2, yTop, firstRectEndX, yBottom, chatBackgroundColor);

        int secondRectStartX = firstRectEndX + gap;
        int secondRectEndX = secondRectStartX + secondRectWidth;
        fill(matrixStack, secondRectStartX, yTop, secondRectEndX, yBottom, chatBackgroundColor);

        int thirdRectStartX = secondRectEndX + gap;
        int thirdRectEndX = thirdRectStartX + thirdRectWidth;
        fill(matrixStack, thirdRectStartX, yTop, thirdRectEndX, yBottom, chatBackgroundColor);

        String textH = "R";
        int textHWidth = this.font.getStringWidth(textH);
        int textHX = secondRectStartX + (secondRectWidth - textHWidth) / 2;
        int textHY = yTop + (rectHeight - this.font.FONT_HEIGHT) / 2 + 1;
        this.font.drawStringWithShadow(matrixStack, textH, textHX, textHY, -1);

        String textR = "H";
        int textRWidth = this.font.getStringWidth(textR);
        int textRX = thirdRectStartX + (thirdRectWidth - textRWidth) / 2;
        int textRY = yTop + (rectHeight - this.font.FONT_HEIGHT) / 2 + 1;
        this.font.drawStringWithShadow(matrixStack, textR, textRX, textRY, -1);

        this.inputField.render(matrixStack, mouseX, mouseY, partialTicks);

        if (this.isHVisible) {
            String inputText = this.inputField.getText();
            int startIndex = -1;
            int endIndex = inputText.length();
            if (inputText.contains("/reg")) {
                startIndex = inputText.indexOf("/reg") + 1;
            } else if (inputText.contains("/l")) {
                startIndex = inputText.indexOf("/l") + 1;
            }
            if (startIndex != -1) {
                String blurText = inputText.substring(startIndex, endIndex);
                int blurTextWidth = this.font.getStringWidth(blurText);
                String textBefore = inputText.substring(0, startIndex);
                int textBeforeWidth = this.font.getStringWidth(textBefore);
                int inputFieldX = 4;
                int inputFieldY = this.height - 12;
                int blurStartX = inputFieldX + textBeforeWidth;
                int blurEndX = blurStartX + blurTextWidth;
                int blurEndY = inputFieldY + rectHeight;
                fill(matrixStack, blurStartX, inputFieldY + 9, blurEndX, blurEndY - 13, 0xFF000000);
            }
        }

        if (mouseX >= secondRectStartX && mouseX <= secondRectEndX && mouseY >= yTop && mouseY <= yBottom) {
            this.renderTooltip(matrixStack, new TranslationTextComponent("Сбросить расположение"), mouseX, mouseY);
        }
        if (mouseX >= thirdRectStartX && mouseX <= thirdRectEndX && mouseY >= yTop && mouseY <= yBottom) {
            String activeIndicator = this.isHVisible ? " +" : " -";
            this.renderTooltip(matrixStack, new TranslationTextComponent("Скрывать информацию" + activeIndicator), mouseX, mouseY);
        }

        this.commandSuggestionHelper.drawSuggestionList(matrixStack, mouseX, mouseY);
        Style style = this.minecraft.ingameGUI.getChatGUI().func_238494_b_(mouseX, mouseY);
        if (style != null && style.getHoverEvent() != null) {
            this.renderComponentHoverEffect(matrixStack, style, mouseX, mouseY);
        }

        EventManager.call(new EventDragging(mouseX, mouseY));
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    public boolean isPauseScreen() {
        return false;
    }

    private void setChatLine(String p_208604_1_) {
        this.inputField.setText(p_208604_1_);
    }
}