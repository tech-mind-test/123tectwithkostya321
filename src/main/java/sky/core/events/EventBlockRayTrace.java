package sky.core.events;

import com.darkmagician6.eventapi.events.callables.EventCancellable;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class EventBlockRayTrace extends EventCancellable {
    private final BlockState state;
    private final BlockPos pos;
    public EventBlockRayTrace(BlockState state, BlockPos pos) {
        this.state = state;
        this.pos = pos;
    }
    public BlockState getBlockState() {
        return state;
    }
    public BlockPos getPos() {
        return pos;
    }
}