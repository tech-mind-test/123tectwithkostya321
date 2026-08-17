package sky.core.modules.impl.movement;


import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;

public class NoJumpDelay extends Module {

    public NoJumpDelay(){
        super("NoJumpDelay", "Убирает задержку между прыжками, позволяя прыгать быстрее", Category.Movement);
    }

    @EventTarget
    public void onUpdate(EventUpdate e) {
        mc.player.jumpTicks = 0;
    }
}