/*
 * InputController.java
 *
 * This class buffers in input from the devices and converts it into its
 * semantic meaning. If your game had an option that allows the player to
 * remap the control keys, you would store this information in this class.
 * That way, the main GameEngine does not have to keep track of the current
 * key mapping.
 *
 * Author: Walker M. White
 * Based on original PhysicsDemo Lab by Don Holden, 2007
 * LibGDX version, 2/6/2015
 */
package com.mygdx.game;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.math.*;

import com.mygdx.game.utility.util.*;
import com.mygdx.game.utility.util.XBoxController;

/**
 * Class for reading player input. 
 *
 * This supports both a keyboard and X-Box controller. In previous solutions, we only 
 * detected the X-Box controller on start-up.  This class allows us to hot-swap in
 * a controller via the new XBox360Controller class.
 */
public class InputController {

    /** minimum mouse movement required to read input */
    private static final float minDeltaX = 0.2f;

    /** The singleton instance of the input controller */
    private static InputController theController = null;

    /**
     * Return the singleton instance of the input controller
     *
     * @return the singleton instance of the input controller
     */
    public static InputController getInstance() {
        if (theController == null) {
            theController = new InputController();
        }
        return theController;
    }

    // Fields to manage buttons
    /** Whether the reset button was pressed. */
    private boolean resetPressed;
    private boolean resetPrevious;
    /** Whether the debug toggle was pressed. */
    private boolean debugPressed;
    private boolean debugPrevious;

    /** Whether the next level button was pressed. */
    private boolean nextLevelPressed;
    private boolean nextPrevious;
    /** Whether the exit button was pressed. */
    private boolean exitPressed;
    private boolean exitPrevious;
    /** Whether the player toggled the umbrella open/closed */
    private boolean togglePressed;
    /** Whether the pause button was toggled */
    private boolean pauseToggled;

    /** whether the zoom button was pressed */
    private boolean zoomPressed;
    /** Whether the player is holding down to toggle the umbrella open/closed */
    private boolean toggleHeld;

    /** How much did we move horizontally? */
    private float horizontal;
    /** How much did the mouse move horizontally? */
    private float mouseMovement;
    /** Whether the lighter button was pressed */
    private boolean lighter;
    /** The crosshair position (for raddoll) */
    private Vector2 crosshair;
    /** The crosshair cache (for using as a return value) */
    private Vector2 crosscache;
    /** For the gamepad crosshair control */
    private float momentum;

    /** whether the secondary umbrella open/close control mode is enabled */
    public boolean secondaryControlMode;

    /** An X-Box controller (if it is connected) */
    XBoxController xbox;
    private Vector2 mousePos = new Vector2();
    public Vector2 getMousePos() { return mousePos;}

    /**
     * Returns the amount of sideways movement.
     *
     * -1 = left, 1 = right, 0 = still
     *
     * @return the amount of sideways movement.
     */
    public float getHorizontal() {
        return horizontal;
    }

    /**
     * Returns the amount of mouse movement.
     *
     * -1 = left, 1 = right, 0 = still
     *
     * @return the amount of movement.
     */
    public float getMouseMovement() {
        return mouseMovement;
    }

    /**
     * Returns if the lighter button is pressed or not
     *
     * @return if the lighter button is pressed or not
     */
    public boolean getLighter() {
        return lighter;
    }

    /** Set secondary control mode */
    public void setSecondaryControlMode(boolean toggleOn) {
        this.secondaryControlMode = toggleOn;
    }

    /**
     * Returns true if the reset button was pressed.
     *
     * @return true if the reset button was pressed.
     */
    public boolean didReset() {
        return resetPressed && !resetPrevious;
    }

    /**
     * Returns true if the player wants to go toggle the debug mode.
     *
     * @return true if the player wants to go toggle the debug mode.
     */
    public boolean didDebug() {
        return debugPressed && !debugPrevious;
    }

    /**
     * Returns true if the player wants to go toggle the debug mode.
     *
     * @return true if the player wants to go toggle the debug mode.
     */
    public boolean didNext() {
        return nextLevelPressed && !nextPrevious;
    }

    /**
     * Returns true if the exit button was pressed.
     *
     * @return true if the exit button was pressed.
     */
    public boolean didExit() {
        return exitPressed && !exitPrevious;
    }

    /**
     * Returns true if the button to toggle whether the umbrella is open or closed
     * was pressed.
     *
     * @return true if the open/closed toggle button was pressed
     */
    public boolean didToggle() { return togglePressed; }
    /**
     * Returns true if the button to toggle whether the umbrella is open or closed
     * is held down.
     */
    public boolean isToggleHeld(){ return toggleHeld; }

    /**
     * Returns true if the pause button was toggled
     * @return
     */
    public boolean didPause() { return pauseToggled; }

    /**
     * Returns true if the zoom button is held
     */
    public boolean didZoom() { return zoomPressed; }

    /**
     * Creates a new input controller
     *
     * The input controller attempts to connect to the X-Box controller at device 0,
     * if it exists.  Otherwise, it falls back to the keyboard control.
     */
    public InputController() {
        // If we have a game-pad for id, then use it.
//        Array<XBoxController> controllers = Controllers.get().getXBoxControllers();
//        if (controllers.size > 0) {
//            xbox = controllers.get( 0 );
//        } else {
//            xbox = null;
//        }

        xbox = null;
        crosshair = new Vector2();
        crosscache = new Vector2();
        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.None);
    }

    /**
     * Reads the input for the player and converts the result into game logic.
     *
     * The method provides both the input bounds and the drawing scale.  It needs
     * the drawing scale to convert screen coordinates to world coordinates.  The
     * bounds are for the crosshair.  They cannot go outside of this zone.
     *
     * @param bounds The input bounds for the crosshair.
     * @param scale  The drawing scale
     */
    public void readInput(Rectangle bounds, Vector2 scale) {
        // Copy state from last animation frame
        // Helps us ignore buttons that are held down
        resetPrevious  = resetPressed;
        debugPrevious  = debugPressed;
        exitPrevious = exitPressed;
        nextPrevious = nextLevelPressed;

        // Check to see if a GamePad is connected
        if (xbox != null && xbox.isConnected()) {
            readGamepad(bounds, scale);
            readKeyboard(bounds, scale, true); // Read as a back-up
        } else {
            readKeyboard(bounds, scale, false);
        }
    }

    /**
     * Reads input from an X-Box controller connected to this computer.
     *
     * The method provides both the input bounds and the drawing scale.  It needs
     * the drawing scale to convert screen coordinates to world coordinates.  The
     * bounds are for the crosshair.  They cannot go outside of this zone.
     *
     * @param bounds The input bounds for the crosshair.
     * @param scale  The drawing scale
     */
    private void readGamepad(Rectangle bounds, Vector2 scale) {
        resetPressed = xbox.getStart();
        exitPressed  = xbox.getBack();
        debugPressed  = xbox.getY();

        // Increase animation frame, but only if trying to move
        horizontal = xbox.getLeftX();
        clampPosition(bounds);
    }

    /**
     * Reads input from the keyboard.
     *
     * This controller reads from the keyboard regardless of whether or not an X-Box
     * controller is connected.  However, if a controller is connected, this method
     * gives priority to the X-Box controller.
     *
     * @param secondary true if the keyboard should give priority to a gamepad
     */
    private void readKeyboard(Rectangle bounds, Vector2 scale, boolean secondary) {
        // Give priority to gamepad results
        resetPressed = (secondary && resetPressed) || (Gdx.input.isKeyPressed(Input.Keys.R));
        debugPressed = (secondary && debugPressed) || (Gdx.input.isKeyPressed(Input.Keys.B));
        nextLevelPressed = (secondary && nextLevelPressed) || (Gdx.input.isKeyPressed(Input.Keys.N));

        // A/D for moving character
        horizontal = (secondary ? horizontal : 0.0f);
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            horizontal += 1.0f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            horizontal -= 1.0f;
        }

        // arrow keys for moving umbrella
        // enable this and put it in a conditional statement if we decide to still have an arrow key mode
//        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)){
//            mouseMovement = 1.0f;
//        } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
//            mouseMovement = -1.0f;
//        } else mouseMovement = 0;

        mousePos.x = Gdx.input.getX();
        mousePos.y = Gdx.input.getY();

        // Left mouse click for toggling umbrella open/closed
        if (!secondaryControlMode){
            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) togglePressed = true;
            else togglePressed = false;
        } else {
            if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) toggleHeld = true;
            else toggleHeld = false;
        }

        // Escape for pausing game

        pauseToggled = Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE);

        // Space for zooming
        zoomPressed = Gdx.input.isKeyPressed(Input.Keys.SPACE);

        // W for using the dash
        lighter = Gdx.input.isKeyJustPressed(Input.Keys.W);
    }

    /**
     * Clamp the cursor position so that it does not go outside the window
     *
     * While this is not usually a problem with mouse control, this is critical
     * for the gamepad controls.
     */
    private void clampPosition(Rectangle bounds) {
        crosshair.x = Math.max(bounds.x, Math.min(bounds.x+bounds.width, crosshair.x));
        crosshair.y = Math.max(bounds.y, Math.min(bounds.y+bounds.height, crosshair.y));
    }
}