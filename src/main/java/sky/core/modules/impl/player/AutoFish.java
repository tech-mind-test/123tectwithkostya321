package sky.core.modules.impl.player;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.events.EventPacket;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.utils.math.TimeUtil;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.network.play.server.SPlaySoundEffectPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;


public class AutoFish extends Module {

    private final BooleanSetting takeRod = new BooleanSetting("Автоматически брать удочку", true);
    private boolean isCached = false;
    private boolean needCached = false;
    private int rodHotbarSlot = -1;
    private final TimeUtil timer = new TimeUtil();

    public AutoFish() {
        super("AutoFish", "Автоматизирует процесс рыбалки", Category.Player);
        addSettings(takeRod);
    }

    @java.lang.Override
    public void onDisable() {
        isCached = false;
        needCached = false;
        rodHotbarSlot = -1;
        timer.reset();
        super.onDisable();
    }


    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        if (takeRod.get() && rodHotbarSlot == -1) {
            findBestFishingRodInHotbar();
        }

        if (rodHotbarSlot != -1 && mc.player.inventory.currentItem != rodHotbarSlot) {
            mc.player.connection.sendPacket(new CHeldItemChangePacket(rodHotbarSlot));
            mc.player.inventory.currentItem = rodHotbarSlot;
        }

        if (isCached && timer.hasReached(600)) {
            useFishingRod();
            isCached = false;
            needCached = true;
            timer.reset();
        }

        if (needCached && timer.hasReached(300)) {
            useFishingRod();
            needCached = false;
            timer.reset();
        }
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (mc.player == null || mc.world == null) return;

        if (event.getPacket() instanceof SPlaySoundEffectPacket packet) {
            if (packet.getSound() == SoundEvents.ENTITY_FISHING_BOBBER_SPLASH) {
                isCached = true;
                timer.reset();
            }
        }
    }

    
    private void useFishingRod() {
        if (rodHotbarSlot != -1 && mc.player.inventory.getStackInSlot(rodHotbarSlot).getItem() instanceof FishingRodItem) {
            if (mc.player.inventory.currentItem != rodHotbarSlot) {
                mc.player.connection.sendPacket(new CHeldItemChangePacket(rodHotbarSlot));
                mc.player.inventory.currentItem = rodHotbarSlot;
            }
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
            mc.player.swingArm(Hand.MAIN_HAND);
        }
    }

    
    private void findBestFishingRodInHotbar() {
        int bestRodSlot = -1;
        int maxEnchantments = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack.getItem() instanceof FishingRodItem) {
                int enchantmentCount = EnchantmentHelper.getEnchantments(stack).size();
                if (enchantmentCount > maxEnchantments) {
                    maxEnchantments = enchantmentCount;
                    bestRodSlot = i;
                }
            }
        }

        if (bestRodSlot != -1) {
            rodHotbarSlot = bestRodSlot;
        }
    }
}