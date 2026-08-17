
package sky.core.modules.impl.player;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.ibm.icu.impl.Pair;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.block.Blocks;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.server.SPlaySoundEffectPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import org.joml.Vector2f;
import sky.core.events.EventPacket;
import sky.core.events.EventRender2D;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.utils.math.MathUtil;
import sky.core.utils.misc.ServerUtil;
import sky.core.utils.player.PlayerUtil;
import sky.core.utils.render.ColorUtil;
import sky.core.utils.render.ProjectUtil;
import sky.core.utils.render.RenderUtil;
import sky.core.utils.render.font.StyledFont;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Test extends Module {

    private final List<Pair<Long, Vector3d>> consumables = new ArrayList<>();
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final float fontSize = 16.0f;
 //   private final CustomFont font = Fonts.SFREGULAR.get(fontSize);

    public Test() {
        super("TrapDuration", "Показывает время жизни расходников.",Category.Visuals);
    }

    @EventTarget
    public void event(EventPacket e) {
        if (e.isReceive()) {
            IPacket<?> packet = e.getPacket();

//                [19:43:28] [Netty Client IO #0/INFO]: [STDERR]: minecraft:entity.wither.break_block
//                        [19:43:28] [Netty Client IO #0/INFO]: [STDERR]: minecraft:entity.evoker_fangs.attack
//                        [19:43:28] [Netty Client IO #0/INFO]: [STDERR]: minecraft:ui.toast.challenge_complete
//                        [19:43:28] [Netty Client IO #0/INFO]: [STDERR]: minecraft:entity.ender_dragon.growl

//                [19:43:48] [Netty Client IO #0/INFO]: [STDERR]: minecraft:item.totem.use
//                        [19:43:48] [Netty Client IO #0/INFO]: [STDERR]: minecraft:entity.evoker.prepare_attack
//                        [19:43:48] [Netty Client IO #0/INFO]: [STDERR]: minecraft:entity.ender_dragon.hurt

            if (packet instanceof SPlaySoundEffectPacket) {
                SPlaySoundEffectPacket wrapper = (SPlaySoundEffectPacket) packet;
                // CMD.addMessage(wrapper.getSound().getName());
                if (wrapper.getSound().getName().getPath().equals("block.piston.contract") || wrapper.getSound().getName().getPath().equals("block.piston.extend") || wrapper.getSound().getName().getPath().equals("entity.wither.break_block")) {
                    consumables.add(Pair.of(System.currentTimeMillis() + 15000,
                            Vector3d.copyCentered(new BlockPos(wrapper.getX(), wrapper.getY(), wrapper.getZ()))));
                }
                if (wrapper.getSound().getName().getPath().equals("block.anvil.place") || wrapper.getSound().getName().getPath().equals("entity.wither.break_block")) {
                    BlockPos soundPos = new BlockPos(wrapper.getX(), wrapper.getY(), wrapper.getZ());
                    long delay = 250;


                    if (scheduler.isShutdown() || scheduler.isTerminated()) {
                        scheduler = Executors.newSingleThreadScheduledExecutor();
                    }
                    scheduler.schedule(() -> {
                        List<BlockPos> cubes = PlayerUtil.getCube(soundPos, 4, 4);
                        cubes.stream()
                                .filter(pos -> MathUtil.getDistance(soundPos, pos) > 2 &&
                                        mc.world.getBlockState(pos).getBlock().equals(Blocks.COBBLESTONE))
                                .min(Comparator.comparing(pos -> MathUtil.getDistance(soundPos, pos)))
                                .ifPresent(pos -> {
                                    long andesiteCount = PlayerUtil.getCube(pos, 1, 1).stream()
                                            .filter(pos2 -> mc.world.getBlockState(pos2).getBlock().equals(Blocks.ANDESITE))
                                            .count();

                                    if (andesiteCount == 16 || andesiteCount == 9 || andesiteCount == 10) {
                                        int time = andesiteCount == 16 ? 60000 : 20000;
                                        consumables.add(Pair.of(System.currentTimeMillis() + time - delay,
                                                Vector3d.copyCentered(pos).add(0, andesiteCount == 16 ? -0.5 : 0, 0)));
                                    }
                                });
                    }, delay, TimeUnit.MILLISECONDS);

                }
            }
        }


    }

    @EventTarget
    public void asad(EventRender2D eventRender2D,StyledFont fonts) {
        if (!ServerUtil.isConnectedToServer("funtime")) return;

        Iterator<Pair<Long, Vector3d>> iterator = consumables.iterator();
        while (iterator.hasNext()) {
            Pair<Long, Vector3d> cons = iterator.next();
            double timeLeft = (double) (cons.first - System.currentTimeMillis()) / 1000;

            if (timeLeft <= 0) {
                iterator.remove();
                continue;
            }

            Vector2f vec2f = ProjectUtil.project2D(cons.second);
            String text = MathUtil.round(timeLeft, 1) + "с";

            int width  = (int) fonts.getWidth(text);
            float posX = vec2f.x - width / 2f;
            float posY = vec2f.y;
            float textOffsetY = 5f;

            RenderUtil.drawRect(posX - 1, posY - 1, posX + width + 2, posY + fontSize - 1, ColorUtil.getColor(0, 0, 0, 128));

            MatrixStack matrixStack = new MatrixStack();
        //    Fonts.sf_medium.drawString(matrixStack, text, posX, posY + textOffsetY, -1);

    }
    }

    @java.lang.Override
    public void onDisable() {
        consumables.clear();
        scheduler.shutdown();
        // super.onDisable();
    }

}

