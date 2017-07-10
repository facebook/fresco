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

import java.util.Arrays;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.widget.EditText;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.ShowcaseFragment;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;

/**
 * The Fragment for settings
 */
public class SettingsFragment extends PreferenceFragmentCompat
    implements SharedPreferences.OnSharedPreferenceChangeListener, ShowcaseFragment {

  private static final String TAG = SettingsFragment.class.getSimpleName();

  public static final String KEY_CLEAR_DISK_CACHE = "clear_disk_cache";
  public static final String KEY_DEBUG_OVERLAY = "debug_overlay";
  public static final String KEY_URI_OVERRIDE = "uri_override";

  public static final String KEY_DETAILS_ANDROID_VERSION = "android_version";
  public static final String KEY_DETAILS_CPU_ARCHITECTURE = "cpu_architecture";
  public static final String KEY_DETAILS_DEVICE_NAME = "device_name";

  private UriOverrideDialog mSetUriOverrideDialog;
  private ShowRestartMessageDialog mShowRestartMessageDialog;

  private ImageUriProvider mImageUriProvider;

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if (!isAdded()) {
      return;
    }
    switch (key) {
      case KEY_URI_OVERRIDE:
        populateUriOverride();
    }
  }

  @Override
  public boolean onPreferenceTreeClick(Preference preference) {
    switch (preference.getKey()) {
      case KEY_CLEAR_DISK_CACHE:
        onClearDiskCachePreferenceClicked();
        return true;
      case KEY_URI_OVERRIDE:
        onUriOverrideClicked();
        return true;
      default:
        return super.onPreferenceTreeClick(preference);
    }
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    mImageUriProvider = ImageUriProvider.getInstance(getContext());
    addPreferencesFromResource(R.xml.preferences);
    getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    populateUriOverride();
    populateVersionAndDeviceDetails();
  }

  private void onClearDiskCachePreferenceClicked() {
    Fresco.getImagePipeline().clearCaches();
    showToastText(getString(R.string.preference_cache_clear_cache_message_success));
  }

  private void onUriOverrideClicked() {
    if (mSetUriOverrideDialog == null) {
      mSetUriOverrideDialog = new UriOverrideDialog();
    }
    mSetUriOverrideDialog.show(getFragmentManager(), "uri_override");
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

  private void populateUriOverride() {
    final String uri = mImageUriProvider.getUriOverride();
    final Preference preferenceUriOverride = findPreference(KEY_URI_OVERRIDE);
    if (uri == null || uri.isEmpty()) {
      preferenceUriOverride.setSummary(R.string.preference_uri_override_summary_none);
    } else {
      preferenceUriOverride.setSummary(
          getString(
              R.string.preference_uri_override_summary_given,
              uri));
    }
  }

  private void populateVersionAndDeviceDetails() {
    final Preference preferenceAndroidVersion = findPreference(KEY_DETAILS_ANDROID_VERSION);
    final String androidVersion = getString(
        R.string.preference_details_android_version_summary,
        String.valueOf(Build.VERSION.SDK_INT),
        Build.VERSION.RELEASE);
    preferenceAndroidVersion.setSummary(androidVersion);

    final Preference preferenceCpuArchitecture = findPreference(KEY_DETAILS_CPU_ARCHITECTURE);
    final String cpuArch = System.getProperty("os.arch");

    final String cpuDetails;
    if (Build.VERSION.SDK_INT < 21) {
      cpuDetails = getString(
          R.string.preference_details_cpu_architecture_summary_before_21,
          cpuArch,
          Build.CPU_ABI,
          Build.CPU_ABI2);
    } else {
      cpuDetails = getString(
          R.string.preference_details_cpu_architecture_summary_after_21,
          cpuArch,
          Arrays.toString(Build.SUPPORTED_ABIS));
    }
    preferenceCpuArchitecture.setSummary(cpuDetails);

    final Preference preferenceDeviceName = findPreference(KEY_DETAILS_DEVICE_NAME);
    final String deviceName = getString(
        R.string.preference_details_device_name_summary,
        Build.MANUFACTURER,
        Build.DEVICE);
    preferenceDeviceName.setSummary(deviceName);
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
   * Dialog asking for a new URI override
   */
  public static class UriOverrideDialog extends DialogFragment
      implements DialogInterface.OnClickListener {

    private ImageUriProvider mImageUriProvider;
    private EditText mEditText;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      mImageUriProvider = ImageUriProvider.getInstance(getContext());
      mEditText = new EditText(getContext());

      final String previousUri = mImageUriProvider.getUriOverride();
      if (previousUri != null && !previousUri.isEmpty()) {
        mEditText.setText(mImageUriProvider.getUriOverride());
      }
      mEditText.setHint(R.string.preference_uri_override_dialog_edittext_hint);

      final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder.setMessage(R.string.preference_uri_override_dialog_message)
          .setView(mEditText)
          .setPositiveButton(R.string.preference_uri_override_dialog_button_set, this)
          .setNeutralButton(R.string.preference_uri_override_dialog_button_cancel, null)
          .setNegativeButton(R.string.preference_uri_override_dialog_button_remove, this);
      return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
      switch (which) {
        case DialogInterface.BUTTON_POSITIVE:
          try {
            mImageUriProvider.setUriOverride(mEditText.getText().toString());
          } catch (IllegalArgumentException e) {
            Toast.makeText(
                getContext(),
                getString(
                    R.string.preference_uri_override_dialog_edittext_error_bad_uri,
                    e.getMessage()),
                Toast.LENGTH_SHORT).show();
          }
          break;
        case DialogInterface.BUTTON_NEGATIVE:
          mImageUriProvider.setUriOverride(null);
          break;
      }
    }
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
