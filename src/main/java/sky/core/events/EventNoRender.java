package sky.core.events;

import com.darkmagician6.eventapi.events.Event;
import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.Data;

@Data
public class EventNoRender extends EventCancellable implements Event {
    public final NorenderType norenderType;

    public enum NorenderType {
        fire, bossbar, scoreboard, title, totem, hurttime, fishingrod,
        destroyparticles, rain, shadows, smoke, vignette, arrows, holograms,
        grass, health, players, bad_effects, underwater_blur, container_background,
        lava, tab, cameraclip, glowing
    }
}
