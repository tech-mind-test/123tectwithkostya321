package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.renderer.ActiveRenderInfo;

@Getter
@AllArgsConstructor
public class EventTest implements Event {

    private final MatrixStack matrixStack;

    private final float partialTicks;

    private final ActiveRenderInfo renderInfo;

}