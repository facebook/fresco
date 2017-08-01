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
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.ShowcaseFragment;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;
import java.util.Arrays;

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

  public static final String KEY_URI_OVERRIDE_HISTORY = "uri_override_previous";

  private UriOverrideDialog mSetUriOverrideDialog;

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
    private SharedPreferences mSharedPreferences;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      mImageUriProvider = ImageUriProvider.getInstance(getContext());
      mSharedPreferences  = PreferenceManager.getDefaultSharedPreferences(getContext());

      final View view = getActivity().getLayoutInflater().inflate(
          R.layout.dialog_fragment_uri_override,
          null);

      // setup edit text
      mEditText = view.findViewById(R.id.dialog_uri_override_edittext);
      final String previousUri = mImageUriProvider.getUriOverride();
      if (!TextUtils.isEmpty(previousUri)) {
        mEditText.setText(mImageUriProvider.getUriOverride());
      }

      // setup history text view
      final TextView historyTextView = view.findViewById(R.id.dialog_uri_override_history_textview);
      final String historyUri = mSharedPreferences.getString(KEY_URI_OVERRIDE_HISTORY, null);
      if (!TextUtils.isEmpty(historyUri)) {
        historyTextView.setText(
            getString(R.string.preference_uri_override_dialog_history, historyUri));
        historyTextView.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            mEditText.setText(historyUri);
          }
        });
        historyTextView.setVisibility(View.VISIBLE);
      }

      final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder.setMessage(R.string.preference_uri_override_dialog_message)
          .setView(view)
          .setPositiveButton(R.string.preference_uri_override_dialog_button_set, this)
          .setNeutralButton(R.string.preference_uri_override_dialog_button_cancel, null)
          .setNegativeButton(R.string.preference_uri_override_dialog_button_remove, this);
      return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
      final String oldUri = mImageUriProvider.getUriOverride();
      final String newUri = mEditText.getText().toString();
      switch (which) {
        case DialogInterface.BUTTON_POSITIVE:
          try {
            if (oldUri != null) {
              mSharedPreferences.edit()
                  .putString(KEY_URI_OVERRIDE_HISTORY, oldUri)
                  .apply();
            }
            mImageUriProvider.setUriOverride(newUri);
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
          if (oldUri != null) {
            mSharedPreferences.edit()
                .putString(KEY_URI_OVERRIDE_HISTORY, oldUri)
                .apply();
          }
          mImageUriProvider.setUriOverride(null);

          break;
      }
    }
  }
}
