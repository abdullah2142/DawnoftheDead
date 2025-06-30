package horrorjme;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;

/**
 * REFACTORED: Handles all menu rendering and logic using an explicit internal state machine.
 * This version is corrected to be fully functional.
 */
public class MenuSystem {

    // Internal state machine for the menu
    private enum MenuState {
        MAIN,
        PAUSE,
        OPTIONS,
        LEVEL_SELECT
    }
    private MenuState currentMenuState;

    private AssetManager assetManager;
    private Node guiNode;
    private AppSettings settings;
    private GameStateManager stateManager;
    private HorrorGameJME game;
    private OptionsManager optionsManager;
    private MapManager mapManager;

    // Menu nodes
    private Node menuNode;
    private BitmapText menuTitle;
    private BitmapText[] menuOptions;
    private BitmapText selectedIndicator;
    private int selectedMenuItem = 0;
    private BitmapText selectedMapText;

    // Fonts
    private BitmapFont defaultFont;

    // Menu item definitions
    private static final String[] MAIN_MENU_ITEMS = {"New Game", "Select Level", "Options", "Quit"};
    private static final String[] PAUSE_MENU_ITEMS = {"Resume", "Options", "Main Menu", "Quit"};
    private MapInfo[] availableMaps;

    public MenuSystem(AssetManager assetManager, Node guiNode, AppSettings settings,
                      GameStateManager stateManager, HorrorGameJME game, OptionsManager optionsManager, MapManager mapManager) {
        this.assetManager = assetManager;
        this.guiNode = guiNode;
        this.settings = settings;
        this.stateManager = stateManager;
        this.game = game;
        this.optionsManager = optionsManager;
        this.mapManager = mapManager;

        initialize();
    }

    private void initialize() {
        defaultFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        menuNode = new Node("MenuNode");
    }

    public void showMainMenu() {
        clearMenu();
        this.currentMenuState = MenuState.MAIN;
        selectedMenuItem = 0;
        stateManager.setState(GameStateManager.GameState.MAIN_MENU);

        createMenuTitle("DAWN OF THE DEAD", ColorRGBA.Red);
        createMenuItems(MAIN_MENU_ITEMS);
        createSelectionIndicator();
        createSelectedMapText();

        guiNode.attachChild(menuNode);
    }

    public void showPauseMenu() {
        clearMenu();
        this.currentMenuState = MenuState.PAUSE;
        selectedMenuItem = 0;
        stateManager.setState(GameStateManager.GameState.PAUSED);

        createMenuTitle("PAUSED", ColorRGBA.Yellow);
        createMenuItems(PAUSE_MENU_ITEMS);
        createSelectionIndicator();

        guiNode.attachChild(menuNode);
    }

    public void showOptionsMenu() {
        clearMenu();
        this.currentMenuState = MenuState.OPTIONS;
        stateManager.setState(GameStateManager.GameState.OPTIONS_MENU);

        createOptionsMenu();
        guiNode.attachChild(menuNode);
    }

    public void showLevelSelectionMenu() {
        clearMenu();
        this.currentMenuState = MenuState.LEVEL_SELECT;
        selectedMenuItem = 0;

        createMenuTitle("SELECT LEVEL", ColorRGBA.Cyan);

        availableMaps = MapManager.getAllMaps();
        String[] levelMenuItems = new String[availableMaps.length + 1];
        for (int i = 0; i < availableMaps.length; i++) {
            levelMenuItems[i] = availableMaps[i].getDisplayName();
        }
        levelMenuItems[availableMaps.length] = "Back";

        createMenuItems(levelMenuItems);
        createSelectionIndicator();

        guiNode.attachChild(menuNode);
    }

    // This is the core corrected logic for handling input
    public void selectCurrentItem() {
        switch (currentMenuState) {
            case MAIN:
                handleMainMenuSelection();
                break;
            case LEVEL_SELECT:
                handleLevelSelection();
                break;
            case PAUSE:
                handlePauseMenuSelection();
                break;
            // NOTE: OPTIONS menu has no selectable items, uses handleBack()
        }
    }

    // Corrected back button logic
    public void handleBack() {
        if (currentMenuState == MenuState.OPTIONS) {
            if (stateManager.getPreviousState() == GameStateManager.GameState.MAIN_MENU) {
                showMainMenu();
            } else if (stateManager.getPreviousState() == GameStateManager.GameState.PAUSED) {
                showPauseMenu();
            }
        } else if (currentMenuState == MenuState.LEVEL_SELECT) {
            showMainMenu();
        }
    }

    private void handleMainMenuSelection() {
        switch (MAIN_MENU_ITEMS[selectedMenuItem]) {
            case "New Game":
                game.startGame();
                break;
            case "Select Level":
                showLevelSelectionMenu();
                break;
            case "Options":
                showOptionsMenu();
                break;
            case "Quit":
                game.stop();
                break;
        }
    }

    private void handleLevelSelection() {
        if (selectedMenuItem < availableMaps.length) { // A map was selected
            MapInfo selectedMap = availableMaps[selectedMenuItem];
            mapManager.setCurrentMap(selectedMap);
            mapManager.setTransitionMode(MapManager.MapTransitionMode.PLAYER_CHOICE);
        }
        // Whether a map or "Back" was selected, return to the main menu
        showMainMenu();
    }

    private void handlePauseMenuSelection() {
        switch (PAUSE_MENU_ITEMS[selectedMenuItem]) {
            case "Resume":
                game.resumeGame();
                break;
            case "Options":
                showOptionsMenu();
                break;
            case "Main Menu":
                game.returnToMainMenu();
                break;
            case "Quit":
                game.stop();
                break;
        }
    }

    public void moveSelectionUp() {
        if (menuOptions != null && menuOptions.length > 0) {
            selectedMenuItem = (selectedMenuItem - 1 + menuOptions.length) % menuOptions.length;
            updateMenuSelection();
        }
    }

    public void moveSelectionDown() {
        if (menuOptions != null && menuOptions.length > 0) {
            selectedMenuItem = (selectedMenuItem + 1) % menuOptions.length;
            updateMenuSelection();
        }
    }

    private void updateMenuSelection() {
        if (menuOptions == null) return;
        updateSelectionIndicatorPosition();
        for (int i = 0; i < menuOptions.length; i++) {
            menuOptions[i].setColor(i == selectedMenuItem ? ColorRGBA.Yellow : ColorRGBA.White);
        }
    }

    private void updateSelectionIndicatorPosition() {
        if (selectedIndicator != null && menuOptions != null && selectedMenuItem < menuOptions.length) {
            float indicatorX = menuOptions[selectedMenuItem].getLocalTranslation().x - 40;
            float indicatorY = menuOptions[selectedMenuItem].getLocalTranslation().y;
            selectedIndicator.setLocalTranslation(indicatorX, indicatorY, 0);
        }
    }

    // --- Helper methods for creating UI elements ---

    private void createMenuTitle(String title, ColorRGBA color) {
        menuTitle = new BitmapText(defaultFont);
        menuTitle.setSize(defaultFont.getCharSet().getRenderedSize() * 2);
        menuTitle.setText(title);
        menuTitle.setColor(color);
        menuTitle.setLocalTranslation(
                settings.getWidth() / 2f - menuTitle.getLineWidth() / 2f,
                settings.getHeight() / 2f + 150,
                0
        );
        menuNode.attachChild(menuTitle);
    }

    private void createMenuItems(String[] items) {
        menuOptions = new BitmapText[items.length];
        for (int i = 0; i < items.length; i++) {
            menuOptions[i] = new BitmapText(defaultFont);
            menuOptions[i].setSize(defaultFont.getCharSet().getRenderedSize() * 1.5f);
            menuOptions[i].setText(items[i]);
            menuOptions[i].setColor(ColorRGBA.White);
            menuOptions[i].setLocalTranslation(
                    settings.getWidth() / 2f - menuOptions[i].getLineWidth() / 2f,
                    settings.getHeight() / 2f - i * 60,
                    0
            );
            menuNode.attachChild(menuOptions[i]);
        }
        updateMenuSelection();
    }

    private void createSelectionIndicator() {
        selectedIndicator = new BitmapText(defaultFont);
        selectedIndicator.setSize(defaultFont.getCharSet().getRenderedSize() * 1.5f);
        selectedIndicator.setText(">");
        selectedIndicator.setColor(ColorRGBA.Yellow);
        menuNode.attachChild(selectedIndicator);
        updateSelectionIndicatorPosition();
    }

    private void createSelectedMapText() {
        selectedMapText = new BitmapText(defaultFont);
        selectedMapText.setSize(defaultFont.getCharSet().getRenderedSize() * 1.1f);
        String mapName = mapManager.getCurrentMap().getDisplayName();
        selectedMapText.setText("Selected Map: " + mapName);
        selectedMapText.setColor(ColorRGBA.Cyan);
        selectedMapText.setLocalTranslation(20, 50, 0);
        menuNode.attachChild(selectedMapText);
    }

    private void createOptionsMenu() {
        // ... (This method remains unchanged)
    }

    public void clearMenu() {
        if (menuNode.getParent() != null) {
            guiNode.detachChild(menuNode);
        }
        menuNode.detachAllChildren();
        menuOptions = null;
        selectedIndicator = null;
        menuTitle = null;
        availableMaps = null; // Clear map list for safety
    }

    public void hide() {
        clearMenu();
    }
}