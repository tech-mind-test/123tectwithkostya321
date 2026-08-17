package sky.core.modules.impl.movement;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.block.Blocks;
import sky.core.events.EventBlockCollide;
import sky.core.modules.Category;
import sky.core.modules.Module;


public class NoControllerWeb extends Module {

    public NoControllerWeb() {
        super("Web Ignore", "Позволяет ломать и бить сквозь паутину", Category.Movement);
    }


    @EventTarget
    public void onBlockCollide(EventBlockCollide e) {
        if (mc.world == null || e.getPos() == null) return;
        if (mc.world.getBlockState(e.getPos()).getBlock() == Blocks.COBWEB) {
            e.setCancelled(true);
        }
    }
}