/*
 *  Copyright (c) 2021 David Allison <davidallisongithub@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation; either version 3 of the License, or (at your option) any later
 *  version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ichi2.anki.reviewer;

import android.content.SharedPreferences;
import android.view.ViewConfiguration;

import com.ichi2.anki.AnkiDroidApp;
import com.ichi2.anki.cardviewer.Gesture;

import timber.log.Timber;

public class GestureMapper {

    private boolean mUseCornerTouch = true;
    private int mSwipeMinDistance = -1;
    private int mSwipeThresholdVelocity = -1;

    private static final int DEFAULT_SWIPE_MIN_DISTANCE;
    private static final int DEFAULT_SWIPE_THRESHOLD_VELOCITY;

    static {
        // Set good default values for swipe detection
        final ViewConfiguration vc = ViewConfiguration.get(AnkiDroidApp.getInstance());
        DEFAULT_SWIPE_MIN_DISTANCE = vc.getScaledPagingTouchSlop();
        DEFAULT_SWIPE_THRESHOLD_VELOCITY = vc.getScaledMinimumFlingVelocity();
    }

    public void init(SharedPreferences preferences) {
        int sensitivity = preferences.getInt("swipeSensitivity", 100);
        boolean useCornerTouch = preferences.getBoolean("gestureCornerTouch", false);
        if (sensitivity != 100) {
            float sens = 100.0f/sensitivity;
            mSwipeMinDistance = (int) (DEFAULT_SWIPE_MIN_DISTANCE * sens + 0.5f);
            mSwipeThresholdVelocity = (int) (DEFAULT_SWIPE_THRESHOLD_VELOCITY * sens  + 0.5f);
        } else {
            mSwipeMinDistance = DEFAULT_SWIPE_MIN_DISTANCE;
            mSwipeThresholdVelocity = DEFAULT_SWIPE_THRESHOLD_VELOCITY;
        }

        mUseCornerTouch = useCornerTouch;

    }

    public Gesture gesture(float dx, float dy, float velocityX, float velocityY,
                           boolean isSelecting, boolean isXScrolling, boolean isYScrolling) {
        try {
            if (Math.abs(dx) > Math.abs(dy)) {
                // horizontal swipe if moved further in x direction than y direction
                if (dx > mSwipeMinDistance
                        && Math.abs(velocityX) > mSwipeThresholdVelocity
                        && !isXScrolling && !isSelecting) {
                    return Gesture.SWIPE_RIGHT;
                } else if (dx < -mSwipeMinDistance
                        && Math.abs(velocityX) > mSwipeThresholdVelocity
                        && !isXScrolling && !isSelecting) {
                    return Gesture.SWIPE_LEFT;
                }
            } else {
                // otherwise vertical swipe
                if (dy > mSwipeMinDistance
                        && Math.abs(velocityY) > mSwipeThresholdVelocity
                        && !isYScrolling) {
                    return Gesture.SWIPE_DOWN;
                } else if (dy < -mSwipeMinDistance
                        && Math.abs(velocityY) > mSwipeThresholdVelocity
                        && !isYScrolling) {
                    return Gesture.SWIPE_UP;
                }
            }
        } catch (Exception e) {
            Timber.e(e, "onFling Exception");
        }

        return null;
    }


    public Gesture gesture(int height, int width, float posX, float posY) {
        if (width == 0 || height == 0) {
            return null;
        }

        Gesture gesture;
        if (mUseCornerTouch) {
            gesture = fromTapCorners(height, width, posX, posY);
        } else {
            gesture = fromTap(height, width, posX, posY);
        }

        return gesture;
    }

    private static Gesture fromTap(int height, int width, float posX, float posY) {
        boolean gestureIsRight = posY > height * (1 - posX / width);
        if (posX > posY / height * width) {
            if (gestureIsRight) {
                return Gesture.TAP_RIGHT;
            } else {
                return Gesture.TAP_TOP;
            }
        } else {
            if (gestureIsRight) {
                return Gesture.TAP_BOTTOM;
            } else {
                return Gesture.TAP_LEFT;
            }
        }
    }

    private static Gesture fromTapCorners(int height, int width, float posX, float posY) {

        double heightSegment = height / 3d;
        double widthSegment = width / 3d;

        TriState wSector = clamp(posX / widthSegment);
        TriState hSector = clamp(posY / heightSegment);

        switch (wSector) {
            case LOW:
                //left
                switch (hSector) {
                    case LOW:
                        return Gesture.TAP_TOP_LEFT;
                    case MID:
                        return Gesture.TAP_LEFT;
                    case HIGH:
                        return Gesture.TAP_BOTTOM_LEFT;
                }
                break;
            case MID:
                //center
                switch (hSector) {
                    case LOW:
                        return Gesture.TAP_TOP;
                    case MID:
                        return Gesture.TAP_CENTER;
                    case HIGH:
                        return Gesture.TAP_BOTTOM;
                }
                break;
            case HIGH:
                //Right
                switch (hSector) {
                    case LOW:
                        return Gesture.TAP_TOP_RIGHT;
                    case MID:
                        return Gesture.TAP_RIGHT;
                    case HIGH:
                        return Gesture.TAP_BOTTOM_RIGHT;
                }
                break;
            default:
                throw new IllegalArgumentException("illegal switch state");
        }
        throw new IllegalArgumentException("illegal switch state");
    }

    // clamps the value from LOW-MID-HIGH
    private static TriState clamp(double value) {
        double val = Math.floor(value);
        if (val >= 2) {
            return TriState.HIGH;
        }
        if (val < 1) {
            return TriState.LOW;
        }

        return TriState.MID;
    }

    private enum TriState {
        LOW,
        MID,
        HIGH
    }
}
