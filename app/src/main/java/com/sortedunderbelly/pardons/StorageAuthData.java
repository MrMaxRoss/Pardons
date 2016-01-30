package com.sortedunderbelly.pardons;

/**
 * Created by max.ross on 1/29/16.
 */
public class StorageAuthData {
    private final String provider;
    private final String displayName;

    public StorageAuthData(String provider, String displayName) {
        this.provider = provider;
        this.displayName = displayName;
    }

    public String getProvider() {
        return provider;
    }

    public String getDisplayName() {
        return displayName;
    }
}
