package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockRayTraceResult;
@Data
@AllArgsConstructor
public class EventClickBlockRight extends EventCancellable implements Event {
    private final ClientPlayerEntity player;
    private final ClientWorld world;
    private final Hand hand;
    private final BlockRayTraceResult result;
}
