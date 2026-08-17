package sky.core.modules.impl.combat;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.events.EventNoRotate;
import sky.core.events.EventPacket;
import sky.core.modules.Category;
import sky.core.modules.Module;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.util.math.vector.Vector2f;

public class NoServerDesync extends Module {

    public NoServerDesync() {
        super("NoServerDesync", "Не позволяет серверу крутить вас", Category.Combat);
    }

    private boolean isPacketSent;
    private Vector2f rotate;

    @EventTarget
    public void onPacket(EventPacket event) {
        if (isPacketSent) {
            if (event.getPacket() instanceof CPlayerPacket packet) {
                packet.setYaw(rotate.x);
                packet.setPitch(rotate.y);
                isPacketSent = false;
            }
        }
    }

    @EventTarget
    public void onEvent(EventNoRotate eventNoRotate) {
        rotate = new Vector2f(eventNoRotate.getYaw(), eventNoRotate.getPitch());
        isPacketSent = true;
        eventNoRotate.setCancelled(true);
    }
}
