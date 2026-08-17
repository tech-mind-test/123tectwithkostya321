package sky.core.cmd.impl;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.adl.nativeprotect.Native;
import sky.core.SkyCore;
import sky.core.cmd.Command;
import sky.core.utils.managers.impl.BlockESPManager;
import sky.core.utils.Wrapper;
import sky.core.utils.misc.ChatUtil;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.ResourceLocationArgument;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class BlockESPCommand extends Command implements ArgumentType<String>, Wrapper {
    private static final Map<String, Integer> COLOR_NAMES = Map.of("white", 0xFFFFFF, "red", 0xFF0000, "blue", 0x0000FF, "green", 0x00FF00, "yellow", 0xFFFF00, "cyan", 0x00FFFF, "magenta", 0xFF00FF, "black", 0x000000, "gray", 0x808080);

    private static final String[] SHULKER_TYPES = {"white_shulker_box", "orange_shulker_box", "magenta_shulker_box", "light_blue_shulker_box", "yellow_shulker_box", "lime_shulker_box", "pink_shulker_box", "gray_shulker_box", "light_gray_shulker_box", "cyan_shulker_box", "purple_shulker_box", "blue_shulker_box", "brown_shulker_box", "green_shulker_box", "red_shulker_box", "black_shulker_box"};

    public BlockESPCommand() {
        super("blockesp");
    }

    @Native
    @Override
    public void run(LiteralArgumentBuilder<ISuggestionProvider> builder) {
        builder.executes(this::invalidArgs);

        builder.then(literal("add").then(literal("shulkers").then(args("color", this).suggests(this::listColorSuggestions).executes(this::handleAddShulkers))).then(args("block", ResourceLocationArgument.resourceLocation()).suggests(this::suggestBlocks).then(args("color", this).suggests(this::listColorSuggestions).executes(this::handleAddBlock))));

        builder.then(literal("remove").then(args("block", ResourceLocationArgument.resourceLocation()).suggests(this::suggestAddedBlocks).executes(this::handleRemove)));

        builder.then(literal("clear").executes(ctx -> {
            getManager().clear();
            IFormattableTextComponent message = new StringTextComponent("Список блоков очищен!");
            message.setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
            ChatUtil.addText(message);
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("list").executes(ctx -> {
            List<BlockESPManager.BlockEntry> list = getManager().getBlocks();
            if (list.isEmpty()) {
                IFormattableTextComponent message = new StringTextComponent("Список пуст!");
                message.setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
                ChatUtil.addText(message);
                return SINGLE_SUCCESS;
            }

            IFormattableTextComponent headerMessage = new StringTextComponent("Список блоков:");
            headerMessage.setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
            ChatUtil.addText(headerMessage);

            for (BlockESPManager.BlockEntry entry : list) {
                IFormattableTextComponent blockMessage = new StringTextComponent(entry.getBlock());
                blockMessage.setStyle(Style.EMPTY.applyFormatting(TextFormatting.WHITE));
                ChatUtil.addText(blockMessage);
            }

            IFormattableTextComponent totalMessage = new StringTextComponent("Всего: " + list.size());
            totalMessage.setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
            ChatUtil.addText(totalMessage);

            return SINGLE_SUCCESS;
        }));
    }

    @Native
    private int handleAddBlock(CommandContext<ISuggestionProvider> ctx) {
        ResourceLocation id = ctx.getArgument("block", ResourceLocation.class);
        String colorArg = ctx.getArgument("color", String.class);
        int color = parseColor(colorArg);
        getManager().addBlock(id.toString(), color);
        IFormattableTextComponent message = new StringTextComponent("");
        message.append(new StringTextComponent("Блок с именем '").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY)));
        message.append(new StringTextComponent(id.toString()).setStyle(Style.EMPTY.applyFormatting(TextFormatting.WHITE)));
        message.append(new StringTextComponent("' добавлен!").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY)));
        ChatUtil.addText(message);
        return SINGLE_SUCCESS;
    }

    @Native
    private int handleAddShulkers(CommandContext<ISuggestionProvider> ctx) {
        String colorArg = ctx.getArgument("color", String.class);
        int color = parseColor(colorArg);
        for (String type : SHULKER_TYPES) {
            getManager().addBlock(type, color);
        }
        IFormattableTextComponent message = new StringTextComponent("");
        message.append(new StringTextComponent("Все ").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY)));
        message.append(new StringTextComponent("шалкеры").setStyle(Style.EMPTY.applyFormatting(TextFormatting.WHITE)));
        message.append(new StringTextComponent(" добавлены!").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY)));
        ChatUtil.addText(message);
        return SINGLE_SUCCESS;
    }

    @Native
    private int handleRemove(CommandContext<ISuggestionProvider> ctx) {
        ResourceLocation id = ctx.getArgument("block", ResourceLocation.class);
        boolean removed = getManager().removeBlock(id.toString());
        if (removed) {
            IFormattableTextComponent message = new StringTextComponent("");
            message.append(new StringTextComponent("Блок с именем '").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY)));
            message.append(new StringTextComponent(id.toString()).setStyle(Style.EMPTY.applyFormatting(TextFormatting.WHITE)));
            message.append(new StringTextComponent("' удалён!").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY)));
            ChatUtil.addText(message);
        } else {
            ChatUtil.addText("BlockESP entry not found: " + id);
        }
        return SINGLE_SUCCESS;
    }

    @Native
    private CompletableFuture<Suggestions> listColorSuggestions(CommandContext<ISuggestionProvider> ctx, SuggestionsBuilder builder) {
        return ISuggestionProvider.suggest(COLOR_NAMES.keySet(), builder);
    }

    @Native
    private CompletableFuture<Suggestions> suggestBlocks(CommandContext<ISuggestionProvider> ctx, SuggestionsBuilder builder) {
        List<String> suggestions = Stream.concat(Registry.BLOCK.keySet().stream().map(ResourceLocation::toString), Stream.of("shulkers")).collect(Collectors.toList());
        return ISuggestionProvider.suggest(suggestions, builder);
    }

    @Native
    private CompletableFuture<Suggestions> suggestAddedBlocks(CommandContext<ISuggestionProvider> ctx, SuggestionsBuilder builder) {
        List<String> suggestions = getManager().getBlocks().stream().map(BlockESPManager.BlockEntry::getBlock).collect(Collectors.toList());
        return ISuggestionProvider.suggest(suggestions, builder);
    }

    @Native
    private int parseColor(String input) {
        if (input.startsWith("#")) {
            try {
                return Integer.parseInt(input.substring(1), 16);
            } catch (NumberFormatException e) {
                return COLOR_NAMES.getOrDefault(input.toLowerCase(), 0xFFFFFF);
            }
        }
        return COLOR_NAMES.getOrDefault(input.toLowerCase(), 0xFFFFFF);
    }

    @Native
    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        return reader.readString();
    }

    private BlockESPManager getManager() {
        return SkyCore.getInstance().getBlockESPManager();
    }
}