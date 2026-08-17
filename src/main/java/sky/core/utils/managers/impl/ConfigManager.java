package sky.core.utils.managers.impl;

import com.google.gson.*;
import com.adl.nativeprotect.Native;
import sky.core.modules.api.constructors.impl.*;
import sky.core.utils.misc.HasherUtil;
import sky.core.SkyCore;
import sky.core.utils.managers.BaseManager;
import sky.core.utils.managers.FilePath;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.Setting;
import sky.core.ui.gui.autobuy.ItemList;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ConfigManager extends BaseManager<ConfigManager.ConfigData> {
    private static final File CONFIG_DIR = new File(FilePath.BASE_PATH + FilePath.CUSTOM_DIR_REL);

    @Getter
    public static class Config {
        private final String name;
        private final File file;
        private final long creationDate;
        private final String creator;

        public Config(String name) {
            this.name = name;
            this.file = new File(CONFIG_DIR, name + ".cfg");
            this.creationDate = System.currentTimeMillis();
            this.creator = "NexusUser";
        }

        public Config(String name, long creationDate, String creator) {
            this.name = name;
            this.file = new File(CONFIG_DIR, name + ".cfg");
            this.creationDate = creationDate;
            this.creator = creator != null ? creator : "Unknown";
        }
    }

    @Getter
    @Setter
    public static class ConfigData {
        private String lastLoadedConfig;
    }

    public ConfigManager() {
        super(FilePath.CUSTOM_DIR_REL);
    }

    @Override
    public void init() {
        CONFIG_DIR.mkdirs();
        super.init();
    }

    @Native
    @Override
    protected void initializeData() {
        this.data = new ConfigData();
    }

    @Native
    @Override
    protected JsonObject serializeData() {
        JsonObject config = new JsonObject();
        if (data.getLastLoadedConfig() != null) {
            config.addProperty("lastLoadedConfig", data.getLastLoadedConfig());
        }
        return config;
    }

    @Override
    protected void deserializeData(JsonObject jsonObject) {
        if (jsonObject.has("lastLoadedConfig") && !jsonObject.get("lastLoadedConfig").isJsonNull()) {
            data.setLastLoadedConfig(jsonObject.get("lastLoadedConfig").getAsString());
        }
    }

    public List<String> getConfigNames() {
        File[] files = CONFIG_DIR.listFiles((dir, name) -> name.endsWith(".cfg"));
        if (files == null) return Collections.emptyList();

        List<String> names = new ArrayList<>();
        for (File file : files) {
            names.add(file.getName().replace(".cfg", ""));
        }
        return names;
    }

    public void loadConfig(String name) {
        if (name == null || name.trim().isEmpty()) return;

        File file = new File(CONFIG_DIR, name.trim() + ".cfg");
        if (!file.exists()) return;

        try {
            String fileContent = Files.readString(file.toPath());
            String processedContent = encrypt ? HasherUtil.decrypt(fileContent) : fileContent;

            JsonObject config = new JsonParser().parse(processedContent).getAsJsonObject();

            if (config.has("module")) {
                try {
                    loadModules(config.getAsJsonObject("module"));
                } catch (Exception e) {
                    System.err.println("Failed to load modules: " + e.getMessage());
                }
            }

            if (config.has("autoBuyItems")) {
                try {
                    loadAutoBuyItems(config.getAsJsonObject("autoBuyItems"));
                } catch (Exception e) {
                    System.err.println("Failed to load autoBuyItems: " + e.getMessage());
                }
            }

            data.setLastLoadedConfig(name);
            save();

            ClientConfig clientConfig = SkyCore.getInstance().getClientConfig();
            if (clientConfig != null) {
                clientConfig.onConfigSelected(name);
            }

        } catch (Exception e) {
            System.err.println("Failed to load config file: " + name + " - " + e.getMessage());
        }
    }

    public void saveConfig(String name) {
        if (name == null || name.trim().isEmpty()) return;

        CONFIG_DIR.mkdirs();
        File file = new File(CONFIG_DIR, name.trim() + ".cfg");

        try {
            JsonObject config = new JsonObject();
            config.add("module", saveModules());
            config.add("autoBuyItems", saveAutoBuyItems());
            config.addProperty("creationDate", System.currentTimeMillis());
            config.addProperty("creator", "NexusUser");

            String jsonContent = GSON.toJson(config);
            String outputContent = encrypt ? HasherUtil.encrypt(jsonContent) : jsonContent;
            Files.writeString(file.toPath(), outputContent);

        } catch (Exception e) {
            System.err.println("Failed to save config: " + name + " - " + e.getMessage());
        }
    }

    @Native
    public boolean deleteConfig(String name) {
        if (name == null || name.trim().isEmpty()) return false;

        boolean deleted = new File(CONFIG_DIR, name.trim() + ".cfg").delete();
        if (deleted && name.equals(data.getLastLoadedConfig())) {
            data.setLastLoadedConfig(null);
            save();
        }
        return deleted;
    }

    public Config getConfigInfo(String name) {
        if (name == null || name.trim().isEmpty()) return null;

        File file = new File(CONFIG_DIR, name.trim() + ".cfg");
        if (!file.exists()) return null;

        try {
            String fileContent = Files.readString(file.toPath());
            String processedContent = encrypt ? HasherUtil.decrypt(fileContent) : fileContent;

            JsonObject config = new JsonParser().parse(processedContent).getAsJsonObject();
            long creationDate = config.has("creationDate") && !config.get("creationDate").isJsonNull() ? config.get("creationDate").getAsLong() : System.currentTimeMillis();
            String creator = config.has("creator") && !config.get("creator").isJsonNull() ? config.get("creator").getAsString() : "Unknown";
            return new Config(name, creationDate, creator);
        } catch (Exception e) {
            return new Config(name);
        }
    }

    @Native
    private JsonObject saveAutoBuyItems() {
        JsonObject itemsObj = new JsonObject();
        int savedCount = 0;
        for (ItemSetting item : ItemList.getItems()) {
            JsonObject itemData = new JsonObject();
            itemData.addProperty("maxPrice", item.getMaxPrice());
            itemData.addProperty("autoSetup", item.isSellEnabled());
            itemData.addProperty("enabled", item.get());
            itemsObj.add(item.getName(), itemData);

            if (item.get() || item.getMaxPrice() > 0 || item.isSellEnabled()) {
                savedCount++;
            }
        }
        return itemsObj;
    }

    private void loadAutoBuyItems(JsonObject itemsObj) {

        for (Map.Entry<String, JsonElement> entry : itemsObj.entrySet()) {
            String itemName = entry.getKey();
            ItemSetting item = ItemList.getByName(itemName);

            if (item == null) {
                continue;
            }

            if (entry.getValue().isJsonObject()) {
                JsonObject itemData = entry.getValue().getAsJsonObject();

                if (itemData.has("enabled")) {
                    boolean enabled = itemData.get("enabled").getAsBoolean();
                    item.set(enabled);
                }

                if (itemData.has("maxPrice")) {
                    long price = itemData.get("maxPrice").getAsLong();
                    item.setMaxPrice(price);
                }

                if (itemData.has("autoSetup")) {
                    boolean autoSetup = itemData.get("autoSetup").getAsBoolean();
                    item.setSellEnabled(autoSetup);
                }

                if (item.get() || item.getMaxPrice() > 0 || item.isSellEnabled()) {
                }
            }
        }
    }

    @Native
    private void loadModules(JsonObject modules) {
        boolean prev = Module.isSuppressToggleEffects();
        Module.setSuppressToggleEffects(true);
        try {
            SkyCore.getInstance().getModuleManager().getModules().forEach(module -> {
                JsonObject moduleData = modules.getAsJsonObject(module.getName().toLowerCase());
                if (moduleData == null) return;

                module.settoggled(false);
                loadProperty(moduleData, "key", v -> module.setBind(v.getAsInt()));
                loadProperty(moduleData, "enabled", v -> module.settoggled(v.getAsBoolean()));
                loadProperty(moduleData, "togglemode", v -> {
                    try {
                        module.setToggleMode(Module.ToggleMode.valueOf(v.getAsString()));
                    } catch (Exception e) {
                        module.setToggleMode(Module.ToggleMode.TOGGLE);
                    }
                });
                loadProperty(moduleData, "keybindvisible", v -> module.setKeybindvisible(v.getAsBoolean()));

                module.getSettings().forEach(setting -> loadSetting(moduleData, setting));
            });
        } finally {
            Module.setSuppressToggleEffects(prev);
        }
    }

    @Native
    private void loadSetting(JsonObject moduleData, Setting<?> setting) {
        JsonElement element = moduleData.get(setting.getName());
        if (element == null || element.isJsonNull()) return;

        try {
            if (setting instanceof SliderSetting) {
                ((SliderSetting) setting).set(element.getAsFloat());
            } else if (setting instanceof BooleanSetting) {
                if (element.isJsonObject()) {
                    JsonObject boolData = element.getAsJsonObject();
                    BooleanSetting boolSetting = (BooleanSetting) setting;
                    loadProperty(boolData, "value", v -> boolSetting.set(v.getAsBoolean()));
                    loadProperty(boolData, "bind", v -> boolSetting.setBind(v.getAsInt()));
                    loadProperty(boolData, "keybindvisible", v -> boolSetting.setKeybindvisible(v.getAsBoolean()));
                    loadProperty(boolData, "togglemode", v -> {
                        try {
                            boolSetting.setToggleMode(Module.ToggleMode.valueOf(v.getAsString()));
                        } catch (Exception e) {
                            boolSetting.setToggleMode(Module.ToggleMode.TOGGLE);
                        }
                    });
                }
            } else if (setting instanceof ColorSetting) {
                ((ColorSetting) setting).set(element.getAsInt());
            } else if (setting instanceof ModeSetting) {
                ((ModeSetting) setting).set(element.getAsString());
            } else if (setting instanceof BindSetting) {
                ((BindSetting) setting).set(element.getAsInt());
            } else if (setting instanceof StringSetting) {
                ((StringSetting) setting).set(element.getAsString());
            } else if (setting instanceof MultiBooleanSetting) {
                if (element.isJsonObject()) {
                    JsonObject multiData = element.getAsJsonObject();
                    MultiBooleanSetting multiSetting = (MultiBooleanSetting) setting;
                    multiSetting.get().forEach(option -> {
                        JsonElement optionElement = multiData.get(option.getName());
                        if (optionElement != null && !optionElement.isJsonNull()) {
                            option.set(optionElement.getAsBoolean());
                        }
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load setting " + setting.getName() + ": " + e.getMessage());
        }
    }

    @Native
    private JsonObject saveModules() {
        JsonObject modules = new JsonObject();
        SkyCore.getInstance().getModuleManager().getModules().forEach(module -> {
            JsonObject moduleData = new JsonObject();
            moduleData.addProperty("key", module.getBind());
            moduleData.addProperty("enabled", module.isEnabled());
            moduleData.addProperty("togglemode", module.getToggleMode().name());
            moduleData.addProperty("keybindvisible", module.isKeybindvisible());

            module.getSettings().forEach(setting -> saveSetting(moduleData, setting));
            modules.add(module.getName().toLowerCase(), moduleData);
        });
        return modules;
    }

    @Native
    private void saveSetting(JsonObject moduleData, Setting<?> setting) {
        try {
            if (setting instanceof BooleanSetting) {
                BooleanSetting boolSetting = (BooleanSetting) setting;
                JsonObject boolData = new JsonObject();
                boolData.addProperty("value", boolSetting.get());
                boolData.addProperty("bind", boolSetting.getBind());
                boolData.addProperty("keybindvisible", boolSetting.isKeybindvisible());
                boolData.addProperty("togglemode", boolSetting.getToggleMode().name());
                moduleData.add(setting.getName(), boolData);
            } else if (setting instanceof SliderSetting) {
                moduleData.addProperty(setting.getName(), ((SliderSetting) setting).get());
            } else if (setting instanceof ModeSetting) {
                moduleData.addProperty(setting.getName(), ((ModeSetting) setting).get());
            } else if (setting instanceof ColorSetting) {
                moduleData.addProperty(setting.getName(), ((ColorSetting) setting).get());
            } else if (setting instanceof BindSetting) {
                moduleData.addProperty(setting.getName(), ((BindSetting) setting).get());
            } else if (setting instanceof StringSetting) {
                moduleData.addProperty(setting.getName(), ((StringSetting) setting).get());
            } else if (setting instanceof MultiBooleanSetting) {
                MultiBooleanSetting multiSetting = (MultiBooleanSetting) setting;
                JsonObject multiData = new JsonObject();
                multiSetting.get().forEach(option -> multiData.addProperty(option.getName(), option.get()));
                moduleData.add(setting.getName(), multiData);
            }
        } catch (Exception e) {
            System.err.println("Failed to save setting " + setting.getName() + ": " + e.getMessage());
        }
    }

    @Native
    private void loadProperty(JsonObject object, String key, Consumer<JsonElement> consumer) {
        JsonElement element = object.get(key);
        if (element != null && !element.isJsonNull()) {
            consumer.accept(element);
        }
    }

    @Native
    public void resetToDefaults() {
        boolean prev = Module.isSuppressToggleEffects();
        Module.setSuppressToggleEffects(true);
        try {
            SkyCore.getInstance().getModuleManager().getModules().forEach(module -> {
                module.settoggled(false);
                module.setBind(-100);
                module.setToggleMode(Module.ToggleMode.TOGGLE);
                module.setKeybindvisible(true);

                module.getSettings().forEach(setting -> {
                    try {
                        if (setting instanceof BooleanSetting s) {
                            s.set(s.defaultVal);
                            s.setBind(-100);
                            s.setToggleMode(Module.ToggleMode.TOGGLE);
                            s.setKeybindvisible(true);
                        } else if (setting instanceof SliderSetting s) {
                            s.set(s.defaultVal);
                        } else if (setting instanceof ModeSetting s) {
                            s.set(s.defaultVal);
                        } else if (setting instanceof ColorSetting s) {
                            s.set(s.defaultVal);
                        } else if (setting instanceof BindSetting s) {
                            s.set(-1);
                        } else if (setting instanceof StringSetting s) {
                            s.set("");
                        } else if (setting instanceof MultiBooleanSetting s) {
                            s.get().forEach(opt -> opt.set(opt.defaultVal));
                        }
                    } catch (Exception ignored) {
                    }
                });
            });
        } finally {
            Module.setSuppressToggleEffects(prev);
        }
        ClientConfig clientConfig = SkyCore.getInstance().getClientConfig();
        if (clientConfig != null) {
            clientConfig.saveCurrentSettings();
        }
    }
}