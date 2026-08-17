package sky.core.events;

import com.darkmagician6.eventapi.events.callables.EventCancellable;
import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.AllArgsConstructor;
import lombok.Data;
import com.darkmagician6.eventapi.events.Event;
import net.minecraft.util.math.vector.Matrix4f;

@Data
@AllArgsConstructor
public class EventRender3D extends EventCancellable implements Event {
    float partialTicks;
    public MatrixStack matrixStack;
}
