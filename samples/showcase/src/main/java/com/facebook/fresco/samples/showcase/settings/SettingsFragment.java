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
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.fresco.samples.showcase.ShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;

import static android.support.v7.preference.R.styleable.PreferenceFragmentCompat;

/**
 * The Fragment for settings
 */
public class SettingsFragment extends PreferenceFragmentCompat
    implements SharedPreferences.OnSharedPreferenceChangeListener, ShowcaseFragment {

  private static final String TAG = SettingsFragment.class.getSimpleName();

  private static final String KEY_CLEAR_DISK_CACHE = "clear_disk_cache";

  /**
   * The Dialog for asking the restart for the application
   */
  private ShowRestartMessageDialog mShowRestartMessageDialog;

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

  }

  @Override
  public boolean onPreferenceTreeClick(Preference preference) {
    switch (preference.getKey()) {
      case KEY_CLEAR_DISK_CACHE:
        onClearDiskCachePreferenceClicked();
        return true;
      default:
        return super.onPreferenceTreeClick(preference);
    }
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    addPreferencesFromResource(R.xml.preferences);
    getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
  }

  private void onClearDiskCachePreferenceClicked() {
    Fresco.getImagePipeline().clearCaches();
    showToastText(getString(R.string.preference_cache_clear_cache_message_success));
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

  private void showToastText(String text) {
    Toast.makeText(getContext(), text, Toast.LENGTH_SHORT).show();
  }

  @Nullable
  @Override
  public String getBackstackTag() {
    return TAG;
  }

  @Override
  public int getTitleId() {
    return R.string.action_settings;
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
