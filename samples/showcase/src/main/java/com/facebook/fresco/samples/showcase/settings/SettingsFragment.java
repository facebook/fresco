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
package com.facebook.fresco.samples.showcase.settings;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.facebook.fresco.samples.showcase.R;

import static android.support.v7.preference.R.styleable.PreferenceFragmentCompat;

/**
 * The Fragment for settings
 */
public class SettingsFragment extends PreferenceFragmentCompat
    implements SharedPreferences.OnSharedPreferenceChangeListener {

  /**
   * The Tag for this Fragment
   */
  public static final String TAG = SettingsFragment.class.getSimpleName();

  /**
   * The Dialog for asking the restart for the application
   */
  private ShowRestartMessageDialog mShowRestartMessageDialog;

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    addPreferencesFromResource(R.xml.preferences);
    getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
  }

  /**
   * @return The reference to the Dialog for asking restart
   */
  private ShowRestartMessageDialog getShowRestartMessageDialog() {
    if (mShowRestartMessageDialog == null) {
      mShowRestartMessageDialog = new ShowRestartMessageDialog();
    }
    return mShowRestartMessageDialog;
  }

  /**
   * Dialog to show when we ask to restart the application after a change
   */
  public static class ShowRestartMessageDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder.setMessage(R.string.message_application_needs_restart)
          .setPositiveButton(android.R.string.ok, null)
          .setNeutralButton(R.string.message_restart_now, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              System.exit(0);
            }
          });
      return builder.create();
    }
  }
}
