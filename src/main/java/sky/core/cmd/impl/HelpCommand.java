package sky.core.cmd.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import sky.core.cmd.Command;
import sky.core.ui.gui.themes.ThemeEditor;
import sky.core.ui.gui.themes.ThemeSettings;
import sky.core.utils.misc.ChatUtil;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.GradientUtil;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class HelpCommand extends Command {

    public HelpCommand() {
        super("help");
    }

    private enum BracketType {
        PARENTHESES("(", ")"),
        SQUARE("[", "]"),
        ANGLE("<", ">"),
        NONE("", "");

        private final String open;
        private final String close;

        BracketType(String open, String close) {
            this.open = open;
            this.close = close;
        }
    }

    private static class CommandInfo {
        String name;
        String[] subcommands;
        BracketType bracketType;
        String alias;

        CommandInfo(String name, String[] subcommands, BracketType bracketType, String alias) {
            this.name = name;
            this.subcommands = subcommands;
            this.bracketType = bracketType;
            this.alias = alias;
        }

        CommandInfo(String name, String[] subcommands, BracketType bracketType) {
            this(name, subcommands, bracketType, null);
        }
    }

    @Override
    public void run(LiteralArgumentBuilder<ISuggestionProvider> builder) {
        builder.executes(ctx -> {
            int start = ThemeEditor.getColor(ThemeSettings.LOGO);
            int end = ColorUtil.darken(start, 0.5f);

            CommandInfo[] commands = {
                    new CommandInfo("config", new String[]{"clear", "dir", "list", "load", "remove", "save", "reset"}, BracketType.PARENTHESES, "cfg"),
                    new CommandInfo("friend", new String[]{"add", "clear", "list", "remove"}, BracketType.PARENTHESES, "fr"),
                    new CommandInfo("macros", new String[]{"add", "clear", "list", "remove"}, BracketType.PARENTHESES, "mac"),
                    new CommandInfo("bind", new String[]{"add", "clear", "list", "remove"}, BracketType.PARENTHESES),
                    new CommandInfo("staff", new String[]{"add", "clear", "list", "remove"}, BracketType.PARENTHESES),
                    new CommandInfo("gps", new String[]{"off", "info", "<pos>"}, BracketType.SQUARE),
                    new CommandInfo("waypoint", new String[]{"add", "clear", "list", "remove"}, BracketType.PARENTHESES, "way"),
                    new CommandInfo("prefix", new String[]{"<prefix>"}, BracketType.ANGLE),
                    new CommandInfo("help", new String[]{"<command>"}, BracketType.SQUARE),
                    new CommandInfo("autocontract", new String[]{"<nickname>"}, BracketType.ANGLE, "act")
                //    new CommandInfo("nuker", new String[]{"add", "remove", "clear", "list"}, BracketType.PARENTHESES, "nuk")
            };

            for (CommandInfo cmd : commands) {
                displayCommand(cmd.name, cmd.subcommands, cmd.bracketType, start, end);
                if (cmd.alias != null) {
                    displayAlias(cmd.alias, cmd.name, start, end);
                }
            }

            return SINGLE_SUCCESS;
        });
    }

    private void displayCommand(String cmd, String[] subcommands, BracketType bracketType, int start, int end) {
        IFormattableTextComponent name = GradientUtil.gradient("." + cmd, start, end);
        IFormattableTextComponent details = new StringTextComponent(bracketType.open).setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));

        for (int i = 0; i < subcommands.length; i++) {
            details.append(new StringTextComponent(subcommands[i]).setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY)));
            if (i < subcommands.length - 1) {
                details.append(new StringTextComponent(" ¦ ").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY)));
            }
        }

        details.append(new StringTextComponent(bracketType.close).setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY)));
        ChatUtil.addText(new StringTextComponent("").append(name).append(new StringTextComponent(" ")).append(details));
    }

    private void displayAlias(String alias, String full, int start, int end) {
        IFormattableTextComponent aliasComp = GradientUtil.gradient("." + alias, start, end);
        IFormattableTextComponent map = new StringTextComponent(" -> " + full).setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
        ChatUtil.addText(new StringTextComponent("").append(aliasComp).append(map));
    }
}