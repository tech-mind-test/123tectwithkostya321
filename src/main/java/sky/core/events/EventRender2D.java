package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class EventRender2D extends EventCancellable implements Event {
    private MatrixStack Stack;
    private float partialTicks;

    public static class Pre extends EventRender2D {
        public Pre(MatrixStack stack, float partialTicks) {
            super(stack, partialTicks);
        }
    }

    public static class Post extends EventRender2D {
        public Post(MatrixStack stack, float partialTicks) {
            super(stack, partialTicks);
        }
    }

    public static class Send extends EventRender2D {
        public Send(MatrixStack stack, float partialTicks) {
            super(stack, partialTicks);
        }
    }
}
