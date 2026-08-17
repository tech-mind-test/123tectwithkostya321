package sky.core.cmd.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import other.bot.Bot;
import other.bot.BotManager;
import other.bot.BotStarter;
import com.adl.nativeprotect.Native;
import sky.core.cmd.Command;
import sky.core.utils.Wrapper;
import sky.core.utils.misc.ChatUtil;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class BotCommand extends Command implements Wrapper {

    public BotCommand() {
        super("b", "bot");
    }
    @Native
    @Override
    public void run(LiteralArgumentBuilder<ISuggestionProvider> builder) {
        builder.executes(this::invalidArgs);

        builder.then(literal("connect")
                .then(args("nick", StringArgumentType.word())
                        .then(args("ip", StringArgumentType.word())
                                .executes(this::handleConnect))));

        builder.then(literal("remove")
                .then(args("nickname", StringArgumentType.word())
                        .suggests(this::suggestAllBots)
                        .executes(this::handleRemove)));

        builder.then(literal("control")
                .then(args("name", StringArgumentType.word())
                        .suggests(this::suggestAllBots)
                        .executes(this::handleControl)));

        builder.then(literal("return").executes(ctx -> {
            mc.loadWorld(mc.world);
            mc.renderViewEntity = mc.player;
            ChatUtil.addText(new StringTextComponent("Вы вернулись к основному игроку.").mergeStyle(TextFormatting.GREEN));
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("chat")
                .then(args("nicknm", StringArgumentType.word())
                        .suggests(this::suggestAllBots)
                        .then(args("message", StringArgumentType.greedyString())
                                .executes(this::handleChat))));

        builder.then(literal("autoles")
                .then(literal("add").then(args("botnick", StringArgumentType.word()).suggests(this::suggestNonAutoLesBots).executes(this::handleAddAutoLes)))
                .then(literal("remove").then(args("botnick", StringArgumentType.word()).suggests(this::suggestAutoLesBots).executes(this::handleRemoveAutoLes)))
                .then(literal("list").executes(this::handleListAutoLes)));
    }

    @Native
    private int handleConnect(CommandContext<ISuggestionProvider> ctx) {
        String name = ctx.getArgument("nick", String.class);
        String ip = ctx.getArgument("ip", String.class);
        BotStarter.run(name, ip);
        return SINGLE_SUCCESS;
    }

    @Native
    private int handleRemove(CommandContext<ISuggestionProvider> ctx) {
        String name = ctx.getArgument("nickname", String.class);
        for (Bot bot : BotManager.allBots) {
            if (bot.connection.bot.getNameClear().equals(name)) {
                bot.connection.world.sendQuittingDisconnectingPacket();
                BotManager.autoLesBots.removeIf(b -> b.connection.bot.getNameClear().equals(name));
                BotManager.tapeMouseBots.removeIf(b -> b.getBot().connection.bot.getNameClear().equals(name));
                BotManager.allBots.remove(bot);
                ChatUtil.addText(new StringTextComponent("Бот " + name + " успешно удален.").mergeStyle(TextFormatting.GRAY));
                return SINGLE_SUCCESS;
            }
        }
        return SINGLE_SUCCESS;
    }

    @Native
    private int handleControl(CommandContext<ISuggestionProvider> ctx) {
        String name = ctx.getArgument("name", String.class);
        for (Bot bot : BotManager.allBots) {
            if (bot.connection.bot.getName().getString().equals(name)) {
                mc.renderViewEntity = bot.connection.bot;
                mc.loadWorld(bot.connection.world);
                ChatUtil.addText(new StringTextComponent("Контроль переключен на бота: " + name).mergeStyle(TextFormatting.AQUA));
            }
        }
        return SINGLE_SUCCESS;
    }

    @Native
    private int handleChat(CommandContext<ISuggestionProvider> ctx) {
        String nick = ctx.getArgument("nicknm", String.class);
        String message = ctx.getArgument("message", String.class);
        for (Bot bot : BotManager.allBots) {
            if (bot.connection.bot.getName().getString().equals(nick)) {
                bot.connection.bot.sendChatMessage(message);
            }
        }
        return SINGLE_SUCCESS;
    }

    @Native
    private int handleAddAutoLes(CommandContext<ISuggestionProvider> ctx) {
        String nick = ctx.getArgument("botnick", String.class);
        for (Bot bot : BotManager.allBots) {
            if (bot.connection.bot.getName().getString().equals(nick)) {
                if (BotManager.autoLesBots.stream().noneMatch(b -> b.connection.bot.getName().getString().equals(nick))) {
                    BotManager.autoLesBots.add(bot);
                    ChatUtil.addText(new StringTextComponent("Бот " + nick + " добавлен в AutoLes.").mergeStyle(TextFormatting.GREEN));
                }
            }
        }
        return SINGLE_SUCCESS;
    }

    @Native
    private int handleRemoveAutoLes(CommandContext<ISuggestionProvider> ctx) {
        String nick = ctx.getArgument("botnick", String.class);
        BotManager.autoLesBots.removeIf(bot -> bot.connection.bot.getName().getString().equals(nick));
        ChatUtil.addText(new StringTextComponent("Бот " + nick + " удален из AutoLes.").mergeStyle(TextFormatting.YELLOW));
        return SINGLE_SUCCESS;
    }

    @Native
    private int handleListAutoLes(CommandContext<ISuggestionProvider> ctx) {
        String list = BotManager.autoLesBots.stream()
                .map(bot -> bot.connection.bot.getName().getString())
                .collect(Collectors.joining(", "));

        if (list.isEmpty()) {
            ChatUtil.addText(new StringTextComponent("autoles: пусто").mergeStyle(TextFormatting.RED));
        } else {
            ChatUtil.addText(new StringTextComponent("autoles: " + list).mergeStyle(TextFormatting.GRAY));
        }
        return SINGLE_SUCCESS;
    }
    @Native
    private CompletableFuture<Suggestions> suggestAllBots(CommandContext<ISuggestionProvider> ctx, SuggestionsBuilder builder) {
        return ISuggestionProvider.suggest(BotManager.allBots.stream()
                .map(bot -> bot.connection.bot.getName().getString()), builder);
    }
    @Native
    private CompletableFuture<Suggestions> suggestAutoLesBots(CommandContext<ISuggestionProvider> ctx, SuggestionsBuilder builder) {
        return ISuggestionProvider.suggest(BotManager.autoLesBots.stream()
                .map(bot -> bot.connection.bot.getName().getString()), builder);
    }
    @Native
    private CompletableFuture<Suggestions> suggestNonAutoLesBots(CommandContext<ISuggestionProvider> ctx, SuggestionsBuilder builder) {
        return ISuggestionProvider.suggest(BotManager.allBots.stream()
                .filter(bot -> BotManager.autoLesBots.stream().noneMatch(b -> b.connection.bot.getName().getString().equals(bot.connection.bot.getName().getString())))
                .map(bot -> bot.connection.bot.getName().getString()), builder);
    }
}