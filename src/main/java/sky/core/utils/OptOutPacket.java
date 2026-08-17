package sky.core.utils;

import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CCustomPayloadPacket;
import net.minecraft.util.ResourceLocation;
import io.netty.buffer.Unpooled;

public class OptOutPacket {
    public static final ResourceLocation CHANNEL = new ResourceLocation("marlow", "mco");

    public CCustomPayloadPacket getRawPacket() {
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        return new CCustomPayloadPacket(CHANNEL, buffer);
    }
}