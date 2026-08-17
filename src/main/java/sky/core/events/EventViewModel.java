package sky.core.events;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.HandSide;
import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;

@Data
@AllArgsConstructor
public class EventViewModel extends EventCancellable implements Event {
    private MatrixStack matrixStack;
    private HandSide handside;
}
