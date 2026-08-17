package sky.core.utils.managers;

import com.google.gson.*;
import com.adl.nativeprotect.Native;
import sky.core.utils.misc.HasherUtil;

import java.io.File;
import java.nio.file.Files;


public abstract class BaseManager<T> {
    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    protected static final String BASE_PATH = FilePath.BASE_PATH;

    protected final File configFile;
    protected final boolean encrypt;
    protected T data;

    protected BaseManager(String relativePath, boolean encrypt) {
        this.configFile = new File(BASE_PATH + relativePath);
        this.encrypt = encrypt;
    }

    protected BaseManager(String relativePath) {
        this(relativePath, true);
    }


    public void init() {
        createDirectoriesIfNeeded();
        initializeData();
        load();
    }


    protected void createDirectoriesIfNeeded() {
        File parentDir = configFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
    }

    public void load() {
        if (!configFile.exists()) {
            return;
        }

        try {
            String fileContent = Files.readString(configFile.toPath());
            String processedContent = encrypt ? HasherUtil.decrypt(fileContent) : fileContent;
            JsonObject jsonObject = new JsonParser().parse(processedContent).getAsJsonObject();
            deserializeData(jsonObject);
        } catch (Exception e) {
            System.err.println("Failed to load config from " + configFile.getName() + ": " + e.getMessage());
            handleLoadError(e);
        }
    }

    public void save() {
        try {
            createDirectoriesIfNeeded();

            JsonObject jsonObject = serializeData();
            String jsonContent = GSON.toJson(jsonObject);
            String outputContent = encrypt ? HasherUtil.encrypt(jsonContent) : jsonContent;

            Files.writeString(configFile.toPath(), outputContent);
        } catch (Exception e) {
            System.err.println("Failed to save config to " + configFile.getName() + ": " + e.getMessage());
            handleSaveError(e);
        }
    }

    public boolean exists() {
        return configFile.exists();
    }

    public boolean delete() {
        return configFile.exists() && configFile.delete();
    }

    public String getConfigPath() {
        return configFile.getAbsolutePath();
    }

    public String getConfigName() {
        return configFile.getName();
    }

    protected abstract void initializeData();

    protected abstract JsonObject serializeData();

    protected abstract void deserializeData(JsonObject jsonObject);

    protected void handleLoadError(Exception exception) {
    }

    protected void handleSaveError(Exception exception) {
    }

    public T getData() {
        return data;
    }

    protected void setData(T data) {
        this.data = data;
    }
}