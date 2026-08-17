package sky.core.modules.impl.miscellaneous;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.events.EventPacket;
import sky.core.modules.Category;
import sky.core.modules.Module;
import net.minecraft.network.play.client.CResourcePackStatusPacket;
import net.minecraft.network.play.server.SSendResourcePackPacket;

public class SRPSpoofer extends Module {

    public SRPSpoofer() {
        super("SRPSpoof", "Подменяет данные о том, что установлен серверный ресурс-пак", Category.Miscellaneous);
    }

    @EventTarget
    public void onPacketReceive(EventPacket e) {
        if ((e.getPacket() instanceof SSendResourcePackPacket) && e.isReceive()) {
            mc.player.connection.sendPacket(new CResourcePackStatusPacket(CResourcePackStatusPacket.Action.ACCEPTED));
            mc.player.connection.sendPacket(new CResourcePackStatusPacket(CResourcePackStatusPacket.Action.SUCCESSFULLY_LOADED));
            e.setCancelled(true);
        }
    }
}