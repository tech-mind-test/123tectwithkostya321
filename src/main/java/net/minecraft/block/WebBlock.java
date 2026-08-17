package net.minecraft.block;

import com.darkmagician6.eventapi.EventManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import sky.core.events.EventNoWeb;

public class WebBlock extends Block {
    public WebBlock(AbstractBlock.Properties properties) {
        super(properties);
    }



        public void onEntityCollision(BlockState state, World worldIn, BlockPos pos, Entity entityIn)
        {
            EventNoWeb event = new EventNoWeb(pos);
            EventManager.call(event);
            if (event.isCancelled()) return;
            entityIn.setInWeb();
        }


}
