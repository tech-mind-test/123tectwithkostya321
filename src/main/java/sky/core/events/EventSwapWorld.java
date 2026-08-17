package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import net.minecraft.world.IWorld;

@Data
@AllArgsConstructor
public class EventSwapWorld extends EventCancellable implements Event {
    public State state;

    public enum State {
        LOAD,
        CHANGED
    }
}
