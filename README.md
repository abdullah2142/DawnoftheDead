# Dawn of The Dead

A first-person horror game built with JMonkeyEngine 3.8.1, featuring atmospheric environments, zombie enemies, weapon systems, and survival gameplay mechanics.

## Features

- **3D First-Person Horror Gameplay**: Navigate through dark, atmospheric environments
- **Dynamic Lighting System**: Real-time lighting with shadows, fog effects, and a player flashlight
- **Weapon System**: Multiple weapons with animations, muzzle flashes, and bullet tracers
- **Zombie Enemies**: AI-driven zombie enemies that spawn and hunt the player
- **Inventory Management**: Pick up weapons, ammo, and health items
- **HUD System**: Comprehensive heads-up display showing health, ammo, score, and game information
- **Audio System**: Immersive sound effects and background music
- **Menu System**: Game menus and options
- **Collision Detection**: Smooth wall collision with sliding mechanics
- **Post-Processing Effects**: Fog filters and visual effects for enhanced atmosphere

## Requirements

- **Java**: JDK 17 or higher
- **Gradle**: 7.x or higher (wrapper included)
- **OpenGL**: Compatible graphics card with OpenGL support

## Project Structure

```
DawnoftheDead/
├── src/main/java/horrorjme/
│   ├── HorrorGameJME.java          # Main game class
│   ├── GameLauncher.java           # Game launcher entry point
│   ├── Player.java                 # Player controller and logic
│   ├── ZombieEnemy.java            # Zombie enemy implementation
│   ├── WeaponInventory.java        # Weapon management system
│   ├── AmmoInventory.java          # Ammunition tracking
│   ├── HUDManager.java             # Heads-up display manager
│   ├── MapManager.java             # Map loading and management
│   ├── AudioManager.java           # Sound and music management
│   ├── PostProcessingManager.java  # Visual effects processing
│   └── ...                         # Additional game systems
├── src/main/resources/
│   ├── Models/                     # 3D model files
│   ├── Textures/                   # Texture images
│   ├── Sounds/                     # Audio files
│   └── fonts/                      # Font files
├── assets/
│   └── Textures/                   # Additional texture assets
└── build.gradle                    # Build configuration
```

## Building the Project

### Using Gradle Wrapper (Recommended)

**Linux/macOS:**
```bash
./gradlew build
```

**Windows:**
```batch
gradlew.bat build
```

### Building a Fat JAR

The build configuration includes a task to create a fat JAR with all dependencies:

```bash
./gradlew jar
```

The JAR file will be created in `build/libs/`.

## Running the Game

### Using Gradle Run Task

**Linux/macOS:**
```bash
./gradlew run
```

**Windows:**
```batch
gradlew.bat run
```

### Running the Fat JAR

After building, you can run the JAR directly:

```bash
java -jar build/libs/HorrorGameJME-1.0-SNAPSHOT.jar
```

### Direct Execution

If you have the project set up in an IDE, run the `GameLauncher` class or `HorrorGameJME` main method.

## Controls

- **W** - Move forward
- **S** - Move backward
- **A** - Strafe left
- **D** - Strafe right
- **Left Arrow** - Turn left
- **Right Arrow** - Turn right
- **F** - Toggle torch/flashlight

## Technologies Used

- **JMonkeyEngine 3.8.1-stable**: 3D game engine
- **LWJGL3**: Window and OpenGL binding
- **Minie 9.0.1**: Physics engine wrapper for JME
- **JavaFX 17.0.2**: UI framework
- **Gradle**: Build automation
- **Java 17**: Programming language

## Dependencies

The project uses the following key dependencies:

- `jme3-core`, `jme3-desktop`, `jme3-lwjgl3`: Core JMonkeyEngine libraries
- `jme3-effects`, `jme3-plugins`: Visual effects and plugins
- `jme3-terrain`, `jme3-networking`: Terrain and networking support
- `jme3-jogg`: Audio support
- `com.github.stephengold:Minie`: Physics engine

See `build.gradle` for the complete dependency list.

## Configuration

Game settings can be modified in `HorrorGameJME.java`:

- Display resolution: Default 1280x720
- Camera settings: FOV, near/far planes
- Movement speed: Default 3.0 units/second
- Rotation speed: Default 2.0 radians/second
- Fog settings: Distance and density parameters
- Lighting: Shadow quality and intensity

## Development

### Code Style

The project follows standard Java conventions and includes deprecation warnings enabled in the build configuration.

### Performance Optimizations

The build includes several performance optimizations:

- Instanced rendering enabled
- LWJGL debug mode disabled for release builds
- Anisotropic filtering (4x) enabled
- Shadow filtering configured for balance between quality and performance

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Acknowledgments

- Built with [JMonkeyEngine](https://jmonkeyengine.org/)
- Physics powered by [Minie](https://github.com/stephengold/Minie)
- Inspired by classic horror games and Doom-style gameplay

---

**Note**: This is a work in progress. Features and gameplay mechanics may change as development continues.

