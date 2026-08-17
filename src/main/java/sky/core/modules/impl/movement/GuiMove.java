package sky.core.modules.impl.movement;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.EditSignScreen;
import net.minecraft.client.gui.screen.EnchantmentScreen;
import net.minecraft.client.gui.screen.inventory.*;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.client.CClickWindowPacket;
import net.minecraft.network.play.client.CCloseWindowPacket;
import sky.core.utils.component.impl.MoveComponent;
import sky.core.events.EventInventoryClose;
import sky.core.events.EventPacket;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.utils.math.TimeUtil;
import sky.core.utils.player.MoveUtil;

import java.util.ArrayList;
import java.util.List;

public class GuiMove extends Module {
    private final List<IPacket<?>> packet = new ArrayList<>();
    public boolean slow = false;

    BooleanSetting stop = new BooleanSetting("Обход кликов/свапов", false);

    private KeyBinding[] pressedKeys() {
        return new KeyBinding[]{mc.gameSettings.keyBindForward, mc.gameSettings.keyBindBack,
                mc.gameSettings.keyBindLeft, mc.gameSettings.keyBindRight, mc.gameSettings.keyBindJump
        };
    }

    public GuiMove() {
        super("Gui Move","Можна ходить с открытым инвентарем", Category.Movement);
        addSettings(stop);
    }

    @EventTarget
    private void onUpdt(EventUpdate e) {
        if (mc.player != null) {
            final KeyBinding[] pressedKeys = pressedKeys();
            if (!wait.isReached(51)) {
                for (KeyBinding keyBinding : pressedKeys) {
                    keyBinding.setPressed(false);
                }
                return;
            }
            if (mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof EditSignScreen ||
                    mc.currentScreen instanceof ChestScreen || mc.currentScreen instanceof ShulkerBoxScreen ||
                    mc.currentScreen instanceof CraftingScreen || mc.currentScreen instanceof AnvilScreen ||
                    mc.currentScreen instanceof FurnaceScreen || mc.currentScreen instanceof BlastFurnaceScreen ||
                    mc.currentScreen instanceof EnchantmentScreen || mc.player.isElytraFlying()) {
                return;
            }
            updateKeyBindingState(pressedKeys);
        }
    }

    @EventTarget
    public void onPacket(EventPacket e) {
        if (e.getPacket() instanceof CClickWindowPacket p && MoveUtil.isMoving()) {
            if (mc.currentScreen instanceof InventoryScreen) {
                packet.add(p);
                e.setCancelled(true);
            }
        }
    }

    public TimeUtil wait = new TimeUtil();

    @EventTarget
    public void onClose(EventInventoryClose e) {
        if (mc.currentScreen instanceof InventoryScreen && MoveUtil.isMoving()) {
            MoveComponent.stopTicks = 2;
            if (stop.get()) MoveComponent.stop = true;
            slow = true;
            new Thread(() -> {
                wait.reset();
                try {
                    Thread.sleep(51);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                for (IPacket p : packet) {
                    mc.player.connection.sendPacketWithoutEvent(p);
                }
                slow = false;
                packet.clear();
                mc.player.connection.sendPacket(new CCloseWindowPacket(mc.player.openContainer.windowId));
            }).start();
            e.setCancelled(true);
        }
    }

    @java.lang.Override
    public void onDisable() {
        super.onDisable();

        if (mc.currentScreen == null) return;
        for (KeyBinding keyBinding : pressedKeys()) {
            keyBinding.setPressed(false);
        }
    }

    private void updateKeyBindingState(KeyBinding[] keyBindings) {
        for (KeyBinding keyBinding : keyBindings) {
            int keyCode = keyBinding.getDefault().getKeyCode();
            boolean isKeyPressed = keyCode >= 0 && InputMappings.isKeyDown(mc.getMainWindow().getHandle(), keyCode);
            keyBinding.setPressed(isKeyPressed);
        }
    }
}

