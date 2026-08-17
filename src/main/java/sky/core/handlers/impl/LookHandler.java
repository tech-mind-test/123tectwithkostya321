package sky.core.handlers.impl;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.utils.component.Component;
import sky.core.events.EventLook;
import sky.core.events.EventRotation;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector2f;

public class LookHandler extends Component {

    @Setter
    @Getter
    private static boolean active;
    @Getter @Setter
    private static float freeYaw, freePitch;


    @EventTarget
    private void onLook(EventLook e) {
        if (active) {
            rotateTowards(e.yaw, e.pitch);
            e.setCancelled(true);
        }
    }

    @EventTarget
    private void onRotation(EventRotation e) {
        if (active) {
            e.setRotation(new Vector2f(freeYaw, freePitch));
        } else {
            freeYaw = e.getRotation().x;
            freePitch = e.getRotation().y;
        }
    }

    private void rotateTowards(double targetYaw, double targetPitch) {
        freePitch = MathHelper.clamp((float) (freePitch + targetPitch * 0.15D), -90.0F, 90.0F);
        freeYaw = (float) (freeYaw + targetYaw * 0.15D);
    }
}
