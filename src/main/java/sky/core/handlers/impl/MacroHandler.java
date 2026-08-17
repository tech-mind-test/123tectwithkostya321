package sky.core.handlers.impl;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.SkyCore;
import sky.core.events.EventKey;
import sky.core.utils.managers.impl.MacroManager;
import sky.core.utils.Wrapper;

public class MacroHandler implements Wrapper {

    @EventTarget
    public void onKey(EventKey e) {
        if (mc.player == null || e.isHold() || mc.world == null) return;
        for (MacroManager.MacroEntry m : SkyCore.getInstance().getMacroManager().getMacros()) {
            if (m.getKeyCode() == e.getKey()) {
                String cmd = m.getCommand();
                if (cmd != null && !cmd.isEmpty()) {
                    mc.player.sendChatMessage(cmd);
                }
            }
        }
    }
}


