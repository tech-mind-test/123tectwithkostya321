package sky.core.modules.impl.player;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.vector.Vector3d;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TestFunction extends Module {


    public TestFunction() {
        super("TestFunction", "Тест Функция", Category.Player);
    }

    private final Map<UUID, Vector3d> lastPearlPos = new HashMap<>();

    @EventTarget
    public void update(EventUpdate eventUpdate) {
        boolean hasEffects = (mc.player.isPotionActive(Effects.LEVITATION));
        if (hasEffects) {
            mc.player.removePotionEffect(Effects.LEVITATION);
        }

    }
}
