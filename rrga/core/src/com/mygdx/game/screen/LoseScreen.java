package com.mygdx.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.mygdx.game.CameraController;
import com.mygdx.game.GameCanvas;
import com.mygdx.game.GameMode;
import com.mygdx.game.mode.MenuButton;
import com.mygdx.game.mode.MenuMode;
import com.mygdx.game.utility.assets.AssetDirectory;
import com.mygdx.game.utility.util.ScreenListener;

public class LoseScreen extends MenuScreen{

    /** Listener that will update the player mode when we are done */
    private ScreenListener listener;

    /** Reference to GameCanvas created by the root */
    protected GameCanvas canvas;

    /** overlay texture */
    private TextureRegion foregroundTexture;

    /** The background tinting color cache */
    private Color overlayTint;

    /////////////////////DRAWING BUTTONS AND TAGS/////////////////////////
    /** menu button*/
    private MenuButton menuButton;
    /** try again button */
    private MenuButton tryAgainButton;
    /** Lose text */
    private TextureRegion loseTag;
    private static final float LOSE_TAG_X_RATIO = .5f;
    private static final float LOSE_TAG_Y_RATIO = .65f;
    private int loseTagX;
    private int loseTagY;
    private final float TAG_SCL = 1f;
    /** Texture for the cursor */
    private TextureRegion cursorTexture;
    /** Height of the button */
    private static final float BUTTON_SCALE  = 1.0f;
    /** Touch range constant */
    private static final float TOUCH_AREA_RATIO = 0.95f;
    /** Scaling factor for when the player changes the resolution. */
    private float scale;
    /** Standard window size (for scaling) */
    private static final int STANDARD_WIDTH  = 1024;
    /** Standard window height (for scaling) */
    private static final int STANDARD_HEIGHT = 576;

    ////////////BUTTON STATES/////////////////
    /** exit code to go to menu */
    public static final int EXIT_MENU = 1;
    /** exit code to try again */
    public static final int EXIT_TRY_AGAIN = 2;

    /** current assigned exit code of mode (valid exits are non-negative) */
    private int currentExitCode;

    /** The current state of the level menu button */
    private int menuPressState;
    /** The current state of the restart button */
    private int tryAgainPressState;

    private Music gameOverMusic;
    private float musicVolume;

    public LoseScreen(GameCanvas canvas) {
        this.canvas = canvas;
        currentExitCode = Integer.MIN_VALUE;

        this.menuButton = new MenuButton(MenuMode.ButtonShape.RECTANGLE, 0.37f, 0.25f, 0);
        this.tryAgainButton = new MenuButton(MenuMode.ButtonShape.RECTANGLE, 0.63f, 0.25f, 0);

        int width = (int) canvas.getCamera().getViewWidth();
        int height = (int) canvas.getCamera().getViewHeight();
        this.menuButton.setPos(width, height, 1);
        this.tryAgainButton.setPos(width, height, 1);

        loseTagY = (int) (LOSE_TAG_Y_RATIO * height);
        loseTagX = (int) (LOSE_TAG_X_RATIO * width);

        float sx = ((float)width)/STANDARD_WIDTH;
        float sy = ((float)height)/STANDARD_HEIGHT;
        scale = (Math.min(sx, sy));
    }

    /**
     * Draw the lose screen. Proceed to new screen if possible.
     * @param delta The time in seconds since the last render.
     */
    @Override
    public void render(float delta) {

        canvas.begin();

        //canvas.draw(foregroundTexture, Color.WHITE, 0, 0, canvas.getWidth(), canvas.getHeight());
        //above line for opaque foreground, below line for transparent foreground
        CameraController camera = canvas.getCamera();
        canvas.draw(foregroundTexture, Color.WHITE, 0, 0, camera.getViewWidth(), camera.getViewHeight());

        canvas.draw(loseTag, Color.WHITE, loseTag.getRegionWidth()/2f, loseTag.getRegionHeight()/2f,
                loseTagX, loseTagY, 0 , TAG_SCL * scale, TAG_SCL * scale );

        menuButton.draw(canvas, menuPressState, BUTTON_SCALE, Color.WHITE);
        tryAgainButton.draw(canvas, tryAgainPressState, BUTTON_SCALE, Color.WHITE);

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

        // transition
        if (currentExitCode >= 0){
            listener.exitScreen(this, currentExitCode);
            currentExitCode = Integer.MIN_VALUE;
        }
    }

    @Override
    public void dispose() {
        listener = null;
        canvas = null;
        foregroundTexture = null;
        gameOverMusic = null;
    }

    @Override
    public void resize(int width, int height) {
    }

    /**
     * Gather the assets for this controller.
     * This method extracts the asset variables from the given asset directory. It
     * should only be called after the asset directory is completed.
     *
     * @param directory	Reference to global asset manager.
     */
    public void gatherAssets(AssetDirectory directory) {
        foregroundTexture = new TextureRegion(directory.getEntry( "menu:background2", Texture.class ));
        TextureRegion menuTexture = new TextureRegion(directory.getEntry("menu:menu_button", Texture.class));
        TextureRegion tryAgainTexture = new TextureRegion(directory.getEntry("menu:tryagain_button", Texture.class));
        menuButton.setTexture(menuTexture);
        tryAgainButton.setTexture(tryAgainTexture);
        loseTag = new TextureRegion(directory.getEntry("menu:lose_text", Texture.class));
        cursorTexture = new TextureRegion(directory.getEntry("menu:cursor_menu", Texture.class));
        gameOverMusic = directory.getEntry("music:game_over", Music.class);
    }

    /**
     * Sets the ScreenListener for this mode.
     * The ScreenListener will respond to requests to quit.
     */
    public void setScreenListener(ScreenListener listener){
        this.listener = listener;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {

        boolean menuPressed = checkClicked2(screenX, screenY, menuButton);
        boolean tryAgainPressed = checkClicked2(screenX, screenY, tryAgainButton);

        if (menuPressed) {
            menuPressState = 1;
        } else if (tryAgainPressed) {
            tryAgainPressState = 1;
        }
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (menuPressState == 1){
            currentExitCode = EXIT_MENU;
            menuPressState = 2;
            listener.exitScreen(this, currentExitCode);
            currentExitCode = Integer.MIN_VALUE;
        } else if (tryAgainPressState == 1) {
            currentExitCode = EXIT_TRY_AGAIN;
            tryAgainPressState = 2;
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

    public void reset() {
        currentExitCode = Integer.MIN_VALUE;
        gameOverMusic.setVolume(musicVolume);
        gameOverMusic.play();
    }

    public void setVolume(float volume){
        musicVolume = volume;
    }
}

