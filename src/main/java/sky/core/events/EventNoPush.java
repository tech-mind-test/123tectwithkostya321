package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.Data;

@Data
public class EventNoPush extends EventCancellable implements Event {

    public final NoPushType noPushType;

    public enum NoPushType {
        Block, Water, Player, FishingRod
    }
}
