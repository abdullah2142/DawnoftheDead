package horrorjme;

import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.FogFilter;
import com.jme3.renderer.Limits;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.system.AppSettings;

/**
 * Refactored Horror Game using clean architecture
 */
public class HorrorGameJME extends SimpleApplication {

    // Core managers
    private GameStateManager stateManager;
    private InputHandler inputHandler;
    private MenuSystem menuSystem;
    private OptionsManager optionsManager;
    private AudioManager audioManager;
    private EntityManager entityManager;
    private Player player;
    private HUDManager hudManager;

    // Game world
    private Node mapNode;

    // Map data - 1 represents walls, 0 is empty space
    private final int[][] mapData = {
            // Row 0 (top)
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},

            // Row 1
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},

            // Row 2
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},

            // Row 3
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},

            // Row 4 - Upper room with pillars
            {1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1},

            // Row 5
            {1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1},

            // Row 6
            {1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1},

            // Row 7 - Room with pillars
            {1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1},

            // Row 8
            {1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1},

            // Row 9 - Door opening (right side)
            {1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},

            // Row 10 - Horizontal corridor
            {1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1},

            // Row 11 - Open area
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},

            // Row 12
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},

            // Row 13 - Lower corridor start
            {1, 0, 0, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},

            // Row 14
            {1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1},

            // Row 15
            {1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1},

            // Row 16
            {1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1},

            // Row 17 - Door on left
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1},

            // Row 18 - Starting area (P = player start position)
            {1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1},

            // Row 19 (bottom)
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}
    };

    public static void main(String[] args) {
        HorrorGameJME app = new HorrorGameJME();

        // Configure display settings
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Dawn of The Dead - JMonkeyEngine");
        settings.setResolution(1280, 720);
        app.setSettings(settings);

        app.start();
    }

    // In your HorrorGameJME.java file, update the simpleInitApp() method:

    @Override
    public void simpleInitApp() {
        // Initialize core managers
        stateManager = new GameStateManager();
        optionsManager = new OptionsManager();
        audioManager = new AudioManager(assetManager, rootNode);
        entityManager = new EntityManager(rootNode);

        // ADD THIS LINE - Initialize HUD Manager
        hudManager = new HUDManager(assetManager, guiNode, settings);

        // Initialize input handler
        inputHandler = new InputHandler(inputManager, stateManager, this);

        // Initialize menu system
        menuSystem = new MenuSystem(assetManager, guiNode, settings, stateManager, this, optionsManager);
        inputHandler.setMenuSystem(menuSystem);

        // Setup camera for better perspective
        cam.setFrustumPerspective(45f, (float)cam.getWidth() / cam.getHeight(), 0.1f, 1000f);
        flyCam.setMoveSpeed(0);
        flyCam.setDragToRotate(false);

        // Load audio with improved error handling
        try {
            audioManager.initializeHorrorSounds();
        } catch (Exception e) {
            System.err.println("Failed to initialize audio, using minimal setup: " + e.getMessage());
            audioManager.initializeMinimalAudio();
        }

        // Show main menu
        menuSystem.showMainMenu();
        inputHandler.disableMouseLook();

        System.out.println("Game initialized successfully!");
    }

    /**
     * Start a new game
     */
// In HorrorGameJME.java, update the startGame() method:
    public void startGame() {
        stateManager.setState(GameStateManager.GameState.PLAYING);

        // Hide menu
        menuSystem.hide();

        // Initialize game world
        initializeGameWorld();

        // Create player - NOW PASS AudioManager
        player = new Player(cam, rootNode, mapData, audioManager);
        inputHandler.setPlayer(player);

        // Apply options
        optionsManager.applySettings(player, audioManager);

        // Enable mouse look
        inputHandler.enableMouseLook();

        //Show HUD when game starts
        hudManager.show();

        // Start background music
        if (audioManager.hasBackgroundMusic("horror_ambient")) {
            audioManager.playBackgroundMusic("horror_ambient");
        }

        System.out.println("Game started!");
    }
    /**
     * Pause the game
     */
    public void pauseGame() {
        if (stateManager.getCurrentState() == GameStateManager.GameState.PLAYING) {
            inputHandler.disableMouseLook();
            audioManager.pauseBackgroundMusic();
            menuSystem.showPauseMenu();
        }
    }

    /**
     * Resume the game
     */
    public void resumeGame() {
        if (stateManager.getCurrentState() == GameStateManager.GameState.PAUSED) {
            stateManager.setState(GameStateManager.GameState.PLAYING);
            menuSystem.hide();
            inputHandler.enableMouseLook();
            audioManager.resumeBackgroundMusic();
        }
    }

    /**
     * Return to main menu
     */
    public void returnToMainMenu() {
        // Cleanup current game
        cleanupGame();

        // Show main menu
        menuSystem.showMainMenu();
        inputHandler.disableMouseLook();

        // Stop game music, could start menu music here
        audioManager.stopBackgroundMusic();
        //Hide HUD when returning to menu
        hudManager.hide();

    }

    /**
     * Initialize the game world
     */
    private void initializeGameWorld() {
        createMap();
        setupLighting();
        setupFog();

        // Enable anisotropic filtering if available
        if (renderer.getLimits().get(Limits.TextureAnisotropy) > 1) {
            renderer.setDefaultAnisotropicFilter(4);
        }
    }

    /**
     * Create the game map
     */
    private void createMap() {
        mapNode = new Node("MapNode");
        rootNode.attachChild(mapNode);

        // Create floor
        Box floorBox = new Box(5f, 0.05f, 5f);
        Geometry floor = new Geometry("Floor", floorBox);
        Material floorMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        floorMat.setColor("Diffuse", ColorRGBA.DarkGray);
        floorMat.setColor("Ambient", ColorRGBA.DarkGray);
        floorMat.setBoolean("UseMaterialColors", true);
        floor.setMaterial(floorMat);
        floor.setLocalTranslation(5f, -0.05f, 5f);
        mapNode.attachChild(floor);

        // Create ceiling
        Box ceilingBox = new Box(5f, 0.05f, 5f);
        Geometry ceiling = new Geometry("Ceiling", ceilingBox);
        Material ceilingMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        ceilingMat.setColor("Diffuse", ColorRGBA.DarkGray.mult(0.5f));
        ceilingMat.setColor("Ambient", ColorRGBA.DarkGray.mult(0.5f));
        ceilingMat.setBoolean("UseMaterialColors", true);
        ceiling.setMaterial(ceilingMat);
        ceiling.setLocalTranslation(5f, 1.05f, 5f);
        mapNode.attachChild(ceiling);

        // Create walls
        for (int x = 0; x < mapData[0].length; x++) {
            for (int z = 0; z < mapData.length; z++) {
                if (mapData[z][x] == 1) {
                    createWall(x, z);
                }
            }
        }
    }

    /**
     * Create a single wall segment
     */
    private void createWall(int x, int z) {
        Box wallBox = new Box(0.5f, 0.55f, 0.5f);
        Geometry wall = new Geometry("Wall_" + x + "_" + z, wallBox);
        Material wallMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");

        ColorRGBA randomColor = new ColorRGBA(
                FastMath.nextRandomFloat(),
                FastMath.nextRandomFloat(),
                FastMath.nextRandomFloat(),
                1f
        );
        wallMat.setColor("Diffuse", randomColor);
        wallMat.setColor("Ambient", randomColor.mult(0.3f));
        wallMat.setColor("Specular", ColorRGBA.White);
        wallMat.setFloat("Shininess", 256f);
        wallMat.setBoolean("UseMaterialColors", true);

        wall.setMaterial(wallMat);
        wall.setLocalTranslation(x + 0.5f, 0.5f, z + 0.5f);
        wall.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        mapNode.attachChild(wall);
    }

    /**
     * Setup lighting for the scene
     */
    private void setupLighting() {
        // Add ambient light
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.5f, -0.5f, -0.5f).normalizeLocal());
        sun.setColor(ColorRGBA.White.mult(0.3f));
        rootNode.addLight(sun);

        // Add directional light shadow
        DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, 1024, 4);
        dlsr.setLight(sun);
        dlsr.setShadowIntensity(0.4f);
        dlsr.setEdgeFilteringMode(EdgeFilteringMode.PCF4);
        dlsr.setLambda(0.65f);
        viewPort.addProcessor(dlsr);

        // Add fill light
        DirectionalLight fillLight = new DirectionalLight();
        fillLight.setDirection(new Vector3f(0.5f, 0.3f, 0.5f).normalizeLocal());
        fillLight.setColor(ColorRGBA.White.mult(0.15f));
        rootNode.addLight(fillLight);
    }

    /**
     * Setup fog effect
     */
    private void setupFog() {
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        FogFilter fog = new FogFilter();
        fog.setFogColor(new ColorRGBA(0, 0, 0, 1.0f));
        fog.setFogDistance(7);
        fog.setFogDensity(2.5f);
        fpp.addFilter(fog);
        viewPort.addProcessor(fpp);
    }

    /**
     * Cleanup game resources
     */
    private void cleanupGame() {
        // Remove player
        player = null;
        inputHandler.setPlayer(null);

        // Clear entities
        if (entityManager != null) {
            entityManager.clear();
        }

        // Remove map
        if (mapNode != null) {
            rootNode.detachChild(mapNode);
            mapNode = null;
        }

        System.out.println("Game cleaned up");
    }

    @Override
    public void simpleUpdate(float tpf) {
        // Only update game logic when playing
        if (stateManager.getCurrentState() == GameStateManager.GameState.PLAYING) {
            if (player != null) {
                player.update(tpf);
                // Update HUD
                hudManager.updateHUD(player);
            }

            if (entityManager != null) {
                entityManager.update(tpf);
            }
        }
    }

    @Override
    public void simpleRender(RenderManager rm) {
        // Additional rendering if needed
    }

    @Override
    public void destroy() {
        // Cleanup resources
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