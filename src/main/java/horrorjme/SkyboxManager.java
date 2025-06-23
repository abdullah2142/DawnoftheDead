package horrorjme;

import com.jme3.asset.AssetManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.SkyFactory;

/**
 * Simple skybox manager - loads and manages a single skybox
 */
public class SkyboxManager {

    private AssetManager assetManager;
    private Node rootNode;
    private Spatial currentSkybox;

    public SkyboxManager(AssetManager assetManager, Node rootNode) {
        this.assetManager = assetManager;
        this.rootNode = rootNode;
    }

    /**
     * Load skybox from file path
     */
    public boolean loadSkybox(String texturePath, SkyFactory.EnvMapType mapType) {
        try {

            // Remove existing skybox if any
            if (currentSkybox != null) {
                rootNode.detachChild(currentSkybox);
            }

            // Create and attach new skybox
            currentSkybox = SkyFactory.createSky(assetManager, texturePath, mapType);
            rootNode.attachChild(currentSkybox);

            return true;

        } catch (Exception e) {
            System.err.println("Failed to load skybox: " + e.getMessage());
            return false;
        }
    }

    /**
     * Load skybox from 6 separate cube face images
     */
    public boolean loadCubemapSkybox(String westPath, String eastPath, String northPath,
                                     String southPath, String upPath, String downPath) {
        try {

            // Remove existing skybox if any
            if (currentSkybox != null) {
                rootNode.detachChild(currentSkybox);
            }

            // Load the 6 textures
            com.jme3.texture.Texture west = assetManager.loadTexture(westPath);
            com.jme3.texture.Texture east = assetManager.loadTexture(eastPath);
            com.jme3.texture.Texture north = assetManager.loadTexture(northPath);
            com.jme3.texture.Texture south = assetManager.loadTexture(southPath);
            com.jme3.texture.Texture up = assetManager.loadTexture(upPath);
            com.jme3.texture.Texture down = assetManager.loadTexture(downPath);

            // Create skybox from 6 textures with larger radius
            currentSkybox = SkyFactory.createSky(assetManager, west, east, north, south, up, down,
                    com.jme3.math.Vector3f.UNIT_XYZ, 350f);
            rootNode.attachChild(currentSkybox);

            return true;

        } catch (Exception e) {
            System.err.println("Failed to load cubemap skybox: " + e.getMessage());
            return false;
        }
    }

    /**
     * Remove current skybox
     */
    public void removeSkybox() {
        if (currentSkybox != null) {
            rootNode.detachChild(currentSkybox);
            currentSkybox = null;

        }
    }
}