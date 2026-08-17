package sky.core.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.IPacket;
import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;


@Data
@AllArgsConstructor
public class EventPacket extends EventCancellable implements Event {
    private final IPacket<?> packet;
    private final PacketType packetType;

    public enum PacketType {
        RECEIVE, SEND
    }

    public boolean isReceive() {
        return this.packetType == PacketType.RECEIVE;
    }

    public boolean isSend() {
        return this.packetType == PacketType.SEND;
    }
}