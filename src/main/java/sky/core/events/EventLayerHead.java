package sky.core.events;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.*;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.entity.LivingEntity;
import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;


@Data
@AllArgsConstructor
public class EventLayerHead extends EventCancellable implements Event {
    private LivingEntity entity;
    private MatrixStack matrix;
    private EntityModel<?> model;
}
