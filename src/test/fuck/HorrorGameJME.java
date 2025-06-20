package horrorjme;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.math.FastMath;
import com.jme3.post.Filter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.FogFilter;
import com.jme3.post.filters.DepthOfFieldFilter;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.ColorOverlayFilter;
import com.jme3.post.filters.FadeFilter;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Horror Game with smooth movement, DOOM map loading, and full horror atmosphere
 */
public class HorrorGameJME extends SimpleApplication {

    // Core managers
    private GameStateManager gameStateManager;
    private InputHandler inputHandler;
    private MenuSystem menuSystem;
    private OptionsManager optionsManager;
    private AudioManager audioManager;
    private EntityManager entityManager;
    private Player player;
    private HUDManager hudManager;

    // Physics
    private BulletAppState bulletAppState;
    private CharacterControl playerControl;
    private RigidBodyControl landscapeControl;

    // Game world
    private Spatial doomMap;
    private DebugNoclipControl debugNoclip;
    private final ConcurrentLinkedQueue<Runnable> sceneCommands = new ConcurrentLinkedQueue<>();

    // Post-processing
    private FilterPostProcessor postProcessor;
    private FadeFilter fadeFilter;
    private ColorOverlayFilter noiseFilter;
    private float noiseTimer = 0f;

    // SMOOTH MOVEMENT SETTINGS
    private static final float MAP_SCALE = 0.075f;  // Smaller scale for DOOM map
    private static final Vector3f PLAYER_START_POS = new Vector3f(10f, 150f, 20f);
    private static final float MOUSE_SENSITIVITY = 0.5f; // Lower mouse sensitivity

    private void processSceneCommands() {
        Runnable command;
        while ((command = sceneCommands.poll()) != null) {
            try {
                command.run();
            } catch (Exception e) {
                System.err.println("Error executing scene command: " + e.getMessage());
            }
        }
    }

    public void enqueueSceneOperation(Runnable operation) {
        if (operation != null) {
            sceneCommands.offer(operation);
        }
    }

    public static void main(String[] args) {
        HorrorGameJME app = new HorrorGameJME();

        AppSettings settings = new AppSettings(true);
        settings.setTitle("Dawn of The Dead - Horror Experience");
        settings.setResolution(1280, 720);
        settings.setVSync(true); // Enable VSync for smoother movement
        app.setSettings(settings);

        app.start();
    }

    @Override
    public void simpleInitApp() {
        System.out.println("=== INITIALIZING HORROR GAME WITH DOOM MAP ===");

        // Initialize physics (without debug for cleaner view)
        initializePhysics();

        // Setup lighting (keep the working lighting)
        setupProperLighting();

        // Initialize managers
        initializeManagers();

        // Setup camera and input with smooth settings
        setupCameraAndInput();

        // Show menu
        showMainMenu();

        System.out.println("=== INITIALIZATION COMPLETE ===");
    }

    private void initializePhysics() {
        System.out.println("Setting up physics...");

        bulletAppState = new BulletAppState();
        // Disable debug for cleaner view (can re-enable with F2 if needed)
        bulletAppState.setDebugEnabled(false);
        stateManager.attach(bulletAppState);

        System.out.println("Physics initialized");
    }

    private void setupProperLighting() {
        System.out.println("Setting up horror lighting for DOOM map...");

        // Clear existing lights
        rootNode.getLocalLightList().clear();

        // Enable shadows on root node
        rootNode.setShadowMode(ShadowMode.CastAndReceive);

        // HORROR: Very dark ambient light
        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White.mult(0.2f)); // Near pitch black
        rootNode.addLight(ambient);

        // Main directional light - dimmer for horror
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.5f, -0.5f, -0.5f).normalizeLocal());
        sun.setColor(ColorRGBA.White.mult(0.05f)); // Much dimmer
        rootNode.addLight(sun);

        // Add shadow renderer
        DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, 2048, 3);
        dlsr.setLight(sun);
        dlsr.setShadowIntensity(0.9f); // Dark shadows
        viewPort.addProcessor(dlsr);

        // Minimal fill light
        DirectionalLight fill = new DirectionalLight();
        fill.setDirection(new Vector3f(0.5f, -0.3f, 0.5f).normalizeLocal());
        fill.setColor(ColorRGBA.White.mult(0.1f)); // Very dim
        rootNode.addLight(fill);

        System.out.println("Horror lighting with shadows setup complete");
    }

    private void setupPostProcessing() {
        System.out.println("Setting up horror post-processing effects (DEBUG MODE)...");

        postProcessor = new FilterPostProcessor(assetManager);

        // Add effects one by one - comment out the ones causing issues

        // 1. FOG - Usually safe, but reduce density
        FogFilter fog = new FogFilter();
        fog.setFogColor(new ColorRGBA(0.1f, 0.1f, 0.1f, 1.0f)); // Lighter fog
        fog.setFogDensity(0.5f);  // Much less dense
        fog.setFogDistance(50f);   // Further distance
        postProcessor.addFilter(fog);
        System.out.println("Added FOG filter");

        // 2. COMMENT OUT DOF - Often causes issues

        DepthOfFieldFilter dof = new DepthOfFieldFilter();
        dof.setFocusDistance(10f);
        dof.setFocusRange(15f);
        dof.setBlurScale(1.4f);
        postProcessor.addFilter(dof);
        System.out.println("Added DOF filter");


        // 3. BLOOM - Sometimes causes black screen

        BloomFilter bloom = new BloomFilter(BloomFilter.GlowMode.Objects);
        bloom.setBloomIntensity(1.5f);
        bloom.setBlurScale(1.5f);
        bloom.setExposurePower(5f);
        bloom.setDownSamplingFactor(2f);
        postProcessor.addFilter(bloom);
        System.out.println("Added BLOOM filter");


        // 4. SKIP VIGNETTE - Was causing the error
        //postProcessor.addFilter(createVignetteFilter());

        // 5. NOISE - Usually safe
        noiseFilter = new ColorOverlayFilter();
        noiseFilter.setColor(ColorRGBA.White);
        postProcessor.addFilter(noiseFilter);
        System.out.println("Added NOISE filter");

        // 6. FADE - Usually safe
        fadeFilter = new FadeFilter();
        postProcessor.addFilter(fadeFilter);
        System.out.println("Added FADE filter");

        viewPort.addProcessor(postProcessor);

        System.out.println("Horror post-processing initialized (DEBUG MODE)");
    }
    // Simple vignette using ColorOverlay
    /*private Filter createVignetteFilter() {
        // Create a darkening effect at screen edges
        ColorOverlayFilter vignetteFilter = new ColorOverlayFilter();
        vignetteFilter.setColor(new ColorRGBA(0, 0, 0, 0.3f)); // Dark overlay
        return vignetteFilter;
    }*/

    private void updateHorrorEffects(float tpf) {
        if (postProcessor == null) return;

        // 1. NOISE EFFECT - Animated film grain
       /* noiseTimer += tpf;
        if (noiseTimer > 10.05f) {
            noiseTimer = 0f;
            float noiseAmount = 0.2f; // Reduced amount
            float grayNoise = FastMath.nextRandomFloat() * noiseAmount;
            noiseFilter.setColor(new ColorRGBA(grayNoise, grayNoise, grayNoise, 1f));

        }*/

        // 2. FEAR EFFECTS - Triggered by events
        if (player != null) {
            float healthPercent = player.getHealth() / player.getMaxHealth();

            // Low health effects
            if (healthPercent < 0.3f) {
                // Screen pulses red
                float pulse = FastMath.sin(tpf * 3f) * 0.1f + 0.1f;
                // Note: In real implementation, you'd manage this filter better
            }
        }
    }

    // Scare effect method
    public void triggerScareEffect() {
        if (fadeFilter != null) {
            // Quick flash
            fadeFilter.setDuration(0.1f);
            fadeFilter.fadeOut();

            // Schedule fade back in
            enqueueSceneOperation(() -> {
                fadeFilter.setDuration(0.5f);
                fadeFilter.fadeIn();
            });
        }
    }

    private void initializeManagers() {
        System.out.println("Initializing game managers...");

        gameStateManager = new GameStateManager();
        optionsManager = new OptionsManager();
        audioManager = new AudioManager(assetManager, rootNode);
        entityManager = new EntityManager(rootNode);
        hudManager = new HUDManager(assetManager, guiNode, settings);
        debugNoclip = new DebugNoclipControl(cam, inputManager);

        // Initialize audio
        try {
            audioManager.initializeHorrorSounds();
        } catch (Exception e) {
            System.err.println("Audio initialization failed: " + e.getMessage());
            audioManager.initializeMinimalAudio();
        }

        System.out.println("Managers initialized");
    }

    private void setupCameraAndInput() {
        System.out.println("Setting up smooth camera and input...");

        // Setup camera with better settings
        cam.setFrustumPerspective(75f, (float)cam.getWidth() / cam.getHeight(), 0.1f, 500f);

        // Disable flyCam
        flyCam.setEnabled(false);

        // Initialize input handler with smooth movement
        inputHandler = new InputHandler(inputManager, gameStateManager, this);

        menuSystem = new MenuSystem(assetManager, guiNode, settings, gameStateManager, this, optionsManager);
        inputHandler.setMenuSystem(menuSystem);

        System.out.println("Smooth camera and input setup complete");
    }

    private void showMainMenu() {
        System.out.println("Showing main menu...");
        menuSystem.showMainMenu();
        inputHandler.disableMouseLook();
    }

    public void startGame() {
        System.out.println("=== STARTING GAME WITH DOOM MAP ===");

        gameStateManager.setState(GameStateManager.GameState.PLAYING);
        menuSystem.hide();

        // Load DOOM map
        loadDoomMap();

        // Setup post-processing AFTER map is loaded
        setupPostProcessing();

        // Create smooth physics player
        createSmoothPhysicsPlayer();

        // Setup player systems
        setupPlayerSystems();

        // Final setup
        finishGameStart();

        System.out.println("=== DOOM MAP LOADED - GAME STARTED ===");
    }

    /**
     * Load the actual DOOM map with proper scaling and materials
     */
    private void loadDoomMap() {
        System.out.println("Loading DOOM2_MAP01.obj...");

        try {
            // Load the DOOM map
            doomMap = assetManager.loadModel("Models/DOOM2_MAP01.j3o");

            if (doomMap == null) {
                throw new RuntimeException("DOOM map file not found at Models/DOOM2_MAP01.obj");
            }

            // Scale the map appropriately
            doomMap.scale(MAP_SCALE);
            System.out.println("DOOM map scaled by: " + MAP_SCALE);

            // Apply proper materials to all geometries
            applyDoomMapMaterials();

            // Create collision shape for the DOOM map
            System.out.println("Creating collision shape for DOOM map...");
            CollisionShape mapCollisionShape = CollisionShapeFactory.createMeshShape(doomMap);


            // Create physics control
            landscapeControl = new RigidBodyControl(mapCollisionShape, 0);
            doomMap.addControl(landscapeControl);

            // Add to scene and physics
            rootNode.attachChild(doomMap);
            bulletAppState.getPhysicsSpace().add(landscapeControl);

            System.out.println("DOOM map loaded successfully!");

        } catch (Exception e) {
            System.err.println("Failed to load DOOM map: " + e.getMessage());
            e.printStackTrace();
            System.out.println("Creating fallback map...");
            createMinimalFallbackMap();
        }
    }

    /**
     * Apply proper materials to DOOM map geometries
     */
    private void applyDoomMapMaterials() {
        System.out.println("Applying materials to DOOM map...");

        doomMap.depthFirstTraversal(spatial -> {
            if (spatial instanceof Geometry) {
                Geometry geom = (Geometry) spatial;

                // Enable shadows for this geometry
                geom.setShadowMode(ShadowMode.CastAndReceive);

                Material mat = geom.getMaterial();

                if (mat == null) {
                    // Create new material if none exists
                    mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
                    mat.setColor("Diffuse", ColorRGBA.Gray);
                    mat.setColor("Ambient", ColorRGBA.Gray.mult(0.3f));
                    mat.setBoolean("UseMaterialColors", true);
                    geom.setMaterial(mat);
                } else {
                    // Ensure existing material works with lighting
                    if (mat.getMaterialDef().getName().contains("Unshaded")) {
                        // Convert unshaded to lit material
                        Material newMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");

                        // Try to preserve diffuse color/texture
                        if (mat.getParam("ColorMap") != null) {
                            newMat.setTexture("DiffuseMap", mat.getParamValue("ColorMap"));
                        } else if (mat.getParam("Color") != null) {
                            ColorRGBA color = (ColorRGBA) mat.getParamValue("Color");
                            newMat.setColor("Diffuse", color);
                            newMat.setColor("Ambient", color.mult(0.3f));
                        } else {
                            newMat.setColor("Diffuse", ColorRGBA.Gray);
                            newMat.setColor("Ambient", ColorRGBA.Gray.mult(0.3f));
                        }

                        newMat.setBoolean("UseMaterialColors", true);
                        geom.setMaterial(newMat);
                    }

                    // Handle alpha blending if needed
                    if (mat.getAdditionalRenderState().getBlendMode() == RenderState.BlendMode.Off) {
                        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
                        mat.setFloat("AlphaDiscardThreshold", 0.1f);
                    }
                }
            }
        });

        System.out.println("DOOM map materials applied");
    }

    /**
     * Create a minimal fallback if DOOM map fails
     */
    private void createMinimalFallbackMap() {
        System.out.println("Creating minimal fallback map...");

        Node fallbackMap = new Node("FallbackMap");

        // Simple floor
        Box floorBox = new Box(50f, 1f, 50f);
        Geometry floor = new Geometry("Floor", floorBox);
        Material floorMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        floorMat.setColor("Diffuse", ColorRGBA.Brown);
        floorMat.setColor("Ambient", ColorRGBA.Brown.mult(0.3f));
        floorMat.setBoolean("UseMaterialColors", true);
        floor.setMaterial(floorMat);
        floor.setLocalTranslation(0, -1f, 0);
        floor.setShadowMode(ShadowMode.CastAndReceive);
        fallbackMap.attachChild(floor);

        // A few walls for testing
        for (int i = 0; i < 2; i++) {
            Box wallBox = new Box(1f, 10f, 30f);
            Geometry wall = new Geometry("Wall" + i, wallBox);
            Material wallMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
            wallMat.setColor("Diffuse", i == 0 ? ColorRGBA.Red : ColorRGBA.Blue);
            wallMat.setColor("Ambient", (i == 0 ? ColorRGBA.Red : ColorRGBA.Blue).mult(0.3f));
            wallMat.setBoolean("UseMaterialColors", true);
            wall.setMaterial(wallMat);
            wall.setLocalTranslation((i * 40f) - 20f, 10f, 20f);
            wall.setShadowMode(ShadowMode.CastAndReceive);
            fallbackMap.attachChild(wall);
        }

        // Add physics
        CollisionShape mapShape = CollisionShapeFactory.createMeshShape(fallbackMap);
        landscapeControl = new RigidBodyControl(mapShape, 0);
        fallbackMap.addControl(landscapeControl);

        rootNode.attachChild(fallbackMap);
        bulletAppState.getPhysicsSpace().add(landscapeControl);
        doomMap = fallbackMap;

        System.out.println("Fallback map created");
    }

    /**
     * Create physics player with smooth movement settings
     */
    private void createSmoothPhysicsPlayer() {
        System.out.println("Creating smooth physics player...");

        // Create smaller capsule for better movement
        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(0.9f, 1.6f, 1);

        // Create CharacterControl with smooth settings
        playerControl = new CharacterControl(capsuleShape, 0.31f); // Lower step height

        // Configure smooth physics
        playerControl.setJumpSpeed(15);      // Lower jump
        playerControl.setFallSpeed(25);      // Slower fall
        playerControl.setGravity(65);        // Lower gravity

        // Set starting position
        Vector3f startPos = PLAYER_START_POS.mult(MAP_SCALE); // Scale with map
        playerControl.setPhysicsLocation(startPos);

        // Add to physics space
        bulletAppState.getPhysicsSpace().add(playerControl);

        // Position camera smoothly
        cam.setLocation(startPos.add(0, 0.8f * MAP_SCALE, 0)); // Scale camera offset

        System.out.println("Smooth physics player created at: " + startPos);
    }

    private void setupPlayerSystems() {
        System.out.println("Setting up player systems with perfect weapon alignment...");

        // 1. FIRST: Create the Player object
        player = new Player(cam, rootNode, null, audioManager);

        // 2. SECOND: Create enhanced HUD with crosshair system
        hudManager = new HUDManager(assetManager, guiNode, settings);
        CrosshairManager crosshairManager = hudManager.getCrosshairManager();

        // 3. THIRD: Create the weapon animator
        ModernWeaponAnimator weaponAnimator = new ModernWeaponAnimator(
                assetManager, guiNode, settings.getWidth(), settings.getHeight()
        );

        // 4. FOURTH: Load gun frames and configure position
        weaponAnimator.loadFrames("Textures/Weapons/sten_frame_", 19);

        // IMPORTANT: Configure weapon position that will be used for alignment
        Vector3f weaponScreenPos = new Vector3f(
                settings.getWidth() / 2f - 100f,  // Adjust these values to match your weapon position
                settings.getHeight() - 200f,      // Adjust these values to match your weapon position
                0
        );
        float weaponScale = 8.0f;

        weaponAnimator.setWeaponPosition(weaponScreenPos);
        weaponAnimator.setWeaponScale(weaponScale);
        weaponAnimator.setProceduralMotion(true);
        weaponAnimator.setMouseSwayIntensity(150f);
        weaponAnimator.setMouseSwayDecay(0.88f);
        weaponAnimator.setMouseSwayLimits(250f, 250f);

        // 5. FIFTH: Create enhanced weapon effects manager
        WeaponEffectsManager weaponEffectsManager = new WeaponEffectsManager(
                assetManager, rootNode, guiNode, cam, bulletAppState, entityManager
        );

        // 6. SIXTH: Connect crosshair to weapon effects for perfect alignment
        weaponEffectsManager.setCrosshairManager(crosshairManager);

        // 7. SEVENTH: Configure weapon type and alignment
        weaponEffectsManager.setupWeaponPreset(
                WeaponEffectsManager.WeaponPreset.SUBMACHINE_GUN,  // Change based on your weapon
                weaponScreenPos,
                weaponScale
        );

        // 8. EIGHTH: Align crosshair with weapon position
        hudManager.alignCrosshairWithWeapon(weaponScreenPos.x, weaponScreenPos.y, weaponScale);

        // 9. NINTH: Create 3D muzzle flash system
        ModelBasedMuzzleFlash muzzleFlashSystem = new ModelBasedMuzzleFlash(rootNode, cam);

        // 10. TENTH: Connect all systems to player
        player.setWeaponAnimator(weaponAnimator);
        player.setWeaponEffectsManager(weaponEffectsManager);
        player.setMuzzleFlashSystem(muzzleFlashSystem);

        // 11. ELEVENTH: Apply options
        optionsManager.applySettings(player, audioManager);

        System.out.println("Player systems setup complete with perfect weapon alignment");
        System.out.println("Weapon Effects Status: " + weaponEffectsManager.getSystemStatus());

        // 12. OPTIONAL: Test alignment
        weaponEffectsManager.testAlignment();
    }

    /**
     * Configure crosshair settings
     */
    private void configureCrosshair() {
        if (hudManager != null && hudManager.getCrosshairManager() != null) {
            CrosshairManager crosshair = hudManager.getCrosshairManager();

            // Basic crosshair configuration
            crosshair.setCrosshairSize(32f);
            crosshair.setCrosshairColor(new ColorRGBA(1f, 1f, 1f, 0.8f)); // White, slightly transparent
            crosshair.setAccuracyIndicatorVisible(true);
            crosshair.setBaseAccuracy(15f, 80f); // Min and max accuracy circle sizes

            System.out.println("Crosshair configured");
        }
    }

    /**
     * Configure weapon alignment for different weapon types
     */
    public void configureWeaponAlignment(String weaponType) {
        if (player != null && player.getWeaponEffectsManager() != null) {
            WeaponEffectsManager effects = player.getWeaponEffectsManager();

            switch (weaponType.toLowerCase()) {
                case "pistol":
                    effects.setupWeaponPreset(WeaponEffectsManager.WeaponPreset.PISTOL,
                            new Vector3f(settings.getWidth() / 2f - 50f, settings.getHeight() - 150f, 0), 6.0f);
                    break;
                case "rifle":
                    effects.setupWeaponPreset(WeaponEffectsManager.WeaponPreset.ASSAULT_RIFLE,
                            new Vector3f(settings.getWidth() / 2f - 120f, settings.getHeight() - 180f, 0), 8.0f);
                    break;
                case "smg":
                    effects.setupWeaponPreset(WeaponEffectsManager.WeaponPreset.SUBMACHINE_GUN,
                            new Vector3f(settings.getWidth() / 2f - 80f, settings.getHeight() - 160f, 0), 7.0f);
                    break;
                case "shotgun":
                    effects.setupWeaponPreset(WeaponEffectsManager.WeaponPreset.SHOTGUN,
                            new Vector3f(settings.getWidth() / 2f - 140f, settings.getHeight() - 200f, 0), 9.0f);
                    break;
            }

            System.out.println("Weapon alignment configured for: " + weaponType);
        }
    }

    private void finishGameStart() {
        System.out.println("Finishing game start...");

        // Enable mouse look
        inputHandler.enableMouseLook();

        // Show HUD
        hudManager.show();

        // Start background music
        if (audioManager.hasBackgroundMusic("horror_ambient")) {
            audioManager.playBackgroundMusic("horror_ambient");
        }

        System.out.println("Game start complete - Horror DOOM map ready!");
    }

    // Game state management methods
    public void pauseGame() {
        if (gameStateManager.getCurrentState() == GameStateManager.GameState.PLAYING) {
            inputHandler.disableMouseLook();
            audioManager.pauseBackgroundMusic();
            menuSystem.showPauseMenu();
        }
    }

    public void resumeGame() {
        if (gameStateManager.getCurrentState() == GameStateManager.GameState.PAUSED) {
            gameStateManager.setState(GameStateManager.GameState.PLAYING);
            menuSystem.hide();
            inputHandler.enableMouseLook();
            audioManager.resumeBackgroundMusic();
        }
    }

    public void returnToMainMenu() {
        cleanupGame();
        menuSystem.showMainMenu();
        inputHandler.disableMouseLook();
        audioManager.stopBackgroundMusic();
        hudManager.hide();
    }

    private void cleanupGame() {
        System.out.println("Cleaning up game...");

        // Remove post-processing
        if (postProcessor != null) {
            viewPort.removeProcessor(postProcessor);
            postProcessor = null;
        }

        if (playerControl != null) {
            bulletAppState.getPhysicsSpace().remove(playerControl);
            playerControl = null;
        }

        if (landscapeControl != null) {
            bulletAppState.getPhysicsSpace().remove(landscapeControl);
            landscapeControl = null;
        }

        if (doomMap != null) {
            rootNode.detachChild(doomMap);
            doomMap = null;
        }

        player = null;
        inputHandler.setPlayer(null);

        if (entityManager != null) {
            entityManager.clear();
        }

        System.out.println("Game cleanup complete");
    }

    @Override
    public void simpleUpdate(float tpf) {
        processSceneCommands();
        debugNoclip.update(tpf);

        if (gameStateManager.getCurrentState() == GameStateManager.GameState.PLAYING) {
            if (inputHandler != null) {
                inputHandler.update(tpf);
            }

            if (player != null && !debugNoclip.isEnabled()) {
                if (playerControl != null) {
                    Vector3f physicsPos = playerControl.getPhysicsLocation();
                    // DON'T sync rotation - only position
                    player.setPositionOnly(physicsPos);
                }
                player.update(tpf);
                hudManager.updateHUD(player);
            }

            if (entityManager != null) {
                entityManager.update(tpf);
            }

            // Update horror post-processing effects
            updateHorrorEffects(tpf);
        }
    }

    @Override
    public void simpleRender(RenderManager rm) {
        // No custom rendering needed
    }

    @Override
    public void destroy() {
        System.out.println("Destroying game...");

        if (inputHandler != null) {
            inputHandler.cleanup();
        }
        if (audioManager != null) {
            audioManager.cleanup();
        }
        if (entityManager != null) {
            entityManager.clear();
        }

        super.destroy();
    }
}