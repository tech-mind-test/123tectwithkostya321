package sky.core.modules.impl.player;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.events.EventClickBlockRight;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.MultiBooleanSetting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class NoInteract extends Module {
    public BooleanSetting noplacesphere = new BooleanSetting("Не ставить шар,сферу", false);
    public BooleanSetting allBlocks = new BooleanSetting("Все блоки", false);
    public MultiBooleanSetting ignoreInteract = new MultiBooleanSetting("Обьекты", () -> !allBlocks.get(), new BooleanSetting("Стойки", true), new BooleanSetting("Сундуки", true), new BooleanSetting("Двери", true), new BooleanSetting("Кнопки", true), new BooleanSetting("Воронки", true), new BooleanSetting("Раздатчики", true), new BooleanSetting("Нотные блоки", true), new BooleanSetting("Верстаки", true), new BooleanSetting("Люки", true), new BooleanSetting("Печки", true), new BooleanSetting("Калитки", true), new BooleanSetting("Наковальни", true), new BooleanSetting("Рычаги", true));

    public NoInteract() {
        super("No Interact", "Отменяет взаимодействие по выбранным сущностям и блокам", Category.Player);
        addSettings(noplacesphere, allBlocks, ignoreInteract);
    }

    public Set<Block> getBlocks() {
            Set<Block> blocks = new HashSet<>();
            addBlocksForInteractionType(blocks, "Двери", Blocks.ACACIA_DOOR, Blocks.DARK_OAK_DOOR, Blocks.BIRCH_DOOR, Blocks.IRON_DOOR, Blocks.JUNGLE_DOOR, Blocks.OAK_DOOR, Blocks.SPRUCE_DOOR);
            addBlocksForInteractionType(blocks, "Кнопки", Blocks.OAK_BUTTON, Blocks.STONE_BUTTON, Blocks.CRIMSON_BUTTON, Blocks.WARPED_BUTTON);
            addBlocksForInteractionType(blocks, "Сундуки", Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.ENDER_CHEST);
            addBlocksForInteractionType(blocks, "Воронки", Blocks.HOPPER);
            addBlocksForInteractionType(blocks, "Раздатчики", Blocks.DISPENSER, Blocks.DROPPER);
            addBlocksForInteractionType(blocks, "Нотные блоки", Blocks.NOTE_BLOCK);
            addBlocksForInteractionType(blocks, "Верстаки", Blocks.CRAFTING_TABLE, Blocks.CARTOGRAPHY_TABLE, Blocks.SMITHING_TABLE, Blocks.LOOM);
            addBlocksForInteractionType(blocks, "Люки", Blocks.OAK_TRAPDOOR, Blocks.IRON_TRAPDOOR);
            addBlocksForInteractionType(blocks, "Печки", Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER);
            addBlocksForInteractionType(blocks, "Калитки", Blocks.ACACIA_FENCE_GATE, Blocks.DARK_OAK_FENCE_GATE, Blocks.BIRCH_FENCE_GATE, Blocks.JUNGLE_FENCE_GATE, Blocks.OAK_FENCE_GATE, Blocks.SPRUCE_FENCE_GATE);
            addBlocksForInteractionType(blocks, "Наковальни", Blocks.ANVIL);
            addBlocksForInteractionType(blocks, "Рычаги", Blocks.LEVER);
            return blocks;
    }

    private void addBlocksForInteractionType(Set<Block> blocks, String interactionType, Block... blockIds) {
        if (ignoreInteract.is(interactionType)) {
            Collections.addAll(blocks, blockIds);
        }
    }

    @EventTarget
    private void onUpdate(EventClickBlockRight event) {
        if (noplacesphere.get() && event.getHand() == Hand.OFF_HAND && mc.player.getHeldItemOffhand().getItem() == Items.PLAYER_HEAD) event.setCancelled(true);
        if (allBlocks.get()) event.setCancelled(true);
        else if (getBlocks().contains(event.getWorld().getBlockState(event.getResult().getPos()).getBlock())) event.setCancelled(true);
    }
}