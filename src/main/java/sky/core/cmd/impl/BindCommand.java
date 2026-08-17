package sky.core.cmd.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.adl.nativeprotect.Native;
import sky.core.SkyCore;
import sky.core.cmd.Command;
import sky.core.events.EventClientClick;
import sky.core.modules.Module;
import sky.core.modules.ModuleManager;
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

public class BindCommand extends Command implements Wrapper {

    public BindCommand() {
        super("bind");
    }

    @Native
    @Override
    public void run(LiteralArgumentBuilder<ISuggestionProvider> builder) {
        builder.executes(this::invalidArgs);

        builder.then(literal("add").then(args("args", StringArgumentType.greedyString()).suggests(this::suggestAdd).executes(this::executeAdd)));
        builder.then(literal("remove").then(args("module", StringArgumentType.greedyString()).suggests(this::suggestModuleNames).executes(this::executeRemove)));
        builder.then(literal("list").executes(this::executeList));
        builder.then(literal("clear").executes(this::executeClear));
    }

    @Native
    private int executeAdd(CommandContext<ISuggestionProvider> context) {
        String rest = context.getArgument("args", String.class).trim();
        if (rest.isEmpty()) return invalidArgs(context);

        String[] tokens = rest.split("\\s+");
        if (tokens.length < 3) return invalidArgs(context);

        String modeToken = tokens[tokens.length - 1];
        String keyToken = tokens[tokens.length - 2];
        String moduleName = String.join(" ", Arrays.asList(tokens).subList(0, tokens.length - 2));

        Module.ToggleMode mode;
        if (modeToken.equalsIgnoreCase("hold")) {
            mode = Module.ToggleMode.HOLD;
        } else if (modeToken.equalsIgnoreCase("toggle")) {
            mode = Module.ToggleMode.TOGGLE;
        } else return invalidArgs(context);

        KeyMapper key = KeyMapper.fromName(keyToken);
        if (key == null) return invalidArgs(context);

        ModuleManager mm = SkyCore.getInstance().getModuleManager();
        Module module = resolveModuleByFlexibleName(mm, moduleName);
        if (module == null) return invalidArgs(context);

        module.setBind(key.getKeyCode());
        module.setToggleMode(mode);

        IFormattableTextComponent msg1 = new StringTextComponent("Установлен бинд ").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
        IFormattableTextComponent msg2 = new StringTextComponent(KeyMapper.getKey(module.getBind())).setStyle(Style.EMPTY.applyFormatting(TextFormatting.WHITE));
        IFormattableTextComponent msg3 = new StringTextComponent(" на модуль '").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
        IFormattableTextComponent msg4 = new StringTextComponent(module.getName()).setStyle(Style.EMPTY.applyFormatting(TextFormatting.WHITE));
        IFormattableTextComponent msg5 = new StringTextComponent("'!").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
        ChatUtil.addText(new StringTextComponent("").append(msg1).append(msg2).append(msg3).append(msg4).append(msg5));
        return SINGLE_SUCCESS;
    }

    @Native
    private int executeRemove(CommandContext<ISuggestionProvider> context) {
        String moduleName = context.getArgument("module", String.class);
        ModuleManager mm = SkyCore.getInstance().getModuleManager();
        Module module = resolveModuleByFlexibleName(mm, moduleName);
        if (module == null) return invalidArgs(context);
        module.setBind(-100);

        IFormattableTextComponent msg1 = new StringTextComponent("Модуль '").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
        IFormattableTextComponent msg2 = new StringTextComponent(module.getName()).setStyle(Style.EMPTY.applyFormatting(TextFormatting.WHITE));
        IFormattableTextComponent msg3 = new StringTextComponent("' теперь не имеет бинда!").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
        ChatUtil.addText(new StringTextComponent("").append(msg1).append(msg2).append(msg3));
        return SINGLE_SUCCESS;
    }

    @Native
    private int executeList(CommandContext<ISuggestionProvider> context) {
        List<Module> bound = SkyCore.getInstance().getModuleManager().getModules().stream().filter(m -> m.getBind() != -100).collect(Collectors.toList());

        if (bound.isEmpty()) {
            IFormattableTextComponent empty = new StringTextComponent("Список биндoв пуст!").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
            ChatUtil.addText(empty);
            return SINGLE_SUCCESS;
        }

        for (Module m : bound) {
            String key = KeyMapper.getKey(m.getBind());
            IFormattableTextComponent name = new StringTextComponent(m.getName()).setStyle(Style.EMPTY.applyFormatting(TextFormatting.WHITE));
            IFormattableTextComponent keyLabel = new StringTextComponent(" клавиша: ").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
            IFormattableTextComponent keyVal = new StringTextComponent(key).setStyle(Style.EMPTY.applyFormatting(TextFormatting.WHITE));
            String removeCmd = SkyCore.getInstance().getCommandsManager().getPrefix() + getCommand() + " remove " + normalizeModuleName(m.getName());
            IFormattableTextComponent removeBtn = new StringTextComponent(" [Удалить]").setStyle(Style.EMPTY.applyFormatting(TextFormatting.RED).setClickEvent(new EventClientClick(ClickEvent.Action.RUN_COMMAND, removeCmd)).setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("Клик для удаления бинда").setStyle(Style.EMPTY.applyFormatting(TextFormatting.RED)))));
            ChatUtil.addText(new StringTextComponent("").append(name).append(keyLabel).append(keyVal).append(removeBtn));
        }
        return SINGLE_SUCCESS;
    }

    @Native
    private int executeClear(CommandContext<ISuggestionProvider> context) {
        boolean anyBound = SkyCore.getInstance().getModuleManager().getModules().stream().anyMatch(m -> m.getBind() != -100);
        if (!anyBound) {
            IFormattableTextComponent emptyMsg = new StringTextComponent("Список биндoв пуст!").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
            ChatUtil.addText(emptyMsg);
            return SINGLE_SUCCESS;
        }
        SkyCore.getInstance().getModuleManager().getModules().forEach(m -> m.setBind(-100));
        IFormattableTextComponent clearedMsg = new StringTextComponent("Список Биндов очищен!").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
        ChatUtil.addText(clearedMsg);
        return SINGLE_SUCCESS;
    }

    @Native
    private CompletableFuture<Suggestions> suggestModuleNames(CommandContext<ISuggestionProvider> ctx, SuggestionsBuilder builder) {
        List<String> names = SkyCore.getInstance().getModuleManager().getModules().stream().map(Module::getName).map(this::normalizeModuleName).collect(Collectors.toList());
        return ISuggestionProvider.suggest(names, builder);
    }

    @Native
    private CompletableFuture<Suggestions> suggestAdd(CommandContext<ISuggestionProvider> ctx, SuggestionsBuilder builder) {
        String input = builder.getInput();
        String lower = input.toLowerCase(Locale.ROOT);
        int addPos = lower.indexOf(".bind add ");
        String afterAdd = addPos == -1 ? "" : input.substring(addPos + ".bind add ".length());
        boolean hasTrailingSpace = afterAdd.endsWith(" ");
        String trimmed = afterAdd.trim();
        String[] parts = trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+");

        int replaceStart;
        if (parts.length == 0) {
            replaceStart = input.length();
        } else if (hasTrailingSpace) {
            replaceStart = input.length();
        } else {
            replaceStart = input.length() - parts[parts.length - 1].length();
        }
        SuggestionsBuilder sb = new SuggestionsBuilder(input, replaceStart);

        if (parts.length == 0 || (parts.length == 1 && !hasTrailingSpace)) {
            String prefix = parts.length == 0 ? "" : normalizeModuleName(parts[0]);
            List<String> names = SkyCore.getInstance().getModuleManager().getModules().stream().map(Module::getName).map(this::normalizeModuleName).filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))).collect(Collectors.toList());
            for (String n : names) sb.suggest(n);
            return sb.buildFuture();
        }

        if (parts.length == 1 || parts.length == 2 && !hasTrailingSpace) {
            String keyPrefix = parts.length == 2 ? parts[1].toUpperCase(Locale.ROOT) : "";
            for (KeyMapper key : KeyMapper.values()) {
                if (key.name().startsWith(keyPrefix)) {
                    sb.suggest(key.name());
                }
            }
            return sb.buildFuture();
        }

        if (parts.length == 2 || !hasTrailingSpace) {
            String modePrefix = parts.length >= 3 ? parts[2].toLowerCase(Locale.ROOT) : "";
            if ("toggle".startsWith(modePrefix)) sb.suggest("toggle");
            if ("hold".startsWith(modePrefix)) sb.suggest("hold");
            return sb.buildFuture();
        }

        return sb.buildFuture();
    }

    @Native
    private Module resolveModuleByFlexibleName(ModuleManager mm, String userInputName) {
        Module direct = mm.findModuleByName(userInputName).orElse(null);
        if (direct != null) return direct;
        String normalized = normalizeModuleName(userInputName);
        for (Module m : mm.getModules()) {
            if (normalizeModuleName(m.getName()).equalsIgnoreCase(normalized)) {
                return m;
            }
        }
        return null;
    }

    @Native
    private String normalizeModuleName(String name) {
        return name.replaceAll("\\s+", "");
    }
}