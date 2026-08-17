package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.item.ItemStack;

public class EventRenderTooltip extends EventCancellable implements Event {
    public final MatrixStack matrixStack;
    public final ItemStack stack;
    public final int mouseX;
    public final int mouseY;

    public EventRenderTooltip(MatrixStack matrixStack, ItemStack stack, int mouseX, int mouseY) {
        this.matrixStack = matrixStack;
        this.stack = stack;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }
} 