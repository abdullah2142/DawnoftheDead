package horrorjme;

import com.jme3.asset.AssetManager;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.ColorOverlayFilter;
import com.jme3.post.filters.DepthOfFieldFilter;
import com.jme3.post.filters.FadeFilter;
import com.jme3.post.filters.FogFilter;
import com.jme3.renderer.ViewPort;

/**
 * Manages all post-processing effects for the horror game.
 */
public class PostProcessingManager {

    private AssetManager assetManager;
    private ViewPort viewPort;

    // Post-processing
    private FilterPostProcessor postProcessor;
    private FadeFilter fadeFilter;
    private ColorOverlayFilter noiseFilter;
    private float noiseTimer = 0f;

    public PostProcessingManager(AssetManager assetManager, ViewPort viewPort) {
        this.assetManager = assetManager;
        this.viewPort = viewPort;
    }

    /**
     * Initializes all the post-processing filters and adds them to the viewport.
     */
    public void initializeEffects() {
        System.out.println("Setting up horror post-processing effects...");

        postProcessor = new FilterPostProcessor(assetManager);

        // 1. FOG
        FogFilter fog = new FogFilter();
        fog.setFogColor(new ColorRGBA(0.1f, 0.1f, 0.1f, 1.0f));
        fog.setFogDensity(0.5f);
        fog.setFogDistance(50f);
        postProcessor.addFilter(fog);
        System.out.println("Added FOG filter");

        // 2. DEPTH OF FIELD
        DepthOfFieldFilter dof = new DepthOfFieldFilter();
        dof.setFocusDistance(10f);
        dof.setFocusRange(15f);
        dof.setBlurScale(1.4f);
        postProcessor.addFilter(dof);
        System.out.println("Added DOF filter");

        // 3. BLOOM
        BloomFilter bloom = new BloomFilter(BloomFilter.GlowMode.Objects);
        bloom.setBloomIntensity(1.5f);
        bloom.setBlurScale(1.5f);
        bloom.setExposurePower(5f);
        bloom.setDownSamplingFactor(2f);
        postProcessor.addFilter(bloom);
        System.out.println("Added BLOOM filter");

        // 4. NOISE
        noiseFilter = new ColorOverlayFilter();
        noiseFilter.setColor(ColorRGBA.White);
        postProcessor.addFilter(noiseFilter);
        System.out.println("Added NOISE filter");

        // 5. FADE
        fadeFilter = new FadeFilter();
        postProcessor.addFilter(fadeFilter);
        System.out.println("Added FADE filter");

        viewPort.addProcessor(postProcessor);
        System.out.println("Horror post-processing initialized.");
    }

    /**
     * Updates time-based or state-based horror effects.
     * @param tpf Time per frame.
     * @param player The player object to check for state changes (e.g., health).
     */
    public void update(float tpf, Player player) {
        if (postProcessor == null) return;

        // Animated film grain effect (currently commented out)
        /*
        noiseTimer += tpf;
        if (noiseTimer > 10.05f) {
            noiseTimer = 0f;
            float noiseAmount = 0.2f; // Reduced amount
            float grayNoise = FastMath.nextRandomFloat() * noiseAmount;
            noiseFilter.setColor(new ColorRGBA(grayNoise, grayNoise, grayNoise, 1f));
        }
        */

        // Low health fear effects
        if (player != null) {
            float healthPercent = player.getHealth() / player.getMaxHealth();
            if (healthPercent < 0.3f) {
                // Example of a screen pulse effect
                float pulse = FastMath.sin(tpf * 3f) * 0.1f + 0.1f;
                // In a real implementation, you might apply this to a red overlay filter.
            }
        }
    }

    /**
     * Triggers a quick fade-out/fade-in effect for scares.
     */
    public void triggerScareEffect() {
        if (fadeFilter != null) {
            fadeFilter.setDuration(0.1f);
            fadeFilter.fadeOut();

            // In a real game, you would use a timer or scene graph update to schedule the fade-in.
            // For simplicity here, it's a conceptual representation.
            // enqueueSceneOperation could be used if this class had access to it.
        }
    }

    /**
     * Removes the filter processor from the viewport to clean up.
     */
    public void cleanup() {
        if (postProcessor != null) {
            viewPort.removeProcessor(postProcessor);
            postProcessor = null;
        }
    }
}