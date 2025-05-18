package horrorjme;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.math.Quaternion;
import com.jme3.renderer.RenderManager;
import com.jme3.system.AppSettings;
import com.jme3.math.FastMath;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.FogFilter;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.material.RenderState;
import com.jme3.renderer.Limits;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.scene.shape.Quad;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.collision.CollisionResults;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.ui.Picture;
import com.jme3.asset.AssetConfig;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioData.DataType;



/**
 * A horror game implemented with JMonkeyEngine
 */
public class HorrorGameJME extends SimpleApplication implements ActionListener, AnalogListener {
    // Game constants
    private static final float MOVE_SPEED = 3f;
    private static final float ROTATION_SPEED = 2f;

    // Map data - 1 represents walls, 0 is empty space
    private final int[][] mapData = {
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 1},
            {1, 0, 0, 1, 1, 1, 1, 1, 0, 1},
            {1, 0, 1, 0, 0, 0, 0, 1, 0, 1},
            {1, 0, 1, 0, 1, 1, 0, 1, 0, 1},
            {1, 0, 1, 0, 0, 0, 0, 1, 0, 1},
            {1, 0, 1, 1, 1, 1, 1, 1, 0, 1},
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 1},
            {1, 1, 1, 1, 1, 1, 1, 1, 0, 1},
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}
    };

    // Game state
    private boolean torchOn = true;
    private Node mapNode;
    private PointLight torch;

    // Movement flags
    private boolean moveForward = false;
    private boolean moveBackward = false;
    private boolean strafeLeft = false;
    private boolean strafeRight = false;
    private boolean turnLeft = false;
    private boolean turnRight = false;

    // Menu system
    private BitmapText menuTitle;
    private BitmapText[] menuOptions;
    private Node menuNode;
    private boolean inMainMenu = true;
    private boolean inOptionsMenu = false;
    private boolean gameStarted = false;
    private int selectedMenuItem = 0;
    private BitmapText selectedIndicator;

    // Options variables
    private float masterVolume = 1.0f;
    private boolean invertMouseY = false;
    private float mouseSensitivity = 1.0f;
    private boolean colorBlindMode = false;
    private float textSize = 1.0f;

    public static void main(String[] args) {
        HorrorGameJME app = new HorrorGameJME();

        // Configure display settings
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Dawn of The Dead - JMonkeyEngine");
        settings.setResolution(1280, 720);
        app.setSettings(settings);

        app.start();
    }

    private AudioNode gameMusic;
    private void initAudio() {
        // Create gameplay music (looped)
        gameMusic = new AudioNode(assetManager, "Sounds/horror_music.ogg", DataType.Stream);
        gameMusic.setLooping(true);
        gameMusic.setVolume(1.0f);
        gameMusic.setPositional(false); // Non-positional for background music
        rootNode.attachChild(gameMusic);
    }

    @Override
    public void simpleInitApp() {
        // Create menu first
        createMainMenu();

        // Don't immediately start the game - wait for menu selection
        inputManager.setCursorVisible(true);

        // Setup menu input
        setupMenuInput();
    }

    private void createMainMenu() {
        menuNode = new Node("MenuNode");
        guiNode.attachChild(menuNode);

        // Create title text
        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");
        menuTitle = new BitmapText(font);
        menuTitle.setSize(font.getCharSet().getRenderedSize() * 2);
        menuTitle.setText("DAWN OF THE DEAD");
        menuTitle.setColor(ColorRGBA.Red);
        menuTitle.setLocalTranslation(
                settings.getWidth() / 2 - menuTitle.getLineWidth() / 2,
                settings.getHeight() / 2 + 100,
                0
        );
        menuNode.attachChild(menuTitle);

        // Create menu options
        String[] menuItems = {"New Game", "Options", "Quit"};
        menuOptions = new BitmapText[menuItems.length];

        for (int i = 0; i < menuItems.length; i++) {
            menuOptions[i] = new BitmapText(font);
            menuOptions[i].setSize(font.getCharSet().getRenderedSize() * 1.5f);
            menuOptions[i].setText(menuItems[i]);
            menuOptions[i].setColor(ColorRGBA.White);
            menuOptions[i].setLocalTranslation(
                    settings.getWidth() / 2 - menuOptions[i].getLineWidth() / 2,
                    settings.getHeight() / 2 - i * 60,
                    0
            );
            menuNode.attachChild(menuOptions[i]);
        }

        // Create selection indicator
        selectedIndicator = new BitmapText(font);
        selectedIndicator.setSize(font.getCharSet().getRenderedSize() * 1.5f);
        selectedIndicator.setText(">");
        selectedIndicator.setColor(ColorRGBA.Yellow);
        updateMenuSelection();
        menuNode.attachChild(selectedIndicator);
    }

    private void setupMenuInput() {
        inputManager.addMapping("MenuUp", new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("MenuDown", new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping("MenuSelect", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addMapping("MenuBack", new KeyTrigger(KeyInput.KEY_ESCAPE));

        ActionListener menuListener = new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (!isPressed) return;

                if (inMainMenu) {
                    switch (name) {
                        case "MenuUp":
                            selectedMenuItem = (selectedMenuItem - 1 + menuOptions.length) % menuOptions.length;
                            updateMenuSelection();
                            break;
                        case "MenuDown":
                            selectedMenuItem = (selectedMenuItem + 1) % menuOptions.length;
                            updateMenuSelection();
                            break;
                        case "MenuSelect":
                            handleMenuSelect();
                            break;
                    }
                } else if (inOptionsMenu) {
                    if (name.equals("MenuBack")) {
                        showMainMenu();
                    }
                }
            }
        };

        inputManager.addListener(menuListener, "MenuUp", "MenuDown", "MenuSelect", "MenuBack");
    }

    private void updateMenuSelection() {
        selectedIndicator.setLocalTranslation(
                settings.getWidth() / 2 - 150,
                settings.getHeight() / 2 - selectedMenuItem * 60,
                0
        );

        // Highlight selected option
        for (int i = 0; i < menuOptions.length; i++) {
            if (i == selectedMenuItem) {
                menuOptions[i].setColor(ColorRGBA.Yellow);
            } else {
                menuOptions[i].setColor(ColorRGBA.White);
            }
        }
    }

    private void handleMenuSelect() {
        switch (selectedMenuItem) {
            case 0: // New Game
                startGame();
                break;
            case 1: // Options
                showOptionsMenu();
                break;
            case 2: // Quit
                stop();
                break;
        }
    }

    private void showOptionsMenu() {
        // Hide main menu
        menuNode.detachAllChildren();
        inMainMenu = false;
        inOptionsMenu = true;

        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");

        // Options title
        BitmapText optionsTitle = new BitmapText(font);
        optionsTitle.setSize(font.getCharSet().getRenderedSize() * 1.5f);
        optionsTitle.setText("OPTIONS");
        optionsTitle.setColor(ColorRGBA.Yellow);
        optionsTitle.setLocalTranslation(
                settings.getWidth() / 2 - optionsTitle.getLineWidth() / 2,
                settings.getHeight() / 2 + 100,
                0
        );
        menuNode.attachChild(optionsTitle);

        // Options info
        String[] optionsText = {
                "Volume: " + (int)(masterVolume * 100) + "%",
                "Mouse Sensitivity: " + (int)(mouseSensitivity * 100) + "%",
                "Invert Mouse Y: " + (invertMouseY ? "ON" : "OFF"),
                "Color Blind Mode: " + (colorBlindMode ? "ON" : "OFF"),
                "",
                "Press ESC to return to main menu"
        };

        for (int i = 0; i < optionsText.length; i++) {
            BitmapText optionText = new BitmapText(font);
            optionText.setSize(font.getCharSet().getRenderedSize() * 1.2f);
            optionText.setText(optionsText[i]);
            optionText.setColor(ColorRGBA.White);
            optionText.setLocalTranslation(
                    settings.getWidth() / 2 - optionText.getLineWidth() / 2,
                    settings.getHeight() / 2 + 50 - i * 40,
                    0
            );
            menuNode.attachChild(optionText);
        }
    }

    private void showMainMenu() {
        menuNode.detachAllChildren();
        inMainMenu = true;
        inOptionsMenu = false;
        selectedMenuItem = 0;
        createMainMenu();
    }

    private void startGame() {
        // Remove menu UI
        guiNode.detachChild(menuNode);
        inMainMenu = false;
        gameStarted = true;

        // Hide cursor
        inputManager.setCursorVisible(false);

        // Initialize game
        initializeGame();

        initAudio();
        gameMusic.play();
    }

    private void initializeGame() {
        // Set up camera
        cam.setLocation(new Vector3f(2.5f, 0.5f, 2.5f));
        cam.lookAt(new Vector3f(3.5f, 0.5f, 2.5f), Vector3f.UNIT_Y);
        cam.setFrustumPerspective(45f, (float)cam.getWidth() / cam.getHeight(), 0.1f, 1000f);

        flyCam.setMoveSpeed(0);
        flyCam.setDragToRotate(false);

        // Initialize input mappings
        initInputs();

        // Create the map
        createMap();

        // Set up lighting
        setupLighting();

        // Add fog effect
        setupFog();

        if (renderer.getLimits().get(Limits.TextureAnisotropy) > 1) {
            renderer.setDefaultAnisotropicFilter(4);
        }
    }

    private void initInputs() {
        // Register action mappings
        inputManager.addMapping("MoveForward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("MoveBackward", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("StrafeLeft", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("StrafeRight", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("TurnLeft", new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("TurnRight", new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("ToggleTorch", new KeyTrigger(KeyInput.KEY_F));
        inputManager.addMapping("Escape", new KeyTrigger(KeyInput.KEY_ESCAPE));

        // Add listeners
        inputManager.addListener(this, "MoveForward", "MoveBackward", "StrafeLeft",
                "StrafeRight", "TurnLeft", "TurnRight", "ToggleTorch", "Escape");
    }

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
                    Box wallBox = new Box(0.5f, 0.55f, 0.5f);
                    Geometry wall = new Geometry("Wall_" + x + "_" + z, wallBox);
                    Material wallMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");

                    ColorRGBA randomColor = new ColorRGBA(FastMath.nextRandomFloat(),
                            FastMath.nextRandomFloat(),
                            FastMath.nextRandomFloat(),
                            1f);
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
            }
        }
    }

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

        // Add player torch
        torch = new PointLight();
        torch.setPosition(cam.getLocation());
        torch.setRadius(9f);
        torch.setColor(ColorRGBA.White.mult(3.5f));
        rootNode.addLight(torch);

        // Add fill light
        DirectionalLight fillLight = new DirectionalLight();
        fillLight.setDirection(new Vector3f(0.5f, 0.3f, 0.5f).normalizeLocal());
        fillLight.setColor(ColorRGBA.White.mult(0.15f));
        rootNode.addLight(fillLight);
    }

    private void setupFog() {
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        FogFilter fog = new FogFilter();
        fog.setFogColor(new ColorRGBA(0, 0, 0, 1.0f));
        fog.setFogDistance(7);
        fog.setFogDensity(2.5f);
        fpp.addFilter(fog);
        viewPort.addProcessor(fpp);
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (!gameStarted) {
            return; // Don't update game logic if we're in the menu
        }

        // Update torch position to match camera
        if (torch != null) {
            torch.setPosition(cam.getLocation());
        }

        // Handle movement
        Vector3f camDir = cam.getDirection().clone().normalizeLocal();
        Vector3f camLeft = cam.getLeft().clone().normalizeLocal();
        camDir.y = 0;
        camLeft.y = 0;

        Vector3f walkDirection = new Vector3f(0, 0, 0);

        if (moveForward) {
            walkDirection.addLocal(camDir.mult(tpf * MOVE_SPEED));
        }
        if (moveBackward) {
            walkDirection.addLocal(camDir.mult(-tpf * MOVE_SPEED));
        }
        if (strafeLeft) {
            walkDirection.addLocal(camLeft.mult(tpf * MOVE_SPEED));
        }
        if (strafeRight) {
            walkDirection.addLocal(camLeft.mult(-tpf * MOVE_SPEED));
        }

        // Apply rotation
        if (turnLeft) {
            rotateCamera(-ROTATION_SPEED * tpf * mouseSensitivity);
        }
        if (turnRight) {
            rotateCamera(ROTATION_SPEED * tpf * mouseSensitivity);
        }

        // Move player if not colliding with walls
        Vector3f newPosition = cam.getLocation().add(walkDirection);
        if (!isColliding(newPosition)) {
            cam.setLocation(newPosition);
        } else {
            // Try to slide along walls
            Vector3f newPositionX = cam.getLocation().clone();
            newPositionX.x += walkDirection.x;
            if (!isColliding(newPositionX)) {
                cam.setLocation(newPositionX);
            }

            Vector3f newPositionZ = cam.getLocation().clone();
            newPositionZ.z += walkDirection.z;
            if (!isColliding(newPositionZ)) {
                cam.setLocation(newPositionZ);
            }
        }
    }

    private void rotateCamera(float angle) {
        Quaternion rot = new Quaternion();
        rot.fromAngleAxis(angle, Vector3f.UNIT_Y);
        cam.setRotation(rot.mult(cam.getRotation()));
    }

    private boolean isColliding(Vector3f position) {
        int mapX = (int)position.x;
        int mapZ = (int)position.z;
        float buffer = 0.3f;

        if (mapX < 0 || mapX >= mapData[0].length || mapZ < 0 || mapZ >= mapData.length) {
            return true;
        }
        if (mapData[mapZ][mapX] == 1) {
            return true;
        }

        if (position.x - mapX < buffer) {
            if (mapX > 0 && mapData[mapZ][mapX - 1] == 1) {
                return true;
            }
        }
        if (mapX + 1 - position.x < buffer) {
            if (mapX < mapData[0].length - 1 && mapData[mapZ][mapX + 1] == 1) {
                return true;
            }
        }
        if (position.z - mapZ < buffer) {
            if (mapZ > 0 && mapData[mapZ - 1][mapX] == 1) {
                return true;
            }
        }
        if (mapZ + 1 - position.z < buffer) {
            if (mapZ < mapData.length - 1 && mapData[mapZ + 1][mapX] == 1) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!gameStarted) {
            return; // Ignore game input during menu
        }

        if (name.equals("Escape") && !isPressed) {
            // Show pause menu or return to main menu
            showMainMenu();
            gameStarted = false;
            inputManager.setCursorVisible(true);
            return;
        }

        if (name.equals("MoveForward")) {
            moveForward = isPressed;
        } else if (name.equals("MoveBackward")) {
            moveBackward = isPressed;
        } else if (name.equals("StrafeLeft")) {
            strafeLeft = isPressed;
        } else if (name.equals("StrafeRight")) {
            strafeRight = isPressed;
        } else if (name.equals("TurnLeft")) {
            turnLeft = isPressed;
        } else if (name.equals("TurnRight")) {
            turnRight = isPressed;
        } else if (name.equals("ToggleTorch") && !isPressed) {
            torchOn = !torchOn;
            if (torchOn) {
                rootNode.addLight(torch);
            } else {
                rootNode.removeLight(torch);
            }
        }
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        // Not needed for this implementation
    }

    @Override
    public void simpleRender(RenderManager rm) {
        // Additional rendering if needed
    }
}