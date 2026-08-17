package sky.core.cmd.impl;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import sky.core.SkyCore;
import sky.core.cmd.Command;
import sky.core.utils.Wrapper;
import sky.core.utils.misc.ChatUtil;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class PartyCommand extends Command implements ArgumentType<String>, Wrapper {

    public PartyCommand() {
        super("party", "p");
    }

    @Override
    public void run(LiteralArgumentBuilder<ISuggestionProvider> builder) {
        builder.executes(context -> {
            showHelp();
            return SINGLE_SUCCESS;
        });

        builder.then(literal("create").executes(context -> {
            sendPartyCommand("create", "");
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("joincode")
                .then(args("code", this).executes(context -> {
                    String code = context.getArgument("code", String.class);
                    sendPartyCommand("joincode", code);
                    return SINGLE_SUCCESS;
                })));

        builder.then(literal("join").executes(context -> {
            sendPartyCommand("join", "");
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("dismiss").executes(context -> {
            sendPartyCommand("dismiss", "");
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("leave").executes(context -> {
            sendPartyCommand("leave", "");
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("kick")
                .then(args("player", this).executes(context -> {
                    String player = context.getArgument("player", String.class);
                    sendPartyCommand("kick", player);
                    return SINGLE_SUCCESS;
                })));

        builder.then(literal("invite")
                .then(args("player", this).executes(context -> {
                    String player = context.getArgument("player", String.class);
                    sendPartyCommand("invite", player);
                    return SINGLE_SUCCESS;
                })));

        builder.then(literal("disband").executes(context -> {
            sendPartyCommand("disband", "");
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("info").executes(context -> {
            sendPartyCommand("info", "");
            return SINGLE_SUCCESS;
        }));
    }

    private void sendPartyCommand(String command, String arg) {

        var client = SkyCore.getInstance().getIrcClient();

        if (client != null && client.isConnected()) {
            client.sendPartyCommand(command, arg);
        } else {
            ChatUtil.addText(new StringTextComponent("IRC не подключен!").mergeStyle(TextFormatting.RED));
        }
    }

    private void showHelp() {
        String prefix = "§b[IRC] §r";
        ChatUtil.addText(new StringTextComponent(prefix + ".party create §7- Создать группу"));
        ChatUtil.addText(new StringTextComponent(prefix + ".party joincode <код> §7- Войти по коду"));
        ChatUtil.addText(new StringTextComponent(prefix + ".party join §7- Принять приглашение"));
        ChatUtil.addText(new StringTextComponent(prefix + ".party dismiss §7- Отклонить приглашение"));
        ChatUtil.addText(new StringTextComponent(prefix + ".party leave §7- Покинуть группу"));
        ChatUtil.addText(new StringTextComponent(prefix + ".party kick <игрок> §7- Кикнуть игрока"));
        ChatUtil.addText(new StringTextComponent(prefix + ".party invite <игрок> §7- Пригласить игрока"));
        ChatUtil.addText(new StringTextComponent(prefix + ".party disband §7- Расформировать группу"));
        ChatUtil.addText(new StringTextComponent(prefix + ".party info §7- Информация о группе"));
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        return reader.readString();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        var client = SkyCore.getInstance().getIrcClient();
        if (client != null && client.isConnected()) {
            List<String> users = client.getOnlineUsers();
            String current = builder.getRemaining().toLowerCase();

            users.stream()
                    .filter(name -> name.toLowerCase().startsWith(current))
                    .filter(name -> !name.equals(client.getUsername()))
                    .forEach(builder::suggest);
        }
        return builder.buildFuture();
    }
}