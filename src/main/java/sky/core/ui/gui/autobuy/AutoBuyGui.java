package sky.core.ui.gui.autobuy;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.item.ItemStack;
import com.adl.nativeprotect.Native;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.ItemSetting;
import sky.core.modules.api.constructors.impl.StringSetting;
import sky.core.modules.api.elements.impl.BooleanElement;
import sky.core.modules.api.elements.impl.ItemElement;
import sky.core.modules.api.elements.impl.StringElement;
import sky.core.modules.impl.player.AutoBuy;
import sky.core.modules.impl.visuals.Interface;
import sky.core.ui.gui.Panel;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.Wrapper;
import sky.core.utils.animation.AnimationUtil;
import sky.core.utils.animation.Easings;
import sky.core.utils.math.MathUtil;
import sky.core.utils.math.NumberUtil;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.ScissorUtil;
import sky.core.utils.render.font.Fonts;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class AutoBuyGui extends Panel implements Wrapper {
    private final List<StringElement> stringElements = new ArrayList<>();
    private final List<ItemElement> itemElements = new ArrayList<>();
    private final List<StringElement> priceElements = new ArrayList<>();
    private final List<StringElement> sellPercentElements = new ArrayList<>();
    private final List<BooleanElement> autoSetupElements = new ArrayList<>();
    private final AnimationUtil listScrollAnimation = new AnimationUtil(0f, 10, Easings.SINE_OUT);
    private float listScroll, animatedListScroll, listMaxHeight;
    private int selectedIndex = -1;
    private String lastMode = "";

    public AutoBuyGui() {
        this.width = 420;
        this.height = 300;
        centerPanel();
        stringElements.add(new StringElement(new StringSetting("Search...")));
    }

    private void centerPanel() {
        this.x = (mc.getMainWindow().getScaledWidth() - this.width) / 2f;
        this.y = (mc.getMainWindow().getScaledHeight() - this.height) / 2f;
    }

    @Native
    private void initItemSettings() {
        itemElements.clear();
        autoSetupElements.clear();
        priceElements.clear();
        sellPercentElements.clear();
        for (ItemSetting setting : ItemList.getItems()) {
            itemElements.add(new ItemElement(setting));
            autoSetupElements.add(new BooleanElement(new BooleanSetting("Auto Resell", setting.isSellEnabled())));
            priceElements.add(new StringElement(new StringSetting("0")));
            sellPercentElements.add(new StringElement(new StringSetting(String.valueOf(setting.getSellPercent()))));
        }
        if (!itemElements.isEmpty()) {
            itemElements.get(0).setSelected(true);
            selectedIndex = 0;
        }
        listScroll = 0;
    }

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY, float alpha) {
        String currentMode = AutoBuy.instance.mode.get();
        if (!currentMode.equals(lastMode)) {
            initItemSettings();
            centerPanel();
            lastMode = currentMode;
        }

        listScrollAnimation.update(listScroll);
        animatedListScroll = listScrollAnimation.getValue();

        RenderUtil.drawBlurredRoundedRectangle(x, y, width, height, 12, ThemeEditor.getColor(ThemeSettings.MAIN), alpha);
        RenderUtil.drawOutlineRectangle(x, y, width, height, 12, ThemeEditor.getColor(ThemeSettings.OUTLINE), ThemeEditor.getAlpha(ThemeSettings.OUTLINE) * alpha);

        boolean hC = MathUtil.isHovered(mouseX, mouseY, x + width - 22, y + 8, 14, 14);
        Fonts.sf_medium[18].drawString(stack, "q", x + width - 20, y + 10, hC ? ColorUtil.gradientInterface(0) : 0x60FFFFFF);

        Fonts.sf_bold[24].drawString(stack, "AutoBuy", x + 15, y + 15, -1);
        if (currentMode.equalsIgnoreCase("Холиворлд")) {
            Fonts.sf_medium[14].drawString(stack, "HOLYWORLD", x + 70, y + 19, 0xFF3399FF);
        } else {
            float sw = Fonts.sf_medium[14].getWidth("SPOOKY");
            Fonts.sf_medium[14].drawString(stack, "SPOOKY", x + 70, y + 19, 0xFFFF4D4D);
            Fonts.sf_medium[14].drawString(stack, "TIME", x + 70 + sw, y + 19, -1);
        }

        StringElement search = stringElements.get(0);
        search.setX(x + 10);
        search.setY(y + 45);
        search.setWidth(width / 2.2f);
        search.render(stack, mouseX, mouseY, alpha);

        float itemBaseY = y + 75, vH = height - 95, iS = 20f;
        List<ItemElement> filtered = getFilteredItems();
        listMaxHeight = filtered.size() * iS;
        listScroll = MathHelper.clamp(listScroll, Math.min(0, -listMaxHeight + vH), 0);

        ScissorUtil.start(x + 10, itemBaseY, width / 2.2f + 10, vH);
        for (int i = 0; i < filtered.size(); i++) {
            ItemElement elem = filtered.get(i);
            elem.setX(x + 15);
            elem.setY(itemBaseY + i * iS + animatedListScroll);
            elem.setWidth(width / 2.2f - 10);
            elem.render(stack, mouseX, mouseY, alpha);
        }
        ScissorUtil.end();

        float rX = x + width / 2.2f + 25, rW = width - (width / 2.2f) - 40;
        if (selectedIndex != -1 && selectedIndex < itemElements.size()) {
            ItemSetting sel = itemElements.get(selectedIndex).getSetting();
            renderScaledItem(stack, sel.getItemStack(), rX + (rW / 2f) - 20, y + 55, 2.5f);

            String n = sel.getName();
            if (n.length() > 16) n = n.substring(0, 14) + "..";
            Fonts.sf_bold[18].drawString(stack, n, rX + (rW - Fonts.sf_bold[18].getWidth(n)) / 2f, y + 105, -1);

            BooleanElement toggle = autoSetupElements.get(selectedIndex);
            toggle.setX(rX);
            toggle.setY(y + 125);
            toggle.setWidth(rW);
            toggle.render(stack, mouseX, mouseY, alpha);
            sel.setSellEnabled(toggle.getSetting().get());

            if (sel.isSellEnabled()) {
                Fonts.sf_medium[12].drawString(stack, "Наценка %:", rX + 6, y + 152, 0x90FFFFFF);
                StringElement pElem = sellPercentElements.get(selectedIndex);
                pElem.setX(rX + 65);
                pElem.setY(y + 143);
                pElem.setWidth(rW - 65);
                pElem.render(stack, mouseX, mouseY, alpha);
                if (pElem.isFocused) {
                    try {
                        sel.setSellPercent(Integer.parseInt(pElem.getInputText().replaceAll("[^0-9]", "")));
                    } catch (Exception ignored) {
                    }
                } else pElem.setInputText(String.valueOf(sel.getSellPercent()));
            }

            Fonts.sf_medium[12].drawString(stack, "Цена за 1:", rX + 6, y + 177, 0x90FFFFFF);
            StringElement price = priceElements.get(selectedIndex);
            price.setX(rX + 65);
            price.setY(y + 168);
            price.setWidth(rW - 65);
            price.render(stack, mouseX, mouseY, alpha);
            if (!price.isFocused) price.setInputText(NumberUtil.formatThousands(String.valueOf(sel.getMaxPrice())));
            else sel.setMaxPriceFromString(price.getInputText());

            float sY = y + 205;
            RenderUtil.drawOutlineRectangle(rX, sY, rW, 45, 6, 0x10FFFFFF, 1f);
            Fonts.sf_bold[11].drawString(stack, "Stats", rX + 6, sY + 6, ColorUtil.gradientInterface(0));
            Fonts.sf_regular[11].drawString(stack, "Active: " + ItemList.getItems().stream().filter(ItemSetting::get).count(), rX + 6, sY + 18, 0x95FFFFFF);
            Fonts.sf_regular[11].drawString(stack, "Total: " + ItemList.getItems().size(), rX + 6, sY + 28, 0x95FFFFFF);
        }

        float bW = rW / 2f - 3, bH = 18, bY = y + height - 30;
        int acc = ColorUtil.gradientInterface(0);
        if (drawBtn(stack, "ENABLE", rX, bY, bW, bH, mouseX, mouseY, acc))
            ItemList.getItems().forEach(i -> i.set(true));
        if (drawBtn(stack, "DISABLE", rX + bW + 6, bY, bW, bH, mouseX, mouseY, acc))
            ItemList.getItems().forEach(i -> i.set(false));
    }


    private boolean drawBtn(MatrixStack s, String t, float x, float y, float w, float h, float mx, float my, int c) {
        boolean hov = MathUtil.isHovered(mx, my, x, y, w, h);
        Fonts.sf_medium[11].drawString(s, t, x + (w - Fonts.sf_medium[11].getWidth(t)) / 2f, y + 4, hov ? -1 : 0x90FFFFFF);
        return hov && GLFW.glfwGetMouseButton(mc.getMainWindow().getHandle(), 0) == 1;
    }

    private void renderScaledItem(MatrixStack stack, ItemStack itemStack, float x, float y, float scale) {
        RenderSystem.pushMatrix();
        RenderSystem.translatef(x, y, 0);
        RenderSystem.scalef(scale, scale, 1.0f);
        mc.getItemRenderer().renderItemAndEffectIntoGUI(itemStack, 0, 0);
        RenderSystem.popMatrix();
    }

    @Native
    @Override
    public void mouseClicked(float mouseX, float mouseY, int button) {

        float rX = x + width / 2.2f + 25, bW = (width - (width / 2.2f) - 40) / 2f - 3, bY = y + height - 30;

        stringElements.forEach(e -> e.mouseClicked(mouseX, mouseY, button));

        if (MathUtil.isHovered(mouseX, mouseY, x, y + 75, width / 2.2f + 10, height - 95)) {
            for (ItemElement elem : getFilteredItems()) {
                if (MathUtil.isHovered(mouseX, mouseY, elem.getX(), elem.getY(), elem.getWidth(), 18f)) {
                    elem.mouseClicked(mouseX, mouseY, button);
                    if (button == 0) {
                        itemElements.forEach(e -> e.setSelected(false));
                        elem.setSelected(true);
                        selectedIndex = itemElements.indexOf(elem);
                    }
                    return;
                }
            }
        }
        if (selectedIndex != -1) {
            autoSetupElements.get(selectedIndex).mouseClicked(mouseX, mouseY, button);
            priceElements.get(selectedIndex).mouseClicked(mouseX, mouseY, button);
            sellPercentElements.get(selectedIndex).mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (MathUtil.isHovered((float) mouseX, (float) mouseY, x, y + 75, width / 2.2f, height - 95)) {
            listScroll += (float) (delta * 20);
            return true;
        }
        return false;
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        stringElements.forEach(e -> e.keyPressed(keyCode, scanCode, modifiers));
        if (selectedIndex != -1) {
            priceElements.get(selectedIndex).keyPressed(keyCode, scanCode, modifiers);
            sellPercentElements.get(selectedIndex).keyPressed(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        stringElements.forEach(e -> e.charTyped(codePoint, modifiers));
        if (selectedIndex != -1) {
            if (priceElements.get(selectedIndex).isFocused && Character.isDigit(codePoint))
                priceElements.get(selectedIndex).charTyped(codePoint, modifiers);
            if (sellPercentElements.get(selectedIndex).isFocused && Character.isDigit(codePoint))
                sellPercentElements.get(selectedIndex).charTyped(codePoint, modifiers);
        }
    }

    private List<ItemElement> getFilteredItems() {
        String f = stringElements.get(0).getInputText().trim().toLowerCase();
        List<ItemElement> res = new ArrayList<>();
        for (ItemElement e : itemElements) {
            String n = e.getSetting().getName().toLowerCase();
            if (f.equals("!вкл") ? e.getSetting().get() : f.equals("!выкл") ? !e.getSetting().get() : f.isEmpty() || n.contains(f))
                res.add(e);
        }
        return res;
    }
}