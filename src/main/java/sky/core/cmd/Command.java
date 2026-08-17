package sky.core.cmd;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.ISuggestionProvider;
import sky.core.utils.misc.ChatUtil;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;

import java.util.Arrays;
import java.util.List;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public abstract class Command {
    public List<String> command;

    public Command(String... command) {
        this.command = Arrays.asList(command);
    }

    public abstract void run(LiteralArgumentBuilder<ISuggestionProvider> builder);

    public static <T> RequiredArgumentBuilder<ISuggestionProvider, T> args(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    public static LiteralArgumentBuilder<ISuggestionProvider> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    public void add(CommandDispatcher<ISuggestionProvider> dispatcher) {
        for (String name : command) {
            LiteralArgumentBuilder<ISuggestionProvider> builder = LiteralArgumentBuilder.literal(name);
            run(builder);
            dispatcher.register(builder);
        }
    }

    public String getCommand() {
        return command.get(0);
    }

    protected int invalidArgs(CommandContext<ISuggestionProvider> ctx) {
        String input = ctx.getInput();
        IFormattableTextComponent errorMsg = new StringTextComponent("Неверно указаны аргументы - \"" + input + "\"").setStyle(Style.EMPTY.applyFormatting(TextFormatting.RED));
        ChatUtil.addText(errorMsg);
        IFormattableTextComponent helpMsg = new StringTextComponent("Для помощи - .help").setStyle(Style.EMPTY.applyFormatting(TextFormatting.RED));
        ChatUtil.addText(helpMsg);
        return SINGLE_SUCCESS;
    }
}