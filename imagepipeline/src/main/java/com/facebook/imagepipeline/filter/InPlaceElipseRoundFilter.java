package com.facebook.imagepipeline.filter;

import android.graphics.Bitmap;
import com.facebook.common.internal.Preconditions;
import com.facebook.imageutils.BitmapUtil;

/**
 * Filter for rounding to elipse.
 */
public final class InPlaceElipseRoundFilter {

    private InPlaceElipseRoundFilter() {}

    /**
     * An implementation for rounding a given bitmap to a circular shape. The underlying
     * implementation uses a modified midpoint circle algorithm but instead of drawing a circle, it
     * clears all pixels starting from the circle all the way to the bitmap edges.
     *
     * @param bitmap The input {@link Bitmap}
     */
    public static void roundElipseBitmapInPlace(Bitmap bitmap) {
        Preconditions.checkNotNull(bitmap);
        final int w = bitmap.getWidth();
        final int h = bitmap.getHeight();

        boolean transparent = true;
        final int[] transparentColor = new int[w];

        final int[] pixels = new int[w * h];
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h);

        for (int y = -1*h/2; y < h/2; y++) {
            int x = -1*w/2;
            while ( ((float) 4*x*x)/(w*w) + ((float) 4*y*y)/(h*h) > 1 ) {
                x++;
            }

            System.arraycopy(transparentColor, 0, pixels, (y+h/2)*w, Math.max(0,(x+w/2)-1));

            while ( ((float) 4*x*x)/(w*w) + ((float) 4*y*y)/(h*h) <= 1 ) {
                x++;
            }

            System.arraycopy(transparentColor, 0, pixels, (y+h/2)*w + (x+w/2) - 1, Math.max(0, w - (x+w/2) + 1));
        }

        bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
    }
}

