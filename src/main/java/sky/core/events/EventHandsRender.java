package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.client.renderer.ActiveRenderInfo;
@Data
@AllArgsConstructor
public class EventHandsRender extends EventCancellable implements Event {
    private ActiveRenderInfo activeRenderInfo;
    private MatrixStack stack;
    private float part;

    public static class Pre extends EventHandsRender {

        public Pre(ActiveRenderInfo activeRenderInfo, MatrixStack stack, float part) {
            super(activeRenderInfo, stack, part);
        }
    }
    public static class Post extends EventHandsRender {
        public Post(ActiveRenderInfo activeRenderInfo, MatrixStack stack, float part) {
            super(activeRenderInfo, stack, part);
        }
    }
}
