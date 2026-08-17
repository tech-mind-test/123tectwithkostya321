package sky.core.events;

import lombok.Data;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
@Data
public class EventPlaceBlock extends EventCancellable implements Event {
    private final Block block;
    private final BlockPos pos;
}

