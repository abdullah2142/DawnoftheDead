package horrorjme;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;

/**
 * Handles all menu rendering and logic
 */
public class MenuSystem {

    private AssetManager assetManager;
    private Node guiNode;
    private AppSettings settings;
    private GameStateManager stateManager;
    private HorrorGameJME game;
    private OptionsManager optionsManager;

    // Menu nodes
    private Node menuNode;
    private BitmapText menuTitle;
    private BitmapText[] menuOptions;
    private BitmapText selectedIndicator;
    private int selectedMenuItem = 0;

    // Fonts
    private BitmapFont defaultFont;

    // Menu items
    private static final String[] MAIN_MENU_ITEMS = {"New Game", "Options", "Quit"};
    private static final String[] PAUSE_MENU_ITEMS = {"Resume", "Options", "Main Menu", "Quit"};

    public MenuSystem(AssetManager assetManager, Node guiNode, AppSettings settings,
                      GameStateManager stateManager, HorrorGameJME game, OptionsManager optionsManager) {
        this.assetManager = assetManager;
        this.guiNode = guiNode;
        this.settings = settings;
        this.stateManager = stateManager;
        this.game = game;
        this.optionsManager = optionsManager;

        initialize();
    }

    private void initialize() {
        defaultFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        menuNode = new Node("MenuNode");
    }

    public void showMainMenu() {
        clearMenu();
        selectedMenuItem = 0;
        stateManager.setState(GameStateManager.GameState.MAIN_MENU);

        createMenuTitle("DAWN OF THE DEAD", ColorRGBA.Red);
        createMenuItems(MAIN_MENU_ITEMS);
        createSelectionIndicator();

        guiNode.attachChild(menuNode);
    }

    public void showPauseMenu() {
        clearMenu();
        selectedMenuItem = 0;
        stateManager.setState(GameStateManager.GameState.PAUSED);

        createMenuTitle("PAUSED", ColorRGBA.Yellow);
        createMenuItems(PAUSE_MENU_ITEMS);
        createSelectionIndicator();

        guiNode.attachChild(menuNode);
    }

    public void showOptionsMenu() {
        clearMenu();
        stateManager.setState(GameStateManager.GameState.OPTIONS_MENU);

        createOptionsMenu();
        guiNode.attachChild(menuNode);
    }

    private void createMenuTitle(String title, ColorRGBA color) {
        menuTitle = new BitmapText(defaultFont);
        menuTitle.setSize(defaultFont.getCharSet().getRenderedSize() * 2);
        menuTitle.setText(title);
        menuTitle.setColor(color);
        menuTitle.setLocalTranslation(
                settings.getWidth() / 2 - menuTitle.getLineWidth() / 2,
                settings.getHeight() / 2 + 100,
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
                    settings.getWidth() / 2 - menuOptions[i].getLineWidth() / 2,
                    settings.getHeight() / 2 - i * 60,
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

    private void createOptionsMenu() {
        // Options title
        BitmapText optionsTitle = new BitmapText(defaultFont);
        optionsTitle.setSize(defaultFont.getCharSet().getRenderedSize() * 1.5f);
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
                "Volume: " + (int)(optionsManager.getMasterVolume() * 100) + "%",
                "Mouse Sensitivity: " + (int)(optionsManager.getMouseSensitivity() * 100) + "%",
                "Invert Mouse Y: " + (optionsManager.isInvertMouseY() ? "ON" : "OFF"),
                "Color Blind Mode: " + (optionsManager.isColorBlindMode() ? "ON" : "OFF"),
                "",
                "Press ESC to return"
        };

        for (int i = 0; i < optionsText.length; i++) {
            BitmapText optionText = new BitmapText(defaultFont);
            optionText.setSize(defaultFont.getCharSet().getRenderedSize() * 1.2f);
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

    public void moveSelectionUp() {
        if (menuOptions != null) {
            selectedMenuItem = (selectedMenuItem - 1 + menuOptions.length) % menuOptions.length;
            updateMenuSelection();
        }
    }

    public void moveSelectionDown() {
        if (menuOptions != null) {
            selectedMenuItem = (selectedMenuItem + 1) % menuOptions.length;
            updateMenuSelection();
        }
    }

    public void selectCurrentItem() {
        if (stateManager.getCurrentState() == GameStateManager.GameState.MAIN_MENU) {
            handleMainMenuSelection();
        } else if (stateManager.getCurrentState() == GameStateManager.GameState.PAUSED) {
            handlePauseMenuSelection();
        }
    }

    private void handleMainMenuSelection() {
        switch (selectedMenuItem) {
            case 0: // New Game
                game.startGame();
                break;
            case 1: // Options
                showOptionsMenu();
                break;
            case 2: // Quit
                game.stop();
                break;
        }
    }

    private void handlePauseMenuSelection() {
        switch (selectedMenuItem) {
            case 0: // Resume
                game.resumeGame();
                break;
            case 1: // Options
                showOptionsMenu();
                break;
            case 2: // Main Menu
                game.returnToMainMenu();
                break;
            case 3: // Quit
                game.stop();
                break;
        }
    }

    public void handleBack() {
        if (stateManager.getCurrentState() == GameStateManager.GameState.OPTIONS_MENU) {
            if (stateManager.getPreviousState() == GameStateManager.GameState.MAIN_MENU) {
                showMainMenu();
            } else if (stateManager.getPreviousState() == GameStateManager.GameState.PAUSED) {
                showPauseMenu();
            }
        }
    }

    private void updateMenuSelection() {
        updateSelectionIndicatorPosition();

        // Highlight selected option
        for (int i = 0; i < menuOptions.length; i++) {
            if (i == selectedMenuItem) {
                menuOptions[i].setColor(ColorRGBA.Yellow);
            } else {
                menuOptions[i].setColor(ColorRGBA.White);
            }
        }
    }

    private void updateSelectionIndicatorPosition() {
        if (selectedIndicator != null && menuOptions != null && selectedMenuItem < menuOptions.length) {
            selectedIndicator.setLocalTranslation(
                    settings.getWidth() / 2 - 150,
                    settings.getHeight() / 2 - selectedMenuItem * 60,
                    0
            );
        }
    }

    public void clearMenu() {
        if (menuNode.getParent() != null) {
            guiNode.detachChild(menuNode);
        }
        menuNode.detachAllChildren();
        menuOptions = null;
        selectedIndicator = null;
        menuTitle = null;
    }

    public void hide() {
        clearMenu();
    }
}