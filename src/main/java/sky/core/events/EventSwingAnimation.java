package sky.core.events;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.util.Hand;
import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;

@Data
@AllArgsConstructor
public class EventSwingAnimation extends EventCancellable implements Event {
    private AbstractClientPlayerEntity player;
    private float swingProgress;
    private Hand hand;
    private MatrixStack matrixStack;
}