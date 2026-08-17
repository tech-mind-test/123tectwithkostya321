package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;

@Data
@AllArgsConstructor
public class EventRenderChunk extends EventCancellable implements Event {
    private ChunkRenderDispatcher.ChunkRender chunkRender;
}
