package sky.core.cmd.impl;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import sky.core.SkyCore;
import sky.core.cmd.Command;
import sky.core.events.EventClientClick;
import sky.core.utils.managers.impl.MacroManager;
import sky.core.utils.Wrapper;
import sky.core.utils.misc.ChatUtil;
import sky.core.utils.misc.KeyMapper;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class MacroCommand extends Command implements ArgumentType<String>, Wrapper {

    public MacroCommand() {
        super("mac", "macros");
    }

    @Override
    public void run(LiteralArgumentBuilder<ISuggestionProvider> builder) {
        builder.executes(this::invalidArgs);

        builder.then(literal("add").then(args("args", StringArgumentType.greedyString()).suggests(this::suggestAdd).executes(this::executeAdd)));

        builder.then(literal("remove").then(args("name", StringArgumentType.word()).suggests(this::suggestMacroNames).executes(ctx -> {
            String name = ctx.getArgument("name", String.class);
            SkyCore.getInstance().getMacroManager().removeMacro(name);
            IFormattableTextComponent msg = new StringTextComponent("Макрос c именем '").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY)).append(new StringTextComponent(name).setStyle(Style.EMPTY.applyFormatting(TextFormatting.WHITE))).append(new StringTextComponent("' удален!").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY)));
            ChatUtil.addText(msg);
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("list").executes(ctx -> {
            List<MacroManager.MacroEntry> macros = SkyCore.getInstance().getMacroManager().getMacros();
            if (macros.isEmpty()) {
                ChatUtil.addText(new StringTextComponent("Список макросов пуст!").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY)));
                return SINGLE_SUCCESS;
            }
            ChatUtil.addText(new StringTextComponent("Список макросов:").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY)));
            for (MacroManager.MacroEntry m : macros) {
                String key = KeyMapper.getKey(m.getKeyCode());
                IFormattableTextComponent nameLbl = new StringTextComponent("Имя: ").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
                IFormattableTextComponent nameVal = new StringTextComponent(m.getName()).setStyle(Style.EMPTY.applyFormatting(TextFormatting.WHITE));
                IFormattableTextComponent keyLbl = new StringTextComponent(" Клавиша: ").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
                IFormattableTextComponent keyVal = new StringTextComponent(key).setStyle(Style.EMPTY.applyFormatting(TextFormatting.WHITE));
                IFormattableTextComponent textLbl = new StringTextComponent(" Текст: ").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
                IFormattableTextComponent textVal = new StringTextComponent(m.getCommand()).setStyle(Style.EMPTY.applyFormatting(TextFormatting.GREEN));
                String removeCmd = SkyCore.getInstance().getCommandsManager().getPrefix() + getCommand() + " remove " + m.getName();
                IFormattableTextComponent removeBtn = new StringTextComponent(" [Удалить]").setStyle(Style.EMPTY.applyFormatting(TextFormatting.RED).setClickEvent(new EventClientClick(ClickEvent.Action.RUN_COMMAND, removeCmd)).setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("Клик для удаления макроса").setStyle(Style.EMPTY.applyFormatting(TextFormatting.RED)))));
                ChatUtil.addText(new StringTextComponent("").append(nameLbl).append(nameVal).append(keyLbl).append(keyVal).append(textLbl).append(textVal).append(removeBtn));
            }
            ChatUtil.addText(new StringTextComponent("Всего: " + macros.size()).setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY)));
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("clear").executes(ctx -> {
            List<MacroManager.MacroEntry> macros = SkyCore.getInstance().getMacroManager().getMacros();
            if (macros.isEmpty()) {
                ChatUtil.addText(new StringTextComponent("Список макросов пуст!").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY)));
                return SINGLE_SUCCESS;
            }
            SkyCore.getInstance().getMacroManager().clear();
            ChatUtil.addText(new StringTextComponent("Список макросов очищен!").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY)));
            return SINGLE_SUCCESS;
        }));
    }

    private int executeAdd(CommandContext<ISuggestionProvider> ctx) {
        String rest = ctx.getArgument("args", String.class).trim();
        if (rest.isEmpty()) return invalidArgs(ctx);
        String[] tokens = rest.split("\\s+");
        if (tokens.length < 3) return invalidArgs(ctx);
        String name = tokens[0];
        String keyToken = tokens[1];
        String command = String.join(" ", Arrays.asList(tokens).subList(2, tokens.length));

        KeyMapper key = KeyMapper.fromName(keyToken);
        if (key == null) return invalidArgs(ctx);
        SkyCore.getInstance().getMacroManager().addMacro(name, key.getKeyCode(), command);

        IFormattableTextComponent msg = new StringTextComponent("Макрос с именем '").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY)).append(new StringTextComponent(name).setStyle(Style.EMPTY.applyFormatting(TextFormatting.WHITE))).append(new StringTextComponent("' и кнопкой активации: ").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY))).append(new StringTextComponent(KeyMapper.getKey(key.getKeyCode())).setStyle(Style.EMPTY.applyFormatting(TextFormatting.WHITE))).append(new StringTextComponent(" добавлен!").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY)));
        ChatUtil.addText(msg);
        return SINGLE_SUCCESS;
    }

    private CompletableFuture<Suggestions> suggestMacroNames(CommandContext<ISuggestionProvider> ctx, SuggestionsBuilder builder) {
        List<String> names = SkyCore.getInstance().getMacroManager().getMacros().stream().map(MacroManager.MacroEntry::getName).collect(Collectors.toList());
        return ISuggestionProvider.suggest(names, builder);
    }

    private CompletableFuture<Suggestions> suggestAdd(CommandContext<ISuggestionProvider> ctx, SuggestionsBuilder builder) {
        String input = ctx.getInput();
        String remaining = builder.getRemaining();
        boolean ends = remaining.endsWith(" ");
        String trimmed = remaining.trim();
        String[] parts = trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+");

        int replaceStart;
        if (parts.length == 0) {
            replaceStart = input.length();
        } else if (ends) {
            replaceStart = input.length();
        } else {
            replaceStart = input.length() - parts[parts.length - 1].length();
        }
        SuggestionsBuilder sb = new SuggestionsBuilder(input, replaceStart);

        if (parts.length == 0 || (parts.length == 1 && !ends)) {
            return sb.buildFuture();
        }

        if (parts.length == 1 || parts.length == 2 && !ends) {
            String keyPrefix = parts.length == 2 ? parts[1].toUpperCase(Locale.ROOT) : "";
            for (KeyMapper key : KeyMapper.values()) {
                if (key.name().startsWith(keyPrefix)) sb.suggest(key.name());
            }
            return sb.buildFuture();
        }

        return sb.buildFuture();
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        return reader.readString();
    }
}