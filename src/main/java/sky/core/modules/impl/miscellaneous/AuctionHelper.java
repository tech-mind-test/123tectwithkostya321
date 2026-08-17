package sky.core.modules.impl.miscellaneous;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.adl.nativeprotect.Native;
import sky.core.events.EventContainerRender;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.ColorSetting;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.modules.impl.player.AutoBuy;
import sky.core.utils.animation.AnimationUtil;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.RenderUtil;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AuctionHelper extends Module {
    private final ModeSetting mode = new ModeSetting("Режим", "HolyWorld", "HolyWorld");
    private final ColorSetting color = new ColorSetting("Цвет выгодного предмета", true, ColorUtil.hex("#53FF00"));
    private final SliderSetting amount = new SliderSetting("Количество выгодных предметов", 3, 1, 5, 1);
    private final AnimationUtil anim = new AnimationUtil(0.0f, 4);
    private boolean animUp = true;

    public AuctionHelper() {
        super("Auction Helper", "Отображает самый выгодный предмет на аукционе", Category.Miscellaneous);
        addSettings(mode, color, amount);
    }

    @Native
    @EventTarget
    public void onEvent(EventContainerRender.Pre event) {
        if (!(mc.currentScreen instanceof ContainerScreen) || !mode.is("HolyWorld")) return;
        if (!mc.currentScreen.getTitle().getString().contains("Аукцион")) return;

        float target = animUp ? 0 : 1;
        anim.update(target);
        if (anim.isDone()) animUp = !animUp;
        int alpha = (int) (105 + anim.getValue() * 120);

        List<AuctionHelper.PriceSlot> priced = new ArrayList<>();
        for (Slot slot : event.getContainer().inventorySlots) {
            if (slot == null || !slot.getHasStack()) continue;
            ItemStack stack = slot.getStack();
            long price = AutoBuy.extractPrice(stack);
            if (price > 0) priced.add(new AuctionHelper.PriceSlot(slot, price));
        }

        if (priced.isEmpty()) return;

        priced.sort(Comparator.comparingLong(ps -> ps.price));
        int toHighlight = (int) Math.min(priced.size(), amount.get());
        int highlightColor = ColorUtil.applyOpacity(color.get(), alpha);

        for (int i = 0; i < toHighlight; i++) {
            Slot slot = priced.get(i).slot;
            float rx = event.getGuiLeft() + slot.xPos;
            float ry = event.getGuiTop() + slot.yPos;
            RenderUtil.drawMinecraftRectangle(event.getStack(), rx, ry, 16, 16, highlightColor);
        }
    }

    private static class PriceSlot {
        final Slot slot;
        final long price;

        PriceSlot(Slot slot, long price) {
            this.slot = slot;
            this.price = price;
        }
    }
}
