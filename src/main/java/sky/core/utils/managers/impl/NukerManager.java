package sky.core.utils.managers.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import sky.core.utils.managers.BaseManager;
import sky.core.utils.managers.FilePath;
import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.List;

public class NukerManager extends BaseManager<List<String>> {

    public NukerManager() {
        super(FilePath.NUKER_FILE_REL);
    }

    @Override
    protected void initializeData() {
        this.data = new ArrayList<>();
    }

    @Override
    protected JsonObject serializeData() {
        JsonObject config = new JsonObject();
        JsonArray arr = new JsonArray();
        for (String blockName : data) {
            arr.add(blockName);
        }
        config.add("nuker_blocks", arr);
        return config;
    }

    @Override
    protected void deserializeData(JsonObject jsonObject) {
        data.clear();
        JsonArray arr = jsonObject.getAsJsonArray("nuker_blocks");
        if (arr != null) {
            for (JsonElement el : arr) {
                if (el.isJsonPrimitive()) {
                    data.add(el.getAsString());
                }
            }
        }
    }

    @Override
    protected void handleLoadError(Exception exception) {
        if (data == null) initializeData();
    }

    public List<Block> getTargetBlocks() {
        List<Block> blocks = new ArrayList<>();
        for (String blockName : data) {
            try {
                ResourceLocation location = new ResourceLocation(blockName);
                Block block = Registry.BLOCK.getOptional(location).orElse(null);
                if (block != null) {
                    blocks.add(block);
                }
            } catch (Exception ignored) {
            }
        }
        return blocks;
    }

    public boolean addTargetBlock(ResourceLocation resourceLocation) {
        if (resourceLocation == null) return false;
        String blockName = resourceLocation.toString();
        Block block = Registry.BLOCK.getOptional(resourceLocation).orElse(null);
        if (block == null || data.contains(blockName)) return false;
        data.add(blockName);
        save();
        return true;
    }

    public boolean removeTargetBlock(ResourceLocation resourceLocation) {
        if (resourceLocation == null) return false;
        String blockName = resourceLocation.toString();
        boolean removed = data.remove(blockName);
        if (removed) save();
        return removed;
    }

    public boolean isTargetBlock(Block block) {
        if (block == null) return false;
        ResourceLocation location = Registry.BLOCK.getKey(block);
        return data.contains(location.toString());
    }

    public void clearTargetBlocks() {
        if (!data.isEmpty()) {
            data.clear();
            save();
        }
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public int size() {
        return data.size();
    }
}
