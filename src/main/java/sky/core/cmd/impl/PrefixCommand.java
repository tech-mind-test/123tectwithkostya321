package sky.core.cmd.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import sky.core.SkyCore;
import sky.core.cmd.Command;
import sky.core.utils.misc.ChatUtil;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class PrefixCommand extends Command {

    public PrefixCommand() {
        super("prefix");
    }

    @Override
    public void run(LiteralArgumentBuilder<ISuggestionProvider> builder) {
        builder.executes(this::invalidArgs);

        builder.then(args("value", StringArgumentType.greedyString()).executes(ctx -> {
            String newPrefix = ctx.getArgument("value", String.class);
            SkyCore.getInstance().getCommandsManager().setPrefix(newPrefix);
            IFormattableTextComponent msg = new StringTextComponent("Префикс для использования команд установлен на '").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY)).append(new StringTextComponent(newPrefix).setStyle(Style.EMPTY.applyFormatting(TextFormatting.WHITE))).append(new StringTextComponent("'").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY)));
            ChatUtil.addText(msg);
            return SINGLE_SUCCESS;
        }));
    }
}