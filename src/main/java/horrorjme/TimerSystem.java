package horrorjme;

/**
 * FIXED: Timer system for survival rounds - prevents infinite bonus points
 * Now round completion only triggers once per round
 */
public class TimerSystem {

    public enum GamePhase {
        WAITING,     // Before round starts
        ACTIVE,      // Round in progress
        COMPLETED,   // Round finished
        GAME_OVER    // Player died
    }

    private GamePhase currentPhase = GamePhase.WAITING;
    private int currentRound = 1;
    private float roundDuration = 120f; // 2 minutes in seconds
    private float currentTime = 0f;
    private float timeSinceLastSpawn = 0f;
    private float spawnInterval = 3f; // Spawn enemy every 3 seconds

    // Round progression
    private boolean roundActive = false;
    private boolean roundCompleted = false;

    // FIXED: Add flag to prevent multiple round completion triggers
    private boolean roundCompletionProcessed = false;

    public TimerSystem() {
        System.out.println("TimerSystem: Initialized - Round " + currentRound + " (" + roundDuration + "s)");
    }

    /**
     * Start the current round
     */
    public void startRound() {
        currentPhase = GamePhase.ACTIVE;
        roundActive = true;
        roundCompleted = false;
        roundCompletionProcessed = false; // FIXED: Reset completion flag
        currentTime = 0f;
        timeSinceLastSpawn = 0f;

        System.out.println("TimerSystem: Round " + currentRound + " STARTED! Survive for " + roundDuration + " seconds!");
    }

    /**
     * Update timer system (call every frame)
     */
    public void update(float tpf) {
        if (!roundActive) return;

        currentTime += tpf;
        timeSinceLastSpawn += tpf;

        // Check if round is complete
        if (currentTime >= roundDuration && !roundCompleted) {
            completeRound();
        }
    }

    /**
     * Complete the current round
     */
    private void completeRound() {
        currentPhase = GamePhase.COMPLETED;
        roundActive = false;
        roundCompleted = true;
        roundCompletionProcessed = false; // FIXED: Not processed yet

        System.out.println("TimerSystem: Round " + currentRound + " COMPLETED!");
        System.out.println("TimerSystem: Survived " + roundDuration + " seconds!");
    }

    /**
     * Check if it's time to spawn an enemy
     */
    public boolean shouldSpawnEnemy() {
        if (!roundActive) return false;

        if (timeSinceLastSpawn >= spawnInterval) {
            timeSinceLastSpawn = 0f;
            return true;
        }
        return false;
    }

    /**
     * FIXED: Check if round was completed (one-time event)
     * Returns true only once per round completion
     */
    public boolean isRoundCompleted() {
        if (roundCompleted && !roundCompletionProcessed) {
            roundCompletionProcessed = true; // Mark as processed
            return true;
        }
        return false;
    }

    /**
     * Start next round (for future rounds)
     */
    public void startNextRound() {
        currentRound++;
        roundDuration += 30f; // Each round is 30 seconds longer
        spawnInterval = Math.max(1f, spawnInterval - 0.2f); // Faster spawning each round

        startRound();
        System.out.println("TimerSystem: Starting Round " + currentRound + " - Duration: " + roundDuration + "s, Spawn every: " + spawnInterval + "s");
    }

    /**
     * Player died - end the game
     */
    public void playerDied() {
        currentPhase = GamePhase.GAME_OVER;
        roundActive = false;

        System.out.println("TimerSystem: GAME OVER! Survived " + currentTime + " seconds in Round " + currentRound);
    }

    /**
     * Reset to initial state
     */
    public void reset() {
        currentPhase = GamePhase.WAITING;
        currentRound = 1;
        roundDuration = 120f;
        currentTime = 0f;
        timeSinceLastSpawn = 0f;
        spawnInterval = 3f;
        roundActive = false;
        roundCompleted = false;
        roundCompletionProcessed = false; // FIXED: Reset completion flag

        System.out.println("TimerSystem: Reset to initial state");
    }

    // Getters
    public GamePhase getCurrentPhase() { return currentPhase; }
    public int getCurrentRound() { return currentRound; }
    public float getRoundDuration() { return roundDuration; }
    public float getCurrentTime() { return currentTime; }
    public float getTimeRemaining() { return Math.max(0f, roundDuration - currentTime); }
    public boolean isRoundActive() { return roundActive; }

    /**
     * FIXED: This getter now just shows state, doesn't trigger events
     */
    public boolean getRoundCompletedStatus() { return roundCompleted; }

    /**
     * Get formatted time string for HUD (MM:SS)
     */
    public String getFormattedTimeRemaining() {
        float timeLeft = getTimeRemaining();
        int minutes = (int) (timeLeft / 60);
        int seconds = (int) (timeLeft % 60);
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * Get formatted current time string for HUD (MM:SS)
     */
    public String getFormattedCurrentTime() {
        int minutes = (int) (currentTime / 60);
        int seconds = (int) (currentTime % 60);
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * Get completion percentage (0.0 to 1.0)
     */
    public float getRoundProgress() {
        if (roundDuration <= 0) return 1f;
        return Math.min(1f, currentTime / roundDuration);
    }
}