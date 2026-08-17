package sky.core.modules.impl.combat;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EnderCrystalEntity;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.network.play.client.CUseEntityPacket;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.events.EventPacket;
import sky.core.utils.math.MathUtil;

public class PacketCriticals  extends Module {
    public static boolean cancelCrit;

    public static ModeSetting critType = new ModeSetting("Тип критов", "Matrix", "Matrix", "Matrix2", "MatrixSmart", "NCP", "MatrixStand", "GrimPacket", "Grim");

    public PacketCriticals() {
        super("Criticals", "Наносит критический удар по противнику под плавным падением или в паутине.", Category.Combat);
        addSettings(critType);
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (!event.isSend()) return;
        if (!(event.getPacket() instanceof CUseEntityPacket packet)) return;
        if (packet.getAction() != CUseEntityPacket.Action.ATTACK) return;
        if (mc.player == null || mc.world == null) return;
        if (mc.player.isElytraFlying()) return;

        if (critType.is("GrimPacket")) {
            Entity entity = packet.getEntityFromWorld(mc.world);
            if (entity == null || entity instanceof EnderCrystalEntity || cancelCrit) return;
            if (AttackAura.target == null) return;
            if (!mc.player.isInWeb()) return;

            mc.player.fallDistance = 0.001f;
            mc.player.connection.sendPacket(new CPlayerPacket.PositionRotationPacket(
                    mc.player.getPosX(),
                    mc.player.getPosY() + (-(mc.player.fallDistance = MathUtil.randomizeFloat(1e-7F, 1e-6F))),
                    mc.player.getPosZ(),
                    AttackAura.rotate.x,
  