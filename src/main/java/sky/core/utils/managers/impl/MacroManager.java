package sky.core.utils.managers.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.adl.nativeprotect.Native;
import sky.core.utils.managers.BaseManager;
import sky.core.utils.managers.FilePath;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MacroManager extends BaseManager<List<MacroManager.MacroEntry>> {

    @Getter
    public static class MacroEntry {
        private final String name;
        private final int keyCode;
        private final String command;

        public MacroEntry(String name, int keyCode, String command) {
            this.name = name;
            this.keyCode = keyCode;
            this.command = command;
        }
    }

    public MacroManager() {
        super(FilePath.MACROS_FILE_REL);
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
        for (MacroEntry entry : data) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", entry.getName());
            obj.addProperty("key", entry.getKeyCode());
            obj.addProperty("cmd", entry.getCommand());
            arr.add(obj);
        }
        config.add("macros", arr);
        return config;
    }

    @Native
    @Override
    protected void deserializeData(JsonObject jsonObject) {
        data.clear();
        JsonArray arr = jsonObject.getAsJsonArray("macros");
        if (arr != null) {
            for (JsonElement el : arr) {
                if (el.isJsonObject()) {
                    JsonObject o = el.getAsJsonObject();
                    loadProperty(o, "name", n -> loadProperty(o, "key", k -> loadProperty(o, "cmd", c -> {
                        try {
                            data.add(new MacroEntry(n.getAsString(), k.getAsInt(), c.getAsString()));
                        } catch (Exception ignored) {
                        }
                    })));
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
    public void addMacro(String name, int keyCode, String command) {
        if (name == null || command == null) return;
        removeMacro(name);
        data.add(new MacroEntry(name.trim(), keyCode, command));
        save();
    }

    @Native
    public void removeMacro(String name) {
        if (name == null) return;
        boolean removed = data.removeIf(m -> m.getName().equalsIgnoreCase(name.trim()));
        if (removed) save();
    }

    @Native
    public void clear() {
        if (!data.isEmpty()) {
            data.clear();
            save();
        }
    }

    @Native
    public List<MacroEntry> getMacros() {
        return new ArrayList<>(data);
    }

    @Native
    private void loadProperty(JsonObject object, String key, Consumer<JsonElement> consumer) {
        JsonElement element = object.get(key);
        if (element != null && !element.isJsonNull()) consumer.accept(element);
    }
}


