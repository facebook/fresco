/*
 * This file provided by Facebook is for non-commercial testing and evaluation
 * purposes only.  Facebook reserves all rights not expressly granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.facebook.samples.demo;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

/**
 * Shared preference helper to enable / disable the debug overlay
 */

public class DebugOverlayHelper {

  private static final String FRESCO_DEBUG_PREFS = "fresco_debug_prefs";
  private static final String FRESCO_DEBUG_PREF_OVERLAY_ENABLED = "debug_overlay_enabled";

  public static boolean isDebugOverlayEnabled(Context context) {
    return context.getSharedPreferences(FRESCO_DEBUG_PREFS, Context.MODE_PRIVATE)
        .getBoolean(FRESCO_DEBUG_PREF_OVERLAY_ENABLED, false);
  }

  public static void toggleDebugOverlayEnabled(Context context) {
    SharedPreferences sharedPreferences =
        context.getSharedPreferences(FRESCO_DEBUG_PREFS, Context.MODE_PRIVATE);
    sharedPreferences
        .edit()
        .putBoolean(
            FRESCO_DEBUG_PREF_OVERLAY_ENABLED,
            !sharedPreferences.getBoolean(FRESCO_DEBUG_PREF_OVERLAY_ENABLED, false))
        .apply();
  }

  public static void showRestartDialogFragment(AppCompatActivity activity) {
    new RestartDialogFragment().show(activity.getSupportFragmentManager(), "debug_restart");
  }

  public static class RestartDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      // Use the Builder class for convenient dialog construction
      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder.setMessage(R.string.debug_overlay_restart)
          .setPositiveButton(R.string.kill_app, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              System.exit(0);
            }
          })
      .setNegativeButton(R.string.later, null);
      return builder.create();
    }
  }
}
