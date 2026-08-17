package sky.core.utils.managers.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.adl.nativeprotect.Native;
import sky.core.utils.managers.BaseManager;
import sky.core.utils.managers.FilePath;
import lombok.Getter;

import java.util.*;
import java.util.function.Consumer;

public class BlockESPManager extends BaseManager<List<BlockESPManager.BlockEntry>> {

    @Getter
    public static class BlockEntry {
        private final String block;
        private final int color;

        public BlockEntry(String block, int color) {
            this.block = block;
            this.color = color;
        }
    }

    public BlockESPManager() {
        super(FilePath.BLOCKESP_FILE_REL);
    }

    @Native
    @Override
    protected void initializeData() {
        this.data = new ArrayList<>();
    }

    @Override
    protected JsonObject serializeData() {
        JsonObject config = new JsonObject();
        JsonArray arr = new JsonArray();
        for (BlockEntry entry : data) {
            JsonObject o = new JsonObject();
            o.addProperty("block", entry.getBlock());
            o.addProperty("color", entry.getColor());
            arr.add(o);
        }
        config.add("blockesp", arr);
        return config;
    }

    @Override
    protected void deserializeData(JsonObject jsonObject) {
        data.clear();
        JsonArray arr = jsonObject.getAsJsonArray("blockesp");
        if (arr != null) {
            for (JsonElement el : arr) {
                if (!el.isJsonObject()) continue;
                JsonObject o = el.getAsJsonObject();
                loadProperty(o, "block", b -> loadProperty(o, "color", c -> {
                    try {
                        data.add(new BlockEntry(b.getAsString(), c.getAsInt()));
                    } catch (Exception ignored) {
                    }
                }));
            }
        }
    }

    @Override
    protected void handleLoadError(Exception exception) {
        if (data == null) initializeData();
    }

    public List<BlockEntry> getBlocks() {
        return new ArrayList<>(data);
    }

    public void addBlock(String block, int color) {
        if (block == null) return;
        String normalized = block.trim().toLowerCase();
        data.removeIf(b -> b.getBlock().equalsIgnoreCase(normalized));
        data.add(new BlockEntry(normalized, color));
        save();
    }

    public boolean removeBlock(String block) {
        if (block == null) return false;
        boolean removed = data.removeIf(b -> b.getBlock().equalsIgnoreCase(block.trim()));
        if (removed) save();
        return removed;
    }

    public void clear() {
        if (!data.isEmpty()) {
            data.clear();
            save();
        }
    }

    private void loadProperty(JsonObject object, String key, Consumer<JsonElement> consumer) {
        JsonElement element = object.get(key);
        if (element != null && !element.isJsonNull()) consumer.accept(element);
    }
}
