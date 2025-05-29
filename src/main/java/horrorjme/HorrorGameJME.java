package horrorjme;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
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
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;



/**
 * Horror Game with smooth movement and DOOM map loading
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

    // SMOOTH MOVEMENT SETTINGS
    private static final float MAP_SCALE = 0.055f;  // Smaller scale for DOOM map
    private static final Vector3f PLAYER_START_POS = new Vector3f(10f, 150f, 20f);

    private static final float MOUSE_SENSITIVITY = 0.5f; // Lower mouse sensitivity

    public static void main(String[] args) {
        HorrorGameJME app = new HorrorGameJME();

        AppSettings settings = new AppSettings(true);
        settings.setTitle("Dawn of The Dead - DOOM Map + Smooth Movement");
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
        System.out.println("Setting up lighting for DOOM map...");

        // Clear existing lights
        rootNode.getLocalLightList().clear();

        // Ambient light for DOOM map visibility
        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White.mult(0.4f)); // Dimmer for horror atmosphere
        rootNode.addLight(ambient);

        // Main directional light
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.5f, -0.5f, -0.5f).normalizeLocal());
        sun.setColor(ColorRGBA.White.mult(0.8f)); // Dimmer for atmosphere
        rootNode.addLight(sun);

        // Fill light
        DirectionalLight fill = new DirectionalLight();
        fill.setDirection(new Vector3f(0.5f, -0.3f, 0.5f).normalizeLocal());
        fill.setColor(ColorRGBA.White.mult(0.3f));
        rootNode.addLight(fill);

        System.out.println("Atmospheric lighting setup complete");
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

        // Configure smooth movement settings


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
        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(0.3f, 1.4f, 1);

        // Create CharacterControl with smooth settings
        playerControl = new CharacterControl(capsuleShape, 0.01f); // Lower step height

        // Configure smooth physics
        playerControl.setJumpSpeed(15);      // Lower jump
        playerControl.setFallSpeed(25);      // Slower fall
        playerControl.setGravity(65);        // Lower gravity
        //playerControl.setPhysicsDamping(0.9f); // Add damping for smoother movement

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
        System.out.println("Setting up player systems...");

        // Create regular player for torch and other features
        player = new Player(cam, rootNode, null, audioManager);


        inputHandler.setPlayer(player);
        inputHandler.setPlayerControl(playerControl);
        debugNoclip.setPlayer(player);

        // Apply options
        optionsManager.applySettings(player, audioManager);

        System.out.println("Player systems setup complete");
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

        System.out.println("Game start complete - DOOM map should be visible!");
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
        debugNoclip.update(tpf);

        if (gameStateManager.getCurrentState() == GameStateManager.GameState.PLAYING) {
            // Update input handler
            if (inputHandler != null) {
                inputHandler.update(tpf);
            }

            // Update player smoothly
            if (player != null && !debugNoclip.isEnabled()) {
                if (playerControl != null) {
                    Vector3f physicsPos = playerControl.getPhysicsLocation();
                    player.syncPositionWithCamera(physicsPos);
                }
                player.update(tpf);
                hudManager.updateHUD(player);
            }

            // Update entities
            if (entityManager != null) {
                entityManager.update(tpf);
            }
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