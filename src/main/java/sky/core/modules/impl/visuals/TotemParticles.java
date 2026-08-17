package sky.core.modules.impl.visuals;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.events.EventPopTotem;
import sky.core.modules.Category;
import sky.core.modules.Module;
import mods.maseffects.TotemParticleSpawner;

public class TotemParticles extends Module {

    public TotemParticles() {
        super("TotemParticles", "Кастомные частицы тотема", Category.Visuals);
    }

    @EventTarget
    public void onPopTotem(EventPopTotem event) {
        if (event.getEntity() == null) {
            return;
        }
        TotemParticleSpawner.spawn(event.getEntity());
    }
}
