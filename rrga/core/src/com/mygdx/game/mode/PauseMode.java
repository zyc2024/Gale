package com.mygdx.game.mode;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
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
public class PauseMode extends MenuScreen {

    /** Listener that will update the player mode when we are done */
    private ScreenListener listener;

    /** Reference to GameCanvas created by the root */
    protected GameCanvas canvas;

    /** screen texture */
    private TextureRegion foregroundTexture;

    /** exit code to toggle pause state */
    public static final int EXIT_RESUME = 1;

    /** exit code to restart game */
    public static final int EXIT_RESTART = 2;
    /** exit code to restart game */
    public static final int EXIT_MENU = 3;
    public static final int EXIT_SETTINGS = 4;

    /** current assigned exit code of mode (valid exits are non-negative) */
    private int currentExitCode;

    /** The current state of the level menu button */
    private int menuPressState;
    /** The current state of the restart button */
    private int restartPressState;
    /** The current state of the back button */
    private int backPressState;
    private int settingsPressState;

    /** exit button*/
    private final MenuButton menuButton;
    /** start button */
    private final MenuButton restartButton;
    /** back button */
    private final MenuButton backButton;
    /** settings button */
    private final MenuButton settingsButton;

    /** Height of the button */
    private static final float BUTTON_SCALE  = 1.0f;
    /** Touch range constant */
    private static final float TOUCH_AREA_RATIO = 0.95f;
    private final float TAG_SCL = 1;
    /** Scaling factor for when the player changes the resolution. */
    private final float scale;

    /** Standard window size (for scaling) */
    private static final int STANDARD_WIDTH  = 1024;
    /** Standard window height (for scaling) */
    private static final int STANDARD_HEIGHT = 576;

    /** Pause text related variables */
    private TextureRegion pauseTag;
    private static final float PAUSE_TAG_X_RATIO = .5f;
    private static final float PAUSE_TAG_Y_RATIO = .65f;
    private final int pauseTagX;
    private final int pauseTagY;

    /** Level display font related variables */
    private BitmapFont levelFont;
    private static final float LEVEL_FONT_X_RATIO = .87f;
    private static final float LEVEL_FONT_Y_RATIO = .97f;
    private final int levelFontX;
    private final int levelFontY;
    private Color fontColor = new Color(244f/255f,92f/255f,20f/255f,1f);

    /** Texture for the cursor */
    private TextureRegion cursorTexture;
    /** current level */
    private int currentLevel;

    public PauseMode(GameCanvas canvas) {
        this.canvas = canvas;
        currentExitCode = Integer.MIN_VALUE;

        this.menuButton = new MenuButton(MenuMode.ButtonShape.RECTANGLE, 0.37f, 0.25f, 0);
        this.restartButton = new MenuButton(MenuMode.ButtonShape.RECTANGLE, 0.63f, 0.25f, 0);
        this.backButton = new MenuButton(MenuMode.ButtonShape.CIRCLE, 0.05f, 0.93f, 0);
        this.settingsButton = new MenuButton(MenuMode.ButtonShape.RECTANGLE, 0.95f, 0.07f, 0);


        int width = (int) canvas.getCamera().getViewWidth();
        int height = (int) canvas.getCamera().getViewHeight();
        float sx = ((float)width)/STANDARD_WIDTH;
        float sy = ((float)height)/STANDARD_HEIGHT;
        scale = Math.min(sx, sy);

        menuButton.setPos(width, height, scale);
        restartButton.setPos(width, height, scale);
        backButton.setPos(width, height, scale);
        settingsButton.setPos(width, height, scale);

        pauseTagY = (int)(PAUSE_TAG_Y_RATIO * height);
        pauseTagX = (int)(PAUSE_TAG_X_RATIO * width);
        levelFontY = (int)(LEVEL_FONT_Y_RATIO * height);
        levelFontX = (int)(LEVEL_FONT_X_RATIO * width);
    }

    /**
     * Gather the assets for this controller.
     * This method extracts the asset variables from the given asset directory. It
     * should only be called after the asset directory is completed.
     *
     * @param directory    Reference to global asset manager.
     */
    public void gatherAssets(AssetDirectory directory) {
        foregroundTexture = new TextureRegion(directory.getEntry( "menu:background2", Texture.class ));

        TextureRegion menuTexture = new TextureRegion(directory.getEntry("menu:menu_button", Texture.class));
        TextureRegion restartTexture = new TextureRegion(directory.getEntry("menu:restart_button", Texture.class));
        TextureRegion backButtonTexture = new TextureRegion(directory.getEntry("menu:back_button", Texture.class));
        TextureRegion settingsTexture = new TextureRegion(directory.getEntry("menu:settings_button", Texture.class));
        levelFont = directory.getEntry("menu:level_font", BitmapFont.class);
        levelFont.setColor(fontColor);


        pauseTag = new TextureRegion(directory.getEntry("pause:pause_tag", Texture.class));
        cursorTexture = new TextureRegion(directory.getEntry("menu:cursor_menu", Texture.class));

        menuButton.setTexture(menuTexture);
        restartButton.setTexture(restartTexture);
        backButton.setTexture(backButtonTexture);
        settingsButton.setTexture(settingsTexture);
    }

    public boolean touchDown(int screenX, int screenY, int pointer, int button) {

        boolean menuPressed = checkClicked2(screenX, screenY, menuButton);
        boolean restartPressed = checkClicked2(screenX, screenY, restartButton);
        boolean backPressed =checkCircleClicked2(screenX, screenY, backButton, BUTTON_SCALE);
        boolean settingsPressed = checkCircleClicked2(screenX, screenY, settingsButton, BUTTON_SCALE);

        if (menuPressed) {
            menuPressState = 1;
        } else if (restartPressed) {
            restartPressState = 1;
        } else if (backPressed) {
            backPressState = 1;
        } else if (settingsPressed){
            settingsPressState = 1;
        }

        return false;
    }

    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (menuPressState == 1){
            currentExitCode = EXIT_MENU;
            menuPressState = 2;
            listener.exitScreen(this, currentExitCode);
            currentExitCode = Integer.MIN_VALUE;
        } else if (restartPressState == 1) {
            currentExitCode = EXIT_RESTART;
            restartPressState = 2;
            listener.exitScreen(this, currentExitCode);
            currentExitCode = Integer.MIN_VALUE;
        } else if (backPressState == 1) {
            currentExitCode = EXIT_RESUME;
            backPressState = 2;
            listener.exitScreen(this, currentExitCode);
            currentExitCode = Integer.MIN_VALUE;
        } else if (settingsPressState == 1){
            currentExitCode = EXIT_SETTINGS;
            settingsPressState = 2;
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

        Vector2 temp = canvas.getCamera().unproject(screenX, screenY);
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
     * Checks if click was in bound for circular buttons
     *
     * @return boolean for whether button is pressed
     */
    private boolean checkCircleClicked2(float screenX, float screenY, MenuButton button, float scl) {

        Vector2 temp = canvas.getCamera().unproject(screenX, screenY);
        screenX = (int) temp.x;
        screenY = (int) temp.y;

        float buttonX = button.getX();
        float buttonY = button.getY();
        float radius = scl*scale*button.getRegionWidth()/2.0f;
        float dist = (screenX-buttonX)*(screenX-buttonX)+(screenY-buttonY)*(screenY-buttonY);

        // Checks if space inside the circle has been clicked
        return dist < radius*radius;
    }


    /**
     * Draw the Pause menu and exit pause mode if possible.
     * @param delta The time in seconds since the last render.
     */
    public void render(float delta) {
        draw(delta);
    }

    /**
     * Draws static pause menu
     * @param delta The time in seconds since the last render
     */
    private void draw(float delta){
        canvas.begin();
        CameraController camera = canvas.getCamera();
        canvas.draw(foregroundTexture, Color.WHITE, 0, 0, camera.getViewWidth(), camera.getViewHeight());


        canvas.draw(pauseTag, Color.WHITE, pauseTag.getRegionWidth()/2f, pauseTag.getRegionHeight()/2f,
                pauseTagX, pauseTagY, 0 , TAG_SCL * scale, TAG_SCL * scale);

        menuButton.draw(canvas, menuPressState, BUTTON_SCALE, Color.WHITE);
        restartButton.draw(canvas, restartPressState, BUTTON_SCALE, Color.WHITE);
        backButton.draw(canvas, backPressState, BUTTON_SCALE, Color.WHITE);
        settingsButton.draw(canvas, settingsPressState, BUTTON_SCALE, Color.WHITE);

        canvas.drawText("Level " + currentLevel, levelFont, levelFontX, levelFontY);

        //draw mouse texture
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
        // resizing done through viewport
        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.None);
    }

    public void dispose() {
        listener = null;
        canvas = null;
        foregroundTexture = null;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.ESCAPE){
            currentExitCode = EXIT_RESUME;
            return true;
        }
        return false;
    }
    @Override
    public boolean keyUp(int keycode) {
        if (currentExitCode > 0) {
            listener.exitScreen(this, currentExitCode);
            currentExitCode = Integer.MIN_VALUE;
            return true;
        }
        return false;
    }

    /** Sets current level */
    public void setCurrentLevel(int level){
        currentLevel = level;
    }


    /**
     * Sets the ScreenListener for this mode.
     * The ScreenListener will respond to requests to quit.
     */
    public void setScreenListener(ScreenListener listener){
        this.listener = listener;
    }

    public void reset() {
        currentExitCode = Integer.MIN_VALUE;
    }
}
