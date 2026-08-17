package sky.core.modules.impl.player;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.Items;
import net.minecraft.item.SkullItem;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import sky.core.handlers.impl.LookHandler;
import sky.core.utils.component.impl.RotationComponent;
import sky.core.events.EventUpdate;
import sky.core.handlers.impl.Rotation;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.impl.combat.AttackAura;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.MultiBooleanSetting;

public class AimingItems extends Module {
    private final MultiBooleanSetting aimAt = new MultiBooleanSetting(
            "Наводиться на",
            new BooleanSetting("Голову игрока", true),
            new BooleanSetting("Элитру", true),
            new BooleanSetting("Осколки", true),
            new BooleanSetting("Ауры", false)
    );

    private Rotation targetRotation;
    private ItemEntity targetItem;

    public AimingItems() {
        super("AutoPilot", "Притягивает к выбранным предметам", Category.Player);
        addSettings(aimAt);
    }

    @EventTarget
    private void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        targetItem = null;
        targetRotation = null;
        for (Entity entity : mc.world.getAllEntities()) {
            if (!(entity instanceof ItemEntity itemEntity)) continue;
            if (!isItemOfInterest(itemEntity)) continue;

            targetItem = itemEntity;
            targetRotation = calculateRotation(itemEntity.getPositionVec().add(itemEntity.getMotion().scale(1.1F)));
            break;
        }

        if (targetItem == null || targetRotation == null) return;

        if (AttackAura.target == null) {
            LookHandler.setActive(false);
        }
        RotationComponent.update(targetRotation, 360f, 360f, 360f, 360f, 1, 5, true);
    }

    private Rotation calculateRotation(Vector3d target) {
        Vector3d eyes = mc.player.getEyePosition(1.0F);
        double diffX = target.x - eyes.x;
        double diffY = target.y - eyes.y;
        double diffZ = target.z - eyes.z;
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) (MathHelper.atan2(diffZ, diffX) * (180.0D / Math.PI)) - 90.0F;
        float pitch = (float) (-(MathHelper.atan2(diffY, diffXZ) * (180.0D / Math.PI)));
        return new Rotation(yaw, pitch);
    }

    private boolean isItemOfInterest(ItemEntity item) {
        var itemType = item.getItem().getItem();
        return (itemType instanceof SkullItem && Boolean.TRUE.equals(aimAt.is("Голову игрока")))
                || (itemType == Items.GHAST_TEAR && Boolean.TRUE.equals(aimAt.is("Осколки")))
                || (itemType == Items.CLAY_BALL && Boolean.TRUE.equals(aimAt.is("Ауры")))
                || (itemType == Items.WHITE_DYE && Boolean.TRUE.equals(aimAt.is("Ауры")))
                || (itemType == Items.POPPED_CHORUS_FRUIT && Boolean.TRUE.equals(aimAt.is("Ауры")))
                || (itemType == Items.GHAST_TEAR && Boolean.TRUE.equals(aimAt.is("Ауры")))
                || (itemType == Items.SUNFLOWER && Boolean.TRUE.equals(aimAt.is("Ауры")))
                || (itemType == Items.GOLD_NUGGET && Boolean.TRUE.equals(aimAt.is("Ауры")))
                || (itemType instanceof ElytraItem && Boolean.TRUE.equals(aimAt.is("Элитру")));
    }

    @Override
    public void onDisable() {
        targetRotation = null;
        targetItem = null;
        super.onDisable();
    }
}
