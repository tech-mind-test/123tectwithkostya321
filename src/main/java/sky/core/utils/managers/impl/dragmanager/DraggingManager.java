package sky.core.utils.managers.impl.dragmanager;

import com.google.gson.*;
import sky.core.utils.misc.HasherUtil;
import sky.core.utils.managers.BaseManager;
import sky.core.utils.managers.FilePath;
import sky.core.modules.api.constructors.Setting;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.api.constructors.impl.ModeSetting;
import lombok.Getter;

import java.util.*;
import java.util.function.Consumer;

public class DraggingManager extends BaseManager<DraggingManager.DragData> {

    @Getter
    public static class DragData {
        private final Map<String, Float> savedX = new LinkedHashMap<>();
        private final Map<String, Float> savedY = new LinkedHashMap<>();
        private final LinkedHashMap<String, Dragging> draggables = new LinkedHashMap<>();
        private final List<Dragging> renderOrder = new ArrayList<>();
    }

    public DraggingManager() {
        super(FilePath.DRAGS_FILE_REL);
    }

    @Override
    protected void initializeData() {
        this.data = new DragData();
    }

    @Override
    protected JsonObject serializeData() {
        JsonObject config = new JsonObject();

        JsonObject positions = new JsonObject();
        data.getDraggables().forEach((name, drag) -> {
            JsonObject pos = new JsonObject();
            pos.addProperty("x", drag.getTargetX());
            pos.addProperty("y", drag.getTargetY());
            positions.add(name, pos);
        });

        JsonArray order = new JsonArray();
        data.getRenderOrder().forEach(drag -> order.add(drag.getName()));

        JsonObject settings = new JsonObject();
        data.getDraggables().forEach((name, drag) -> {
            JsonObject dragSettings = new JsonObject();
            drag.getSettings().forEach(setting -> saveSetting(dragSettings, setting));
            if (dragSettings.size() > 0) settings.add(name, dragSettings);
        });

        config.add("positions", positions);
        config.add("renderOrder", order);
        config.add("settings", settings);

        return config;
    }

    @Override
    protected void deserializeData(JsonObject jsonObject) {
        loadPositions(jsonObject);
        loadRenderOrder(jsonObject);
        loadSettings(jsonObject);
    }

    private void loadPositions(JsonObject jsonObject) {
        loadProperty(jsonObject, "positions", positionsElement -> {
            data.getSavedX().clear();
            data.getSavedY().clear();

            JsonObject positions = positionsElement.getAsJsonObject();
            positions.entrySet().forEach(entry -> {
                String name = entry.getKey();
                JsonObject pos = entry.getValue().getAsJsonObject();
                try {
                    float x = pos.get("x").getAsFloat();
                    float y = pos.get("y").getAsFloat();

                    data.getSavedX().put(name, x);
                    data.getSavedY().put(name, y);

                    Dragging drag = data.getDraggables().get(name);
                    if (drag != null) {
                        drag.setX(x);
                        drag.setY(y);
                        drag.syncSmoothPosition();
                    }
                } catch (Exception e) {
                    System.err.println("Failed to parse position for " + name + ": " + e.getMessage());
                }
            });
        });
    }

    private void loadRenderOrder(JsonObject jsonObject) {
        loadProperty(jsonObject, "renderOrder", orderElement -> {
            data.getRenderOrder().clear();
            JsonArray renderOrder = orderElement.getAsJsonArray();
            renderOrder.forEach(element -> {
                try {
                    Dragging drag = data.getDraggables().get(element.getAsString());
                    if (drag != null) data.getRenderOrder().add(drag);
                } catch (Exception e) {
                    System.err.println("Failed to parse render order element: " + e.getMessage());
                }
            });
            data.getDraggables().values().forEach(drag -> {
                if (!data.getRenderOrder().contains(drag)) data.getRenderOrder().add(drag);
            });
        });
    }

    private void loadSettings(JsonObject jsonObject) {
        loadProperty(jsonObject, "settings", settingsElement -> {
            JsonObject settings = settingsElement.getAsJsonObject();
            settings.entrySet().forEach(entry -> {
                Dragging drag = data.getDraggables().get(entry.getKey());
                if (drag != null) {
                    try {
                        JsonObject dragSettings = entry.getValue().getAsJsonObject();
                        drag.getSettings().forEach(setting -> loadSetting(dragSettings, setting));
                    } catch (Exception e) {
                        System.err.println("Failed to load settings for " + entry.getKey() + ": " + e.getMessage());
                    }
                }
            });
        });
    }

    public void addDraggable(Dragging draggable) {
        String name = draggable.getName();

        if (data.getSavedX().containsKey(name) && data.getSavedY().containsKey(name)) {
            draggable.setX(data.getSavedX().remove(name));
            draggable.setY(data.getSavedY().remove(name));
            draggable.syncSmoothPosition();
        }

        data.getDraggables().put(name, draggable);
        if (!data.getRenderOrder().contains(draggable)) data.getRenderOrder().add(draggable);
    }

    public void bringToFront(Dragging draggable) {
        data.getRenderOrder().remove(draggable);
        data.getRenderOrder().add(draggable);
    }

    public void resetAllToInitialPositions() {
        data.getDraggables().values().forEach(drag -> {
            drag.setX(drag.getInitialX());
            drag.setY(drag.getInitialY());
            drag.syncSmoothPosition();
        });
        save();
    }

    public LinkedHashMap<String, Dragging> getDraggables() {
        return data.getDraggables();
    }

    public List<Dragging> getRenderOrder() {
        return data.getRenderOrder();
    }

    public void loadSettingsFor(Dragging draggable) {
        try {
            JsonObject config = loadJsonConfig();
            if (config != null && config.has("settings")) {
                JsonObject settings = config.getAsJsonObject("settings");
                JsonObject dragSettings = settings.getAsJsonObject(draggable.getName());
                if (dragSettings != null) {
                    draggable.getSettings().forEach(setting -> loadSetting(dragSettings, setting));
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load settings for draggable " + draggable.getName() + ": " + e.getMessage());
        }
    }

    private void saveSetting(JsonObject dragSettings, Setting<?> setting) {
        try {
            if (setting instanceof BooleanSetting) {
                dragSettings.addProperty(setting.getName(), ((BooleanSetting) setting).get());
            } else if (setting instanceof ModeSetting) {
                dragSettings.addProperty(setting.getName(), ((ModeSetting) setting).get());
            }
        } catch (Exception e) {
            System.err.println("Failed to save setting " + setting.getName() + ": " + e.getMessage());
        }
    }

    private void loadSetting(JsonObject dragSettings, Setting<?> setting) {
        loadProperty(dragSettings, setting.getName(), element -> {
            try {
                if (setting instanceof BooleanSetting) {
                    ((BooleanSetting) setting).set(element.getAsBoolean());
                } else if (setting instanceof ModeSetting) {
                    ((ModeSetting) setting).set(element.getAsString());
                }
            } catch (Exception e) {
                System.err.println("Failed to load setting " + setting.getName() + ": " + e.getMessage());
            }
        });
    }

    private JsonObject loadJsonConfig() {
        try {
            if (!configFile.exists()) return null;
            String content = java.nio.file.Files.readString(configFile.toPath());
            return new JsonParser().parse(encrypt ? HasherUtil.decrypt(content) : content).getAsJsonObject();
        } catch (Exception e) {
            System.err.println("Failed to load dragging config: " + e.getMessage());
            return null;
        }
    }

    private void loadProperty(JsonObject object, String key, Consumer<JsonElement> consumer) {
        JsonElement element = object.get(key);
        if (element != null && !element.isJsonNull()) {
            consumer.accept(element);
        }
    }
}