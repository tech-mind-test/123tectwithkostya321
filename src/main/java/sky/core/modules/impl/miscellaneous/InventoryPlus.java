package sky.core.modules.impl.miscellaneous;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.events.EventPacket;
import sky.core.modules.Category;
import sky.core.modules.Module;
import net.minecraft.network.play.client.CCloseWindowPacket;

public class InventoryPlus extends Module {

    public InventoryPlus() {
        super("Xcarry", "Добовляет дополнительные 4 слота в инвентаре, увеличивая его вместимость", Category.Miscellaneous);
    }

    @EventTarget
    public void onEvent(EventPacket event) {
        if (mc.player == null) return;

        if (event.getPacket() instanceof CCloseWindowPacket) {
            event.setCancelled(true);
        }
    }
}
