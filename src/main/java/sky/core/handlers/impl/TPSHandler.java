package sky.core.handlers.impl;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.events.EventPacket;
import lombok.Getter;
import net.minecraft.network.play.server.SUpdateTimePacket;
import net.minecraft.util.math.MathHelper;

public class TPSHandler {
    @Getter
    private static float adjustTicks = 0;
    long timestamp;
    @Getter
    private static float TPS = 20;

    @EventTarget
    private void onPacket(EventPacket e) {
        if (e.getPacket() instanceof SUpdateTimePacket) updateTPS();
    }

    private void updateTPS() {
        long delay = System.nanoTime() - timestamp;
        float maxTPS = 20;
        float rawTPS = maxTPS * (1e9f / delay);
        float boundedTPS = MathHelper.clamp(rawTPS, 0, maxTPS);
        TPS = Math.round(boundedTPS * 2) / 2.0f;
        adjustTicks = boundedTPS - maxTPS;
        timestamp = System.nanoTime();
    }
}
