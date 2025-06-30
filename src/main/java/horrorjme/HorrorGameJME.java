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
 * COMPLETE UPDATED HorrorGameJME with Timer System and Point Economy
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
    private SkyboxManager skyboxManager;
    private MapManager mapManager;

    // NEW: Timer and Score systems
    private TimerSystem timerSystem;
    private ScoreSystem scoreSystem;

    // Pickup system managers
    private PickupSpawner pickupSpawner;
    private PickupProcessor pickupProcessor;

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
    private ZombieSpawner zombieSpawner;
    private float noiseTimer = 0f;

    // Movement settings
    private static final float MAP_SCALE = 1.50f;
    private static final Vector3f PLAYER_START_POS = new Vector3f(10f, 0f, 20f);
    private static final float MOUSE_SENSITIVITY = 0.5f;

    // Add fields for inter-round delay
    private boolean interRoundDelayActive = false;
    private float interRoundDelayTimer = 0f;
    private static final float INTER_ROUND_DELAY = 15f;

    @Override
    public void simpleInitApp() {
        System.out.println("=== HORROR GAME INITIALIZATION ===");

        removeDefaultESCMapping();
        initializePhysics();
        setupProperLighting();
        initializeManagers();
        setupCameraAndInput();
        showMainMenu();

        System.out.println("=== INITIALIZATION COMPLETE ===");
    }

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

    private void removeDefaultESCMapping() {
        if (inputManager.hasMapping(SimpleApplication.INPUT_MAPPING_EXIT)) {
            inputManager.deleteMapping(SimpleApplication.INPUT_MAPPING_EXIT);
            System.out.println("Removed default ESC->exit mapping");
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
        rootNode.getLocalLightList().clear();
        rootNode.setShadowMode(ShadowMode.CastAndReceive);

        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White.mult(0.2f));
        rootNode.addLight(ambient);

        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.5f, -0.5f, -0.5f).normalizeLocal());
        sun.setColor(ColorRGBA.White.mult(0.05f));
        rootNode.addLight(sun);

        DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, 2048, 3);
        dlsr.setLight(sun);
        dlsr.setShadowIntensity(0.9f);
        viewPort.addProcessor(dlsr);

        DirectionalLight fill = new DirectionalLight();
        fill.setDirection(new Vector3f(0.5f, -0.3f, 0.5f).normalizeLocal());
        fill.setColor(ColorRGBA.White.mult(0.1f));
        rootNode.addLight(fill);

        System.out.println("Lighting setup complete");
    }

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
        zombieSpawner.setAudioManager(audioManager);
        mapManager = new MapManager();
        // NEW: Initialize timer and score systems
        timerSystem = new TimerSystem();
        scoreSystem = new ScoreSystem();

        // Initialize pickup system
        pickupSpawner = new PickupSpawner(assetManager, audioManager, bulletAppState);

        try {
            audioManager.initializeHorrorSounds();
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
        // Inside setupCameraAndInput()
// menuSystem = new MenuSystem(assetManager, guiNode, settings, gameStateManager, this, optionsManager); // OLD
        menuSystem = new MenuSystem(assetManager, guiNode, settings, gameStateManager, this, optionsManager, mapManager); // NEW
        inputHandler.setMenuSystem(menuSystem);
        System.out.println("Camera and input setup complete");
    }

    private void showMainMenu() {
        menuSystem.showMainMenu();
        inputHandler.disableMouseLook();
    }

    public void startGame() {
        System.out.println("=== STARTING GAME ===");

        gameStateManager.setState(GameStateManager.GameState.PLAYING);
        menuSystem.hide();

        // NEW: Reset HUD for new game
        if (hudManager != null) {
            hudManager.reset();
        }

        loadSelectedMap();
        setupPostProcessing();
        createSmoothPhysicsPlayer();
        setupPlayerSystemsWithoutWeapons();
        setupPickupSystem();
        finishGameStart();

        System.out.println("=== GAME START COMPLETE ===");
    }

    private void loadSelectedMap() {
        MapInfo mapToLoad = mapManager.getCurrentMap();
        System.out.println("Loading map: " + mapToLoad.getDisplayName() + " from " + mapToLoad.getModelPath());

        try {
            doomMap = assetManager.loadModel(mapToLoad.getModelPath());
            if (doomMap == null) {
                throw new RuntimeException("Map file not found: " + mapToLoad.getModelPath());
            }

            doomMap.scale(MAP_SCALE);
            // applyDoomMapMaterials(); // Keep this helper method if it exists

            CollisionShape mapCollisionShape = CollisionShapeFactory.createMeshShape(doomMap);
            landscapeControl = new RigidBodyControl(mapCollisionShape, 0);
            doomMap.addControl(landscapeControl);

            rootNode.attachChild(doomMap);
            bulletAppState.getPhysicsSpace().add(landscapeControl);

            System.out.println(mapToLoad.getDisplayName() + " loaded successfully.");
        } catch (Exception e) {
            System.err.println("Failed to load map: " + mapToLoad.getDisplayName());
            e.printStackTrace();
            // createMinimalFallbackMap(); // Keep your fallback logic
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

        MapInfo currentMap = mapManager.getCurrentMap();
        Vector3f startPos = currentMap.getStartPosition().mult(MAP_SCALE);

        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(0.45f, 0.3f, 1);
        playerControl = new CharacterControl(capsuleShape, 0.31f);

        playerControl.setJumpSpeed(15);
        playerControl.setFallSpeed(25);
        playerControl.setGravity(65);

        //Vector3f startPos = PLAYER_START_POS.mult(MAP_SCALE);
        playerControl.setPhysicsLocation(startPos);

        bulletAppState.getPhysicsSpace().add(playerControl);
        cam.setLocation(startPos.add(0, 0.8f * MAP_SCALE, 0));
    }

    private void setupPlayerSystemsWithoutWeapons() {
        System.out.println("Setting up player systems (without weapons)...");

        // NEW: Reset existing player or create new one
        if (player != null) {
            player.reset();
        } else {
            player = new Player(cam, rootNode, null, audioManager);
        }
        
        player.setGameInstance(this);

        // Set player reference in ZombieSpawner for damage dealing
        zombieSpawner.setPlayer(player);
        
        // NEW: Set HUD manager reference in player for damage effects
        player.setHUDManager(hudManager);

        inputHandler.setPlayer(player);
        inputHandler.setPlayerControl(playerControl);
        debugNoclip.setPlayer(player);

        optionsManager.applySettings(player, audioManager);

        System.out.println("Player systems setup complete (weapons will be initialized on first pickup)");
    }

    private void setupPickupSystem() {
        System.out.println("=== SETTING UP PICKUP SYSTEM ===");

        pickupSpawner.applySpawnPreset(PickupSpawner.SpawnPreset.NORMAL_RESOURCES);

        Vector3f playerStart = PLAYER_START_POS.mult(MAP_SCALE);
        pickupSpawner.scanMapForSpawnPoints(doomMap, playerStart);
        pickupSpawner.spawnAllPickups(entityManager);

        pickupProcessor = new PickupProcessor(entityManager, player, hudManager);
        pickupProcessor.setScoreSystem(scoreSystem);

        System.out.println(pickupSpawner.getSpawnStatistics());
        System.out.println("=== PICKUP SYSTEM SETUP COMPLETE ===");
    }

    public void initializePlayerWeaponSystems(WeaponType firstWeapon) {
        System.out.println("=== INITIALIZING WEAPON SYSTEMS ===");
        System.out.println("Initializing weapon systems for first weapon: " + firstWeapon.displayName);

        try {
            ModernWeaponAnimator weaponAnimator = new ModernWeaponAnimator(
                    assetManager, guiNode, settings.getWidth(), settings.getHeight()
            );

            weaponAnimator.setProceduralMotion(true);
            weaponAnimator.setMouseSwayIntensity(150f);
            weaponAnimator.setMouseSwayDecay(0.88f);
            weaponAnimator.setMouseSwayLimits(250f, 250f);

            WeaponEffectsManager weaponEffectsManager = new WeaponEffectsManager(
                    assetManager, rootNode, guiNode, cam, bulletAppState, entityManager
            );

            weaponEffectsManager.setWeaponOffset(1f, 0.55f, 0f);
            weaponEffectsManager.enableWeaponCameraDebug(true);

            ModelBasedMuzzleFlash muzzleFlashSystem = new ModelBasedMuzzleFlash(rootNode, cam);

            player.setWeaponAnimator(weaponAnimator);
            player.setWeaponEffectsManager(weaponEffectsManager);
            player.setMuzzleFlashSystem(muzzleFlashSystem);

            System.out.println("Weapon systems initialization complete!");
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR - Failed to initialize weapon systems: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void finishGameStart() {
        inputHandler.enableMouseLook();
        hudManager.show();

        if (audioManager.hasBackgroundMusic("horror_ambient")) {
            audioManager.playBackgroundMusic("horror_ambient");
        }

        spawnZombiesWithDrops();

        // NEW: Start the survival round
        timerSystem.startRound();
        scoreSystem.startNewRound();

        System.out.println("=== SURVIVAL ROUND STARTED ===");
        System.out.println("Objective: Survive " + (int)(timerSystem.getRoundDuration() / 60) + " minutes!");
        System.out.println("Starting points: " + scoreSystem.getCurrentPoints());
    }

    private void spawnZombiesWithDrops() {
        System.out.println("=== SPAWNING ZOMBIES ===");

        Vector3f playerStartPos = PLAYER_START_POS.mult(MAP_SCALE);
        zombieSpawner.spawnInitialZombies(playerStartPos);

        System.out.println("=== ZOMBIE SPAWNING COMPLETE ===");
    }

    private void processEnemyDeaths() {
        for (Entity entity : entityManager.getEntitiesByType(Entity.EntityType.ENEMY)) {
            if (entity instanceof ZombieEnemy) {
                ZombieEnemy zombie = (ZombieEnemy) entity;

                if (zombie.getCurrentState() == ZombieEnemy.ZombieState.DEAD && !zombie.hasProcessedDrop()) {
                    System.out.println("Processing death drop for zombie: " + zombie.getEntityId());

                    // NEW: Create drops with score system integration
                    DropSystem.processEnemyDeath(zombie, entityManager, assetManager, audioManager, scoreSystem);
                    zombie.setDropProcessed(true);
                }
            }
        }
    }

    private void setupPostProcessing() {
        postProcessor = new FilterPostProcessor(assetManager);

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
    
        // FIXED: Clean up player weapon systems BEFORE setting player to null
        if (player != null && player.areWeaponSystemsInitialized()) {
            System.out.println("Cleaning up player weapon systems...");
            
            // Clean up weapon animator (removes weapon sprites from GUI)
            if (player.getWeaponAnimator() != null) {
                player.getWeaponAnimator().cleanup();
            }
            
            // Clean up weapon effects manager (removes crosshair, etc.)
            if (player.getWeaponEffectsManager() != null) {
                player.getWeaponEffectsManager().cleanup();
            }
            
            // Clean up muzzle flash system
            if (player.getMuzzleFlashSystem() != null) {
                player.getMuzzleFlashSystem().cleanup();
            }
            
            System.out.println("Player weapon systems cleaned up");
        }
    
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
    
        // NEW: Reset timer and score systems
        if (timerSystem != null) {
            timerSystem.reset();
        }
    
        if (scoreSystem != null) {
            scoreSystem.reset();
        }
    
        if (pickupSpawner != null) {
            pickupSpawner.clearSpawnPoints();
        }
        pickupProcessor = null;
    
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
                    player.setPositionOnly(physicsPos);
                }
                player.update(tpf);

                // NEW: Update HUD with timer and score systems
                updateHUD(tpf);
            }

            if (entityManager != null) {
                entityManager.update(tpf);
                updateZombieAI();

                if (pickupProcessor != null) {
                    pickupProcessor.update(tpf);
                }

                processEnemyDeaths();
            }

            // NEW: Update timer system and handle events
            if (timerSystem != null) {
                // If inter-round delay is active, update the timer and HUD
                if (interRoundDelayActive) {
                    interRoundDelayTimer -= tpf;
                    if (hudManager != null) {
                        int secondsLeft = Math.max(0, (int)Math.ceil(interRoundDelayTimer));
                        hudManager.showTemporaryMessage("Next round in: " + secondsLeft + "...", 0.5f, ColorRGBA.Cyan);
                    }
                    if (interRoundDelayTimer <= 0f) {
                        interRoundDelayActive = false;
                        timerSystem.startNextRound();
                        scoreSystem.startNewRound();
                        if (hudManager != null) {
                            hudManager.showRoundStart(timerSystem.getCurrentRound(), timerSystem.getRoundDuration());
                        }
                        spawnZombiesWithDrops();
                    }
                } else {
                    timerSystem.update(tpf);
                    handleRoundEvents();
                    handleContinuousSpawning();
                }
            }
        }
    }

    private void updateHUD(float tpf) {
        // Simple HUD update - just pass all systems
        if (hudManager != null) {
            if (timerSystem != null && scoreSystem != null) {
                // Use updated HUD method with all systems
                updateHUDWithSystems(tpf);
            } else {
                // Fallback to basic HUD
                hudManager.updateHUD(player, tpf);
            }
        }
    }

    private void updateHUDWithSystems(float tpf) {
        // FIXED: Now properly pass timer and score systems to HUD
        if (hudManager != null) {
            hudManager.updateHUD(player, timerSystem, scoreSystem, tpf);
        }
    }

    private void handleRoundEvents() {
        if (timerSystem.isRoundCompleted()) {
            int bonusPoints = 50;
            scoreSystem.awardBonusPoints(bonusPoints, "Round " + timerSystem.getCurrentRound() + " completion");

            // NEW: Show round completion in HUD
            if (hudManager != null) {
                hudManager.showRoundComplete(timerSystem.getCurrentRound(), bonusPoints);
            }

            System.out.println("=== ROUND " + timerSystem.getCurrentRound() + " COMPLETE ===");

            // Start inter-round delay
            interRoundDelayActive = true;
            interRoundDelayTimer = INTER_ROUND_DELAY;
        }

        if (player != null && player.isDead() && timerSystem.getCurrentPhase() != TimerSystem.GamePhase.GAME_OVER) {
            timerSystem.playerDied();

            // NEW: Show game over in HUD
            if (hudManager != null) {
                hudManager.showGameOver(timerSystem.getCurrentRound(),
                        timerSystem.getCurrentTime(),
                        scoreSystem.getTotalKills());
            }

            System.out.println("=== GAME OVER ===");
        }
    }

    private void handleContinuousSpawning() {
        if (timerSystem.shouldSpawnEnemy() && player != null) {
            Vector3f playerPos = player.getPosition();
            zombieSpawner.spawnAdditionalZombies(playerPos, 1);
            System.out.println("Spawned additional zombie - Round time: " + timerSystem.getFormattedCurrentTime());
        }
    }

    private void updateZombieAI() {
        if (player == null) return;

        for (Entity entity : entityManager.getEntitiesByType(Entity.EntityType.ENEMY)) {
            if (entity instanceof ZombieEnemy) {
                ZombieEnemy zombie = (ZombieEnemy) entity;
                zombie.setPlayerPosition(player.getPosition());
                zombie.setPlayer(player);
            } else if (entity instanceof SimpleEnemy) {
                SimpleEnemy enemy = (SimpleEnemy) entity;
                enemy.setPlayerPosition(player.getPosition());
                enemy.setPlayer(player);
            }
        }
    }

    @Override
    public void simpleRender(RenderManager rm) {
        // No custom rendering needed
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

    // Public getters for debugging
    public Player getPlayer() { return player; }
    public EntityManager getEntityManager() { return entityManager; }
    public TimerSystem getTimerSystem() { return timerSystem; }
    public ScoreSystem getScoreSystem() { return scoreSystem; }
    public PickupSpawner getPickupSpawner() { return pickupSpawner; }
    public PickupProcessor getPickupProcessor() { return pickupProcessor; }
    public AudioManager getAudioManager() { return audioManager; }
    public GameStateManager getGameStateManager() { return gameStateManager; }

    /**
     * Get zombie type statistics for debugging
     */
    public String getZombieTypeStatistics() {
        return zombieSpawner != null ? zombieSpawner.getZombieTypeStatistics() : "ZombieSpawner not available";
    }
}