package horrorjme;

import com.jme3.asset.AssetManager;
import com.jme3.light.PointLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.ColorOverlayFilter;
import com.jme3.post.filters.DepthOfFieldFilter;
import com.jme3.post.filters.FadeFilter;
import com.jme3.post.filters.FogFilter;
import com.jme3.post.filters.LightScatteringFilter;
import com.jme3.renderer.ViewPort;

/**
 * Enhanced post-processing manager that integrates with the advanced lighting system
 */
public class PostProcessingManager {

    private AssetManager assetManager;
    private ViewPort viewPort;

    // Post-processing
    private FilterPostProcessor postProcessor;
    private FadeFilter fadeFilter;
    private ColorOverlayFilter noiseFilter;
    private BloomFilter bloomFilter;
    private FogFilter fogFilter;
    private DepthOfFieldFilter dofFilter;
    private LightScatteringFilter lightScatteringFilter;

    // Lighting integration
    private AdvancedLightingManager lightingManager;

    // Effect timers
    private float noiseTimer = 0f;
    private float atmosphereTimer = 0f;
    private boolean dynamicAtmosphereEnabled = false; // DISABLED: No dynamic changes
    private boolean filmGrainEnabled = false; // DISABLED: No film grain

    public PostProcessingManager(AssetManager assetManager, ViewPort viewPort) {
        this.assetManager = assetManager;
        this.viewPort = viewPort;
    }

    /**
     * Set lighting manager for enhanced integration
     */
    public void setLightingManager(AdvancedLightingManager lightingManager) {
        this.lightingManager = lightingManager;

        // Update effects based on lighting
        if (postProcessor != null) {
            updateEffectsForLighting();
        }
    }

    /**
     * Initializes all the post-processing filters with enhanced horror effects
     */
    public void initializeEffects() {

        postProcessor = new FilterPostProcessor(assetManager);

        // 1. ENHANCED FOG - More atmospheric
        fogFilter = new FogFilter();
        fogFilter.setFogColor(new ColorRGBA(0.05f, 0.05f, 0.08f, 1.0f)); // Dark blue-gray fog
        fogFilter.setFogDensity(0.3f); // Reduced density for better visibility
        fogFilter.setFogDistance(80f); // Increased distance
        postProcessor.addFilter(fogFilter);

        // 2. LIGHT SCATTERING - God rays and atmospheric scattering (if available)
        try {
            // Note: LightScatteringFilter may not be available in all JME3 versions
            Class<?> lightScatteringClass = Class.forName("com.jme3.post.filters.LightScatteringFilter");
            lightScatteringFilter = (LightScatteringFilter) lightScatteringClass.newInstance();
            lightScatteringFilter.setLightDensity(1.2f);
            lightScatteringFilter.setLightPosition(viewPort.getCamera().getDirection().mult(-1f));
            postProcessor.addFilter(lightScatteringFilter);

        } catch (Exception e) {

            lightScatteringFilter = null;
        }

        // 3. ENHANCED DEPTH OF FIELD - More cinematic
        dofFilter = new DepthOfFieldFilter();
        dofFilter.setFocusDistance(8f); // Closer focus
        dofFilter.setFocusRange(12f); // Wider range
        dofFilter.setBlurScale(0.5f); // More blur
        postProcessor.addFilter(dofFilter);

        // 4. ENHANCED BLOOM - For atmospheric lights
        bloomFilter = new BloomFilter(BloomFilter.GlowMode.Objects);
        bloomFilter.setBloomIntensity(2.0f); // Increased intensity
        bloomFilter.setBlurScale(2.0f); // More blur
        bloomFilter.setExposurePower(3f); // Reduced exposure power
        bloomFilter.setDownSamplingFactor(2f);
        postProcessor.addFilter(bloomFilter);

        // 5. DYNAMIC NOISE - Adaptive film grain
        noiseFilter = new ColorOverlayFilter();
        noiseFilter.setColor(ColorRGBA.White);
        postProcessor.addFilter(noiseFilter);

        // 6. FADE FILTER - For special effects
        fadeFilter = new FadeFilter();
        postProcessor.addFilter(fadeFilter);

        viewPort.addProcessor(postProcessor);

    }

    /**
     * Update effects based on current lighting configuration
     */
    private void updateEffectsForLighting() {
        if (lightingManager == null) return;

        // Use single optimized fog and bloom settings
        fogFilter.setFogDensity(0.9f);
        fogFilter.setFogColor(new ColorRGBA(0.05f, 0.05f, 0.08f, 1.0f));
        bloomFilter.setBloomIntensity(2.0f);

    }

    /**
     * Enhanced update with ALL FLICKERING DISABLED
     */
    public void update(float tpf, Player player) {
        if (postProcessor == null) return;  // FIXED: Changed from postProcessingManager to postProcessor

        atmosphereTimer += tpf;

        // ALL DYNAMIC EFFECTS DISABLED - no flickering
        // - No dynamic atmosphere
        // - No film grain
        // - No animated effects

        // Health-based effects (static, no animation)
        if (player != null) {
            updateStaticHealthEffects(player);
        }

        // No lighting integration effects that cause changes
    }

    /**
     * Dynamic atmosphere DISABLED - completely static values
     */
    private void updateDynamicAtmosphere(float tpf) {
        // No variations - fog and bloom stay exactly the same
        fogFilter.setFogDensity(getFogDensityForCurrentPreset());
        bloomFilter.setBloomIntensity(getBloomIntensityForCurrentPreset());
    }

    /**
     * More subtle film grain effect
     */
    private void updateFilmGrain(float tpf) {
        noiseTimer += tpf;

        // Update every 0.08 seconds (about 12 FPS for film grain)
        if (noiseTimer > 0.08f) {
            noiseTimer = 0f;
            float noiseAmount = 0.08f; // Reduced from 0.2f for subtlety
            float grayNoise = FastMath.nextRandomFloat() * noiseAmount;
            noiseFilter.setColor(new ColorRGBA(grayNoise, grayNoise, grayNoise, 1f));
        }
    }

    /**
     * Static health-based visual effects (no animation/flickering)
     */
    private void updateStaticHealthEffects(Player player) {
        float healthPercent = player.getHealth() / player.getMaxHealth();

        if (healthPercent < 0.3f) {
            // Static increased fog when injured (no animation)
            float injuryFogBonus = (0.3f - healthPercent) * 0.3f; // Reduced bonus
            float currentFog = getFogDensityForCurrentPreset() + injuryFogBonus;
            fogFilter.setFogDensity(currentFog);
        } else {
            // Normal fog density
            fogFilter.setFogDensity(getFogDensityForCurrentPreset());
        }

        // No pulse effects, no animated red tints - completely static
    }

    /**
     * Lighting integration effects DISABLED - completely static
     */
    private void updateLightingIntegrationEffects(float tpf) {
        // No dynamic updates - light scattering and bloom stay static
        // All effects maintain their initialized values with no changes
    }

    /**
     * Calculate average intensity of atmospheric lights
     */
    private float calculateAverageLightIntensity() {
        if (lightingManager == null || lightingManager.getAtmosphericLights() == null) {
            return 0f;
        }

        float totalIntensity = 0f;
        int lightCount = 0;

        for (PointLight light : lightingManager.getAtmosphericLights()) {
            if (light != null) {
                ColorRGBA color = light.getColor();
                float intensity = (color.r + color.g + color.b) / 3f;
                totalIntensity += intensity;
                lightCount++;
            }
        }

        return lightCount > 0 ? totalIntensity / lightCount : 0f;
    }

    /**
     * Get fog density for current lighting configuration
     */
    private float getFogDensityForCurrentPreset() {
        return 0.3f; // Single fixed value
    }

    /**
     * Get bloom intensity for current lighting configuration
     */
    private float getBloomIntensityForCurrentPreset() {
        return 2.0f; // Single fixed value
    }

    /**
     * Trigger enhanced scare effect with lighting integration
     */
    public void triggerScareEffect() {
        if (fadeFilter != null) {
            fadeFilter.setDuration(0.1f);
            fadeFilter.fadeOut();
        }

        // If lighting manager is available, create lightning flash
        if (lightingManager != null) {
            lightingManager.createLightningFlash(1.5f, 0.1f);
        }

    }

    /**
     * Create power outage effect
     */
    public void triggerPowerOutage(float duration) {
        if (lightingManager != null) {
            lightingManager.setEmergencyLighting(true);

            // Increase fog during power outage
            fogFilter.setFogDensity(0.7f);
            fogFilter.setFogColor(new ColorRGBA(0.01f, 0.0f, 0.0f, 1.0f)); // Dark red fog

        }
    }

    /**
     * Restore normal atmosphere after power outage
     */
    public void restoreNormalAtmosphere() {
        if (lightingManager != null) {
            lightingManager.setEmergencyLighting(false);
            updateEffectsForLighting(); // Restore normal fog settings

        }
    }

    // ==== CONFIGURATION METHODS ====

    public void setDynamicAtmosphereEnabled(boolean enabled) {
        this.dynamicAtmosphereEnabled = enabled;

    }

    public void setFilmGrainIntensity(float intensity) {
        // Update noise filter intensity

    }

    /**
     * Removes the filter processor from the viewport to clean up
     */
    public void cleanup() {
        if (postProcessor != null) {
            viewPort.removeProcessor(postProcessor);
            postProcessor = null;
        }

    }
}