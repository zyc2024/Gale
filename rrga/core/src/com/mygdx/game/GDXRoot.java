package com.mygdx.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.mygdx.game.mode.*;
import com.mygdx.game.screen.LoseScreen;
import com.mygdx.game.screen.VictoryScreen;
import com.mygdx.game.utility.assets.AssetDirectory;
import com.mygdx.game.utility.util.ScreenListener;


public class GDXRoot extends Game implements ScreenListener {
    /**
     * AssetManager to load game assets (textures, sounds, etc.)
     */
    AssetDirectory directory;
    /**
     * Drawing context to display graphics (VIEW CLASS)
     */
    private GameCanvas canvas;
    /**
     * Player mode for the asset loading screen (CONTROLLER CLASS)
     */
    private LoadingMode loading;
    /**
     * Player mode for the main menu screen (CONTROLLER CLASS)
     */
    private MenuMode menu;
    /**
     * Player mode for the game play (CONTROLLER CLASS)
     */
    private GameMode playing;

    /**
     * Player mode for pausing the game (CONTROLLER CLASS)
     */
    private PauseMode pausing;

	private ConfirmationMode confirmation;
    private CutSceneMode cutscene;

	/** Screen mode for transitioning between levels */
	private VictoryScreen victory;

    /**
     * Screen mode for transitioning between levels
     */
    private LoseScreen defeat;

    private String filePath = "";
    private JsonValue sampleLevel;

    /**
     * Creates a new game from the configuration settings.
     * <p>
     * This method configures the asset manager, but does not load any assets
     * or assign any screen.
     */
    public GDXRoot() {
    }

    /**
     * this is a temporary constructor for quick development.
     *
     * @param filepath the filepath to a local Tiled JSON file
     */
    public GDXRoot(String filepath) {
        this();
        this.filePath = filepath;
    }

    /**
     * Called when the Application is first created.
     * <p>
     * This is method immediately loads assets for the loading screen, and prepares
     * the asynchronous loader for all other assets.
     */
    public void create() {
        canvas = new GameCanvas();
        loading = new LoadingMode("assets.json", canvas, 1);
        menu = new MenuMode(canvas);
        playing = new GameMode();
        pausing = new PauseMode(canvas);
        victory = new VictoryScreen(canvas);
        defeat = new LoseScreen(canvas);
        confirmation = new ConfirmationMode(canvas);
        cutscene = new CutSceneMode(canvas);

        loading.setScreenListener(this);
        pausing.setScreenListener(this);
        victory.setScreenListener(this);
        defeat.setScreenListener(this);
        confirmation.setScreenListener(this);
        cutscene.setScreenListener(this);
        setScreen(loading);
    }

    /**
     * Called when the Application is destroyed.
     * <p>
     * This is preceded by a call to pause().
     */
    public void dispose() {
        // Call dispose on our children
        setScreen(null);

        canvas.dispose();
        canvas = null;
        menu.dispose();
        menu = null;
        playing.dispose();
        playing = null;
        pausing.dispose();
        pausing = null;
        victory.dispose();
        victory = null;
        defeat.dispose();
        defeat = null;
        cutscene.dispose();
        cutscene = null;

        // Unload all of the resources
        if (directory != null) {
            directory.unloadAssets();
            directory.dispose();
            directory = null;
        }
        super.dispose();
    }

    /**
     * Called when the Application is resized.
     * <p>
     * This can happen at any point during a non-paused state but will never happen
     * before a call to create().
     *
     * @param width  The new width in pixels
     * @param height The new height in pixels
     */
    public void resize(int width, int height) {
        Gdx.gl.glViewport(0, 0,width, height);
        canvas.resize();
        super.resize(width, height);
    }

	/**
	 * The given screen has made a request to exit its player mode.
	 * The value exitCode can be used to implement menu options.
	 *
	 * @param screen   The screen requesting to exit
	 * @param exitCode The state of the screen upon exit
	 */
	public void exitScreen(Screen screen, int exitCode) {
		if (screen == loading) {
			// finish loading assets, let all other controllers gather necessary assets
			directory = loading.getAssets();
			playing.gatherAssets(directory);
			menu.gatherAssets(directory);
			pausing.gatherAssets(directory);
			victory.gatherAssets(directory);
			defeat.gatherAssets(directory);
			confirmation.gatherAssets(directory);
            cutscene.gatherAssets(directory);

            // transition to gameplay screen.
            menu.setScreenListener(this);
            setScreen(menu);
            loading.dispose();
            loading = null;
        } else if (screen == menu) {
            switch (exitCode) {
                case MenuMode.EXIT_QUIT:
                    Gdx.app.exit();
                    break;
                case MenuMode.EXIT_PLAY:
                    menu.cameForPauseSettings = false;
                    // TODO: DISABLE FOR SUBMISSION
                    if (filePath.length() > 0) {
                        String filePath = Gdx.files.local(this.filePath).file().getAbsolutePath();

                        // Load the JSON file into a FileHandle
                        FileHandle fileHandle = Gdx.files.absolute(filePath);

                        String jsonString = fileHandle.readString();

                        // Parse the JSON string into a JsonValue object
                        sampleLevel = new JsonReader().parse(jsonString);
                    }
                    playing.setSampleLevel(sampleLevel);
                    playing.setScreenListener(this);
                    playing.setCanvas(canvas);
                    
					// Transferring menu mode information to game mode
					playing.setLevel(menu.getCurrentLevel());
					playing.setVolume(menu.getSfxVolume(), menu.getMusicVolume());
					playing.setSecondaryControlMode(menu.getControlToggle());
					playing.reset();
					setScreen(playing);
					menu.pause();
					break;
				case MenuMode.EXIT_CONFIRM:
					confirmation.setPreviousExitCode(menu.getScreenMode());
					confirmation.setScreenListener(this);
					confirmation.setBackgroundScreen(menu);
					confirmation.setMusic(menu.getMusic());
					confirmation.setVolume(menu.getMusicVolume());
					confirmation.reset();
					setScreen(confirmation);
					break;
                case MenuMode.EXIT_PAUSE:
                    menu.cameForPauseSettings = false;
                    playing.setVolume(menu.getSfxVolume(), menu.getMusicVolume());
                    setScreen(pausing);
                    break;
			}
		} else if (screen == pausing){
			switch (exitCode){
				case PauseMode.EXIT_RESUME:
                    // Pause screen -> return -> gameplay
					setScreen(playing);
                    playing.setSecondaryControlMode(menu.getControlToggle());
					break;
				case PauseMode.EXIT_RESTART:
                    playing.stopMusic();
					playing.reset();
					setScreen(playing);
                    playing.setSecondaryControlMode(menu.getControlToggle());
					break;
				case PauseMode.EXIT_MENU:
                    playing.stopMusic();
					menu.setScreenListener(this);
                    menu.cameForPauseSettings = false;
					menu.reset();
					setScreen(menu);
                    playing.setSecondaryControlMode(menu.getControlToggle());
					break;
				case PauseMode.EXIT_SETTINGS:
                    menu.setScreenListener(this);
                    menu.cameForPauseSettings = true;
                    menu.setMusic(playing.getMusic());
                    menu.reset();
                    menu.setScreenMode(3);
                    setScreen(menu);
					break;
				default:
					Gdx.app.exit();
			}
		} else if (screen == playing) {
			switch (exitCode){
				case GameMode.EXIT_VICTORY:
                    playing.stopSFX();
                    playing.stopMusic(); //play victory music only
                    victory.setVolume(playing.getVolume());
                    victory.reset();
					setScreen(victory);
					break;
				case GameMode.EXIT_FAIL:
                    playing.stopSFX();
                    // in defeat, play the short game over music on top of level music
                    // MAKE SURE TO STOP GAME-MODE MUSIC WHEN EXITING LOSE SCREEN
                    defeat.setVolume(playing.getVolume());
					setScreen(defeat);
                    defeat.reset();
					break;
				case GameMode.EXIT_PAUSE:
                    playing.stopSFX();
                    pausing.setCurrentLevel(playing.getCurrentLevel());
					setScreen(pausing);
					break;
				case GameMode.EXIT_QUIT:
					Gdx.app.exit();
                case GameMode.EXIT_CUTSCENE:
                    cutscene.setCurrentLevel(playing.getCurrentLevel());
                    cutscene.setCurrentScene(playing.getCutsceneNum());
                    setScreen(cutscene);
				default:
					break;
			}
		} else if (screen == confirmation){
			if (exitCode == ConfirmationMode.EXIT_LEVEL_SELECTOR || exitCode == ConfirmationMode.EXIT_SETTINGS){
				menu.setScreenListener(this);
				menu.reset();
				menu.setScreenMode(exitCode);
				setScreen(menu);
			}
			else if (exitCode == ConfirmationMode.EXIT_PAUSE){
				setScreen(pausing);
			}
		
        } else if (screen == victory) {
            switch (exitCode){
                case VictoryScreen.EXIT_MENU:
                    victory.stopMusic();
                    menu.setScreenListener(this);
                    menu.reset();
                    setScreen(menu);
                    break;
                case VictoryScreen.EXIT_NEXT:
                    victory.stopMusic();
                    playing.setNextLevel();
                    playing.reset();
                    setScreen(playing);
                    break;
            }
        } else if (screen == defeat) {
            switch (exitCode) {
                case LoseScreen.EXIT_TRY_AGAIN:
                    playing.stopMusic();    // made sure to stop music
                    playing.reset();
                    setScreen(playing);
                    break;
                case LoseScreen.EXIT_MENU:
                    playing.stopMusic();     // made sure to stop music
                    menu.setScreenListener(this);
                    menu.reset();
                    setScreen(menu);
                    break;
            }
        } else if (screen == cutscene) {
            switch (exitCode) {
                case CutSceneMode.EXIT_RESUME:
                    setScreen(playing);
                    playing.setCutsceneBool();
                    playing.setSecondaryControlMode(menu.getControlToggle());
                    break;
                case CutSceneMode.EXIT_MENU:
                    menu.setScreenListener(this);
                    menu.cameForPauseSettings = false;
                    menu.reset();
                    playing.stopMusic();
                    setScreen(menu);
                    playing.setSecondaryControlMode(menu.getControlToggle());
            }
        }
    }
}