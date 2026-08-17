package sky.core.cmd.impl;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import sky.core.cmd.Command;
import sky.core.utils.Wrapper;
import sky.core.utils.misc.ChatUtil;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class GpsCommand extends Command implements ArgumentType<String>, Wrapper {
    public static boolean gpsEnabled = false;
    public static double x = 0.0;
    public static double z = 0.0;

    public GpsCommand() {
        super("gps");
    }

    @Override
    public void run(LiteralArgumentBuilder<ISuggestionProvider> builder) {
        builder.executes(this::invalidArgs);

        builder.then(args("x", new DoubleArgumentType()).executes(context -> {
            String input = context.getInput();
            return invalidArgs(context);
        }).then(args("z", new DoubleArgumentType()).executes(context -> {
            double newX = context.getArgument("x", Double.class);
            double newZ = context.getArgument("z", Double.class);
            gpsEnabled = true;
            x = newX;
            z = newZ;
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("off").executes(context -> {
            gpsEnabled = false;
            x = 0.0;
            z = 0.0;
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("info").executes(context -> {
            if (!gpsEnabled) {
                IFormattableTextComponent message = new StringTextComponent("Сейчас GPS отключен!").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
                ChatUtil.addText(message);
            } else {
                IFormattableTextComponent prefix = new StringTextComponent("Информация о текущем GPS: ").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
                IFormattableTextComponent xLabel = new StringTextComponent("x: ").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
                IFormattableTextComponent xValue = new StringTextComponent(String.format("%.1f", x).replace(",", ".")).setStyle(Style.EMPTY.applyFormatting(TextFormatting.WHITE));
                IFormattableTextComponent zLabel = new StringTextComponent(" z: ").setStyle(Style.EMPTY.applyFormatting(TextFormatting.GRAY));
                IFormattableTextComponent zValue = new StringTextComponent(String.format("%.1f", z).replace(",", ".")).setStyle(Style.EMPTY.applyFormatting(TextFormatting.WHITE));
                ChatUtil.addText(new StringTextComponent("").append(prefix).append(xLabel).append(xValue).append(zLabel).append(zValue));
            }
            return SINGLE_SUCCESS;
        }));
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        return reader.readString();
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
}