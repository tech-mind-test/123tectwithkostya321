package sky.core.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import net.minecraft.client.gui.screen.Screen;
import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;

@Data
@AllArgsConstructor
public class EventMouseClicked extends EventCancellable implements Event {
    private final int key;
    private final float mouseX, mouseY;
}