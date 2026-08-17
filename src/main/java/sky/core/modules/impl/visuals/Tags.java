package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.mojang.blaze3d.matrix.MatrixStack;
import other.bot.Bot;
import other.bot.BotManager;
import sky.core.SkyCore;
import sky.core.events.EventRender2D;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.MultiBooleanSetting;
import sky.core.modules.impl.miscellaneous.ScoreboardHealth;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.math.MathUtil;
import sky.core.utils.math.NumberUtil;
import sky.core.utils.misc.ServerUtil;
import sky.core.utils.misc.TargetUtil;
import sky.core.utils.player.PotionUtil;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.ProjectUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.Fonts;
import sky.core.utils.render.font.StyledFont;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.resources.I18n;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import org.joml.Vector2f;

import java.util.*;

public class Tags extends Module {

    private static final ResourceLocation STEVE_SKIN = new ResourceLocation("textures/entity/steve.png");

    public MultiBooleanSetting rendering = new MultiBooleanSetting("Отображать",
            new BooleanSetting("Игроков", true),
            new BooleanSetting("Монстров", false),
            new BooleanSetting("Друзей", false),
            new BooleanSetting("Животных", false),
            new BooleanSetting("Себя", false),
            new BooleanSetting("Жителей", false),
            new BooleanSetting("Голых", false),
            new BooleanSetting("Предметы", true));
    public BooleanSetting viewhead = new BooleanSetting("Отображать голову", true);
    public BooleanSetting vieweffects = new BooleanSetting("Отображать эффекты", false);
    public BooleanSetting viewarmor = new BooleanSetting("Отображать броню", false);
    public BooleanSetting viewenchants = new BooleanSetting("Отображать зачарования", false);
    public BooleanSetting viewitem = new BooleanSetting("Отображать левую руку", false);
    public BooleanSetting viewdisplayname = new BooleanSetting("Отображать визуальное имя предмета", false, () -> viewitem.get());
    public BooleanSetting viewitemicon = new BooleanSetting("Отображать иконку предмета", true);

    public Tags() {
        super("Tags", "Отображает имя и информацию об существах", Category.Visuals);
        addSettings(rendering, viewhead, viewitemicon, vieweffects, viewarmor, viewenchants, viewitem, viewdisplayname);
    }

    @EventTarget
    public void onRender(EventRender2D event) {
        final float partialTicks = mc.getRenderPartialTicks();
        final StyledFont fontNameCached = Fonts.sf_medium[13];
        final int rectBgColor = ColorUtil.getColor(0, 0, 0, 85);
        Bot bot = null;

        for (Bot bot1 : BotManager.allBots) {
            if (mc.renderViewEntity == bot1.connection.bot) {
                bot = bot1;
            }
        }

        for (Entity baseEntity : bot != null ? bot.connection.getWorld().getAllEntities() : mc.world.getAllEntities()) {
            if (!ProjectUtil.isInView(baseEntity)) continue;

            if (TargetUtil.isItemTarget(baseEntity, rendering) && baseEntity instanceof ItemEntity itemEntity) {
                Vector3d interpolatedItem = MathUtil.interpolate(itemEntity, partialTicks);
                AxisAlignedBB itemAabb = ProjectUtil.getEntityBox(itemEntity, interpolatedItem);
                Vector4f bounds = computeScreenBounds(itemAabb);
                renderItemTag(event, itemEntity, bounds, fontNameCached, rectBgColor);
                continue;
            }

            if (!(baseEntity instanceof LivingEntity entity)) continue;
            if (!entity.isAlive()) continue;

            boolean isTarget = isEntityTarget(entity);
            if (!isTarget) continue;

            Vector3d interpolated = MathUtil.interpolate(entity, partialTicks);
            AxisAlignedBB aabb = ProjectUtil.getEntityBox(entity, interpolated);
            Vector4f bounds = computeScreenBounds(aabb);
            renderEntityTag(event, entity, bounds, fontNameCached, rectBgColor);
        }
    }

    private boolean isEntityTarget(LivingEntity entity) {
        return TargetUtil.isSelfTarget(entity, rendering)
                || TargetUtil.isPlayerTarget(entity, rendering, true)
                || TargetUtil.isVillagerTarget(entity, rendering)
                || TargetUtil.isAnimalTarget(entity, rendering)
                || TargetUtil.isMonsterTarget(entity, rendering);
    }

    private Vector4f computeScreenBounds(AxisAlignedBB aabb) {
        Vector3d[] corners = ProjectUtil.getCorners(aabb);
        Vector2f min = new Vector2f(Float.MAX_VALUE, Float.MAX_VALUE);
        Vector2f max = new Vector2f(Float.MIN_VALUE, Float.MIN_VALUE);
        for (Vector3d corner : corners) {
            Vector2f projected = ProjectUtil.project2D(corner);
            if (projected.x < min.x) min.x = projected.x;
            if (projected.y < min.y) min.y = projected.y;
            if (projected.x > max.x) max.x = projected.x;
            if (projected.y > max.y) max.y = projected.y;
        }
        return new Vector4f(min.x, min.y, max.x, max.y);
    }

    private ResourceLocation getSkinTexture(PlayerEntity player) {
        if (player instanceof AbstractClientPlayerEntity) {
            return ((AbstractClientPlayerEntity) player).getLocationSkin();
        }
        return STEVE_SKIN;
    }

    private void renderItemTag(EventRender2D event, ItemEntity itemEntity, Vector4f b, StyledFont fontName, int rectColor) {
        ItemStack stack = itemEntity.getItem();
        ITextComponent nameComp = viewdisplayname.get() ? stack.getDisplayName() : stack.getItem().getDisplayName(stack);
        int count = stack.getCount();
        Rarity rarity = stack.getRarity();

        String plainName = nameComp.getString();
        IFormattableTextComponent nameBlockComponent = new StringTextComponent("").append(nameComp).mergeStyle(rarity.color);

        float fontHeight = fontName.getHeight();
        float centerX = (b.getX() + b.getZ()) / 2f;

        float rectHeight = 11f;
        float iconSize = rectHeight;
        float gap = 1f;
        float padding = 3f;

        float nameBlockWidth = fontName.getWidth(plainName) + padding * 2;

        boolean showCount = count > 1;
        String countText = "x" + count;
        float countBlockWidth = showCount ? fontName.getWidth(countText) + padding * 2 : 0;
        int countColor = ColorUtil.rgb(255, 68, 68);

        boolean showIcon = viewitemicon.get();

        float totalWidth = (showIcon ? iconSize + gap : 0) + nameBlockWidth + (showCount ? gap + countBlockWidth : 0);
        float startX = centerX - totalWidth / 2f;
        float tagY = b.getY() - rectHeight - 3f;
        float currentX = startX;

        if (showIcon) {
            RenderUtil.drawRoundedRectangle(currentX, tagY, iconSize, iconSize, 2, rectColor);
            RenderUtil.drawStack(stack, currentX + 1.5f, tagY + 1.5f, 0.5f);
            currentX += iconSize + gap;
        }

        RenderUtil.drawRoundedRectangle(currentX, tagY, nameBlockWidth, rectHeight, 2, rectColor);

        float nameTextX = currentX + padding;
        float nameTextY = tagY + (rectHeight - fontHeight) / 2f + 1f;
        fontName.drawText(event.getStack(), nameBlockComponent, nameTextX, nameTextY, -1);

        currentX += nameBlockWidth;

        if (showCount) {
            currentX += gap;
            RenderUtil.drawRoundedRectangle(currentX, tagY, countBlockWidth, rectHeight, 2, rectColor);

            float countTextX = currentX + padding;
            float countTextY = tagY + (rectHeight - fontHeight) / 2f + 1f;
            fontName.drawString(event.getStack(), countText, countTextX, countTextY, countColor);
        }
    }

    private void renderEntityTag(EventRender2D event, LivingEntity entity, Vector4f b, StyledFont fontName, int rectColor) {
        ITextComponent nameComponent = entity.getDisplayName();
        String plainName = TextFormatting.getTextWithoutFormattingCodes(nameComponent.getString());

        boolean isFriend = entity instanceof PlayerEntity
                && SkyCore.getInstance().getFriendManager().isFriend(((PlayerEntity) entity).getGameProfile().getName());

        boolean useScoreboard = SkyCore.getInstance().getModuleManager().getModule(ScoreboardHealth.class).isEnabled();
        float hp = useScoreboard ? ServerUtil.getHealth(entity) : entity.getHealth();
        int healthValue = Math.max(0, Math.round(hp));

        int finalRectColor = isFriend ? ColorUtil.getColor(0, 120, 0, 120) : rectColor;

        float centerX = (b.getX() + b.getZ()) / 2f;
        float fontHeight = fontName.getHeight();

        float rectHeight = 11f;
        float headSize = rectHeight;
        float gap = 0.5f;
        float padding = 3f;

        String strippedName = TextFormatting.getTextWithoutFormattingCodes(nameComponent.getString());
        String actualName = "";
        if (entity instanceof PlayerEntity playerEntity) {
            actualName = playerEntity.getGameProfile().getName();
        }

        IFormattableTextComponent nameBlockComponent = new StringTextComponent("").append(nameComponent);
        String nameBlockText = strippedName;

        if (isFriend) {
            nameBlockComponent
                    .append(new StringTextComponent(" [").mergeStyle(TextFormatting.GRAY))
                    .append(new StringTextComponent("F").mergeStyle(TextFormatting.GREEN))
                    .append(new StringTextComponent("]").mergeStyle(TextFormatting.GRAY));
            nameBlockText += " [F]";
        }

        float nameBlockWidth = fontName.getWidth(nameBlockText) + padding * 2;

        String hpText = String.valueOf(healthValue);
        float hpBlockWidth = fontName.getWidth(hpText) + padding * 2;
        int hpColor = ColorUtil.rgb(255, 68, 68);

        boolean showHead = viewhead.get() && entity instanceof PlayerEntity;
        float totalWidth = (showHead ? headSize + gap : 0) + nameBlockWidth + gap + hpBlockWidth;
        float startX = centerX - totalWidth / 2f;
        float tagY = b.getY() - rectHeight - 3f;
        float currentX = startX;

        if (showHead) {
            RenderUtil.drawRoundedRectangle(currentX, tagY, headSize, headSize, 2, finalRectColor);

            ResourceLocation skin = getSkinTexture((PlayerEntity) entity);
            RenderUtil.drawRoundedHead(
                    skin, entity,
                    currentX + 1.5f, tagY + 1.5f,
                    headSize - 3f, headSize - 3f,
                    1f, 1f
            );

            currentX += headSize + gap;
        }

        RenderUtil.drawRoundedRectangle(currentX, tagY, nameBlockWidth, rectHeight, 2, finalRectColor);

        float nameTextX = currentX + padding;
        float nameTextY = tagY + (rectHeight - fontHeight) / 2f + 1f;
        fontName.drawText(event.getStack(), nameBlockComponent, nameTextX, nameTextY, -1);

        currentX += nameBlockWidth + gap;

        RenderUtil.drawRoundedRectangle(currentX, tagY, hpBlockWidth, rectHeight, 2, finalRectColor);

        float hpTextX = currentX + padding;
        float hpTextY = tagY + (rectHeight - fontHeight) / 2f + 1f;
        fontName.drawString(event.getStack(), hpText, hpTextX, hpTextY, hpColor);

        if (viewarmor.get()) {
            drawArmorRow(entity, centerX, tagY);
        }

        float effectsBaseY = b.getW() + 6f;
        if (viewitem.get()) {
            effectsBaseY = drawHeldItems(event, entity, centerX, effectsBaseY, finalRectColor);
        }

        if (vieweffects.get()) {
            drawEffects(event, entity, centerX, effectsBaseY, Fonts.sf_regular[11]);
        }
    }

    private int getHealthColor(int health, float maxHealth) {
        float ratio = Math.min(1f, health / Math.max(1f, maxHealth));

        if (ratio > 0.66f) {
            float t = (ratio - 0.66f) / 0.34f;
            int r = (int) lerp(255, 0, t);
            int g = 255;
            return ColorUtil.rgb(r, g, 0);
        } else if (ratio > 0.33f) {
            float t = (ratio - 0.33f) / 0.33f;
            int g = (int) lerp(100, 255, t);
            return ColorUtil.rgb(255, g, 0);
        } else {
            float t = ratio / 0.33f;
            int g = (int) lerp(0, 100, t);
            return ColorUtil.rgb(255, g, 0);
        }
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * Math.max(0f, Math.min(1f, t));
    }

    private void drawArmorRow(LivingEntity entity, float centerX, float baseY) {
        List<ItemStack> armor = new ArrayList<>();
        entity.getArmorInventoryList().forEach(armor::add);

        ItemStack mainHand = entity.getHeldItemMainhand();
        ItemStack offHand = entity.getHeldItemOffhand();

        float y = baseY - 12;
        float totalWidth = 0;

        for (int i = 3; i >= 0; i--) if (!armor.get(i).isEmpty()) totalWidth += 9.5f;
        if (!mainHand.isEmpty()) totalWidth += 9.5f;
        if (!offHand.isEmpty()) totalWidth += 9.5f;

        float offsetX = -totalWidth / 2 + 3;

        for (int i = 0; i < 6; i++) {
            ItemStack stack = i == 0 ? offHand : i == 5 ? mainHand : armor.get(4 - i);
            if (!stack.isEmpty()) {
                if (viewenchants.get()) {
                    List<String> enchantments = getEnchantments(stack);
                    if (!enchantments.isEmpty()) {
                        float enchantY = y - 4;
                        for (String enchantment : enchantments) {
                            String plainText = TextFormatting.getTextWithoutFormattingCodes(enchantment);
                            float enchantWidth = Fonts.sf_medium[10].getWidth(plainText);
                            Fonts.sf_medium[10].drawString(new MatrixStack(), plainText, centerX + offsetX - enchantWidth / 2 + 5, enchantY, -1);
                            enchantY -= 6;
                        }
                    }
                }
                RenderUtil.drawStack(stack, centerX + offsetX, y, 0.5f);
                offsetX += 10.5f;
            }
        }
    }

    private float drawHeldItems(EventRender2D event, LivingEntity entity, float centerX, float startY, int rectColor) {
        float lineHeight = 10f;
        float y = startY;

        ItemStack main = entity.getHeldItemMainhand();
        if (!main.isEmpty()) {
            ITextComponent comp = viewdisplayname.get() ? main.getDisplayName() : main.getItem().getDisplayName(main);
            String text = comp.getString();
            float w = Fonts.sf_medium[12].getWidth(text);
            float x = centerX - w / 2f;
            RenderUtil.drawMinecraftRectangle(event.getStack(), x - 1.5f, y - 4, w + 3f, 10, rectColor);
            Fonts.sf_medium[12].drawText(event.getStack(), comp, x, y, -1);
            y += lineHeight;
        }

        ItemStack off = entity.getHeldItemOffhand();
        if (!off.isEmpty()) {
            ITextComponent comp = viewdisplayname.get() ? off.getDisplayName() : off.getItem().getDisplayName(off);
            String text = comp.getString();
            float w = Fonts.sf_medium[12].getWidth(text);
            float x = centerX - w / 2f;
            RenderUtil.drawMinecraftRectangle(event.getStack(), x - 1.5f, y - 3, w + 3f, 10, rectColor);
            Fonts.sf_medium[12].drawText(event.getStack(), comp, x, y + 1, -1);
            y += lineHeight;
        }

        return y;
    }

    private List<String> getEnchantments(ItemStack stack) {
        List<String> enchantments = new ArrayList<>();
        Map<Enchantment, Integer> enchantmentMap = EnchantmentHelper.getEnchantments(stack);

        for (Map.Entry<Enchantment, Integer> entry : enchantmentMap.entrySet()) {
            Enchantment enchantment = entry.getKey();
            String enchantName = I18n.format(enchantment.getName());
            String shortName = enchantName.length() >= 2 ? enchantName.substring(0, 2) : enchantName;
            TextFormatting color = TextFormatting.WHITE;
            if (enchantment.getMaxLevel() == 1 && entry.getValue() == 1) {
                enchantments.add(color + shortName);
            } else {
                enchantments.add(color + shortName + entry.getValue());
            }
        }

        return enchantments;
    }

    private void drawEffects(EventRender2D event, LivingEntity entity, float centerX, float startY, StyledFont fontEff) {
        Collection<EffectInstance> activeEffects = entity.getActivePotionEffects();
        if (activeEffects.isEmpty()) return;

        List<EffectLine> lines = new ArrayList<>(activeEffects.size());
        for (EffectInstance effectInstance : activeEffects) {
            int durationTicks = effectInstance.getDuration();
            if (durationTicks <= 20) continue;
            String key = Registry.EFFECTS.getKey(effectInstance.getPotion()).getPath();
            TextFormatting tf = PotionUtil.getEffectColor(key);
            Integer rgb = tf.getColor();
            int color = rgb != null ? (0xFF000000 | rgb) : 0xFFFFFFFF;

            String name = effectInstance.getPotion().getDisplayName().getString();
            int amp = effectInstance.getAmplifier() + 1;
            StringBuilder sb = new StringBuilder(name);
            if (amp > 1) sb.append(' ').append(NumberUtil.toRomanNumeral(amp));
            sb.append(' ').append(PotionUtil.formatDuration(durationTicks));
            String text = sb.toString();
            float width = fontEff.getWidth(text);
            lines.add(new EffectLine(text, width, color));
        }

        if (lines.size() > 1) {
            lines.sort(Comparator.comparingDouble(e -> e.width));
        }

        float lineStep = fontEff.getHeight() + 2f;
        for (int i = 0; i < lines.size(); i++) {
            EffectLine line = lines.get(i);
            float drawX = centerX - line.width / 2f;
            float drawY = startY + i * lineStep;
            fontEff.drawString(event.getStack(), line.text, drawX, drawY, line.color);
        }
    }

}

final class EffectLine {
    final String text;
    final float width;
    final int color;

    EffectLine(String text, float width, int color) {
        this.text = text;
        this.width = width;
        this.color = color;
    }
}
