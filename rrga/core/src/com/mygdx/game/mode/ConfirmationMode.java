package com.mygdx.game.mode;

import com.badlogic.gdx.*;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.math.Vector2;
import com.mygdx.game.CameraController;
import com.mygdx.game.GameCanvas;
import com.mygdx.game.screen.MenuScreen;
import com.mygdx.game.utility.assets.AssetDirectory;
import com.mygdx.game.utility.util.ScreenListener;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * A PauseMode is a pause menu screen. User can interact with this screen
 * through keyboard or mouse cursor.
 */
public class ConfirmationMode extends MenuScreen {

    /** Listener that will update the player mode when we are done */
    private ScreenListener listener;

    /** The Screen to draw underneath this screen*/
    private MenuScreen backgroundScreen;

    /** Reference to GameCanvas created by the root */
    protected GameCanvas canvas;

    /** overlay texture */
    private TextureRegion foregroundTexture;

    /** The background tinting color cache */
    private Color overlayTint;

    /** exit code to toggle pause state */
    public static final int EXIT_RESUME = 1;

    /** current assigned exit code of mode (valid exits are non-negative) */
    private int currentExitCode;

    private int prevModeExitCode;

    /** The current state of the yes button */
    private int yesPressState;
    /** The current state of the no button */
    private int noPressState;

    /** yes button*/
    private final MenuButton yesButton;
    /** no button */
    private final MenuButton noButton;

    /** Height of the button */
    private static float BUTTON_SCALE  = 1.0f;
    /** Touch range constant */
    private static float TOUCH_AREA_RATIO = 0.95f;
    private float TAG_SCL = 1;
    /** Scaling factor for when the player changes the resolution. */
    private float scale;

    /** Standard window size (for scaling) */
    private static int STANDARD_WIDTH  = 1024;
    /** Standard window height (for scaling) */
    private static int STANDARD_HEIGHT = 576;

    /** Pause text related variables */
    private TextureRegion confirmationTag;
    private TextureRegion popup;
    private static float CONFIRMATION_TAG_X_RATIO = .5f;
    private static float CONFIRMATION_TAG_Y_RATIO = .65f;
    private int confirmationTagX;
    private int confirmationTagY;
    private int popupX;
    private int popupY;

    public static final int EXIT_LEVEL_SELECTOR = 2;
    public static final int EXIT_SETTINGS = 3;
    public static final int EXIT_PAUSE = 4;

    private Music music;
    private float volume;
    private TextureRegion cursorTexture;

    public void setMusic(Music music){this.music=music;}
    public void setVolume(float vol){volume=vol;}

    public void setPreviousExitCode(int code){
        prevModeExitCode = code;
    }

    public ConfirmationMode(GameCanvas canvas) {
        this.canvas = canvas;
        overlayTint = new Color(0,0,0,0.6f);
        currentExitCode = Integer.MIN_VALUE;

        this.yesButton = new MenuButton(MenuMode.ButtonShape.RECTANGLE, 0.37f, 0.4f, 0);
        this.noButton = new MenuButton(MenuMode.ButtonShape.RECTANGLE, 0.63f, 0.4f, 0);

        // scaling
        CameraController camera = canvas.getCamera();
        float width = camera.getViewWidth();
        float height = camera.getViewHeight();
        float sx = width /STANDARD_WIDTH;
        float sy = height /STANDARD_HEIGHT;
        scale = Math.min(sx, sy);

        yesButton.setPos((int)width, (int)height, scale);
        noButton.setPos((int)width, (int)height, scale);

        confirmationTagY = (int)(CONFIRMATION_TAG_Y_RATIO * height);
        confirmationTagX = (int)(CONFIRMATION_TAG_X_RATIO * width);
        popupY = (int)((CONFIRMATION_TAG_Y_RATIO-0.05f) * height);
        popupX = (int)(CONFIRMATION_TAG_X_RATIO * width);
    }

    /**
     * Gather the assets for this controller.
     * This method extracts the asset variables from the given asset directory. It
     * should only be called after the asset directory is completed.
     *
     * @param directory    Reference to global asset manager.
     */
    public void gatherAssets(AssetDirectory directory) {
        foregroundTexture = new TextureRegion(directory.getEntry("game:platform", Texture.class));
        cursorTexture = new TextureRegion(directory.getEntry( "menu:cursor_menu", Texture.class ));
        confirmationTag = new TextureRegion(directory.getEntry("menu:confirm_text", Texture.class));
        popup = new TextureRegion(directory.getEntry("menu:popup", Texture.class));

        TextureRegion yesTexture = new TextureRegion(directory.getEntry("menu:checkmark", Texture.class));
        TextureRegion noTexture = new TextureRegion(directory.getEntry("menu:x", Texture.class));
        yesButton.setTexture(yesTexture);
        noButton.setTexture(noTexture);
    }

    public boolean touchDown(int screenX, int screenY, int pointer, int button) {

        boolean yesPressed = checkClicked2(screenX, screenY, yesButton);
        boolean noPressed = checkClicked2(screenX, screenY, noButton);

        if (yesPressed) {
            yesPressState = 1;
        } else if (noPressed) {
            noPressState = 1;
        }

        return false;
    }

    /** preferences object to store user settings */
    Preferences settings = Gdx.app.getPreferences("settings");
    /** preferences object to store which levels the user has unlocked */
    Preferences unlocked = Gdx.app.getPreferences("unlocked");
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (yesPressState == 1){
            currentExitCode = prevModeExitCode;
            yesPressState = 2;
            if (prevModeExitCode == EXIT_PAUSE || prevModeExitCode == EXIT_SETTINGS){
                settings.putFloat("musicVolume", 0.5f);
                settings.putFloat("sfxVolume", 0.5f);
                settings.putBoolean("toggle", false);
                settings.flush();
            } else if (prevModeExitCode == EXIT_LEVEL_SELECTOR){
                for (int i = 2; i <= MenuMode.LEVEL_COUNT; i++){
                    unlocked.putBoolean(i+"unlocked", false);
                }
                unlocked.flush();
            }
            listener.exitScreen(this, currentExitCode);
            currentExitCode = Integer.MIN_VALUE;
        } else if (noPressState == 1) {
            currentExitCode = prevModeExitCode;
            noPressState = 2;
            listener.exitScreen(this, currentExitCode);
            currentExitCode = Integer.MIN_VALUE;
        }
        return true;
    }

    /**
     * Checks if click was in bound for rectangular buttons
     *
     * @return boolean for whether button is pressed
     */
    private boolean checkClicked2(int screenX, int screenY,  MenuButton button) {

        // convert mouse screen coordinate to viewport world coordinate
        CameraController camera = canvas.getCamera();
        Vector2 temp = camera.unproject(screenX, screenY);
        screenX = (int) temp.x;
        screenY = (int) temp.y;

        // TODO: TEMPORARY touch range to make it smaller than button
        // Gets positional data of button
        float buttonX = button.getX();
        float buttonY = button.getY();
        float angle = button.getAngle();

        // Gives linear translation for tilted buttons
        float buttonTX = buttonX * (float)Math.cos(angle) + buttonY * (float)Math.sin(angle);
        float buttonTY = -buttonX * (float)Math.sin(angle) + buttonY * (float)Math.cos(angle);
        float screenTX = screenX * (float)Math.cos(angle) + screenY * (float)Math.sin(angle);
        float screenTY = -screenX * (float)Math.sin(angle) + screenY * (float)Math.cos(angle);

        // Checks if appropriate area was clicked
        boolean buttonPressedX = buttonTX - TOUCH_AREA_RATIO*BUTTON_SCALE*scale*button.getRegionWidth()/2 <= screenTX &&
                screenTX <= buttonTX + TOUCH_AREA_RATIO*BUTTON_SCALE*scale*button.getRegionWidth()/2;
        boolean buttonPressedY = buttonTY - BUTTON_SCALE*scale*button.getRegionHeight()/2 <= screenTY &&
                screenTY <= buttonTY + BUTTON_SCALE*scale*button.getRegionHeight()/2;

        return buttonPressedX && buttonPressedY;
    }


    /**
     * Draw the Pause menu and exit pause mode if possible.
     * @param delta The time in seconds since the last render.
     */
    public void render(float delta) {
        // safe guard conditional
        if (backgroundScreen != null){
            backgroundScreen.render(delta);
        }
        draw(delta);
    }

    /**
     * Draws static pause menu
     * @param delta The time in seconds since the last render
     */
    private void draw(float delta){
        canvas.begin();
        CameraController camera = canvas.getCamera();
        canvas.draw(foregroundTexture, overlayTint, 0, 0, camera.getViewWidth(), camera.getViewHeight());

        canvas.draw(popup, Color.WHITE, popup.getRegionWidth()/2f, popup.getRegionHeight()/2f,
                popupX, popupY, 0, scale, scale);
        canvas.draw(confirmationTag, Color.WHITE, confirmationTag.getRegionWidth()/2f, confirmationTag.getRegionHeight()/2f,
                confirmationTagX, confirmationTagY, 0 , TAG_SCL * scale, TAG_SCL * scale);

        yesButton.draw(canvas, yesPressState, BUTTON_SCALE, Color.WHITE);
        noButton.draw(canvas, noPressState, 1.3f, Color.WHITE);

        // draw mouse texture
        int mx = Gdx.input.getX();
        int my = Gdx.input.getY();
        // retrieve the viewport coordinate to draw cursor
        Vector2 pos = camera.unproject(mx, my);
        if(pos.x <= camera.getViewWidth() && pos.x>= 0 && pos.y < camera.getViewHeight() && pos.y >0) {
            canvas.draw(cursorTexture, Color.WHITE, 0, cursorTexture.getRegionHeight(),
                    pos.x, pos.y, 0, .4f, .4f);
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.None);
        }
        else {
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
        }

        canvas.end();
    }

    public void resize(int width, int height) {
        // resize handled through viewport
    }

    public void dispose() {
        listener = null;
        backgroundScreen = null;
        canvas = null;
        foregroundTexture = null;
        overlayTint = null;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.P){
            currentExitCode = EXIT_RESUME;
        }
        return false;
    }
    @Override
    public boolean keyUp(int keycode) {
        if (currentExitCode > 0) {
            listener.exitScreen(this, currentExitCode);
            currentExitCode = Integer.MIN_VALUE;
        }

        return false;
    }


    /**
     * Sets the ScreenListener for this mode.
     * The ScreenListener will respond to requests to quit.
     */
    public void setScreenListener(ScreenListener listener){
        this.listener = listener;
    }

    public void setBackgroundScreen(MenuScreen backgroundScreen){
        this.backgroundScreen = backgroundScreen;
    }

    public MenuScreen getBackgroundScreen(){
        return this.backgroundScreen;
    }

    public void reset() {
        overlayTint = new Color(0,0,0,0.6f);
        currentExitCode = Integer.MIN_VALUE;
        music.setVolume(volume);
        music.play();
    }
}


