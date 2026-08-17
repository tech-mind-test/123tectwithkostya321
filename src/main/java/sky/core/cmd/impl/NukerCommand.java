//package sky.core.cmd.impl;
//
//import com.mojang.brigadier.builder.LiteralArgumentBuilder;
//import com.mojang.brigadier.context.CommandContext;
//import com.mojang.brigadier.suggestion.Suggestions;
//import com.mojang.brigadier.suggestion.SuggestionsBuilder;
//import sky.core.SkyCore;
//import cmd.sky.core.Command;
//import impl.managers.sky.core.NukerManager;
//import utils.sky.core.Wrapper;
//import misc.utils.sky.core.ChatUtil;
//import net.minecraft.block.Block;
//import net.minecraft.command.ISuggestionProvider;
//import net.minecraft.command.arguments.ResourceLocationArgument;
//import net.minecraft.util.ResourceLocation;
//import net.minecraft.util.registry.Registry;
//import net.minecraft.util.text.IFormattableTextComponent;
//import net.minecraft.util.text.StringTextComponent;
//import net.minecraft.util.text.Style;
//import net.minecraft.util.text.TextFormatting;
//
//import java.util.List;
//import java.util.concurrent.CompletableFuture;
//import java.util.stream.Collectors;
//
//import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
//
//public class NukerCommand extends Command implements Wrapper {
//
//    public NukerCommand() {
//        super("nuker", "nuk");
//    }
//
//    @EventTarget
//    public void run(LiteralArgumentBuilder<ISuggestionProvider> builder) {
//        builder.executes(this::invalidArgs);
//        builder.then(literal("add").then(args("block", ResourceLocationArgument.resourceLocation()).suggests(this::suggestBlocks).executes(this::handleAdd)));
//        builder.then(literal("remove").then(args("block", ResourceLocationArgument.resourceLocation()).suggests(this::suggestAddedBlocks).executes(this::handleRemove)));
//        builder.then(literal("clear").executes(this::handleClear));
//        builder.then(literal("list").executes(this::handleList));
//    }
//
//    private int handleAdd(CommandContext<ISuggestionProvider> ctx) {
//        ResourceLocation id = ctx.getArgument("block", ResourceLocation.class);
//        NukerManager manager = SkyCore.getInstance().getNukerManager();
//
//        boolean added = manager.addTargetBlock(id);
//        IFormattableTextComponent message = new StringTextComponent("");
//        if (added) {
//            message.append(new StringTextComponent("Блок с именем '").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY)));
//            message.append(new StringTextComponent(id.toString()).setStyle(Style.EMPTY.applyFormatting(TextFormatting.WHITE)));
//            message.append(new StringTextComponent("' добавлен!").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY)));
//        } else {
//            message.append(new StringTextComponent("Блок уже в списке или некорректен: ").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY)));
//            message.append(new StringTextComponent(id.toString()).setStyle(Style.EMPTY.applyFormatting(TextFormatting.WHITE)));
//        }
//
//        ChatUtil.addText(message);
//        return SINGLE_SUCCESS;
//    }
//
//    private int handleRemove(CommandContext<ISuggestionProvider> ctx) {
//        ResourceLocation id = ctx.getArgument("block", ResourceLocation.class);
//        NukerManager manager = SkyCore.getInstance().getNukerManager();
//
//        boolean removed = manager.removeTargetBlock(id);
//        if (removed) {
//            IFormattableTextComponent message = new StringTextComponent("");
//            message.append(new StringTextComponent("Блок с именем '").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY)));
//            message.append(new StringTextComponent(id.toString()).setStyle(Style.EMPTY.applyFormatting(TextFormatting.WHITE)));
//            message.append(new StringTextComponent("' удалён!").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY)));
//            ChatUtil.addText(message);
//        }
//        return SINGLE_SUCCESS;
//    }
//
//    private int handleClear(CommandContext<ISuggestionProvider> ctx) {
//        NukerManager manager = SkyCore.getInstance().getNukerManager();
//
//        manager.clearTargetBlocks();
//        IFormattableTextComponent message = new StringTextComponent("Список блоков очищен!");
//        message.setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
//        ChatUtil.addText(message);
//        return SINGLE_SUCCESS;
//    }
//
//    private int handleList(CommandContext<ISuggestionProvider> ctx) {
//        NukerManager manager = SkyCore.getInstance().getNukerManager();
//
//        List<Block> list = manager.getTargetBlocks();
//        if (list.isEmpty()) {
//            IFormattableTextComponent message = new StringTextComponent("Список пуст!");
//            message.setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
//            ChatUtil.addText(message);
//            return SINGLE_SUCCESS;
//        }
//
//        IFormattableTextComponent headerMessage = new StringTextComponent("Список блоков:");
//        headerMessage.setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
//        ChatUtil.addText(headerMessage);
//
//        for (Block b : list) {
//            IFormattableTextComponent blockMessage = new StringTextComponent(Registry.BLOCK.getKey(b).toString());
//            blockMessage.setStyle(Style.EMPTY.applyFormatting(TextFormatting.WHITE));
//            ChatUtil.addText(blockMessage);
//        }
//
//        IFormattableTextComponent totalMessage = new StringTextComponent("Всего: " + list.size());
//        totalMessage.setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
//        ChatUtil.addText(totalMessage);
//
//        return SINGLE_SUCCESS;
//    }
//
//    private CompletableFuture<Suggestions> suggestBlocks(CommandContext<ISuggestionProvider> ctx, SuggestionsBuilder builder) {
//        List<String> suggestions = Registry.BLOCK.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
//        return ISuggestionProvider.suggest(suggestions, builder);
//    }
//
//    private CompletableFuture<Suggestions> suggestAddedBlocks(CommandContext<ISuggestionProvider> ctx, SuggestionsBuilder builder) {
//        NukerManager manager = SkyCore.getInstance().getNukerManager();
//        List<String> suggestions = manager.getTargetBlocks().stream().map(block -> Registry.BLOCK.getKey(block).toString()).collect(Collectors.toList());
//        return ISuggestionProvider.suggest(suggestions, builder);
//    }
//}