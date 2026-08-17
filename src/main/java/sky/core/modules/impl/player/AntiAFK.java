package sky.core.modules.impl.player;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.adl.nativeprotect.Native;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.MultiBooleanSetting;
import sky.core.utils.math.TimeUtil;
import sky.core.utils.player.MoveUtil;
import net.minecraft.util.Hand;
import org.apache.commons.lang3.RandomStringUtils;


public class AntiAFK extends Module {
    private final MultiBooleanSetting actions = new MultiBooleanSetting("Действия", new BooleanSetting("Прыжок", false), new BooleanSetting("Команда", true), new BooleanSetting("Качание рукой", false));

    private final TimeUtil timer = new TimeUtil();

    public AntiAFK() {
        super("AntiAFK", "Предотвращает кик сервером за AFK", Category.Player);
        addSettings(actions);
    }

    @java.lang.Override
    public void onDisable() {
        timer.reset();
        super.onDisable();
    }

    @Native
    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        if (MoveUtil.isMoving()) {
            timer.reset();
            return;
        }

        if (timer.hasReached(20000)) {
            if (actions.is("Прыжок") && mc.player.isOnGround()) {
                mc.player.jump();
            }
            if (actions.is("Команда")) {
                mc.player.sendChatMessage("/" + RandomStringUtils.randomAlphabetic(5));
            }
            if (actions.is("Качание рукой")) {
                mc.player.swingArm(Hand.MAIN_HAND);
            }
            timer.reset();
        }
    }
}