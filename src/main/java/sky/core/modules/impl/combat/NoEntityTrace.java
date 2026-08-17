package sky.core.modules.impl.combat;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.events.EventEntityRayTrace;
import sky.core.modules.Category;
import sky.core.modules.Module;

public class NoEntityTrace extends Module {

    public NoEntityTrace() {
        super("No Entity Trace", "Отключает взаимодействие с игроками" ,Category.Combat);
    }

    @EventTarget
    public void onEvent(EventEntityRayTrace event) {
        event.isCancelled();
    }
}
