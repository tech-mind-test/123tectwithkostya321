package sky.core.modules.impl.player;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.network.play.client.CPlayerPacket;
import sky.core.events.EventPacket;
import sky.core.modules.Category;
import sky.core.modules.Module;

public class NoFall extends Module {

    public NoFall() {
        super("NoFall", "Уберает урон от падения", Category.Player);
    }

    @EventTarget
    public void onPacket(EventPacket e) {
        if (mc.player == null) return;

        if (e.getPacket() instanceof CPlayerPacket) {
            CPlayerPacket packet = (CPlayerPacket) e.getPacket();

            if (mc.player.fallDistance > 3.0F && !mc.player.isOnGround() && isBlockUnder()) {
                packet.setOnGround(true);
                mc.player.fallDistance = 0;
            }
        }
    }

    private boolean isBlockUnder() {
        if (mc.player.getPosY() < 0) return false;
        for (int offset = 0; offset < (int) mc.player.getPosY(); offset += 2) {
            if (!mc.world.isAirBlock(mc.player.getPosition().down(offset))) {
                return true;
            }
        }
        return false;
    }
}