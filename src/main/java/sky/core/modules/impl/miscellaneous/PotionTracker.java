package sky.core.modules.impl.miscellaneous;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.events.EventThrowPotion;
import sky.core.utils.managers.impl.notificationmanager.NotificationManager;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.utils.math.NumberUtil;
import sky.core.utils.misc.ChatUtil;
import sky.core.utils.render.ColorUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.HoverEvent;

import java.util.ArrayList;
import java.util.List;

public class PotionTracker extends Module {
    private final BooleanSetting ignoreself = new BooleanSetting("Игнорировать себя", false);

    public PotionTracker() {
        super("Potion Tracker", "У данного модуля нет описания!", Category.Miscellaneous);
        addSettings(ignoreself);
    }

    @EventTarget
    public void onThrowPotion(EventThrowPotion event) {
        if (ignoreself.get() && event.getEntity() instanceof PlayerEntity && event.getEntity().equals(mc.player)) {
            return;
        }

        int totalReceivedDuration = 0;
        int totalMaxDuration = 0;

        for (EffectInstance effect : event.getEffects()) {
            totalReceivedDuration += (int) (effect.getDuration() * event.getDistanceFactor());
            totalMaxDuration += effect.getDuration();
        }

        int receivedSeconds = totalReceivedDuration / 20;
        int maxSeconds = totalMaxDuration / 20;
        int successPercentage = (maxSeconds > 0) ? (receivedSeconds * 100) / maxSeconds : 100;

        StringBuilder effectsList = new StringBuilder();
        for (EffectInstance effect : event.getEffects()) {
            String effectName = effect.getPotion().getDisplayName().getString();
            int duration = (int) (effect.getDuration() * event.getDistanceFactor());
            int minutes = (duration / 20) / 60;
            int seconds = (duration / 20) % 60;
            String romanAmplifier = NumberUtil.toRomanNumeral(effect.getAmplifier() + 1);

            effectsList.append(TextFormatting.GRAY).append("● ").append(TextFormatting.RED).append(effectName).append(" ").append(romanAmplifier).append(TextFormatting.GRAY).append(" ").append(String.format("%d:%02d", minutes, seconds)).append("\n");
        }

        if (effectsList.length() > 1) {
            effectsList.setLength(effectsList.length() - 1);
        }

        ItemStack stack = event.getItemStack();
        List<ITextComponent> tooltipLines = new ArrayList<>();

        tooltipLines.add(stack.getDisplayName().deepCopy());
        PotionUtils.addPotionTooltip(stack, tooltipLines, 1.0F);

        IFormattableTextComponent hoverText = new StringTextComponent("");
        for (int i = 0; i < tooltipLines.size(); i++) {
            hoverText.append(tooltipLines.get(i));
            if (i < tooltipLines.size() - 1) hoverText.appendString("\n");
        }

        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText);

        IFormattableTextComponent chatMessage = new StringTextComponent("").append(new StringTextComponent(event.getEntity().getName().getString()).setStyle(new StringTextComponent("").getStyle().applyFormatting(TextFormatting.GRAY))).append(new StringTextComponent(" получил эффекты из ").setStyle(new StringTextComponent("").getStyle().applyFormatting(TextFormatting.GRAY))).append(event.getItemStack().getDisplayName()).append(new StringTextComponent("\n").setStyle(new StringTextComponent("").getStyle().applyFormatting(TextFormatting.GRAY))).append(new StringTextComponent("● Успешность ").setStyle(new StringTextComponent("").getStyle().applyFormatting(TextFormatting.GRAY))).append(new StringTextComponent(successPercentage + "%").setStyle(new StringTextComponent("").getStyle().setColor(Color.fromInt(ColorUtil.interpolateSuccessColor(successPercentage))))).append(new StringTextComponent("\n" + effectsList).setStyle(new StringTextComponent("").getStyle().applyFormatting(TextFormatting.GRAY)));

        chatMessage.setStyle(chatMessage.getStyle().setHoverEvent(hoverEvent));

        ChatUtil.addText(chatMessage);

        IFormattableTextComponent notificationMessage = new StringTextComponent("").append(new StringTextComponent(event.getEntity().getName().getString()).setStyle(new StringTextComponent("").getStyle().applyFormatting(TextFormatting.WHITE))).append(new StringTextComponent(" получил эффекты из '").setStyle(new StringTextComponent("").getStyle().applyFormatting(TextFormatting.WHITE))).append(((IFormattableTextComponent) stack.getDisplayName()).setStyle(new StringTextComponent("").getStyle().applyFormatting(TextFormatting.WHITE))).append(new StringTextComponent("'\n").setStyle(new StringTextComponent("").getStyle().applyFormatting(TextFormatting.RESET))).append(new StringTextComponent(" " + successPercentage + "%").setStyle(new StringTextComponent("").getStyle().setColor(Color.fromInt(ColorUtil.interpolateSuccessColor(successPercentage)))));

        int potionColor = PotionUtils.getColor(stack);
           NotificationManager.addNotification(new ResourceLocation("minecraft", "textures/item/splash_potion.png"), notificationMessage, potionColor);
    }
}