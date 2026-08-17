package sky.core.modules.impl.movement;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;

import java.util.*;

public class TPLoot extends Module {

    private final ModeSetting returnMode = new ModeSetting("Возврат", "Назад", "Назад", "/home", "Никуда");
    private final SliderSetting commandDelay = new SliderSetting("Задержка /home", 100, 50, 200, 10, () -> returnMode.is("/home"));
    private final SliderSetting radius = new SliderSetting("Радиус", 100, 10, 150, 5);

    private final Set<Item> valuable = new HashSet<>(Arrays.asList(
            Items.TOTEM_OF_UNDYING,
            Items.CRYING_OBSIDIAN,
            Items.ENCHANTED_GOLDEN_APPLE,
            Items.ELYTRA,
            Items.NETHERITE_INGOT,
            Items.PLAYER_HEAD
    ));

    private Vector3d deathPos, startPos;
    private boolean pickedLoot;
    private int cooldown;

    public TPLoot() {
        super("TP Loot", "Телепортирует к вещам", Category.Movement);
        addSettings(returnMode, commandDelay, radius);
    }

    @java.lang.Override
    public void onDisable() {
        reset();
        super.onDisable();
    }

    @EventTarget
    public void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        if (cooldown > 0) cooldown--;

        if (!mc.player.abilities.allowFlying && !mc.player.abilities.isFlying) {
            reset();
            return;
        }

        if (mc.player.getHealth() <= 0) {
            deathPos = mc.player.getPositionVec();
            pickedLoot = false;
            startPos = null;
            return;
        }

        if (deathPos != null) {
            tp(mc.player, deathPos);
            deathPos = null;
            return;
        }

        ItemEntity target = getTarget(mc.player);

        if (target != null) {
            if (startPos == null) startPos = mc.player.getPositionVec();
            pickedLoot = true;
            tp(mc.player, findSafePos(mc.player, target.getPositionVec()));
        } else if (pickedLoot) {
            if (returnMode.is("Назад") && startPos != null) {
                tp(mc.player, startPos);
                reset();
            } else if (returnMode.is("/home")) {
                sendHome(mc.player);
            } else {
                reset();
            }
        } else {
            startPos = null;
        }
    }

    private ItemEntity getTarget(ClientPlayerEntity p) {
        Vector3d pos = p.getPositionVec();
        double r = radius.get();
        AxisAlignedBB box = new AxisAlignedBB(pos.x - r, pos.y - r, pos.z - r, pos.x + r, pos.y + r, pos.z + r);

        return mc.world.getEntitiesWithinAABB(ItemEntity.class, box, this::isValid)
                .stream()
                .max(Comparator.comparingDouble(i -> getPriority(i.getItem().getItem()) * 10000 - i.getDistanceSq(pos)))
                .orElse(null);
    }

    private void tp(ClientPlayerEntity p, Vector3d pos) {
        if (p.getDistanceSq(pos) < 0.01) return;
        p.setMotion(0, 0, 0);
        p.setPosition(pos.x, pos.y, pos.z);
        if (mc.getConnection() != null) {
            mc.getConnection().sendPacket(new CPlayerPacket.PositionPacket(pos.x, pos.y, pos.z, true));
            mc.getConnection().sendPacket(new CPlayerPacket.PositionRotationPacket(pos.x, pos.y, pos.z, p.rotationYaw, p.rotationPitch, true));
        }
    }

    private void sendHome(ClientPlayerEntity p) {
        if (!pickedLoot || cooldown > 0) return;
        p.sendChatMessage("/home home");
        reset();
        cooldown = (int) (commandDelay.get() / 50f);
    }

    private boolean isValid(ItemEntity e) {
        return e.isAlive() && valuable.contains(e.getItem().getItem());
    }

    private int getPriority(Item i) {
        if (i == Items.PLAYER_HEAD) return 10;
        if (i == Items.ENCHANTED_GOLDEN_APPLE) return 8;
        if (i == Items.TOTEM_OF_UNDYING || i == Items.ELYTRA) return 5;
        if (i == Items.NETHERITE_INGOT) return 4;
        return 1;
    }

    private Vector3d findSafePos(ClientPlayerEntity p, Vector3d pos) {
        int x = (int) Math.floor(pos.x), y = (int) Math.floor(pos.y), z = (int) Math.floor(pos.z);
        double w = p.getWidth(), h = p.getHeight();
        int[][] offs = {{0,0}, {-1,0}, {1,0}, {0,-1}, {0,1}, {-1,-1}, {-1,1}, {1,-1}, {1,1}};

        for (int[] o : offs) {
            double nx = x + o[0] + 0.5, ny = y, nz = z + o[1] + 0.5;
            AxisAlignedBB bb = new AxisAlignedBB(nx - w/2, ny, nz - w/2, nx + w/2, ny + h, nz + w/2);
            if (mc.world.getCollisionShapes(p, bb).count() == 0) return new Vector3d(nx, ny, nz);
        }
        return new Vector3d(x + 0.5, y + 1, z + 0.5);
    }

    private void reset() {
        deathPos = null;
        startPos = null;
        pickedLoot = false;
        cooldown = 0;
    }
}