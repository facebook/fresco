package com.facebook.imagepipeline.filter;

import android.graphics.Bitmap;
import com.facebook.common.internal.Preconditions;
import com.facebook.imageutils.BitmapUtil;

/**
 * Filter for rounding to ellipse.
 */
public final class InPlaceEllipseRoundFilter {

    private InPlaceEllipseRoundFilter() {}

    /**
     * An implementation for rounding a given bitmap to an ellipse shape.
     *
     * @param bitmap The input {@link Bitmap}
     */
    public static void roundEllipseBitmapInPlace(Bitmap bitmap) {
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

