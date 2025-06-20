package horrorjme;

/**
 * Manages different game states (Menu, Playing, Paused, Options)
 */
public class GameStateManager {
    public enum GameState {
        MAIN_MENU,
        OPTIONS_MENU,
        PLAYING,
        PAUSED,
        GAME_OVER
    }

    private GameState currentState;
    private GameState previousState;

    public GameStateManager() {
        currentState = GameState.MAIN_MENU;
        previousState = GameState.MAIN_MENU;
    }

    public void setState(GameState newState) {
        previousState = currentState;
        currentState = newState;
        onStateChanged();
    }

    public GameState getCurrentState() {
        return currentState;
    }

    public GameState getPreviousState() {
        return previousState;
    }

    public boolean isInMenu() {
        return currentState == GameState.MAIN_MENU || currentState == GameState.OPTIONS_MENU;
    }

    public boolean isPlaying() {
        return currentState == GameState.PLAYING;
    }

    public boolean isPaused() {
        return currentState == GameState.PAUSED;
    }

    private void onStateChanged() {
        System.out.println("Game state changed from " + previousState + " to " + currentState);
    }
}