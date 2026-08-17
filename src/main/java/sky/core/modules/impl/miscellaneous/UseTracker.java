package sky.core.modules.impl.miscellaneous;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.mojang.datafixers.util.Pair;
import sky.core.events.EventPacket;
import sky.core.events.EventPopTotem;
import sky.core.utils.managers.impl.notificationmanager.NotificationManager;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.utils.math.TimeUtil;
import sky.core.utils.misc.ChatUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.network.play.server.SEntityEquipmentPacket;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;

import java.util.*;

public class UseTracker extends Module {
    private final BooleanSetting totem = new BooleanSetting("Отслеживать снесенные тотемы", true);
    private static final Map<UUID, Boolean> enchanted = new HashMap<>();
    private final Set<Integer> dropSet = new HashSet<>();
    private final TimeUtil timerEat = new TimeUtil();

    public UseTracker() {
        super("Use Tracker", "Оповещает об использовании еды/зелий другими игроками", Category.Miscellaneous);
        addSettings(totem);
    }

    @EventTarget
    public void onThrowPotion(EventPopTotem event) {
        if (!(event.getEntity() instanceof PlayerEntity player) || !totem.get()) return;

        ItemStack totemStack = player.getHeldItemOffhand().getItem() == Items.TOTEM_OF_UNDYING ? player.getHeldItemOffhand() : player.getHeldItemMainhand().getItem() == Items.TOTEM_OF_UNDYING ? player.getHeldItemMainhand() : new ItemStack(Items.TOTEM_OF_UNDYING);

        boolean isEnchanted = totemStack.isEnchanted();
        enchanted.put(player.getUniqueID(), isEnchanted);

        IFormattableTextComponent itemDisplayName = (IFormattableTextComponent) totemStack.getDisplayName();

        List<ITextComponent> tooltipLines = totemStack.getTooltip(player, ITooltipFlag.TooltipFlags.NORMAL);

        IFormattableTextComponent hoverText = new StringTextComponent("");
        for (int i = 0; i < tooltipLines.size(); i++) {
            ITextComponent line = tooltipLines.get(i);
            if (line instanceof IFormattableTextComponent) {
                hoverText.append(line);
            } else {
                hoverText.append(new StringTextComponent(line.getString()));
            }
            if (i < tooltipLines.size() - 1) hoverText.appendString("\n");
        }

        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText);
        IFormattableTextComponent message = new StringTextComponent("").append(new StringTextComponent(player.getName().getString()).setStyle(new StringTextComponent("").getStyle().applyFormatting(TextFormatting.WHITE))).append(new StringTextComponent(" потерял ").setStyle(new StringTextComponent("").getStyle().applyFormatting(TextFormatting.GRAY))).append(itemDisplayName).append(new StringTextComponent(", зачарован: ").setStyle(new StringTextComponent("").getStyle().applyFormatting(TextFormatting.GRAY))).append(new StringTextComponent("●").setStyle(new StringTextComponent("").getStyle().applyFormatting(isEnchanted ? TextFormatting.GREEN : TextFormatting.RED)));
        message.setStyle(message.getStyle().setHoverEvent(hoverEvent));

        ChatUtil.addText(message);
        NotificationManager.addNotification(new ResourceLocation("minecraft", "textures/item/totem_of_undying.png"), message, isEnchanted ? 0x00FF00 : 0xFF0000);
    }

    @EventTarget
    public void onPacket(EventPacket e) {
        if (!e.isReceive()) return;
        Object packet = e.getPacket();

        if (packet instanceof SAnimateHandPacket sAnimateHandPacket) {
            if (sAnimateHandPacket.getAnimationType() == 0) {
                dropSet.add(sAnimateHandPacket.getEntityID());
                timerEat.reset();
            }
        }
        if (packet instanceof SEntityEquipmentPacket sEntityEquipmentPacket) {
            int entityId = sEntityEquipmentPacket.getEntityID();

            if (dropSet.contains(entityId)) {
                if (timerEat.getTimePassed() < 300) {
                    return;
                }
                dropSet.remove(entityId);
            }

            Entity raw = mc.world.getEntityByID(entityId);
            if (!(raw instanceof LivingEntity living)) return;
            ItemStack before = living.getItemStackFromSlot(EquipmentSlotType.MAINHAND);
            for (Pair<EquipmentSlotType, ItemStack> entry : sEntityEquipmentPacket.func_241790_c_()) {
                if (entry.getFirst() != EquipmentSlotType.MAINHAND) continue;

                ItemStack after = entry.getSecond();
                if (after.isEmpty()) break;
                if (!isConsumeEvent(before, after, living)) break;

                IFormattableTextComponent itemDisplayName = (IFormattableTextComponent) after.getDisplayName();
                List<ITextComponent> tooltipLines = after.getTooltip(living instanceof PlayerEntity ? (PlayerEntity) living : null, ITooltipFlag.TooltipFlags.NORMAL);

                IFormattableTextComponent hoverText = new StringTextComponent("");
                for (int i = 0; i < tooltipLines.size(); i++) {
                    ITextComponent line = tooltipLines.get(i);
                    if (line instanceof IFormattableTextComponent) {
                        hoverText.append(line);
                    } else {
                        hoverText.append(new StringTextComponent(line.getString()));
                    }
                    if (i < tooltipLines.size() - 1) hoverText.appendString("\n");
                }

                HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText);
                IFormattableTextComponent message = new StringTextComponent("").append(new StringTextComponent(living.getName().getString()).setStyle(new StringTextComponent("").getStyle().applyFormatting(TextFormatting.WHITE))).append(new StringTextComponent(" использовал ").setStyle(new StringTextComponent("").getStyle().applyFormatting(TextFormatting.GRAY))).append(itemDisplayName);
                message.setStyle(message.getStyle().setHoverEvent(hoverEvent));

                ResourceLocation texture;
                if (after.getItem() == Items.GOLDEN_APPLE || after.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
                    texture = new ResourceLocation("minecraft", "textures/item/golden_apple.png");
                } else {
                    texture = new ResourceLocation("minecraft", "textures/item/" + after.getItem() + ".png");
                }

                int potionColor = PotionUtils.getColor(after);

                ChatUtil.addText(message);
                NotificationManager.addNotification(texture, message, potionColor);

                break;
            }
        }
    }

    private boolean isConsumeEvent(ItemStack before, ItemStack after, LivingEntity living) {
        return ItemStack.areItemsEqual(before, after) && after.getCount() < before.getCount() && living.isHandActive();
    }
}