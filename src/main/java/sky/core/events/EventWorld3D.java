package sky.core.events;

import com.darkmagician6.eventapi.events.callables.EventCancellable;
import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Data;
import com.darkmagician6.eventapi.events.Event;
import lombok.Getter;
import net.minecraft.util.math.vector.Matrix4f;

@Data
@Getter
public class EventWorld3D extends EventCancellable implements Event {
    public float partialTicks;
    public MatrixStack matrixStack;
    public Matrix4f matrix;

    public EventWorld3D(float partialTicks, MatrixStack matrixStack, Matrix4f matrix) {
        this.partialTicks = partialTicks;
        this.matrixStack = matrixStack;
        this.matrix = matrix;
    }




}
