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
import com.jme3.util.SkyFactory;
import com.jme3.asset.AssetManager;
import com.jme3.renderer.Camera;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * COMPLETE IMPROVED HorrorGameJME - Full Implementation with Weapon/Pickup System
 *
 * MAJOR IMPROVEMENTS:
 * 1. Player starts UNARMED with no weapon systems initialized
 * 2. PickupSpawner scans map and places weapons/ammo randomly
 * 3. PickupProcessor handles automatic pickup detection
 * 4. Weapon systems initialize only when player finds first weapon
 * 5. Enemy drops create ammo pickups with 30% chance
 * 6. Complete error handling and debug output
 */
public class HorrorGameJME extends SimpleApplication {

    // Existing core managers
    private GameStateManager gameStateManager;
    private InputHandler inputHandler;
    private MenuSystem menuSystem;
    private OptionsManager optionsManager;
    private AudioManager audioManager;
    private EntityManager entityManager;
    private Player player;
    private HUDManager hudManager;
    private SkyboxManager skyboxManager;

    // NEW: Pickup system managers
    private PickupSpawner pickupSpawner;
    private PickupProcessor pickupProcessor;

    // Existing physics
    private BulletAppState bulletAppState;
    private CharacterControl playerControl;
    private RigidBodyControl landscapeControl;

    // Existing game world
    private Spatial doomMap;
    private DebugNoclipControl debugNoclip;
    private final ConcurrentLinkedQueue<Runnable> sceneCommands = new ConcurrentLinkedQueue<>();

    // Existing post-processing
    private FilterPostProcessor postProcessor;
    private FadeFilter fadeFilter;
    private ColorOverlayFilter noiseFilter;
    private ZombieSpawner zombieSpawner;
    private float noiseTimer = 0f;

    // Existing movement settings
    private static final float MAP_SCALE = 1.50f;
    private static final Vector3f PLAYER_START_POS = new Vector3f(10f, 0f, 20f);
    private static final float MOUSE_SENSITIVITY = 0.5f;

    public static void main(String[] args) {
        HorrorGameJME app = new HorrorGameJME();

        AppSettings settings = new AppSettings(true);
        settings.setTitle("Dawn of The Dead - Horror Experience");
        settings.setResolution(1280, 720);
        settings.setVSync(true);
        app.setSettings(settings);

        app.start();
    }

    @Override
    public void simpleInitApp() {
        System.out.println("=== HORROR GAME INITIALIZATION ===");

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

    // EXISTING METHODS (unchanged)
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

    private void initializePhysics() {
        bulletAppState = new BulletAppState();
        bulletAppState.setDebugEnabled(false);
        stateManager.attach(bulletAppState);
        System.out.println("Physics initialized");
    }

    private void setupProperLighting() {
        // Clear existing lights
        rootNode.getLocalLightList().clear();

        // Enable shadows on root node
        rootNode.setShadowMode(ShadowMode.CastAndReceive);

        // HORROR: Very dark ambient light
        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White.mult(0.2f));
        rootNode.addLight(ambient);

        // Main directional light - dimmer for horror
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.5f, -0.5f, -0.5f).normalizeLocal());
        sun.setColor(ColorRGBA.White.mult(0.05f));
        rootNode.addLight(sun);

        // Add shadow renderer
        DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, 2048, 3);
        dlsr.setLight(sun);
        dlsr.setShadowIntensity(0.9f);
        viewPort.addProcessor(dlsr);

        // Minimal fill light
        DirectionalLight fill = new DirectionalLight();
        fill.setDirection(new Vector3f(0.5f, -0.3f, 0.5f).normalizeLocal());
        fill.setColor(ColorRGBA.White.mult(0.1f));
        rootNode.addLight(fill);

        System.out.println("Lighting setup complete");
    }

    // UPDATED: Initialize managers with NEW pickup system
    private void initializeManagers() {
        System.out.println("Initializing managers...");

        gameStateManager = new GameStateManager();
        optionsManager = new OptionsManager();
        audioManager = new AudioManager(assetManager, rootNode);
        entityManager = new EntityManager(rootNode);
        hudManager = new HUDManager(assetManager, guiNode, settings);
        debugNoclip = new DebugNoclipControl(cam, inputManager);
        skyboxManager = new SkyboxManager(assetManager, rootNode);
        zombieSpawner = new ZombieSpawner(assetManager, cam, bulletAppState, entityManager);

        // NEW: Initialize pickup system
        pickupSpawner = new PickupSpawner(assetManager, audioManager, bulletAppState);
        // PickupProcessor will be initialized after player is created

        // Initialize audio with NEW pickup sounds
        try {
            audioManager.initializeHorrorSounds(); // Now includes pickup sounds
            System.out.println("Audio system initialized successfully");
        } catch (Exception e) {
            System.err.println("Audio initialization failed: " + e.getMessage());
            audioManager.initializeMinimalAudio();
        }

        System.out.println("All managers initialized");
    }

    private void setupCameraAndInput() {
        cam.setFrustumPerspective(75f, (float)cam.getWidth() / cam.getHeight(), 0.1f, 500f);

        flyCam.setEnabled(false);

        inputHandler = new InputHandler(inputManager, gameStateManager, this);

        menuSystem = new MenuSystem(assetManager, guiNode, settings, gameStateManager, this, optionsManager);
        inputHandler.setMenuSystem(menuSystem);

        System.out.println("Camera and input setup complete");
    }

    private void showMainMenu() {
        menuSystem.showMainMenu();
        inputHandler.disableMouseLook();
    }

    // UPDATED: Start game without weapon systems
    public void startGame() {
        System.out.println("=== STARTING GAME ===");

        gameStateManager.setState(GameStateManager.GameState.PLAYING);
        menuSystem.hide();

        // Load DOOM map
        loadDoomMap();

        // Setup post-processing AFTER map is loaded
        setupPostProcessing();

        // Create physics player (NO weapon systems yet)
        createSmoothPhysicsPlayer();

        // CRITICAL FIX: Setup player systems WITHOUT weapons but WITH game connection
        setupPlayerSystemsWithoutWeapons();

        // NEW: Scan map and spawn pickups
        setupPickupSystem();

        // Final setup
        finishGameStart();

        System.out.println("=== GAME START COMPLETE ===");
    }

    private void loadDoomMap() {
        System.out.println("Loading DOOM map...");

        try {
            doomMap = assetManager.loadModel("Models/scene.gltf");

            if (doomMap == null) {
                throw new RuntimeException("DOOM map file not found");
            }

            doomMap.scale(MAP_SCALE);
            applyDoomMapMaterials();

            CollisionShape mapCollisionShape = CollisionShapeFactory.createMeshShape(doomMap);
            landscapeControl = new RigidBodyControl(mapCollisionShape, 0);
            doomMap.addControl(landscapeControl);

            rootNode.attachChild(doomMap);
            bulletAppState.getPhysicsSpace().add(landscapeControl);

            System.out.println("DOOM map loaded successfully");

        } catch (Exception e) {
            System.err.println("Failed to load DOOM map: " + e.getMessage());
            e.printStackTrace();
            createMinimalFallbackMap();
        }
    }

    private void applyDoomMapMaterials() {
        doomMap.depthFirstTraversal(spatial -> {
            if (spatial instanceof Geometry) {
                Geometry geom = (Geometry) spatial;
                Material mat = geom.getMaterial();

                if (mat == null) {
                    mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
                    mat.setInt("NbLights", 8);
                    mat.setColor("Diffuse", ColorRGBA.Gray);
                    mat.setColor("Ambient", ColorRGBA.Gray.mult(0.3f));
                    mat.setBoolean("UseMaterialColors", true);
                    geom.setMaterial(mat);
                } else {
                    if (mat.getMaterialDef().getName().contains("Unshaded")) {
                        Material newMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
                        newMat.setInt("NbLights", 8);

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
                    } else {
                        if (mat.getAdditionalRenderState().getBlendMode() == RenderState.BlendMode.Off) {
                            mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
                            mat.setFloat("AlphaDiscardThreshold", 0.1f);
                        }
                    }
                }
            }
        });
    }

    private void createMinimalFallbackMap() {
        System.out.println("Creating fallback map...");

        Node fallbackMap = new Node("FallbackMap");

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

        CollisionShape mapShape = CollisionShapeFactory.createMeshShape(fallbackMap);
        landscapeControl = new RigidBodyControl(mapShape, 0);
        fallbackMap.addControl(landscapeControl);

        rootNode.attachChild(fallbackMap);
        bulletAppState.getPhysicsSpace().add(landscapeControl);
        doomMap = fallbackMap;
    }

    private void createSmoothPhysicsPlayer() {
        System.out.println("Creating physics player...");

        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(0.45f, 0.3f, 1);
        playerControl = new CharacterControl(capsuleShape, 0.31f);

        playerControl.setJumpSpeed(15);
        playerControl.setFallSpeed(25);
        playerControl.setGravity(65);

        Vector3f startPos = PLAYER_START_POS.mult(MAP_SCALE);
        playerControl.setPhysicsLocation(startPos);

        bulletAppState.getPhysicsSpace().add(playerControl);
        cam.setLocation(startPos.add(0, 0.8f * MAP_SCALE, 0));
    }

    // CRITICAL FIX: Setup player WITHOUT weapon systems initially but WITH proper game connection
    private void setupPlayerSystemsWithoutWeapons() {
        System.out.println("Setting up player systems (without weapons)...");

        // Create player WITHOUT weapon systems (they'll be added when first weapon is picked up)
        player = new Player(cam, rootNode, null, audioManager);

        // CRITICAL FIX: Connect player to game instance for weapon system initialization
        player.setGameInstance(this);
        System.out.println("Game: Player connected to game instance - " + (player != null ? "SUCCESS" : "FAILED"));

        inputHandler.setPlayer(player);
        inputHandler.setPlayerControl(playerControl);
        debugNoclip.setPlayer(player);

        // Apply options
        optionsManager.applySettings(player, audioManager);

        System.out.println("Game: Player systems setup complete (weapons will be initialized on first pickup)");
    }

    // NEW: Setup pickup system after map is loaded
    private void setupPickupSystem() {
        System.out.println("=== SETTING UP PICKUP SYSTEM ===");

        // Configure spawn settings (adjust these as needed)
        pickupSpawner.applySpawnPreset(PickupSpawner.SpawnPreset.NORMAL_RESOURCES); //NORMAL_RESOURCES

        // Scan the map for valid spawn points
        Vector3f playerStart = PLAYER_START_POS.mult(MAP_SCALE);
        pickupSpawner.scanMapForSpawnPoints(doomMap, playerStart);

        // Spawn weapons and ammo randomly throughout the map
        pickupSpawner.spawnAllPickups(entityManager);

        // Initialize pickup processor for automatic pickup detection
        pickupProcessor = new PickupProcessor(entityManager, player, hudManager);

        // Print spawn statistics
        System.out.println(pickupSpawner.getSpawnStatistics());
        System.out.println("=== PICKUP SYSTEM SETUP COMPLETE ===");
    }

    // CRITICAL FIX: Method called when player picks up their first weapon
    public void initializePlayerWeaponSystems(WeaponType firstWeapon) {
        System.out.println("=== INITIALIZING WEAPON SYSTEMS ===");
        System.out.println("Game: Initializing weapon systems for first weapon: " + firstWeapon.displayName);

        try {
            // Create weapon animator
            ModernWeaponAnimator weaponAnimator = new ModernWeaponAnimator(
                    assetManager, guiNode, settings.getWidth(), settings.getHeight()
            );

          /**  // Configure weapon position and motion
            Vector3f weaponScreenPos = new Vector3f(
                    settings.getWidth() * 0.3f,
                    settings.getHeight() * -0.02f,
                    0
            );
            float weaponScale = Math.min(settings.getWidth(), settings.getHeight()) / 120f; **/

            //weaponAnimator.setWeaponPosition(weaponScreenPos);
            //weaponAnimator.setWeaponScale(weaponScale);
            weaponAnimator.setProceduralMotion(true);
            weaponAnimator.setMouseSwayIntensity(150f);
            weaponAnimator.setMouseSwayDecay(0.88f);
            weaponAnimator.setMouseSwayLimits(250f, 250f);

            System.out.println("Game: Weapon animator created and configured");

            // Create weapon effects manager
            WeaponEffectsManager weaponEffectsManager = new WeaponEffectsManager(
                    assetManager, rootNode, guiNode, cam, bulletAppState, entityManager
            );

            weaponEffectsManager.setWeaponOffset(1f, 0.55f, 0f);
            weaponEffectsManager.enableWeaponCameraDebug(true);

            System.out.println("Game: Weapon effects manager created");

            // Create 3D muzzle flash system
            ModelBasedMuzzleFlash muzzleFlashSystem = new ModelBasedMuzzleFlash(rootNode, cam);

            System.out.println("Game: Muzzle flash system created");

            // Connect all systems to player
            player.setWeaponAnimator(weaponAnimator);
            player.setWeaponEffectsManager(weaponEffectsManager);
            player.setMuzzleFlashSystem(muzzleFlashSystem);

            System.out.println("Game: All weapon systems connected to player");
            System.out.println("Game: Weapon systems initialization complete!");
            System.out.println("===========================================");

        } catch (Exception e) {
            System.err.println("Game: CRITICAL ERROR - Failed to initialize weapon systems: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void finishGameStart() {
        inputHandler.enableMouseLook();
        hudManager.show();

        if (audioManager.hasBackgroundMusic("horror_ambient")) {
            audioManager.playBackgroundMusic("horror_ambient");
        }

        // UPDATED: Spawn zombies with new drop system
        spawnZombiesWithDrops();
    }

    // UPDATED: Spawn zombies with new enemy drop system
    private void spawnZombiesWithDrops() {
        System.out.println("=== SPAWNING ZOMBIES WITH DROP SYSTEM ===");

        Vector3f playerStartPos = PLAYER_START_POS.mult(MAP_SCALE);

        // Optional: Only configure if you want to override defaults
        // zombieSpawner.setZombieCount(15); // Already defaults to 15
        // zombieSpawner.setSpawnDistanceRange(20f, 80f); // Can customize if needed

        // Print configuration to see what defaults are being used
        System.out.println(zombieSpawner.getConfigurationSummary());

        // Spawn the zombies using the spawner's default configuration
        zombieSpawner.spawnInitialZombies(playerStartPos);

        System.out.println("=== ZOMBIE SPAWNING COMPLETE ===");
    }

    // UPDATED: Process enemy deaths with new drop system
    private void processEnemyDeaths() {
        // Track which zombies we've already processed to avoid duplicate drops
        for (Entity entity : entityManager.getEntitiesByType(Entity.EntityType.ENEMY)) {
            if (entity instanceof ZombieEnemy) {
                ZombieEnemy zombie = (ZombieEnemy) entity;

                // FIXED: Process drops when zombie enters DEAD state, not when destroyed
                if (zombie.getCurrentState() == ZombieEnemy.ZombieState.DEAD && !zombie.hasProcessedDrop()) {
                    System.out.println("Processing death drop for zombie: " + zombie.getEntityId());

                    // Create ammo drop using the DropSystem
                    DropSystem.processEnemyDeath(zombie, entityManager, assetManager, audioManager);

                    // Mark this zombie as processed to prevent duplicate drops
                    zombie.setDropProcessed(true);
                }
            }
        }
    }

    // Setup post-processing effects
    private void setupPostProcessing() {
        postProcessor = new FilterPostProcessor(assetManager);

        // Add effects one by one
        FogFilter fog = new FogFilter();
        fog.setFogColor(new ColorRGBA(0.1f, 0.1f, 0.1f, 1.0f));
        fog.setFogDensity(0.5f);
        fog.setFogDistance(50f);
        postProcessor.addFilter(fog);

        DepthOfFieldFilter dof = new DepthOfFieldFilter();
        dof.setFocusDistance(10f);
        dof.setFocusRange(15f);
        dof.setBlurScale(0.4f);
        postProcessor.addFilter(dof);

        BloomFilter bloom = new BloomFilter(BloomFilter.GlowMode.Objects);
        bloom.setBloomIntensity(1.5f);
        bloom.setBlurScale(1.5f);
        bloom.setExposurePower(5f);
        bloom.setDownSamplingFactor(2f);
        postProcessor.addFilter(bloom);

        noiseFilter = new ColorOverlayFilter();
        noiseFilter.setColor(ColorRGBA.White);
        postProcessor.addFilter(noiseFilter);

        fadeFilter = new FadeFilter();
        postProcessor.addFilter(fadeFilter);

        viewPort.addProcessor(postProcessor);
    }

    private void updateHorrorEffects(float tpf) {
        if (postProcessor == null) return;

        if (player != null) {
            float healthPercent = player.getHealth() / player.getMaxHealth();

            if (healthPercent < 0.3f) {
                float pulse = FastMath.sin(tpf * 3f) * 0.1f + 0.1f;
            }
        }
    }

    public void triggerScareEffect() {
        if (fadeFilter != null) {
            fadeFilter.setDuration(0.1f);
            fadeFilter.fadeOut();

            enqueueSceneOperation(() -> {
                fadeFilter.setDuration(0.5f);
                fadeFilter.fadeIn();
            });
        }
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

    // UPDATED: Cleanup with pickup system
    private void cleanupGame() {
        System.out.println("Cleaning up game...");

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

        // NEW: Cleanup pickup system
        if (pickupSpawner != null) {
            pickupSpawner.clearSpawnPoints();
        }
        pickupProcessor = null;

        System.out.println("Game cleanup complete");
    }

    // UPDATED: Main update loop with pickup processing
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
                    player.setPositionOnly(physicsPos);
                }
                player.update(tpf);
                hudManager.updateHUD(player);
            }

            if (entityManager != null) {
                entityManager.update(tpf);
                updateZombieAI();

                // CRITICAL FIX: Process pickup detection every frame
                if (pickupProcessor != null) {
                    pickupProcessor.update(tpf);
                }

                // CRITICAL FIX: Process enemy deaths for drops
                processEnemyDeaths();
            }
        }
    }

    private void updateZombieAI() {
        if (player == null) return;

        for (Entity entity : entityManager.getEntitiesByType(Entity.EntityType.ENEMY)) {
            if (entity instanceof ZombieEnemy) {
                ZombieEnemy zombie = (ZombieEnemy) entity;
                zombie.setPlayerPosition(player.getPosition());
            }
        }
    }

    @Override
    public void simpleRender(RenderManager rm) {
        // No custom rendering needed
    }

    // NEW: Debug method to check weapon system status
    public void debugWeaponSystem() {
        System.out.println("=== WEAPON SYSTEM DEBUG ===");

        if (player != null) {
            System.out.println(player.getWeaponSystemsStatus());
        } else {
            System.out.println("Player is null!");
        }

        // Check if pickup system is working
        if (pickupProcessor != null) {
            System.out.println("Pickup Processor: Connected");
        } else {
            System.out.println("Pickup Processor: Missing");
        }

        if (pickupSpawner != null) {
            System.out.println("Pickup Spawner: Connected");
            System.out.println(pickupSpawner.getSpawnStatistics());
        } else {
            System.out.println("Pickup Spawner: Missing");
        }

        // Debug entity counts
        if (entityManager != null) {
            System.out.println("Total Entities: " + entityManager.getEntityCount());
            System.out.println("Pickups: " + entityManager.getEntityCount(Entity.EntityType.PICKUP));
            System.out.println("Enemies: " + entityManager.getEntityCount(Entity.EntityType.ENEMY));
        }

        System.out.println("============================");
    }

    // NEW: Debug method for available textures
    public void debugAvailableTextures() {
        System.out.println("=== CHECKING AVAILABLE TEXTURES ===");

        String[] testPaths = {
                "Interface/Logo/Monkey.jpg",
                "Common/Textures/MissingTexture.png",
                "Textures/Terrain/splat/grass.jpg",
                "Textures/Weapons/sten_frame_000.png",
                "Textures/Pickups/weapon_sten_gun.png"
        };

        for (String path : testPaths) {
            try {
                assetManager.loadTexture(path);
                System.out.println("✓ Available: " + path);
            } catch (Exception e) {
                System.out.println("✗ Not found: " + path);
            }
        }
        System.out.println("====================================");
    }

    // NEW: Manual weapon spawn for testing
    public void spawnTestWeapon() {
        if (entityManager != null && assetManager != null && audioManager != null) {
            Vector3f playerPos = player != null ? player.getPosition() : PLAYER_START_POS.mult(MAP_SCALE);
            Vector3f spawnPos = playerPos.add(2f, 0.5f, 0f);

            WeaponPickup testWeapon = new WeaponPickup(spawnPos, WeaponType.STEN_GUN, assetManager, audioManager);
            entityManager.addEntity(testWeapon);

            System.out.println("Spawned test weapon at: " + spawnPos);
        }
    }

    // NEW: Manual ammo spawn for testing
    public void spawnTestAmmo() {
        if (entityManager != null && assetManager != null && audioManager != null) {
            Vector3f playerPos = player != null ? player.getPosition() : PLAYER_START_POS.mult(MAP_SCALE);
            Vector3f spawnPos = playerPos.add(-2f, 0.5f, 0f);

            AmmoPickup testAmmo = new AmmoPickup(spawnPos, AmmoType.SMG_9MM, assetManager, audioManager);
            entityManager.addEntity(testAmmo);

            System.out.println("Spawned test ammo at: " + spawnPos);
        }
    }

    @Override
    public void destroy() {
        System.out.println("Destroying application...");

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

    // NEW: Public getter methods for debugging
    public Player getPlayer() { return player; }
    public EntityManager getEntityManager() { return entityManager; }
    public PickupSpawner getPickupSpawner() { return pickupSpawner; }
    public PickupProcessor getPickupProcessor() { return pickupProcessor; }
    public AudioManager getAudioManager() { return audioManager; }
    public GameStateManager getGameStateManager() { return gameStateManager; }

    // NEW: Configuration methods
    public void setPickupSpawnPreset(PickupSpawner.SpawnPreset preset) {
        if (pickupSpawner != null) {
            pickupSpawner.applySpawnPreset(preset);
            System.out.println("Applied pickup spawn preset: " + preset);
        }
    }
}