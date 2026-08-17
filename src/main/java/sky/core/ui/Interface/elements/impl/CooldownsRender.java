package sky.core.ui.Interface.elements.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import sky.core.events.EventRender2D;
import sky.core.utils.managers.impl.dragmanager.Dragging;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.ui.Interface.elements.ElementRender;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.animation.AnimationUtil;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.ScissorUtil;
import sky.core.utils.render.font.Fonts;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;

import java.util.*;

@RequiredArgsConstructor
public class CooldownsRender implements ElementRender {

    private final Dragging dragging;
    private final AnimationUtil alphaAnimation = new AnimationUtil(0.0f, 10);
    private final Map<Item, AnimationUtil[]> itemAnimations = new HashMap<>();
    private final Map<Item, String> lastCooldownTexts = new HashMap<>();
    private final Map<Item, Float> lastCooldowns = new HashMap<>();
    public static BooleanSetting alphabg = new BooleanSetting("Прозрачный фон", false);
    private final AnimationUtil widthAnimation = new AnimationUtil(0.0f, 15);
    private final AnimationUtil heightAnimation = new AnimationUtil(0.0f, 15);

    @Override
    public void render(EventRender2D.Post event) {
        MatrixStack ms = event.getStack();
        float posX = dragging.getX(), posY = dragging.getY();
        List<Item> activeItems = getActiveItems();

        alphaAnimation.update(mc.currentScreen instanceof ChatScreen || !activeItems.isEmpty() ? 1.0f : 0.0f);
        float globalAlpha = alphaAnimation.getValue();

        Map<Item, Float> itemCooldowns = new HashMap<>();
        for (int i = 0; i < mc.player.inventory.getSizeInventory(); i++) {
            ItemStack itemStack = mc.player.inventory.getStackInSlot(i);
            if (!itemStack.isEmpty()) {
                Item item = itemStack.getItem();
                float cooldownSeconds = mc.player.getCooldownTracker().getCustomCooldown(item, mc.getRenderPartialTicks());
                if (cooldownSeconds > 0) {
                    itemCooldowns.put(item, cooldownSeconds);
                    lastCooldownTexts.put(item, formatCooldown(cooldownSeconds));
                    lastCooldowns.put(item, cooldownSeconds);
                }
            }
        }

        int textColor = ColorUtil.applyOpacity(0xFFFFFFFF, globalAlpha);
        int darkBgColor = ColorUtil.darken(ThemeEditor.getColor(ThemeSettings.LOGO), 0.1f);

        List<Item> dimsItems = new ArrayList<>(activeItems);
        dimsItems.addAll(itemAnimations.keySet());
        float[] dimensions = calculateDimensions(dimsItems, itemCooldowns);
        float width = dimensions[0];

        RenderUtil.drawBlurredRoundedRectangle(posX, posY, widthAnimation.getValue(), heightAnimation.getValue(), 3, alphabg.get() ? ColorUtil.applyOpacity(darkBgColor, 0) : darkBgColor, globalAlpha);
//        RenderUtil.drawOutlineRectangle(posX, posY, widthAnimation.getValue(), heightAnimation.getValue(), 3, ThemeEditor.getColor(ThemeSettings.OUTLINE), ThemeEditor.getAlpha(ThemeSettings.OUTLINE));
        Fonts.icons[16].drawString(ms, "T", posX - 11.5f + widthAnimation.getValue(), posY + 6.5f, textColor);
        Fonts.sf_medium[15].drawString(ms, "Cooldowns", posX + 3.5f, posY + 5.5f, textColor);

        float baseItemY = posY + 12.5f;
        List<Item> toRemove = new ArrayList<>();

        for (int i = 0; i < activeItems.size(); i++) {
            Item item = activeItems.get(i);
            int finalI = i;
            AnimationUtil[] anims = itemAnimations.computeIfAbsent(item, k -> new AnimationUtil[]{new AnimationUtil(0.0f, 10), new AnimationUtil(-5, 10), new AnimationUtil(finalI * 10, 15)});
            anims[0].update(1.0f);
            anims[1].update(0.0f);
            float targetY = i * 10;
            anims[2].update(targetY);
        }

        for (var entry : itemAnimations.entrySet()) {
            if (!activeItems.contains(entry.getKey())) {
                AnimationUtil[] anims = entry.getValue();
                anims[0].update(0.0f);
                anims[1].update(-5);
                if (anims[0].isDone() && anims[1].isDone()) {
                    toRemove.add(entry.getKey());
                }
            }
        }

        toRemove.forEach(item -> {
            itemAnimations.remove(item);
            lastCooldownTexts.remove(item);
            lastCooldowns.remove(item);
        });

        ScissorUtil.start(posX, posY, widthAnimation.getValue(), heightAnimation.getValue());
        for (var entry : itemAnimations.entrySet()) {
            Item item = entry.getKey();
            AnimationUtil[] anims = entry.getValue();
            float itemAlpha = globalAlpha * anims[0].getValue();
            if (itemAlpha <= 0.01f && anims[0].isDone()) continue;

            float itemY = baseItemY + anims[2].getValue();
            float cooldown = itemCooldowns.getOrDefault(item, 0f);
            String nameText = new ItemStack(item).getDisplayName().getString();
            String cooldownText = itemCooldowns.containsKey(item) ? formatCooldown(cooldown) : lastCooldownTexts.getOrDefault(item, "");
            float cooldownWidth = Fonts.sf_medium[13].getWidth(cooldownText);
            String path = Registry.ITEM.getKey(item).getPath();
            String texturePath = "enchanted_golden_apple".equals(path) ? "golden_apple" : "crossbow".equals(path) ? "crossbow_arrow" : path;
            ResourceLocation texture = new ResourceLocation(Registry.ITEM.getKey(item).getNamespace(), "textures/item/" + texturePath + ".png");
            int animatedTextColor = ColorUtil.applyOpacity(0xFFFFFFFF, itemAlpha);
            int animatedRectColor = ColorUtil.applyOpacity(ThemeEditor.getColor(ThemeSettings.SEPARATOR), ThemeEditor.getAlpha(ThemeSettings.SEPARATOR) / 255f * itemAlpha);

            Fonts.sf_medium[13].drawString(ms, nameText, posX + 3.5f + 5.0f + 2.5f + 2, itemY + 4.5f, animatedTextColor);
            Fonts.sf_medium[13].drawString(ms, cooldownText, posX + 3.5f + widthAnimation.getValue() - 7.5f - cooldownWidth, itemY + 4.5f, animatedTextColor);
            RenderUtil.drawMinecraftRectangle(ms, posX + 10.5f, itemY + 3, 0.5f, 5.5f, animatedRectColor);
            RenderUtil.drawImage2D(texture, posX + 2.5f, itemY + 2, 7, 7, ColorUtil.applyOpacity(-1, itemAlpha));
        }
        ScissorUtil.end();

        widthAnimation.update(width);
        heightAnimation.update(15 + activeItems.size() * 10);
        dragging.setHeight(heightAnimation.getValue());
        dragging.setWidth(widthAnimation.getValue());
    }

    private List<Item> getActiveItems() {
        Set<Item> activeSet = new HashSet<>();
        for (int i = 0; i < mc.player.inventory.getSizeInventory(); i++) {
            ItemStack itemStack = mc.player.inventory.getStackInSlot(i);
            if (!itemStack.isEmpty()) {
                Item item = itemStack.getItem();
                float cooldown = mc.player.getCooldownTracker().getCustomCooldown(item, mc.getRenderPartialTicks());
                if (cooldown > 0) {
                    activeSet.add(item);
                }
            }
        }
        return new ArrayList<>(activeSet);
    }

    private float[] calculateDimensions(List<Item> dimsItems, Map<Item, Float> itemCooldowns) {
        float maxNameWidth = 0, maxCooldownWidth = 0;
        for (Item item : dimsItems) {
            maxNameWidth = Math.max(maxNameWidth, Fonts.sf_medium[13].getWidth(new ItemStack(item).getDisplayName().getString()));
            String cooldownStr = itemCooldowns.containsKey(item) ? formatCooldown(itemCooldowns.get(item)) : lastCooldownTexts.getOrDefault(item, "");
            maxCooldownWidth = Math.max(maxCooldownWidth, Fonts.sf_medium[13].getWidth(cooldownStr));
        }
        float width = Math.max(62, 3.5f + maxNameWidth + 3.5f + maxCooldownWidth + 3.5f + 1.0f + 6 + 3.5f);
        return new float[]{width};
    }

    private String formatCooldown(float cooldown) {
        int minutes = (int) (cooldown / 60);
        int seconds = (int) (cooldown % 60);
        StringBuilder sb = new StringBuilder();
        if (minutes > 0) {
            sb.append(minutes).append("м");
        }
        if (seconds > 0 || minutes == 0) {
            sb.append(seconds).append("с");
        }
        return sb.toString();
    }
}