
package horrorjme;

import javafx.application.Application;

/**
 * Main entry point - launches JavaFX menu instead of default JME startup
 * This replaces the monkey logo and settings dialog with our custom menu
 */
public class GameLauncher {

    public static void main(String[] args) {
        // Launch JavaFX menu as the primary entry point
        // This bypasses JME's default startup completely
        Application.launch(HorrorGameMenu.class, args);
    }
}