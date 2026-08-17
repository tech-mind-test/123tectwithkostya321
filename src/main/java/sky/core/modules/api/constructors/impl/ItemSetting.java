package sky.core.modules.api.constructors.impl;

import sky.core.modules.api.constructors.Setting;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ItemSetting extends Setting<Boolean> {

    public enum Mode {
        HOLIWORLD,
        SPOOKYTIME,
        BOTH
    }

    private final ItemStack itemStack;
    private String nbt = "";
    private long maxPrice = 0L;
    private boolean sellEnabled = false;
    private int sellPercent = 0;
    private final List<String> requiredNbtParameters = new ArrayList<>();
    private Map<String, Integer> requiredEnchantments = new HashMap<>();
    private boolean requireUnbreakable = false;
    private String searchQuery = null;
    private Mode mode = Mode.BOTH;

    public ItemSetting(ItemStack itemStack, String name, Boolean defaultVal) {
        super(name, defaultVal);
        this.itemStack = itemStack;
    }

    public ItemSetting mode(Mode mode) {
        this.mode = mode;
        return this;
    }

    public boolean matchesMode(String currentMode) {
        if (mode == Mode.BOTH) return true;
        if ("Холиворлд".equalsIgnoreCase(currentMode) && mode == Mode.HOLIWORLD) return true;
        if ("Спукитайм".equalsIgnoreCase(currentMode) && mode == Mode.SPOOKYTIME) return true;
        return false;
    }

    public ItemSetting addNBTparametr(String... params) {
        if (params == null) return this;
        for (String p : params) {
            if (p == null) continue;
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                this.requiredNbtParameters.add(trimmed);
            }
        }
        return this;
    }

    public ItemSetting addEnchantment(String enchantName, int level) {
        if (this.requiredEnchantments == null) {
            this.requiredEnchantments = new HashMap<>();
        }
        this.requiredEnchantments.put(enchantName.toLowerCase(), level);
        return this;
    }

    public ItemSetting requireUnbreakable() {
        this.requireUnbreakable = true;
        return this;
    }

    public ItemSetting setSearchQuery(String query) {
        this.searchQuery = query == null ? null : query.trim();
        return this;
    }

    public ItemSetting sellPercent(int percent) {
        this.sellPercent = percent;
        return this;
    }

    public ItemSetting sellEnabled(boolean enabled) {
        this.sellEnabled = enabled;
        return this;
    }

    public void setMaxPriceFromString(String priceText) {
        if (priceText == null) {
            this.maxPrice = 0L;
            return;
        }
        String digits = priceText.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            this.maxPrice = 0L;
            return;
        }
        try {
            this.maxPrice = Long.parseLong(digits);
        } catch (NumberFormatException e) {
            this.maxPrice = 0L;
        }
    }

    public void setSellPercentFromString(String percentText) {
        if (percentText == null) {
            this.sellPercent = 0;
            return;
        }
        String digits = percentText.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            this.sellPercent = 0;
            return;
        }
        try {
            this.sellPercent = Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            this.sellPercent = 0;
        }
    }
}