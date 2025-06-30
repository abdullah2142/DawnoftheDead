package horrorjme;

import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.*;
import javafx.stage.*;
import javafx.util.*;
import com.jme3.system.AppSettings;

/**
 * JavaFX startup menu that replaces JME's default settings dialog
 * Launches HorrorGameJME when NEW GAME is pressed
 */
public class HorrorGameMenu extends Application {

    private VBox levelOptions;
    private boolean levelOptionsVisible = false;
    private String selectedDifficulty = "MEDIUM"; // Default difficulty

    // Game settings that will be passed to JME
    private int gameResolutionWidth = 1280;
    private int gameResolutionHeight = 720;
    private boolean gameVSync = true;
    private boolean gameFullscreen = false;
    private int gameAntiAliasing = 0; // 0 = off, 2, 4, 8, 16
    private boolean gameDepthBuffer = true;
    private int gameFrameRate = 60; // Target FPS

    // Difficulty button styles
    private final String EASY_STYLE = createDifficultyStyle("#00aa00", "#00cc00", "#008800");
    private final String MEDIUM_STYLE = createDifficultyStyle("#ffaa00", "#ffcc00", "#ee8800");
    private final String HARD_STYLE = createDifficultyStyle("#dd0000", "#ff0000", "#aa0000");

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Show splash screen first
        showSplashScreen(primaryStage);
    }

    /**
     * Show splash screen with engine credits
     */
    private void showSplashScreen(Stage primaryStage) {
        // Splash screen layout
        StackPane splashRoot = new StackPane();
        splashRoot.setStyle("-fx-background-color: #000000;");

        // "Powered by" text
        Text poweredByText = new Text("Powered by");
        poweredByText.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
        poweredByText.setFill(Color.rgb(150, 150, 150));

        // JavaFX logo text
        Text javaFXText = new Text("JavaFX");
        javaFXText.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        javaFXText.setFill(Color.rgb(0, 120, 215)); // JavaFX blue color

        // "&" text
        Text andText = new Text("&");
        andText.setFont(Font.font("Arial", FontWeight.NORMAL, 24));
        andText.setFill(Color.rgb(150, 150, 150));

        // JMonkeyEngine text
        Text jmeText = new Text("jMonkeyEngine");
        jmeText.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        jmeText.setFill(Color.rgb(255, 140, 0)); // Orange color for JME

        // Team credit text
        Text teamText = new Text("Made by Team YouBeSoft");
        teamText.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
        teamText.setFill(Color.rgb(200, 200, 200)); // Light gray

        // Arrange texts vertically
        VBox textBox = new VBox(10);
        textBox.setAlignment(Pos.CENTER);
        textBox.getChildren().addAll(poweredByText, javaFXText, andText, jmeText, teamText);

        // Add subtle glow effect
        DropShadow glow = new DropShadow();
        glow.setColor(Color.rgb(100, 100, 255, 0.6));
        glow.setRadius(20);
        textBox.setEffect(glow);

        splashRoot.getChildren().add(textBox);

        // Create splash scene
        Scene splashScene = new Scene(splashRoot, 800, 600);
        primaryStage.setTitle("Dawn of the Dead");
        primaryStage.setScene(splashScene);
        primaryStage.setResizable(false);
        primaryStage.show();

        // Fade in animation
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.8), textBox);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        // Fade out animation (after delay)
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.8), textBox);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        // Chain animations: fade in -> wait -> fade out -> show main menu
        fadeIn.setOnFinished(e -> {
            PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
            pause.setOnFinished(e2 -> {
                fadeOut.setOnFinished(e3 -> showMainMenu(primaryStage));
                fadeOut.play();
            });
            pause.play();
        });

        fadeIn.play();
    }

    /**
     * Show the main menu (moved from start method)
     */
    private void showMainMenu(Stage primaryStage) {
        // Main layout container
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: black;");

        // Background image with subtle pulse animation
        try {
            Image bgImage = new Image(getClass().getResource("/images/haunted_hallway.jpg").toExternalForm());
            ImageView background = new ImageView(bgImage);
            background.setOpacity(0.9);

            // Make background fit screen
            background.setFitWidth(800);
            background.setFitHeight(600);
            background.setPreserveRatio(false);

            // Background pulse animation
            ScaleTransition bgPulse = new ScaleTransition(Duration.seconds(8), background);
            bgPulse.setFromX(1.0);
            bgPulse.setFromY(1.0);
            bgPulse.setToX(1.02);
            bgPulse.setToY(1.02);
            bgPulse.setAutoReverse(true);
            bgPulse.setCycleCount(Animation.INDEFINITE);
            bgPulse.play();

            root.getChildren().add(background);
        } catch (Exception e) {
            System.out.println("Background image not found, using solid color");
        }

        // Red ambient light effect
        Circle ambientLight = new Circle(400, 300, 500);
        ambientLight.setFill(Color.rgb(150, 0, 0, 0.15));

        // Light flicker animation
        FadeTransition lightFlicker = new FadeTransition(Duration.seconds(1.5), ambientLight);
        lightFlicker.setFromValue(0.15);
        lightFlicker.setToValue(0.08);
        lightFlicker.setCycleCount(Animation.INDEFINITE);
        lightFlicker.setAutoReverse(true);
        lightFlicker.play();

        // Game title with blood-red stroke and pulse effect
        Text title = new Text("DAWN OF THE DEAD");
        try {
            title.setFont(Font.loadFont(getClass().getResourceAsStream("/fonts/HouseofDead.ttf"), 72));
        } catch (Exception e) {
            title.setFont(Font.font("Arial Black", 72));
        }
        title.setFill(Color.TRANSPARENT);
        title.setStroke(Color.rgb(150, 0, 0));
        title.setStrokeWidth(2);
        title.setTextAlignment(TextAlignment.CENTER);

        // Title pulse animation
        ScaleTransition titlePulse = new ScaleTransition(Duration.seconds(1.8), title);
        titlePulse.setFromX(1);
        titlePulse.setFromY(1);
        titlePulse.setToX(1.05);
        titlePulse.setToY(1.05);
        titlePulse.setAutoReverse(true);
        titlePulse.setCycleCount(ScaleTransition.INDEFINITE);
        titlePulse.play();

        // Common button style
        String buttonStyle = "-fx-font-size: 22px; " +
                "-fx-font-weight: bold; " +
                "-fx-background-color: #300000, linear-gradient(#7a1515 0%, #3a0000 50%, #1a0000 100%); " +
                "-fx-text-fill: #e0e0e0; " +
                "-fx-border-color: #5a0000; " +
                "-fx-border-width: 2px; " +
                "-fx-border-radius: 5px; " +
                "-fx-background-radius: 5px; " +
                "-fx-padding: 10 30 10 30; " +
                "-fx-min-width: 220px; " +
                "-fx-pref-width: 220px; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(255,0,0,0.5), 10, 0, 0, 0);";

        String hoverStyle = "-fx-background-color: #400000, linear-gradient(#8a2525 0%, #4a0000 50%, #2a0000 100%); " +
                "-fx-text-fill: #ffffff; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(255,50,50,0.8), 15, 0, 0, 0);";

        // Create main buttons
        Button newGameButton = createUniformButton("NEW GAME", buttonStyle, hoverStyle);
        Button levelButton = createUniformButton("LEVEL", buttonStyle, hoverStyle);
        Button weaponsButton = createUniformButton("WEAPONS", buttonStyle, hoverStyle);
        Button settingsButton = createUniformButton("SETTINGS", buttonStyle, hoverStyle);
        Button quitButton = createUniformButton("QUIT", buttonStyle, hoverStyle);

        // Button actions
        newGameButton.setOnAction(e -> startGame(primaryStage));
        quitButton.setOnAction(e -> primaryStage.close());
        settingsButton.setOnAction(e -> showSettings());
        weaponsButton.setOnAction(e -> showWeaponsInfo());

        // Create level options with intuitive colors
        Button easyButton = createUniformButton("EASY", EASY_STYLE,
                "-fx-background-color: #00ff00; -fx-text-fill: black;");
        Button mediumButton = createUniformButton("MEDIUM", MEDIUM_STYLE,
                "-fx-background-color: #ffff00; -fx-text-fill: black;");
        Button hardButton = createUniformButton("HARD", HARD_STYLE,
                "-fx-background-color: #ff3333; -fx-text-fill: white;");

        // Difficulty selection
        easyButton.setOnAction(e -> {
            selectedDifficulty = "EASY";
            System.out.println("Selected difficulty: EASY");
        });
        mediumButton.setOnAction(e -> {
            selectedDifficulty = "MEDIUM";
            System.out.println("Selected difficulty: MEDIUM");
        });
        hardButton.setOnAction(e -> {
            selectedDifficulty = "HARD";
            System.out.println("Selected difficulty: HARD");
        });

        levelOptions = new VBox(10, easyButton, mediumButton, hardButton);
        levelOptions.setAlignment(Pos.CENTER);
        levelOptions.setVisible(false);
        levelOptions.setManaged(false);

        // Toggle level options visibility when LEVEL button is clicked
        levelButton.setOnAction(e -> {
            levelOptionsVisible = !levelOptionsVisible;
            levelOptions.setVisible(levelOptionsVisible);
            levelOptions.setManaged(levelOptionsVisible);
        });

        // Button container
        VBox buttonBox = new VBox(15, newGameButton, levelButton, levelOptions, weaponsButton, settingsButton, quitButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(20));

        // Version text in corner
        Text versionText = new Text("v1.0");
        versionText.setFont(Font.font("Arial", 12));
        versionText.setFill(Color.rgb(100, 0, 0));

        // Add all elements to root
        root.getChildren().addAll(ambientLight, title, buttonBox, versionText);

        // Position elements
        StackPane.setAlignment(title, Pos.TOP_CENTER);
        StackPane.setMargin(title, new Insets(50, 0, 0, 0));
        StackPane.setAlignment(buttonBox, Pos.CENTER);
        StackPane.setAlignment(versionText, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(versionText, new Insets(0, 10, 10, 0));

        // Create scene
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Dawn of the Dead - Main Menu");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        // Stage is already showing, just change scene

        // Fade in the main menu
        FadeTransition mainMenuFadeIn = new FadeTransition(Duration.seconds(0.5), root);
        mainMenuFadeIn.setFromValue(0.0);
        mainMenuFadeIn.setToValue(1.0);
        mainMenuFadeIn.play();
    }

    /**
     * Launch the JME game with settings from JavaFX menu
     */
    private void startGame(Stage menuStage) {
        System.out.println("Starting game with settings:");
        System.out.println("- Difficulty: " + selectedDifficulty);
        System.out.println("- Resolution: " + gameResolutionWidth + "x" + gameResolutionHeight);
        System.out.println("- Fullscreen: " + gameFullscreen);
        System.out.println("- VSync: " + gameVSync);
        System.out.println("- Anti-Aliasing: " + gameAntiAliasing);

        // Close JavaFX menu
        menuStage.close();

        // Launch JME game in separate thread to avoid blocking
        new Thread(() -> {
            try {
                // Create JME application
                HorrorGameJME game = new HorrorGameJME();

                // Configure JME settings with user preferences
                AppSettings settings = new AppSettings(true);
                settings.setTitle("Dawn of The Dead - Horror Experience");

                // Apply user-selected resolution
                settings.setResolution(gameResolutionWidth, gameResolutionHeight);

                // Apply display settings
                settings.setFullscreen(gameFullscreen);
                settings.setVSync(gameVSync);

                // Apply graphics settings
                if (gameAntiAliasing > 0) {
                    settings.setSamples(gameAntiAliasing);
                }
                settings.setDepthBits(gameDepthBuffer ? 24 : 16);

                // Performance settings
                settings.setFrameRate(60); // Target 60 FPS

                // CRITICAL: Disable JME's settings dialog
                game.setShowSettings(false);
                game.setSettings(settings);

                // Pass difficulty to game
                // game.setDifficulty(selectedDifficulty);

                // Start the game
                game.start();

            } catch (Exception e) {
                System.err.println("Failed to start game: " + e.getMessage());
                e.printStackTrace();

                // Show error dialog on JavaFX thread
                Platform.runLater(() -> {
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Game Launch Error");
                    errorAlert.setHeaderText("Failed to start the game");
                    errorAlert.setContentText("Error: " + e.getMessage() + "\n\nTry adjusting your settings and try again.");
                    errorAlert.showAndWait();
                });
            }
        }).start();
    }

    /**
     * Show comprehensive settings dialog
     */
    private void showSettings() {
        // Create settings window
        Stage settingsStage = new Stage();
        settingsStage.setTitle("Game Settings");
        settingsStage.initModality(Modality.APPLICATION_MODAL);
        settingsStage.setResizable(false);

        // Main container
        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setStyle("-fx-background-color: #1a0000;");

        // Title
        Text settingsTitle = new Text("GAME SETTINGS");
        settingsTitle.setFont(Font.font("Arial Black", 24));
        settingsTitle.setFill(Color.rgb(200, 50, 50));

        // Resolution Section
        VBox resolutionSection = createSettingsSection("Display Resolution");
        ComboBox<String> resolutionCombo = new ComboBox<>();
        resolutionCombo.getItems().addAll(
                "800x600", "1024x768", "1280x720", "1280x1024",
                "1366x768", "1600x900", "1920x1080", "2560x1440"
        );
        resolutionCombo.setValue(gameResolutionWidth + "x" + gameResolutionHeight);
        resolutionCombo.setStyle(getComboBoxStyle());
        resolutionSection.getChildren().add(resolutionCombo);

        // Display Mode Section
        VBox displaySection = createSettingsSection("Display Mode");
        CheckBox fullscreenCheck = new CheckBox("Fullscreen");
        fullscreenCheck.setSelected(gameFullscreen);
        fullscreenCheck.setStyle(getCheckBoxStyle());
        displaySection.getChildren().add(fullscreenCheck);

        // Graphics Section
        VBox graphicsSection = createSettingsSection("Graphics Quality");

        // VSync
        CheckBox vsyncCheck = new CheckBox("Vertical Sync (VSync)");
        vsyncCheck.setSelected(gameVSync);
        vsyncCheck.setStyle(getCheckBoxStyle());

        // Frame Rate Limit
        HBox fpsBox = new HBox(10);
        fpsBox.setAlignment(Pos.CENTER_LEFT);
        Text fpsLabel = new Text("Frame Rate Limit: ");
        fpsLabel.setFill(Color.WHITE);
        fpsLabel.setFont(Font.font("Arial", 14));

        ComboBox<String> fpsCombo = new ComboBox<>();
        fpsCombo.getItems().addAll("30 FPS", "60 FPS", "120 FPS", "144 FPS", "Unlimited");
        fpsCombo.setValue(gameFrameRate == -1 ? "Unlimited" : gameFrameRate + " FPS");
        fpsCombo.setStyle(getComboBoxStyle());
        fpsBox.getChildren().addAll(fpsLabel, fpsCombo);

        // Anti-Aliasing
        HBox aaBox = new HBox(10);
        aaBox.setAlignment(Pos.CENTER_LEFT);
        Text aaLabel = new Text("Anti-Aliasing: ");
        aaLabel.setFill(Color.WHITE);
        aaLabel.setFont(Font.font("Arial", 14));

        ComboBox<String> aaCombo = new ComboBox<>();
        aaCombo.getItems().addAll("Off", "2x", "4x", "8x", "16x");
        aaCombo.setValue(gameAntiAliasing == 0 ? "Off" : gameAntiAliasing + "x");
        aaCombo.setStyle(getComboBoxStyle());
        aaBox.getChildren().addAll(aaLabel, aaCombo);

        // Depth Buffer
        CheckBox depthCheck = new CheckBox("Depth Buffer (Recommended)");
        depthCheck.setSelected(gameDepthBuffer);
        depthCheck.setStyle(getCheckBoxStyle());

        graphicsSection.getChildren().addAll(vsyncCheck, fpsBox, aaBox, depthCheck);

        // Buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button applyButton = createSettingsButton("APPLY", "#00aa00");
        Button cancelButton = createSettingsButton("CANCEL", "#aa0000");
        Button resetButton = createSettingsButton("RESET", "#aaaa00");

        buttonBox.getChildren().addAll(applyButton, resetButton, cancelButton);

        // Button actions
        applyButton.setOnAction(e -> {
            // Apply resolution
            String[] resolution = resolutionCombo.getValue().split("x");
            gameResolutionWidth = Integer.parseInt(resolution[0]);
            gameResolutionHeight = Integer.parseInt(resolution[1]);

            // Apply display settings
            gameFullscreen = fullscreenCheck.isSelected();
            gameVSync = vsyncCheck.isSelected();
            gameDepthBuffer = depthCheck.isSelected();

            // Apply frame rate
            String fpsValue = fpsCombo.getValue();
            if ("Unlimited".equals(fpsValue)) {
                gameFrameRate = -1;
            } else {
                gameFrameRate = Integer.parseInt(fpsValue.replace(" FPS", ""));
            }

            // Apply anti-aliasing
            String aaValue = aaCombo.getValue();
            if ("Off".equals(aaValue)) {
                gameAntiAliasing = 0;
            } else {
                gameAntiAliasing = Integer.parseInt(aaValue.replace("x", ""));
            }

            System.out.println("Settings applied:");
            System.out.println("Resolution: " + gameResolutionWidth + "x" + gameResolutionHeight);
            System.out.println("Fullscreen: " + gameFullscreen);
            System.out.println("VSync: " + gameVSync);
            System.out.println("Frame Rate: " + (gameFrameRate == -1 ? "Unlimited" : gameFrameRate + " FPS"));
            System.out.println("Anti-Aliasing: " + gameAntiAliasing);

            settingsStage.close();
        });

        cancelButton.setOnAction(e -> settingsStage.close());

        resetButton.setOnAction(e -> {
            // Reset to defaults
            resolutionCombo.setValue("1280x720");
            fullscreenCheck.setSelected(false);
            vsyncCheck.setSelected(true);
            fpsCombo.setValue("60 FPS");
            aaCombo.setValue("Off");
            depthCheck.setSelected(true);
        });

        // Add all sections to main container
        mainContainer.getChildren().addAll(
                settingsTitle, resolutionSection, displaySection,
                graphicsSection, buttonBox
        );

        // Create and show scene
        Scene settingsScene = new Scene(new ScrollPane(mainContainer), 500, 600);
        ((ScrollPane) settingsScene.getRoot()).setStyle("-fx-background-color: #1a0000;");
        ((ScrollPane) settingsScene.getRoot()).setFitToWidth(true);

        settingsStage.setScene(settingsScene);
        settingsStage.showAndWait();
    }

    /**
     * Create a settings section with title
     */
    private VBox createSettingsSection(String title) {
        VBox section = new VBox(10);
        section.setPadding(new Insets(10));
        section.setStyle("-fx-background-color: #2a0000; -fx-border-color: #5a0000; -fx-border-width: 1px;");

        Text sectionTitle = new Text(title);
        sectionTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        sectionTitle.setFill(Color.rgb(255, 200, 200));

        section.getChildren().add(sectionTitle);
        return section;
    }

    /**
     * Create styled button for settings dialog
     */
    private Button createSettingsButton(String text, String color) {
        Button button = new Button(text);
        button.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-color: " + color + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-border-color: #333; " +
                        "-fx-border-radius: 3px; " +
                        "-fx-background-radius: 3px; " +
                        "-fx-padding: 8 20 8 20;"
        );

        button.setOnMouseEntered(e -> {
            button.setStyle(button.getStyle() + "-fx-background-color: " + adjustColor(color, 0.2f) + ";");
        });

        button.setOnMouseExited(e -> {
            button.setStyle(button.getStyle().replace("-fx-background-color: " + adjustColor(color, 0.2f), "-fx-background-color: " + color));
        });

        return button;
    }

    /**
     * Get ComboBox styling
     */
    private String getComboBoxStyle() {
        return "-fx-background-color: #3a0000; " +
                "-fx-text-fill: white; " +
                "-fx-border-color: #5a0000; " +
                "-fx-border-width: 1px;";
    }

    /**
     * Get CheckBox styling
     */
    private String getCheckBoxStyle() {
        return "-fx-text-fill: white; " +
                "-fx-font-size: 14px;";
    }

    /**
     * Adjust color brightness
     */
    private String adjustColor(String hexColor, float factor) {
        // Simple color adjustment - in a real app you'd want more sophisticated color manipulation
        if (hexColor.equals("#00aa00")) return "#00cc00"; // Green lighter
        if (hexColor.equals("#aa0000")) return "#cc0000"; // Red lighter
        if (hexColor.equals("#aaaa00")) return "#cccc00"; // Yellow lighter
        return hexColor;
    }

    /**
     * Show weapons info (placeholder for now)
     */
    private void showWeaponsInfo() {
        Alert weaponsAlert = new Alert(Alert.AlertType.INFORMATION);
        weaponsAlert.setTitle("Weapons");
        weaponsAlert.setHeaderText("Available Weapons");
        weaponsAlert.setContentText(
                "• Sten Gun - Rapid fire SMG\n" +
                        "• Revolver - High damage pistol\n" +
                        "• More weapons coming soon!\n\n" +
                        "Find weapons scattered throughout the world."
        );
        weaponsAlert.showAndWait();
    }

    private String createDifficultyStyle(String base, String light, String dark) {
        return "-fx-font-size: 22px; " +
                "-fx-font-weight: bold; " +
                "-fx-background-color: " + base + ", linear-gradient(" + light + " 0%, " + base + " 50%, " + dark + " 100%); " +
                "-fx-text-fill: " + (base.equals("#ffaa00") ? "black" : "white") + ";" +
                "-fx-border-color: " + dark + ";" +
                "-fx-border-width: 2px;" +
                "-fx-border-radius: 5px;" +
                "-fx-background-radius: 5px;" +
                "-fx-padding: 10 30 10 30;" +
                "-fx-min-width: 220px;" +
                "-fx-pref-width: 220px;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 0);";
    }

    private Button createUniformButton(String text, String style, String hoverStyle) {
        Button button = new Button(text);
        button.setStyle(style);
        button.setMinSize(Button.USE_PREF_SIZE, Button.USE_PREF_SIZE);
        button.setMaxSize(Button.USE_PREF_SIZE, Button.USE_PREF_SIZE);
        button.setOnMouseEntered(e -> {
            button.setStyle(style + hoverStyle);
            button.setEffect(new Glow(0.4));
        });
        button.setOnMouseExited(e -> {
            button.setStyle(style);
            button.setEffect(null);
        });
        return button;
    }
}