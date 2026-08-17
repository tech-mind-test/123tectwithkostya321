package sky.core.modules.impl.player;

import com.adl.nativeprotect.Native;
import sky.core.handlers.impl.ReallyWorldJoinHandler;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.SliderSetting;

public class AutoJoiner extends Module {

    private final SliderSetting grief = new SliderSetting("Grief", 5, 1, 76, 1);

    public AutoJoiner() {
        super("Auto Joiner", "Автоматически заходит на сервер", Category.Miscellaneous);
        addSettings(grief);
    }

    @Native
    @java.lang.Override
    public void onEnable() {
        ReallyWorldJoinHandler.setManualGrief(grief.get().intValue());
        ReallyWorldJoinHandler.startRejoin();
        super.onEnable();
    }

    @Native
    @java.lang.Override
    public void onDisable() {
        ReallyWorldJoinHandler.disable();
        super.onDisable();
    }
}