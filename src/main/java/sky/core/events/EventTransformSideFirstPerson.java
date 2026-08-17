package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.util.HandSide;

@Data
@AllArgsConstructor
public class EventTransformSideFirstPerson implements Event {
    private HandSide handSide;
    private float equippedProg;
}