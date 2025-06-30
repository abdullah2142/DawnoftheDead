package horrorjme;

import com.jme3.math.Vector3f;

/**
 * Defines the available maps and their specific properties
 * Used for map switching between rounds
 */
public enum MapInfo {
    // Available maps with their model paths and spawn positions
    ORIGINAL_MAP("Original Facility", "Models/scene.gltf", new Vector3f(10f, 0f, 20f)),
    NEW_CITY_MAP("Abandoned City", "Models2/scene.gltf", new Vector3f(5f, 0f, 10f)),
    INDUSTRIAL_COMPLEX("Industrial Complex", "Models3/scene.gltf", new Vector3f(0f, 0f, 0f)),
    ABANDONED_STATION("Abandoned Station", "Models4/scene.gltf", new Vector3f(10f, 5f, 20f));

    private final String displayName;
    private final String modelPath;
    private final Vector3f startPosition;

    MapInfo(String displayName, String modelPath, Vector3f startPosition) {
        this.displayName = displayName;
        this.modelPath = modelPath;
        this.startPosition = startPosition;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getModelPath() {
        return modelPath;
    }

    public Vector3f getStartPosition() {
        return startPosition.clone();
    }

    /**
     * Get next map in sequence (for automatic progression)
     */
    public MapInfo getNextMap() {
        MapInfo[] maps = values();
        int currentIndex = this.ordinal();
        int nextIndex = (currentIndex + 1) % maps.length;
        return maps[nextIndex];
    }

    /**
     * Get previous map in sequence
     */
    public MapInfo getPreviousMap() {
        MapInfo[] maps = values();
        int currentIndex = this.ordinal();
        int prevIndex = (currentIndex - 1 + maps.length) % maps.length;
        return maps[prevIndex];
    }

    /**
     * Get random map (excluding current)
     */
    public MapInfo getRandomMap() {
        MapInfo[] maps = values();
        MapInfo randomMap;
        do {
            int randomIndex = (int)(Math.random() * maps.length);
            randomMap = maps[randomIndex];
        } while (randomMap == this && maps.length > 1);
        return randomMap;
    }

    /**
     * Get default starting map
     */
    public static MapInfo getDefaultMap() {
        return ORIGINAL_MAP;
    }

    /**
     * Find map by display name
     */
    public static MapInfo findByDisplayName(String displayName) {
        for (MapInfo map : values()) {
            if (map.displayName.equals(displayName)) {
                return map;
            }
        }
        return getDefaultMap();
    }
}