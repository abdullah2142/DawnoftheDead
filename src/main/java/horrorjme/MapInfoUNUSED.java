package horrorjme;

import com.jme3.math.Vector3f;

/**
 * Defines the available maps and their specific properties,
 * like the model path and player start position.
 */
public enum MapInfo {
    // Existing maps
    ORIGINAL_MAP("Models/scene.gltf", new Vector3f(10f, 0f, 20f)),
    NEW_CITY_MAP("Models2/scene.gltf", new Vector3f(5f, 0f, 10f)),

    // NEW: Add your third map from Models3
    // Make sure to adjust the Vector3f to a good starting point for this map.
    INDUSTRIAL_COMPLEX("Models3/scene.gltf", new Vector3f(0f, 0f, 0f)),

    // NEW: Add your fourth map from Models4
    // Make sure to adjust the Vector3f to a good starting point for this map.
    ABANDONED_STATION("Models4/scene.gltf", new Vector3f(10f, 5f, 20f));

    private final String modelPath;
    private final Vector3f startPosition;

    MapInfo(String modelPath, Vector3f startPosition) {
        this.modelPath = modelPath;
        this.startPosition = startPosition;
    }

    public String getModelPath() {
        return modelPath;
    }

    public Vector3f getStartPosition() {
        return startPosition;
    }
}