package sky.core.events;


import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.client.audio.ISound;

@Data
@AllArgsConstructor
public class EventSound extends EventCancellable implements Event {
    private ISound iSound;
    private float factor;
}