package com.facebook.imagepipeline.decoder;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.view.ContextThemeWrapper;

/**
 * Handles decoding Drawables with the v7 support library if present and falling back to the v4
 * support library otherwise.
 */
public final class DrawableDecoderCompat {
    private static volatile boolean shouldCallAppCompatResources = true;
    private DrawableDecoderCompat() {
        // Utility class.
    }

    /**
     * See {@code getDrawable(Context, int, Theme)}.
     */
    public static Drawable getDrawable(
            Context ourContext, Context targetContext, @DrawableRes int id) {
        return getDrawable(ourContext, targetContext, id, /*theme=*/ null);
    }

    /**
     * Loads a Drawable using {@link AppCompatResources} if available and {@link ResourcesCompat}
     * otherwise, depending on whether or not the v7 support library is included in the application.
     *
     * @param theme Used instead of the {@link Resources.Theme} returned from the given {@link Context} if
     * non-null when loading the {@link Drawable}.
     */
    public static Drawable getDrawable(
            Context ourContext, @DrawableRes int id, @Nullable Resources.Theme theme) {
        return getDrawable(ourContext, ourContext, id, theme);
    }

    private static Drawable getDrawable(
            Context ourContext, Context targetContext, @DrawableRes int id, @Nullable Resources.Theme theme) {
        try {
            // Race conditions may cause us to attempt to load using v7 more than once. That's ok since
            // this check is a modest optimization and the output will be correct anyway.
            if (shouldCallAppCompatResources) {
                return loadDrawableV7(targetContext, id, theme);
            }
        } catch (NoClassDefFoundError error) {
            shouldCallAppCompatResources = false;
        } catch (IllegalStateException e) {
            if (ourContext.getPackageName().equals(targetContext.getPackageName())) {
                throw e;
            }
            return ContextCompat.getDrawable(targetContext, id);
        } catch (Resources.NotFoundException e) {
            // Ignored, this can be thrown when drawable compat attempts to decode a canary resource. If
            // that decode attempt fails, we still want to try with the v4 ResourcesCompat below.
        }

        return loadDrawableV4(targetContext, id, theme != null ? theme : targetContext.getTheme());
    }

    private static Drawable loadDrawableV7(Context context, @DrawableRes int id,
                                           @Nullable Resources.Theme theme) {
        Context resourceContext = theme != null ? new ContextThemeWrapper(context, theme) : context;
        return AppCompatResources.getDrawable(resourceContext, id);
    }

    private static Drawable loadDrawableV4(
            Context context, @DrawableRes int id, @Nullable Resources.Theme theme) {
        Resources resources = context.getResources();
        return ResourcesCompat.getDrawable(resources, id, theme);
    }
}
