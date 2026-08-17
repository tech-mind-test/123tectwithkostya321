package sky.core.modules.impl.player;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.events.EventBlockDamage;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.SliderSetting;

public class FastBreak extends Module {
    private SliderSetting breakspeed = new SliderSetting("Скорость", 0.8f, 0.1f, 1.0f, 0.1F);

    public FastBreak() {
        super("FastBreak", "Позволяет быстрее ломать блоки", Category.Player);
        addSettings(breakspeed);
    }

    @EventTarget
    public void onUpdate(EventBlockDamage e) {
        mc.playerController.blockHitDelay = 0;
        if (mc.playerController.curBlockDamageMP > breakspeed.get()) {
            mc.playerController.curBlockDamageMP = 1F;
        }
    }
}
