package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class EventChat implements Event {
    private String message;
    private boolean cancelled;

    public EventChat(String message) {
        this.message = message;
        this.cancelled = false;
    }
}