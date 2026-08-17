package sky.core.modules.impl.player;

import com.darkmagician6.eventapi.eventapinew.EventTarget;
import com.adl.nativeprotect.Native;
import sky.core.events.EventChat;
import sky.core.events.EventContainerTick;
import sky.core.events.EventUpdate;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.ItemSetting;
import sky.core.modules.api.constructors.impl.ModeSetting;
import sky.core.modules.api.constructors.impl.SliderSetting;
import sky.core.ui.gui.autobuy.ItemList;
import sky.core.ui.gui.autobuy.PurchaseHistory;
import sky.core.utils.math.NumberUtil;
import sky.core.utils.misc.ChatUtil;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.text.ITextComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoBuy extends Module {
    public static AutoBuy instance;
    public final ModeSetting mode = new ModeSetting("Режим", "Спукитайм", "Спукитайм", "Холиворлд");
    private final BooleanSetting autosetup = new BooleanSetting("Авто сетап", false);
    private final SliderSetting procent = new SliderSetting("На сколько процентов меньше", 20, 1, 100, 1);

    private ItemSetting lastMatchedSetting = null;
    private boolean pendingSell = false;
    private long pendingSellPrice = 0L;
    private boolean moveInitiated = false;
    private int lastPurchasedCount = 1;
    private final List<SellTask> sellQueue = new ArrayList<>();
    private long nextActionTime = 0L;
    private long nextRefreshTime = 0L;
    private long reopenTime = 0L;
    private boolean autosetupRunning = false;
    private final List<ItemSetting> autosetupQueue = new ArrayList<>();
    private int autosetupIndex = 0;
    private int setupState = 0;
    private int stateTimer = 0;
    private long currentItemMinPrice = Long.MAX_VALUE;
    private ItemStack pendingPurchaseItem = ItemStack.EMPTY;
    private long pendingPurchasePrice = 0L;
    private ItemSetting pendingPurchaseSetting = null;
    private static final Pattern PURCHASE_PATTERN = Pattern.compile("Вы успешно купили (.+) за \\$([\\d,]+)");
    private long chatBlockTime = 0L;

    public AutoBuy() {
        super("Auto Buy", "Упрощает покупку предметов", Category.Player);
        instance = this;
        addSettings(mode, autosetup, procent);
    }

    @java.lang.Override
    public void onEnable() {
        super.onEnable();
        resetAll();
    }

    @java.lang.Override
    public void onDisable() {
        super.onDisable();
        resetAll();
    }

    @Native
    @EventTarget
    public void onChat(EventChat event) {
        if (mc.player == null || !isEnabled()) return;
        Matcher matcher = PURCHASE_PATTERN.matcher(event.getMessage().replaceAll("§.", "").replaceAll("[☃\\[\\]]", "").trim());
        if (matcher.find()) {
            try {
                long boughtPrice = Long.parseLong(matcher.group(2).replace(",", ""));
                if (!pendingPurchaseItem.isEmpty() && pendingPurchaseSetting != null) {
                    PurchaseHistory.add(pendingPurchaseItem.copy(), boughtPrice, "Аукцион");

                    if (pendingPurchaseSetting.isSellEnabled()) {
                        double markup = 1.0 + (pendingPurchaseSetting.getSellPercent() / 100.0);
                        long totalSellPrice = (long) (boughtPrice * markup);

                        sellQueue.add(new SellTask(pendingPurchaseSetting, lastPurchasedCount, totalSellPrice));
                        pendingSell = true;
                        pendingSellPrice = totalSellPrice;
                    }
                    pendingPurchaseItem = ItemStack.EMPTY;
                    pendingPurchaseSetting = null;
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Native
    @EventTarget
    public void onUpdate(EventUpdate e) {
        if (mc.player == null) return;
        if (autosetup.get()) {
            if (!autosetupRunning) {
                autosetupQueue.clear();
                ItemList.getItems().stream().filter(ItemSetting::get).forEach(autosetupQueue::add);
                if (autosetupQueue.isEmpty()) {
                    autosetup.set(false);
                    return;
                }
                autosetupRunning = true;
                autosetupIndex = 0;
                setupState = 0;
            }
            processAutoSetupState();
        } else if (autosetupRunning) autosetupRunning = false;
        if (pendingSell) handleAutoSell();
        if (!pendingSell && !sellQueue.isEmpty()) {
            SellTask next = sellQueue.get(0);
            lastMatchedSetting = next.setting;
            lastPurchasedCount = next.count;
            pendingSellPrice = next.price;
            pendingSell = true;
            moveInitiated = false;
        }

        if (!pendingSell && reopenTime > 0 && System.currentTimeMillis() > reopenTime) {
            if (System.currentTimeMillis() < chatBlockTime) return;
            if (mc.currentScreen != null) {
                if (!(mc.currentScreen instanceof ContainerScreen) || !mc.currentScreen.getTitle().getString().toLowerCase().contains("аукцион")) {
                    mc.player.closeScreen();
                    return;
                }
                return;
            }
            mc.player.sendChatMessage("/ah");
            chatBlockTime = System.currentTimeMillis() + 1000;
            reopenTime = 0;
        }
    }

    @Native
    private void processAutoSetupState() {
        if (autosetupIndex >= autosetupQueue.size()) {
            autosetupRunning = false;
            autosetup.set(false);
            if (mc.player != null) mc.player.closeScreen();
            return;
        }
        ItemSetting cur = autosetupQueue.get(autosetupIndex);
        boolean isHoly = mode.is("Холиворлд");
        switch (setupState) {
            case 0 -> {
                if (mc.currentScreen != null) mc.player.closeScreen();
                String searchTerm = ItemList.getSearchNameForItem(cur);
                mc.player.sendChatMessage("/ah search " + searchTerm.replaceAll("§.", "").trim());
                currentItemMinPrice = Long.MAX_VALUE;
                stateTimer = isHoly ? 80 : 10;
                setupState = 1;
            }
            case 1 -> {
                if (isAuctionOpen()) {
                    stateTimer = 15;
                    setupState = 2;
                } else if (--stateTimer <= 0) setupState = 3;
            }
            case 2 -> {
                if (--stateTimer <= 0 && !isAuctionOpen()) setupState = 3;
            }
            case 3 -> {
                if (currentItemMinPrice != Long.MAX_VALUE) {
                    long np = Math.max(1, (long) (currentItemMinPrice * (1.0 - procent.get() / 100.0)));
                    cur.setMaxPrice(np);
                    cur.setMaxPriceFromString(NumberUtil.formatThousands(String.valueOf(np)));
                    ChatUtil.addChatMessage(String.format("&f%s: &a%s$ &f-> &b%s$",
                            cur.getName(),
                            NumberUtil.formatThousands(String.valueOf(currentItemMinPrice)),
                            NumberUtil.formatThousands(String.valueOf(np))));
                } else ChatUtil.addChatMessage("&f" + cur.getName() + ": &cПредмет не найден");
                autosetupIndex++;
                if (mc.player != null) mc.player.closeScreen();
                stateTimer = isHoly ? 40 : 2;
                setupState = 4;
            }
            case 4 -> {
                if (--stateTimer <= 0) setupState = 0;
            }
        }
    }

    @EventTarget
    public void onEvent(EventContainerTick e) {
        if (mc.player == null) return;
        if (System.currentTimeMillis() < chatBlockTime) return;
        long now = System.currentTimeMillis();

        if (mode.is("Холиворлд")) {
            String title = getCurrentScreenTitle();
            if (title != null) {
                String clean = title.replaceAll("§.", "").toLowerCase();
                if (clean.contains("покупка") || clean.contains("confirm")) {
                    if (now < nextActionTime) return;
                    List<Integer> green = new ArrayList<>();
                    for (int i = 0; i < e.getItems().size(); i++)
                        if (e.getItems().get(i).getItem() == Items.LIME_STAINED_GLASS_PANE) green.add(i);

                    if (!green.isEmpty()) {
                        clickSlot(green.get(ThreadLocalRandom.current().nextInt(green.size())));
                        setRandomDelay();
                    }
                    return;
                }
            }
        }
        if (autosetup.get() && autosetupRunning && setupState == 2 && stateTimer <= 0) scanCurrentPage(e);
        else if (!autosetup.get()) handleAutoBuy(e);
    }


    private void scanCurrentPage(EventContainerTick e) {
        if (autosetupIndex >= autosetupQueue.size()) return;
        ItemSetting current = autosetupQueue.get(autosetupIndex);
        boolean isChunker = current.getName().contains("Прогрузчик чанков");
        for (int i = 0; i < 45; i++) {
            ItemStack stack = e.getItems().get(i);
            if (stack == null || stack.isEmpty() || isDecorativeItem(stack)) continue;
            long totalPrice = extractPrice(stack);
            if (totalPrice <= 0) continue;
            boolean matches = false;
            if (isChunker) {
                String displayName = stack.getDisplayName().getString().replaceAll("§.", "");
                if (current.getName().contains("[1x1]") && displayName.contains("[1x1]")) matches = true;
                else if (current.getName().contains("[3x3]") && displayName.contains("[3x3]")) matches = true;
                else if (current.getName().contains("[5x5]") && displayName.contains("[5x5]")) matches = true;
            } else {
                matches = matchesForAutoSetup(stack, current);
            }
            if (matches) {
                long unitPrice = totalPrice / Math.max(1, stack.getCount());
                if (unitPrice < currentItemMinPrice) currentItemMinPrice = unitPrice;
            }
        }
        setupState = 3;
    }

    private void handleAutoBuy(EventContainerTick e) {
        if (mc.player.openContainer == null || mc.player.openContainer == mc.player.container || pendingSell) return;
        long now = System.currentTimeMillis();
        if (now < nextActionTime) return;

        if (mode.is("Холиворлд") && ThreadLocalRandom.current().nextFloat() > 0.9f) return;

        String title = getCurrentScreenTitle();
        if (title == null || (!mode.is("Холиворлд") && !title.replaceAll("§.", "").toLowerCase().contains("аукцион")))
            return;

        List<ItemSetting> settings = ItemList.getItems();
        int startSlot = mode.is("Холиворлд") ? ThreadLocalRandom.current().nextInt(5) : 0;

        for (int i = startSlot; i < 45; i++) {
            if (mode.is("Холиворлд") && i >= 36) continue;
            ItemStack stack = e.getItems().get(i);
            if (stack == null || stack.isEmpty() || isDecorativeItem(stack)) continue;

            long price = extractPrice(stack);
            if (price <= 0) continue;

            String fullText = getStackFullText(stack);
            for (ItemSetting setting : settings) {
                if (setting.get() && matchesRequiredNbt(stack, setting, fullText)) {
                    long unitPrice = price / Math.max(1, stack.getCount());
                    if (setting.getMaxPrice() > 0 && unitPrice <= setting.getMaxPrice()) {
                        pendingPurchaseItem = stack.copy();
                        pendingPurchasePrice = price;
                        pendingPurchaseSetting = setting;
                        lastPurchasedCount = stack.getCount();
                        if (mode.is("Холиворлд")) clickSlot(i);
                        else shiftClickSlot(i);
                        lastMatchedSetting = setting;
                        nextActionTime = System.currentTimeMillis() + (mode.is("Холиворлд") ? 500 : 0);
                        return;
                    }
                }
            }
        }

        if (now >= nextRefreshTime) {
            for (int i = 45; i < 54; i++) {
                ItemStack s = mc.player.openContainer.inventorySlots.get(i).getStack();
                if (!s.isEmpty() && (s.getItem() == Items.SUNFLOWER || s.getDisplayName().getString().toLowerCase().contains("обновить") || (mode.is("Спукитайм") && s.getItem() == Items.NETHER_STAR))) {
                    clickSlot(i);
                    long delay = mode.is("Холиворлд") ? ThreadLocalRandom.current().nextLong(1200, 2400) : ThreadLocalRandom.current().nextLong(20, 120);
                    nextRefreshTime = System.currentTimeMillis() + delay;
                    nextActionTime = System.currentTimeMillis() + 200;
                    break;
                }
            }
        }
    }

    private void setRandomDelay() {
        nextActionTime = System.currentTimeMillis() + (mode.is("Холиворлд") ? ThreadLocalRandom.current().nextLong(80, 300) : 0);
    }

    private void handleAutoSell() {
        if (sellQueue.isEmpty()) {
            pendingSell = false;
            return;
        }

        if (System.currentTimeMillis() < chatBlockTime) return;

        SellTask currentTask = sellQueue.get(0);
        if (mc.currentScreen != null && !(mc.currentScreen instanceof ContainerScreen)) {
            mc.player.closeScreen();
            return;
        }

        if (!moveInitiated) {
            int invSlot = findInventorySlotForSetting(currentTask.setting);
            if (invSlot >= 0) {
                int targetSlot = (invSlot < 9) ? invSlot : splitStackToExact(invSlot, currentTask.count);
                if (targetSlot >= 0 && targetSlot < 9) {
                    mc.player.inventory.currentItem = targetSlot;
                    moveInitiated = true;
                    chatBlockTime = System.currentTimeMillis() + 500;
                }
            }
            return;
        }

        mc.player.sendChatMessage("/ah sell " + currentTask.price);
        chatBlockTime = System.currentTimeMillis() + 1000;
        sellQueue.remove(0);
        moveInitiated = false;
        lastMatchedSetting = null;

        if (sellQueue.isEmpty()) {
            pendingSell = false;
            reopenTime = System.currentTimeMillis() + 2000;
        }
    }

    private void resetAll() {
        pendingSell = false;
        autosetupRunning = false;
        autosetupQueue.clear();
        reopenTime = 0;
        nextActionTime = 0;
        setupState = 0;
        stateTimer = 0;
        currentItemMinPrice = Long.MAX_VALUE;
        lastMatchedSetting = null;
        pendingPurchaseItem = ItemStack.EMPTY;
        pendingPurchasePrice = 0L;
        pendingPurchaseSetting = null;
    }

    public static long extractPrice(ItemStack stack) {
        if (!stack.hasTag()) return 0L;
        CompoundNBT display = stack.getTag().getCompound("display");
        if (!display.contains("Lore", 9)) return 0L;
        ListNBT lore = display.getList("Lore", 8);
        for (int i = 0; i < lore.size(); i++) {
            String text = "";
            try {
                text = ITextComponent.Serializer.getComponentFromJsonLenient(lore.getString(i)).getString();
            } catch (Exception e) {
                text = lore.getString(i);
            }
            String clean = text.replaceAll("§.", "").toLowerCase();
            if (clean.contains("$") || clean.contains("цена") || clean.contains("price") || clean.contains("монет")) {
                try {
                    long price = Long.parseLong(clean.replaceAll("[^0-9]", ""));
                    if (price > 0) return price;
                } catch (Exception ignored) {
                }
            }
        }
        return 0L;
    }

    private boolean matchesForAutoSetup(ItemStack stack, ItemSetting setting) {
        if (mode.is("Холиворлд")) {
            return matchesRequiredNbt(stack, setting, getStackFullText(stack));
        }
        if (setting.getItemStack() != null && stack.getItem() != setting.getItemStack().getItem()) return false;
        String t = (setting.getSearchQuery() == null ? setting.getName() : setting.getSearchQuery()).toLowerCase();
        return stack.getDisplayName().getString().replaceAll("§.", "").toLowerCase().contains(t);
    }

    private boolean matchesRequiredNbt(ItemStack stack, ItemSetting setting, String fullText) {
        List<String> pr = setting.getRequiredNbtParameters();
        if (pr != null && !pr.isEmpty()) {
            for (String p : pr) if (!fullText.contains(p.toLowerCase())) return false;
        }
        if (mode.is("Холиворлд")) {
            return (pr != null && !pr.isEmpty()) || stack.getItem() == setting.getItemStack().getItem();
        }

        if (setting.getItemStack() != null && stack.getItem() != setting.getItemStack().getItem()) return false;
        String t = (setting.getSearchQuery() == null ? setting.getName() : setting.getSearchQuery()).toLowerCase();
        if (!fullText.contains(t)) return false;

        if (stack.hasTag()) {
            if (!matchesRequiredEnchantments(stack.getTag(), setting)) return false;
            if (setting.isRequireUnbreakable() && !(stack.getTag().getBoolean("Unbreakable") || stack.getTag().getByte("Unbreakable") != 0))
                return false;
        } else if (setting.isRequireUnbreakable()) return false;

        return true;
    }

    private boolean matchesRequiredEnchantments(CompoundNBT tag, ItemSetting setting) {
        Map<String, Integer> req = setting.getRequiredEnchantments();
        if (req == null || req.isEmpty()) return true;
        Map<String, Integer> present = new HashMap<>();
        if (tag.contains("Enchantments", 9)) addEnch(present, tag.getList("Enchantments", 10));
        if (tag.contains("StoredEnchantments", 9)) addEnch(present, tag.getList("StoredEnchantments", 10));
        for (Map.Entry<String, Integer> e : req.entrySet())
            if (present.getOrDefault(e.getKey(), 0) < e.getValue()) return false;
        return true;
    }

    private void addEnch(Map<String, Integer> map, ListNBT list) {
        for (int i = 0; i < list.size(); i++) {
            CompoundNBT e = list.getCompound(i);
            map.put(e.getString("id").replace("minecraft:", ""), Math.max(map.getOrDefault(e.getString("id"), 0), e.getInt("lvl")));
        }
    }

    private void clickSlot(int slot) {
        if (mc.player != null && mc.player.openContainer != null)
            mc.playerController.windowClick(mc.player.openContainer.windowId, slot, 0, ClickType.PICKUP, mc.player);
    }

    private void shiftClickSlot(int slot) {
        if (mc.player != null && mc.player.openContainer != null)
            mc.playerController.windowClick(mc.player.openContainer.windowId, slot, 0, ClickType.QUICK_MOVE, mc.player);
    }

    private String getStackFullText(ItemStack stack) {
        StringBuilder sb = new StringBuilder(stack.getDisplayName().getString());
        if (stack.hasTag() && stack.getTag().getCompound("display").contains("Lore", 9)) {
            ListNBT lore = stack.getTag().getCompound("display").getList("Lore", 8);
            for (int i = 0; i < lore.size(); i++) {
                try {
                    sb.append(" ").append(ITextComponent.Serializer.getComponentFromJsonLenient(lore.getString(i)).getString());
                } catch (Exception e) {
                    sb.append(" ").append(lore.getString(i));
                }
            }
        }
        return sb.toString().replaceAll("(?i)§[0-9A-FK-ORX]", "").toLowerCase();
    }

    private String getCurrentScreenTitle() {
        return mc.currentScreen instanceof ContainerScreen ? mc.currentScreen.getTitle().getString() : null;
    }

    private boolean isAuctionOpen() {
        if (mc.player == null || mc.player.openContainer == null) return false;
        String t = getCurrentScreenTitle();
        return t != null && (t.toLowerCase().contains("аукцион") || t.toLowerCase().contains("поиск")) || mc.player.openContainer.inventorySlots.size() > 53;
    }

    private boolean isDecorativeItem(ItemStack s) {
        return s.getItem() == Items.BLACK_STAINED_GLASS_PANE || s.getItem() == Items.GRAY_STAINED_GLASS_PANE || s.getItem() == Items.WHITE_STAINED_GLASS_PANE || s.getItem() == Items.LIGHT_GRAY_STAINED_GLASS_PANE || s.getDisplayName().getString().trim().isEmpty();
    }

    private int findInventorySlotForSetting(ItemSetting setting) {
        for (int i = 0; i < 45; i++) {
            ItemStack s = mc.player.inventory.getStackInSlot(i);
            if (!s.isEmpty() && matchesRequiredNbt(s, setting, getStackFullText(s))) return i;
        }
        return -1;
    }

    private int splitStackToExact(int src, int count) {
        if (count <= 0) return -1;
        int winId = mc.player.openContainer.windowId;
        int empty = -1;
        for (int i = 9; i < 36; i++)
            if (mc.player.inventory.getStackInSlot(i).isEmpty()) {
                empty = i;
                break;
            }
        if (empty == -1) return -1;
        mc.playerController.windowClick(winId, src < 9 ? src + 36 : src, 0, ClickType.PICKUP, mc.player);
        for (int i = 0; i < count; i++)
            mc.playerController.windowClick(winId, empty < 9 ? empty + 36 : empty, 1, ClickType.PICKUP, mc.player);
        mc.playerController.windowClick(winId, src < 9 ? src + 36 : src, 0, ClickType.PICKUP, mc.player);
        return empty;
    }

    private static class SellTask {
        final ItemSetting setting;
        final int count;
        final long price;

        SellTask(ItemSetting s, int c, long p) {
            this.setting = s;
            this.count = c;
            this.price = p;
        }
    }
}