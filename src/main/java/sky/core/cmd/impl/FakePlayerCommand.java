package sky.core.cmd.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.entity.player.RemoteClientPlayerEntity;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import sky.core.cmd.Command;
import sky.core.utils.Wrapper;
import sky.core.utils.misc.ChatUtil;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class FakePlayerCommand extends Command implements Wrapper {

    // ID для сущности фейк-игрока (отрицательное значение, чтобы не конфликтовать с сервером)
    private static final int FAKE_PLAYER_ID = -1337;

    public FakePlayerCommand() {
        super("fp", "fakeplayer");
    }

    @Override
    public void run(LiteralArgumentBuilder<ISuggestionProvider> builder) {
        builder.executes(context -> {
            ChatUtil.addText(new StringTextComponent("Ошибка в использовании").mergeStyle(TextFormatting.GRAY));
            ChatUtil.addText(new StringTextComponent(".fp add ").mergeStyle(TextFormatting.WHITE)
                    .append(new StringTextComponent("- добавить бота").mergeStyle(TextFormatting.GRAY)));
            ChatUtil.addText(new StringTextComponent(".fp remove ").mergeStyle(TextFormatting.WHITE)
                    .append(new StringTextComponent("- удалить бота").mergeStyle(TextFormatting.GRAY)));
            return SINGLE_SUCCESS;
        });

        builder.then(literal("add").executes(context -> {
            if (mc.world == null || mc.player == null) {
                return 0;
            }

            try {
                RemoteClientPlayerEntity fakePlayer = new RemoteClientPlayerEntity(mc.world, mc.player.getGameProfile());
                fakePlayer.inventory = mc.player.inventory;
                fakePlayer.copyLocationAndAnglesFrom(mc.player);
                fakePlayer.rotationYawHead = mc.player.rotationYawHead;

                mc.world.addEntity(FAKE_PLAYER_ID, fakePlayer);

                ChatUtil.addText(new StringTextComponent("Бот успешно добавлен.").mergeStyle(TextFormatting.GREEN));
            } catch (Exception e) {
                ChatUtil.addText(new StringTextComponent("Произошла ошибка при создании бота.").mergeStyle(TextFormatting.RED));
                e.printStackTrace();
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("remove").executes(context -> {
            if (mc.world == null) {
                return 0;
            }

            try {
                mc.world.removeEntityFromWorld(FAKE_PLAYER_ID);
                ChatUtil.addText(new StringTextComponent("Бот успешно удален.").mergeStyle(TextFormatting.GRAY));
            } catch (Exception e) {
                ChatUtil.addText(new StringTextComponent("Ошибка при удалении бота.").mergeStyle(TextFormatting.RED));
            }
            return SINGLE_SUCCESS;
        }));
    }
}