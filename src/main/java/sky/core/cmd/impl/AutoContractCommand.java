package sky.core.cmd.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import com.adl.nativeprotect.Native;
import sky.core.cmd.Command;
import sky.core.utils.misc.ChatUtil;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class AutoContractCommand extends Command {
    public static String targetNickname = null;

    public AutoContractCommand() {
        super("autocontract", "act");
    }

    @Native
    @Override
    public void run(LiteralArgumentBuilder<ISuggestionProvider> builder) {
        builder.executes(ctx -> {
            String current = targetNickname;
            IFormattableTextComponent msg = new StringTextComponent("Текущая цель: ").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY)).append(new StringTextComponent(current == null ? "не задано" : current).setStyle(Style.EMPTY.applyFormatting(TextFormatting.WHITE)));
            ChatUtil.addText(msg);
            return SINGLE_SUCCESS;
        });

        builder.then(args("nickname", StringArgumentType.greedyString()).executes(ctx -> {
            String nickname = ctx.getArgument("nickname", String.class);
            targetNickname = nickname;
            IFormattableTextComponent msg = new StringTextComponent("Цель установлена: ").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY)).append(new StringTextComponent(nickname).setStyle(Style.EMPTY.applyFormatting(TextFormatting.WHITE)));
            ChatUtil.addText(msg);
            return SINGLE_SUCCESS;
        }));
    }
}


