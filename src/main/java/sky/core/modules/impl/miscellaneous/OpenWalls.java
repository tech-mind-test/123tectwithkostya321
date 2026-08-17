package sky.core.modules.impl.miscellaneous;

import sky.core.modules.Category;
import sky.core.modules.Module;
import lombok.Getter;
import net.minecraft.block.*;


import java.util.Set;

public class OpenWalls extends Module {
    
    public OpenWalls() {
        super("Open Walls", "Позволяет открывать контейнеры через стены", Category.Miscellaneous);
    }

    @Getter
    private final Set<Class<?>> blockClasses = Set.of(
            AbstractChestBlock.class,
            FurnaceBlock.class,
            CraftingTableBlock.class,
            ShulkerBoxBlock.class,
            ContainerBlock.class,
            AnvilBlock.class,
            BeaconBlock.class,
            BlastFurnaceBlock.class,
            BrewingStandBlock.class,
            CampfireBlock.class,
            CartographyTableBlock.class,
            GrindstoneBlock.class,
            LecternBlock.class,
            LoomBlock.class,
            SmokerBlock.class,
            StonecutterBlock.class,
            LeverBlock.class
    );
}