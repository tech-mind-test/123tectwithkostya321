package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
//import lombok.Setter;

@Getter
@Setter
public class EventGameUpdate implements Event {
    int smoothIntervalMS;
}
