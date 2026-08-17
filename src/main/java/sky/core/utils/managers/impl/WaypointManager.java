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

public class WaypointManager extends BaseManager<Map<String, List<WaypointManager.WaypointEntry>>> {

    @Getter
    public static class WaypointEntry {
        private final String name;
        private final double x;
        private final double y;
        private final double z;

        public WaypointEntry(String name, double x, double y, double z) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public WaypointManager() {
        super(FilePath.WAYPOINTS_FILE_REL);
    }

    @Native
    @Override
    protected void initializeData() {
        this.data = new HashMap<>();
    }

    @Native
    @Override
    protected JsonObject serializeData() {
        JsonObject root = new JsonObject();
        for (Map.Entry<String, List<WaypointEntry>> byServer : data.entrySet()) {
            String server = byServer.getKey();
            JsonArray arr = new JsonArray();
            for (WaypointEntry wp : byServer.getValue()) {
                JsonObject o = new JsonObject();
                o.addProperty("name", wp.getName());
                o.addProperty("x", wp.getX());
                o.addProperty("y", wp.getY());
                o.addProperty("z", wp.getZ());
                arr.add(o);
            }
            root.add(server, arr);
        }
        return root;
    }

    @Native
    @Override
    protected void deserializeData(JsonObject jsonObject) {
        data.clear();
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String server = entry.getKey();
            JsonElement element = entry.getValue();
            if (!element.isJsonArray()) continue;
            JsonArray arr = element.getAsJsonArray();
            List<WaypointEntry> list = new ArrayList<>();
            for (JsonElement el : arr) {
                if (!el.isJsonObject()) continue;
                JsonObject o = el.getAsJsonObject();
                loadProperty(o, "name", n -> loadProperty(o, "x", x -> loadProperty(o, "y", y -> loadProperty(o, "z", z -> {
                    try {
                        list.add(new WaypointEntry(
                                n.getAsString(),
                                x.getAsDouble(),
                                y.getAsDouble(),
                                z.getAsDouble()
                        ));
                    } catch (Exception ignored) {
                    }
                }))));
            }
            data.put(server, list);
        }
    }

    @Native
    @Override
    protected void handleLoadError(Exception exception) {
        if (data == null) initializeData();
    }

    @Native
    public List<WaypointEntry> getWaypoints(String serverKey) {
        return new ArrayList<>(data.getOrDefault(serverKey, Collections.emptyList()));
    }

    @Native
    public void addWaypoint(String serverKey, String name, double x, double y, double z) {
        if (serverKey == null || name == null) return;
        String normalized = name.trim();
        if (normalized.isEmpty()) return;
        List<WaypointEntry> list = data.computeIfAbsent(serverKey, k -> new ArrayList<>());
        list.removeIf(w -> w.getName().equalsIgnoreCase(normalized));
        list.add(new WaypointEntry(normalized, x, y, z));
        save();
    }

    @Native
    public boolean removeWaypoint(String serverKey, String name) {
        if (serverKey == null || name == null) return false;
        List<WaypointEntry> list = data.get(serverKey);
        if (list == null) return false;
        boolean removed = list.removeIf(w -> w.getName().equalsIgnoreCase(name.trim()));
        if (removed) save();
        return removed;
    }

    @Native
    public void clear(String serverKey) {
        if (serverKey == null) return;
        List<WaypointEntry> list = data.get(serverKey);
        if (list != null && !list.isEmpty()) {
            list.clear();
            save();
        }
    }

    @Native
    private void loadProperty(JsonObject object, String key, Consumer<JsonElement> consumer) {
        JsonElement element = object.get(key);
        if (element != null && !element.isJsonNull()) consumer.accept(element);
    }
}


