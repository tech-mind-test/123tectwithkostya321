package sky.core.ui.screens;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Slot;
import sky.core.modules.impl.combat.AutoSwap;
import sky.core.utils.Wrapper;


public class TripleAutoSwapInventorySelectScreen extends InventoryScreen implements Wrapper {

    private final AutoSwap module;
    private final int slotIndex;
    private final Screen back;

    public TripleAutoSwapInventorySelectScreen(AutoSwap module, int slotIndex, Screen back) {
        super(playerOrNull());
        this.module = module;
        this.slotIndex = slotIndex;
        this.back = back;
    }

    private static PlayerEntity playerOrNull() {
        return net.minecraft.client.Minecraft.getInstance().player;
    }



    @Override
    protected void handleMouseClick(Slot slotIn, int slotId, int mouseButton, ClickType type) {
        if (slotIn != null && slotIn.getHasStack()) {
         //   module.setSlotStack(slotIndex, slotIn.getStack());
            mc.displayGuiScreen(back);
            return;
        }
        super.handleMouseClick(slotIn, slotId, mouseButton, type);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
