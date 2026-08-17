package sky.core.ui.Interface.elements.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.vector.Vector4f;
import sky.core.SkyCore;
import sky.core.events.EventRender2D;
import sky.core.utils.managers.impl.dragmanager.Dragging;
import sky.core.modules.Module;
import sky.core.modules.impl.miscellaneous.FunTimeHelper;
import sky.core.ui.Interface.elements.ElementRender;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.client.KeyStorage;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;

public class FuntimeRender implements ElementRender {

    private final Dragging dragging;

    public FuntimeRender(Dragging dragging) {
        this.dragging = dragging;
    }

    @Override
    public void render(EventRender2D.Post event) {
        float posX = dragging.getX();
        float posY = dragging.getY();
        MatrixStack ms = event.getStack();

        renderKeybinds(ms, posX, posY);
    }

    private void renderKeybinds(MatrixStack ms, float posX, float posY) {
        Module funTimeHelper = SkyCore.getInstance().getModuleManager().getModule(FunTimeHelper.class);
        if (funTimeHelper != null && funTimeHelper instanceof FunTimeHelper helper) {
            String[][] binds = new String[][]{
                    {"Дезорент", KeyStorage.getKey(helper.useDezor.get())},
                    {"Трапка", KeyStorage.getKey(helper.useTrap.get())},
                    {"Снежок", KeyStorage.getKey(helper.useSnow.get())},
                    {"Явная Пыль", KeyStorage.getKey(helper.usePil.get())},
                    {"Пласт", KeyStorage.getKey(helper.usePlast.get())},
                    {"Божья Аура", KeyStorage.getKey(helper.useAura.get())},
                    {"Смерч", KeyStorage.getKey(helper.useSmerch.get())}
            };
            float currentX = posX;

            for (String[] bind : binds) {
                String name = bind[0];
                String key = formatKey(bind[1]);
                if (!key.equals("None")) {
                    ItemStack icon = switch (name) {
                        case "Дезорент" -> Items.ENDER_EYE.getDefaultInstance();
                        case "Трапка" -> Items.NETHERITE_SCRAP.getDefaultInstance();
                        case "Снежок" -> Items.SNOWBALL.getDefaultInstance();
                        case "Явная Пыль" -> Items.SUGAR.getDefaultInstance();
                        case "Пласт" -> Items.DRIED_KELP.getDefaultInstance();
                        case "Божья Аура" -> Items.PHANTOM_MEMBRANE.getDefaultInstance();
                        case "Смерч" -> Items.FIRE_CHARGE.getDefaultInstance();
                        default -> Items.BARRIER.getDefaultInstance();
                    };

                    float nameWidth = Fonts.sf_medium[14].getWidth(name);
                    float keyWidth = Fonts.sf_medium[14].getWidth(key);
                    float maxTextWidth = Math.max(nameWidth, keyWidth);
                    float rectWidth = 11.0F + 3.0F * 3.0F + maxTextWidth + 6.0F;

                    int logoColor = ThemeEditor.getColor(ThemeSettings.LOGO);
                    int darkBgColor = ColorUtil.darken(logoColor, 0.1f);
                    RenderUtil.drawRoundedRectangle(currentX - 0.5F, posY - 0.5F, rectWidth + 1.0F, 16.0F + 1.0F,
                            new Vector4f(4.0F, 4.0F, 4.0F, 4.0F), ColorUtil.applyOpacity(darkBgColor, 90));
                    RenderUtil.drawRoundedRectangle(currentX, posY, rectWidth, 16.0F,
                            new Vector4f(3.5F, 3.5F, 3.5F, 3.5F), ColorUtil.applyOpacity(darkBgColor, 190));


                    float iconBgSize = 13.0F;
                    float iconBgX = currentX + 3.0F - 1.0F;
                    float iconBgY = posY + 16.0F / 2.0F - iconBgSize / 2.0F;
                    RenderUtil.drawRoundedRectangle(iconBgX, iconBgY, iconBgSize, iconBgSize,
                            new Vector4f(3.0F, 3.0F, 3.0F, 3.0F), ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.MODULE_VISUAL), 60));

                    float iconX = currentX + 3.0F - 1.7f;
                    float iconY = posY + 16.0F / 2.5F - 5.5F;
                    RenderUtil.drawStack(icon, iconX, iconY, 0.85F);

                    float textX = currentX + 3.0F + 11.0F + 3.0F + 1.0F;
                    Fonts.sf_medium[14].drawString(ms, name, textX, posY + 3.5F, 0xFFFFFFFF);
                    Fonts.sf_medium[11].drawString(ms, key, textX, posY + 16.0F - 5.5F, 0xFFFFFFFF);

                    currentX += rectWidth + 4.0F;
                }
            }

            dragging.setWidth(Math.max(0.0F, currentX - posX - 4.0F));
            dragging.setHeight(16.0F);
        }
    }

    private void drawShadow(float x, float y, float width, float height, int blur, int color) {
        RenderUtil.drawRoundedRectangle(x - blur, y - blur, width + blur * 2, height + blur * 2,
                new Vector4f(4.0F, 4.0F, 4.0F, 4.0F), color);
    }

    private String formatKey(String key) {
        if (key != null && !key.isEmpty() && !key.equals("None")) {
            if (key.toLowerCase().startsWith("mouse")) {
                String num = key.replaceAll("[^0-9]", "");
                if (!num.isEmpty()) {
                    return switch (Integer.parseInt(num)) {
                        case 0 -> "LMB";
                        case 1 -> "RMB";
                        case 2 -> "MMB";
                        case 3 -> "MB4";
                        case 4 -> "MB5";
                        default -> "MB" + num;
                    };
                }
            }

            key = key.replace("key.keyboard.", "");
            return switch (key.toLowerCase()) {
                case "left.shift", "shift" -> "Shift";
                case "left.control", "control" -> "Ctrl";
                case "left.alt", "alt" -> "Alt";
                case "space" -> "Space";
                case "caps.lock" -> "Caps";
                case "enter" -> "Enter";
                case "backspace" -> "Back";
                case "tab" -> "Tab";
                case "escape" -> "Esc";
                default -> key.length() == 1 ? key.toUpperCase() : key.substring(0, 1).toUpperCase() + key.substring(1).toLowerCase();
            };
        } else {
            return "None";
        }
    }
}