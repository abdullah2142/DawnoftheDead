package horrorjme;

import com.jme3.asset.AssetManager;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.PointLightShadowRenderer;
import com.jme3.shadow.SpotLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode; // <- ADD THIS IMPORT

/**
 * Advanced lighting and shadow system for atmospheric horror environments
 * Integrates with JME3's shadow rendering system
 */
public class AdvancedLightingManager {

    private AssetManager assetManager;
    private ViewPort viewPort;
    private Node rootNode;
    private Camera camera;

    // Main lighting
    private AmbientLight ambientLight;
    private DirectionalLight mainDirectionalLight;
    private DirectionalLight fillLight;

    // Atmospheric point lights
    private PointLight[] atmosphericLights;
    private SpotLight playerSpotLight; // Additional player area lighting

    // Shadow renderers
    private DirectionalLightShadowRenderer mainShadowRenderer;
    private PointLightShadowRenderer pointShadowRenderer;
    private SpotLightShadowRenderer spotShadowRenderer;

    // Configuration
    private boolean shadowsEnabled = true;
    private boolean dynamicLightsEnabled = true;
    private boolean atmosphericFlickerEnabled = false; // ADDED: Disabled by default

    // Shadow quality settings
    private int shadowMapSize = 1024; // Can be 512, 1024, 2048, 4096
    private int numShadowSplits = 3;  // For PSSM (Parallel Split Shadow Mapping)

    public AdvancedLightingManager(AssetManager assetManager, ViewPort viewPort,
                                   Node rootNode, Camera camera) {
        this.assetManager = assetManager;
        this.viewPort = viewPort;
        this.rootNode = rootNode;
        this.camera = camera;

    }

    /**
     * Initialize the complete lighting and shadow system
     */
    public void initializeLightingSystem() {

        // Clear existing lights
        rootNode.getLocalLightList().clear();

        // Setup base lighting
        setupBaseLighting();

        // Setup atmospheric lighting
        setupAtmosphericLighting();

        // Setup shadows
        if (shadowsEnabled) {
            setupShadowRendering();
        }

    }

    /**
     * Setup base ambient and directional lighting - Single optimized configuration
     */
    private void setupBaseLighting() {

        // Ambient light - balanced for horror atmosphere with good visibility
        ambientLight = new AmbientLight();
        ambientLight.setColor(new ColorRGBA(0.25f, 0.22f, 0.28f, 1.0f)); // Slight blue tint
        rootNode.addLight(ambientLight);

        // Main directional light - primary light source
        mainDirectionalLight = new DirectionalLight();
        mainDirectionalLight.setDirection(new Vector3f(-0.3f, -0.7f, -0.5f).normalizeLocal());
        mainDirectionalLight.setColor(new ColorRGBA(0.6f, 0.55f, 0.5f, 1.0f)); // Warm dim light
        rootNode.addLight(mainDirectionalLight);

        // Fill light - prevents completely black areas
        fillLight = new DirectionalLight();
        fillLight.setDirection(new Vector3f(0.4f, -0.2f, 0.6f).normalizeLocal());
        fillLight.setColor(new ColorRGBA(0.2f, 0.2f, 0.25f, 1.0f));  // Cool dim fill
        rootNode.addLight(fillLight);

    }

    /**
     * Setup atmospheric point lights for enhanced mood
     */
    private void setupAtmosphericLighting() {
        if (!dynamicLightsEnabled) return;

        // Create atmospheric point lights
        atmosphericLights = new PointLight[4];

        // Flickering light sources at strategic positions
        for (int i = 0; i < atmosphericLights.length; i++) {
            atmosphericLights[i] = new PointLight();
            atmosphericLights[i].setRadius(15f);
            rootNode.addLight(atmosphericLights[i]);
        }

        // Position atmospheric lights (these would be adjusted based on your map)
        atmosphericLights[0].setPosition(new Vector3f(10f, 8f, 10f));
        atmosphericLights[0].setColor(new ColorRGBA(1.0f, 0.8f, 0.6f, 1.0f)); // Warm light

        atmosphericLights[1].setPosition(new Vector3f(-15f, 6f, 20f));
        atmosphericLights[1].setColor(new ColorRGBA(0.6f, 0.8f, 1.0f, 1.0f)); // Cool light

        atmosphericLights[2].setPosition(new Vector3f(25f, 10f, -10f));
        atmosphericLights[2].setColor(new ColorRGBA(1.0f, 0.6f, 0.6f, 1.0f)); // Red light

        atmosphericLights[3].setPosition(new Vector3f(-10f, 12f, -25f));
        atmosphericLights[3].setColor(new ColorRGBA(0.8f, 1.0f, 0.8f, 1.0f)); // Green light

        // Player area spot light (follows player)
        playerSpotLight = new SpotLight();
        playerSpotLight.setSpotRange(20f);
        playerSpotLight.setSpotInnerAngle(15f * FastMath.DEG_TO_RAD);
        playerSpotLight.setSpotOuterAngle(35f * FastMath.DEG_TO_RAD);
        playerSpotLight.setColor(new ColorRGBA(0.8f, 0.8f, 1.0f, 1.0f));
        rootNode.addLight(playerSpotLight);

    }

    /**
     * Setup shadow rendering for realistic shadows
     */
    private void setupShadowRendering() {

        try {
            // Main directional light shadows (for overall scene shadows)
            mainShadowRenderer = new DirectionalLightShadowRenderer(assetManager, shadowMapSize, numShadowSplits);
            mainShadowRenderer.setLight(mainDirectionalLight);
            mainShadowRenderer.setShadowIntensity(0.7f); // Strong shadows for horror
            mainShadowRenderer.setEdgeFilteringMode(EdgeFilteringMode.PCF4); // Smooth shadow edges
            mainShadowRenderer.setEnabledStabilization(true); // ADDED: Prevents shadow edge flickering

            // Configure shadow distance and quality
            mainShadowRenderer.setShadowZExtend(100f); // How far shadows extend
            mainShadowRenderer.setShadowZFadeLength(20f); // Shadow fade distance

            viewPort.addProcessor(mainShadowRenderer);

            // Point light shadows (for atmospheric lights)
            if (atmosphericLights != null && atmosphericLights.length > 0) {
                pointShadowRenderer = new PointLightShadowRenderer(assetManager, 512); // Smaller for performance
                pointShadowRenderer.setLight(atmosphericLights[0]); // Shadow from first atmospheric light
                pointShadowRenderer.setShadowIntensity(0.5f);

                viewPort.addProcessor(pointShadowRenderer);

            }

            // Spot light shadows (for player area lighting)
            if (playerSpotLight != null) {
                spotShadowRenderer = new SpotLightShadowRenderer(assetManager, 512);
                spotShadowRenderer.setLight(playerSpotLight);
                spotShadowRenderer.setShadowIntensity(0.4f);

                viewPort.addProcessor(spotShadowRenderer);

            }

        } catch (Exception e) {
            System.err.println("Failed to setup shadow rendering: " + e.getMessage());
            shadowsEnabled = false;
        }
    }

    /**
     * Update dynamic lighting effects (call every frame)
     */
    public void update(float tpf, Vector3f playerPosition) {
        if (!dynamicLightsEnabled) return;

        // Update player spot light position
        if (playerSpotLight != null && camera != null) {
            Vector3f lightPos = playerPosition.add(0, 2f, 0); // Above player
            Vector3f lightDir = camera.getDirection().clone();

            playerSpotLight.setPosition(lightPos);
            playerSpotLight.setDirection(lightDir);
        }

        // UPDATED: Only flicker atmospheric lights if enabled
        if (atmosphericLights != null && atmosphericFlickerEnabled) {
            updateFlickeringLights(tpf);
        }
    }

    /**
     * Create flickering light effects for atmosphere
     */
    private void updateFlickeringLights(float tpf) {
        float time = System.currentTimeMillis() * 0.001f;

        for (int i = 0; i < atmosphericLights.length; i++) {
            if (atmosphericLights[i] != null) {
                // Different flicker patterns for each light
                float flicker = 0.7f + 0.3f * FastMath.sin(time * (2f + i * 0.5f)) *
                        FastMath.sin(time * (3f + i * 0.3f));

                // Occasionally make lights flicker more dramatically
                if (FastMath.nextRandomFloat() < 0.01f) { // 1% chance per frame
                    flicker *= 0.3f; // Dramatic dim
                }

                // Apply flicker to existing color
                ColorRGBA baseColor = getBaseColorForLight(i);
                ColorRGBA flickeredColor = baseColor.mult(flicker);
                atmosphericLights[i].setColor(flickeredColor);
            }
        }
    }

    /**
     * Get base color for atmospheric light
     */
    private ColorRGBA getBaseColorForLight(int index) {
        switch (index) {
            case 0: return new ColorRGBA(1.0f, 0.8f, 0.6f, 1.0f); // Warm
            case 1: return new ColorRGBA(0.6f, 0.8f, 1.0f, 1.0f); // Cool
            case 2: return new ColorRGBA(1.0f, 0.6f, 0.6f, 1.0f); // Red
            case 3: return new ColorRGBA(0.8f, 1.0f, 0.8f, 1.0f); // Green
            default: return ColorRGBA.White;
        }
    }

    /**
     * Add a temporary light source (for events, explosions, etc.)
     */
    public PointLight addTemporaryLight(Vector3f position, ColorRGBA color, float radius, float duration) {
        PointLight tempLight = new PointLight();
        tempLight.setPosition(position);
        tempLight.setColor(color);
        tempLight.setRadius(radius);

        rootNode.addLight(tempLight);

        // In a real implementation, you'd want a timer system to remove this

        return tempLight;
    }

    /**
     * Create lightning flash effect
     */
    public void createLightningFlash(float intensity, float duration) {
        // Store original ambient color
        ColorRGBA originalAmbient = ambientLight.getColor().clone();

        // Bright white flash
        ColorRGBA flashColor = ColorRGBA.White.mult(intensity);
        ambientLight.setColor(flashColor);

        // In a real implementation, you'd gradually fade back to original

        // Immediately restore for now (you'd want to animate this)
        ambientLight.setColor(originalAmbient);
    }

    // ==== CONFIGURATION METHODS ====

    public void setShadowsEnabled(boolean enabled) {
        this.shadowsEnabled = enabled;

        if (!enabled) {
            // Remove shadow processors
            if (mainShadowRenderer != null) {
                viewPort.removeProcessor(mainShadowRenderer);
            }
            if (pointShadowRenderer != null) {
                viewPort.removeProcessor(pointShadowRenderer);
            }
            if (spotShadowRenderer != null) {
                viewPort.removeProcessor(spotShadowRenderer);
            }
        } else {
            // Re-add shadow processors
            setupShadowRendering();
        }

    }

    public void setDynamicLightsEnabled(boolean enabled) {
        this.dynamicLightsEnabled = enabled;

    }

    public void setAtmosphericFlickerEnabled(boolean enabled) {
        this.atmosphericFlickerEnabled = enabled;

    }

    public void setShadowQuality(int mapSize) {
        this.shadowMapSize = mapSize;

        // You'd need to recreate shadow renderers to apply this change
        if (shadowsEnabled) {
            setShadowsEnabled(false);
            setShadowsEnabled(true);
        }
    }

    // ==== GETTERS ====

    public boolean areShadowsEnabled() {
        return shadowsEnabled;
    }

    public boolean areDynamicLightsEnabled() {
        return dynamicLightsEnabled;
    }

    public boolean isAtmosphericFlickerEnabled() {
        return atmosphericFlickerEnabled;
    }

    public DirectionalLight getMainDirectionalLight() {
        return mainDirectionalLight;
    }

    /**
     * Get atmospheric lights for external effects (like post-processing)
     */
    public PointLight[] getAtmosphericLights() {
        return atmosphericLights;
    }

    /**
     * Get player spot light for external effects
     */
    public SpotLight getPlayerSpotLight() {
        return playerSpotLight;
    }

    /**
     * Create emergency lighting (for power outage effects)
     */
    public void setEmergencyLighting(boolean enabled) {
        if (enabled) {
            // Very dim red emergency lighting
            ambientLight.setColor(new ColorRGBA(0.2f, 0.05f, 0.05f, 1.0f));
            mainDirectionalLight.setColor(new ColorRGBA(0.4f, 0.1f, 0.1f, 1.0f));

            // Make atmospheric lights flicker more dramatically
            if (atmosphericLights != null) {
                for (PointLight light : atmosphericLights) {
                    light.setColor(new ColorRGBA(1.0f, 0.3f, 0.3f, 1.0f));
                }
            }

        } else {
            // Restore normal lighting - back to original values
            ambientLight.setColor(new ColorRGBA(0.15f, 0.12f, 0.18f, 1.0f));
            mainDirectionalLight.setColor(new ColorRGBA(0.4f, 0.35f, 0.3f, 1.0f));
            fillLight.setColor(new ColorRGBA(0.1f, 0.1f, 0.15f, 1.0f));

            // Restore atmospheric light colors
            if (atmosphericLights != null && atmosphericLights.length >= 4) {
                atmosphericLights[0].setColor(new ColorRGBA(1.0f, 0.8f, 0.6f, 1.0f)); // Warm
                atmosphericLights[1].setColor(new ColorRGBA(0.6f, 0.8f, 1.0f, 1.0f)); // Cool
                atmosphericLights[2].setColor(new ColorRGBA(1.0f, 0.6f, 0.6f, 1.0f)); // Red
                atmosphericLights[3].setColor(new ColorRGBA(0.8f, 1.0f, 0.8f, 1.0f)); // Green
            }

        }
    }

    /**
     * Cleanup all lighting and shadow resources
     */
    public void cleanup() {

        // Remove shadow processors
        if (mainShadowRenderer != null) {
            viewPort.removeProcessor(mainShadowRenderer);
        }
        if (pointShadowRenderer != null) {
            viewPort.removeProcessor(pointShadowRenderer);
        }
        if (spotShadowRenderer != null) {
            viewPort.removeProcessor(spotShadowRenderer);
        }

        // Clear lights from scene
        rootNode.getLocalLightList().clear();

    }
}