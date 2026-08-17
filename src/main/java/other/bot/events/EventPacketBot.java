/* Decompiler 44ms, total 290ms, lines 49 */
package other.bot.events;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import net.minecraft.network.IPacket;

public class EventPacketBot extends EventCancellable implements Event {
    private IPacket<?> packet;
    private EventPacketBot.Type type;

    public boolean isSend() {
        return this.type == EventPacketBot.Type.SEND;
    }

    public boolean isReceive() {
        return this.type == EventPacketBot.Type.RECEIVE;
    }

    public void setPacket(IPacket<?> packet) {
        this.packet = packet;
    }

    public void setType(EventPacketBot.Type type) {
        this.type = type;
    }

    public IPacket<?> getPacket() {
        return this.packet;
    }

    public EventPacketBot.Type getType() {
        return this.type;
    }

    public EventPacketBot(IPacket<?> packet, EventPacketBot.Type type) {
        this.packet = packet;
        this.type = type;
    }

    public static enum Type {
        RECEIVE,
        SEND;

        // $FF: synthetic method
        private static EventPacketBot.Type[] $values() {
            return new EventPacketBot.Type[]{RECEIVE, SEND};
        }
    }
}