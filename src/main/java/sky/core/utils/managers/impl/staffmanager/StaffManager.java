package sky.core.utils.managers.impl.staffmanager;

import com.google.gson.*;
import com.adl.nativeprotect.Native;
import sky.core.utils.managers.BaseManager;
import sky.core.utils.managers.FilePath;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class StaffManager extends BaseManager<List<StaffManager.StaffEntry>> {

    @Getter
    public static class StaffEntry {
        private final String name;

        public StaffEntry(String name) {
            this.name = name;
        }
    }

    public StaffManager() {
        super(FilePath.STAFF_FILE_REL);
    }

    @Native
    @Override
    protected void initializeData() {
        this.data = new ArrayList<>();
    }

    @Native
    @Override
    protected JsonObject serializeData() {
        JsonObject config = new JsonObject();
        JsonArray arr = new JsonArray();
        for (StaffEntry entry : data) {
            arr.add(new JsonPrimitive(entry.getName()));
        }
        config.add("staff", arr);
        return config;
    }

    @Native
    @Override
    protected void deserializeData(JsonObject jsonObject) {
        data.clear();
        JsonArray arr = jsonObject.getAsJsonArray("staff");
        if (arr != null) {
            for (JsonElement element : arr) {
                if (element.isJsonPrimitive()) {
                    data.add(new StaffEntry(element.getAsString()));
                } else if (element.isJsonObject()) {
                    JsonObject obj = element.getAsJsonObject();
                    loadProperty(obj, "name", nameEl -> data.add(new StaffEntry(nameEl.getAsString())));
                }
            }
        }
    }

    @Native
    @Override
    protected void handleLoadError(Exception exception) {
        if (data == null) initializeData();
    }

    @Native
    public void addStaff(String name) {
        if (name == null || name.trim().isEmpty()) return;
        String n = name.trim();
        if (!isStaff(n)) {
            data.add(new StaffEntry(n));
            save();
        }
    }

    @Native
    public void removeStaff(String name) {
        if (name == null) return;
        boolean removed = data.removeIf(e -> e.getName().equalsIgnoreCase(name.trim()));
        if (removed) save();
    }

    @Native
    public void clearStaff() {
        if (!data.isEmpty()) {
            data.clear();
            save();
        }
    }

    public boolean isStaff(String name) {
        if (name == null) return false;
        return data.stream().anyMatch(e -> e.getName().equalsIgnoreCase(name.trim()));
    }

    public List<StaffEntry> getStaff() {
        return new ArrayList<>(data);
    }

    @Native
    private void loadProperty(JsonObject object, String key, Consumer<JsonElement> consumer) {
        JsonElement element = object.get(key);
        if (element != null && !element.isJsonNull()) {
            consumer.accept(element);
        }
    }
}


