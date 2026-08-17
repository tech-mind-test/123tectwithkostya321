package de.maxhenkel.configbuilder;

public interface MigratableConfig {

    String get(String key);

    void set(String key, String value);
}
