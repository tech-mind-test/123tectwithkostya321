package sky.core.cmd.impl;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import sky.core.SkyCore;
import sky.core.cmd.Command;
import sky.core.utils.managers.impl.WaypointManager;
import sky.core.utils.Wrapper;
import sky.core.utils.misc.ChatUtil;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class WaypointCommand extends Command implements ArgumentType<String>, Wrapper {

    public WaypointCommand() {
        super("waypoint", "way");
    }

    @Override
    public void run(LiteralArgumentBuilder<ISuggestionProvider> builder) {
        builder.executes(this::invalidArgs);

        builder.then(literal("add").then(args("name", this).executes(ctx -> {
            String name = ctx.getArgument("name", String.class);
            if (!isValidName(name)) {
                return invalidArgs(ctx);
            }
            if (mc.player == null) return invalidArgs(ctx);
            double px = mc.player.getPosX();
            double py = mc.player.getPosY();
            double pz = mc.player.getPosZ();
            String serverKey = currentServerKey();
            getManager().addWaypoint(serverKey, name, px, py, pz);
            IFormattableTextComponent prefix = new StringTextComponent("Точка с именем '").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
            IFormattableTextComponent nameText = new StringTextComponent(name).setStyle(Style.EMPTY.applyFormatting(TextFormatting.WHITE));
            IFormattableTextComponent suffix = new StringTextComponent("' установлена!").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
            ChatUtil.addText(new StringTextComponent("").append(prefix).append(nameText).append(suffix));
            return SINGLE_SUCCESS;
        }).then(args("x", new DoubleArgumentType()).then(args("y", new DoubleArgumentType()).then(args("z", new DoubleArgumentType()).executes(ctx -> {
            String name = ctx.getArgument("name", String.class);
            double x = ctx.getArgument("x", Double.class);
            double y = ctx.getArgument("y", Double.class);
            double z = ctx.getArgument("z", Double.class);
            String serverKey = currentServerKey();
            getManager().addWaypoint(serverKey, name, x, y, z);
            IFormattableTextComponent prefix = new StringTextComponent("Точка с именем '").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
            IFormattableTextComponent nameText = new StringTextComponent(name).setStyle(Style.EMPTY.applyFormatting(TextFormatting.WHITE));
            IFormattableTextComponent suffix = new StringTextComponent("' установлена!").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
            ChatUtil.addText(new StringTextComponent("").append(prefix).append(nameText).append(suffix));
            return SINGLE_SUCCESS;
        }))))));

        builder.then(literal("remove").then(args("name", this).suggests(this::suggestWaypointNames).executes(ctx -> {
            String name = ctx.getArgument("name", String.class);
            String serverKey = currentServerKey();
            boolean removed = getManager().removeWaypoint(serverKey, name);
            if (removed) {
                IFormattableTextComponent prefix = new StringTextComponent("Точка с именем '").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
                IFormattableTextComponent nameText = new StringTextComponent(name).setStyle(Style.EMPTY.applyFormatting(TextFormatting.WHITE));
                IFormattableTextComponent suffix = new StringTextComponent("' удалена!").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
                ChatUtil.addText(new StringTextComponent("").append(prefix).append(nameText).append(suffix));
            }
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("clear").executes(ctx -> {
            String serverKey = currentServerKey();
            getManager().clear(serverKey);
            ChatUtil.addText(new StringTextComponent("Все точки успешно удалены!").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY)));
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("list").executes(ctx -> {
            String serverKey = currentServerKey();
            List<WaypointManager.WaypointEntry> list = getManager().getWaypoints(serverKey);
            if (list.isEmpty()) {
                ChatUtil.addText(new StringTextComponent("Список точек пуст!").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY)));
                return SINGLE_SUCCESS;
            }
            for (WaypointManager.WaypointEntry wp : list) {
                ChatUtil.addText(new StringTextComponent("Список точек:").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY)));
                IFormattableTextComponent name = new StringTextComponent(wp.getName()).setStyle(Style.EMPTY.applyFormatting(TextFormatting.WHITE));
                IFormattableTextComponent xLabel = new StringTextComponent(" {x: ").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
                IFormattableTextComponent xVal = new StringTextComponent(String.format("%.1f", wp.getX()).replace(",", ".")).setStyle(Style.EMPTY.applyFormatting(TextFormatting.WHITE));
                IFormattableTextComponent yLabel = new StringTextComponent(", y: ").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
                IFormattableTextComponent yVal = new StringTextComponent(String.format("%.1f", wp.getY()).replace(",", ".")).setStyle(Style.EMPTY.applyFormatting(TextFormatting.WHITE));
                IFormattableTextComponent zLabel = new StringTextComponent(", z: ").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
                IFormattableTextComponent zVal = new StringTextComponent(String.format("%.1f", wp.getZ()).replace(",", ".")).setStyle(Style.EMPTY.applyFormatting(TextFormatting.WHITE));
                IFormattableTextComponent closeBrace = new StringTextComponent("}").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));

                IFormattableTextComponent ipLabel = new StringTextComponent(" {ip: ").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
                IFormattableTextComponent ipVal = new StringTextComponent(serverKey).setStyle(Style.EMPTY.applyFormatting(TextFormatting.WHITE));
                IFormattableTextComponent ipClose = new StringTextComponent("} ").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));

                String removeCmd = ".way remove " + wp.getName();
                IFormattableTextComponent removeBtn = new StringTextComponent("[Удалить]").setStyle(Style.EMPTY.applyFormatting(TextFormatting.RED).setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, removeCmd)).setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("Клик для удаления точки").setStyle(Style.EMPTY.applyFormatting(TextFormatting.RED)))));

                ChatUtil.addText(new StringTextComponent("").append(name).append(xLabel).append(xVal).append(yLabel).append(yVal).append(zLabel).append(zVal).append(closeBrace).append(ipLabel).append(ipVal).append(ipClose).append(new StringTextComponent(" ").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY))).append(removeBtn));
            }
            ChatUtil.addText(new StringTextComponent("Всего: ").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY)).append(new StringTextComponent(String.valueOf(list.size())).setStyle(Style.EMPTY.applyFormatting(TextFormatting.WHITE))));
            return SINGLE_SUCCESS;
        }));
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        return reader.readString();
    }

    private boolean isValidName(String name) {
        if (name == null) return false;
        String s = name.trim();
        if (s.length() > 8) s = s.substring(0, 8);
        return s.matches("^[A-Za-z0-9]{3,8}$");
    }

    private WaypointManager getManager() {
        return SkyCore.getInstance().getWaypointManager();
    }

    private String currentServerKey() {
        ServerData data = mc.getCurrentServerData();
        return data != null ? data.serverIP : "singleplayer";
    }

    private static class DoubleArgumentType implements ArgumentType<Double> {
        @Override
        public Double parse(StringReader reader) throws CommandSyntaxException {
            try {
                return reader.readDouble();
            } catch (CommandSyntaxException e) {
                throw new DynamicCommandExceptionType(value -> new StringTextComponent("Неверный формат координаты: " + value)).create(reader.getString());
            }
        }
    }

    private CompletableFuture<Suggestions> suggestWaypointNames(com.mojang.brigadier.context.CommandContext<ISuggestionProvider> ctx, SuggestionsBuilder builder) {
        String serverKey = currentServerKey();
        List<String> names = getManager().getWaypoints(serverKey).stream().map(WaypointManager.WaypointEntry::getName).collect(Collectors.toList());
        return ISuggestionProvider.suggest(names, builder);
    }
}


