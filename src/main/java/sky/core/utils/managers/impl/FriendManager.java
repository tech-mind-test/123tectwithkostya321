package sky.core.utils.managers.impl;

import com.google.gson.*;
import com.adl.nativeprotect.Native;
import sky.core.utils.managers.BaseManager;
import sky.core.utils.managers.FilePath;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FriendManager extends BaseManager<List<FriendManager.FriendEntry>> {

    @Getter
    public static class FriendEntry {
        private final String name;
        private final LocalDateTime addedTime;

        public FriendEntry(String name, LocalDateTime addedTime) {
            this.name = name;
            this.addedTime = addedTime;
        }

        @Override
        public String toString() {
            return name + " (" + addedTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + ")";
        }
    }

    public FriendManager() {
        super(FilePath.FRIENDS_FILE_REL);
    }

    @Native
    @Override
    protected void initializeData() {
        this.data = new ArrayList<>();
    }

    @Override
    protected JsonObject serializeData() {
        JsonObject config = new JsonObject();
        JsonArray friendsArray = new JsonArray();

        for (FriendEntry friend : data) {
            JsonObject friendObj = new JsonObject();
            friendObj.addProperty("name", friend.getName());
            friendObj.addProperty("addedTime", friend.getAddedTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            friendsArray.add(friendObj);
        }

        config.add("friends", friendsArray);
        return config;
    }
    @Native
    @Override
    protected void deserializeData(JsonObject jsonObject) {
        data.clear();

        JsonArray friendsArray = jsonObject.getAsJsonArray("friends");
        if (friendsArray != null) {
            for (JsonElement element : friendsArray) {
                if (element.isJsonObject()) {
                    JsonObject friendObj = element.getAsJsonObject();
                    loadProperty(friendObj, "name", nameElement -> {
                        loadProperty(friendObj, "addedTime", timeElement -> {
                            try {
                                String friendName = nameElement.getAsString();
                                String timeStr = timeElement.getAsString();
                                LocalDateTime addedTime = LocalDateTime.parse(timeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                                data.add(new FriendEntry(friendName, addedTime));
                            } catch (Exception e) {
                                System.err.println("Failed to parse friend entry: " + e.getMessage());
                            }
                        });
                    });
                }
            }
        }
    }

    @Override
    protected void handleLoadError(Exception exception) {
        if (data == null) {
            initializeData();
        }
    }

    public void addFriend(String friendName) {
        if (friendName == null || friendName.trim().isEmpty()) {
            System.err.println("Cannot add friend: name is null or empty");
            return;
        }

        String name = friendName.trim();
        if (!isFriend(name)) {
            data.add(new FriendEntry(name, LocalDateTime.now()));
            save();
        }
    }

    public void removeFriend(String friendName) {
        if (friendName == null) {
            System.err.println("Cannot remove friend: name is null");
            return;
        }

        boolean removed = data.removeIf(entry -> entry.getName().equalsIgnoreCase(friendName.trim()));
        if (removed) {
            save();
        }
    }

    public void clearFriends() {
        if (!data.isEmpty()) {
            data.clear();
            save();
        }
    }

    public boolean isFriend(String friendName) {
        if (friendName == null) return false;
        return data.stream().anyMatch(entry -> entry.getName().equalsIgnoreCase(friendName.trim()));
    }

    public List<FriendEntry> getFriends() {
        return new ArrayList<>(data);
    }

    private void loadProperty(JsonObject object, String key, Consumer<JsonElement> consumer) {
        JsonElement element = object.get(key);
        if (element != null && !element.isJsonNull()) {
            consumer.accept(element);
        }
    }
}