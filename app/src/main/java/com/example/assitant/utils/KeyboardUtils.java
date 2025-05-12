package com.example.assitant.utils;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;

/**
 * Utility class to assist with keyboard visibility issues
 */
public class KeyboardUtils {

    // Threshold for detecting keyboard visibility
    private static final int KEYBOARD_VISIBLE_THRESHOLD_DP = 100;

    /**
     * Setup keyboard visibility listener to adjust content layout
     * @param activity The activity containing the layout
     * @param contentView The root content view of the activity
     * @param chatInputArea The input area view that should be visible when keyboard appears
     */
    public static void setUpKeyboardVisibilityListener(Activity activity, View contentView, View chatInputArea) {
        // Get the main FrameLayout of the window
        final FrameLayout rootLayout = (FrameLayout) contentView.getRootView();
        
        // Create and add a ViewTreeObserver to monitor layout changes
        rootLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            private final Rect r = new Rect();
            private int lastVisibleDecorHeight = 0;
            
            @Override
            public void onGlobalLayout() {
                // Get the height of the visible window
                rootLayout.getWindowVisibleDisplayFrame(r);
                int visibleDecorHeight = r.height();
                
                // If there's a significant change in height, the keyboard is likely visible/hidden
                if (lastVisibleDecorHeight != 0) {
                    if (lastVisibleDecorHeight > visibleDecorHeight) {
                        // Keyboard is now shown
                        int keyboardHeight = lastVisibleDecorHeight - visibleDecorHeight;
                        adjustForKeyboard(chatInputArea, keyboardHeight, true);
                    } else if (lastVisibleDecorHeight < visibleDecorHeight) {
                        // Keyboard is now hidden
                        adjustForKeyboard(chatInputArea, 0, false);
                    }
                }
                
                lastVisibleDecorHeight = visibleDecorHeight;
            }
        });
    }
    
    /**
     * Adjusts the position of the chat input area based on keyboard status
     * @param chatInputArea The input area view to adjust
     * @param keyboardHeight Height of the keyboard in pixels
     * @param keyboardVisible Whether the keyboard is visible or not
     */
    private static void adjustForKeyboard(View chatInputArea, int keyboardHeight, boolean keyboardVisible) {
        if (keyboardVisible && keyboardHeight > 0) {
            // When keyboard is visible, push the input area up above the keyboard
            chatInputArea.setTranslationY(-keyboardHeight);
        } else {
            // Reset the position when keyboard is hidden
            chatInputArea.setTranslationY(0);
        }
    }
}