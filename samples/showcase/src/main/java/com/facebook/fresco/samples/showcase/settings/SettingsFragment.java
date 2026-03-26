/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.settings;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.ShowcaseApplication;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;
import java.util.Arrays;

/** The Fragment for settings */
public class SettingsFragment extends PreferenceFragmentCompat
    implements SharedPreferences.OnSharedPreferenceChangeListener {

  private static final String TAG = SettingsFragment.class.getSimpleName();

  public static final String KEY_CLEAR_DISK_CACHE = "clear_disk_cache";
  public static final String KEY_DEBUG_OVERLAY = "debug_overlay";
  public static final String KEY_URI_OVERRIDE = "uri_override";
  public static final String KEY_VITO_KOTLIN = "vito_use_kotlin";

  public static final String KEY_DETAILS_ANDROID_VERSION = "android_version";
  public static final String KEY_DETAILS_CPU_ARCHITECTURE = "cpu_architecture";
  public static final String KEY_DETAILS_DEVICE_NAME = "device_name";

  public static final String KEY_URI_OVERRIDE_HISTORY = "uri_override_previous";

  private @Nullable UriOverrideDialog mSetUriOverrideDialog;

  private @Nullable ImageUriProvider mImageUriProvider;

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
    final String key = preference.getKey();
    if (key == null) {
      return super.onPreferenceTreeClick(preference);
    }
    switch (key) {
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
    mImageUriProvider = ShowcaseApplication.Companion.getImageUriProvider();
    addPreferencesFromResource(R.xml.preferences);
    androidx.preference.PreferenceManager prefManager = getPreferenceManager();
    if (prefManager != null) {
      SharedPreferences sharedPreferences = prefManager.getSharedPreferences();
      if (sharedPreferences != null) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
      }
    }
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
    FragmentManager fm = getParentFragmentManager();
    mSetUriOverrideDialog.show(fm, "uri_override");
  }

  private void populateUriOverride() {
    if (mImageUriProvider == null) {
      return;
    }
    final String uri = mImageUriProvider.getUriOverride();
    final Preference preferenceUriOverride = findPreference(KEY_URI_OVERRIDE);
    if (preferenceUriOverride == null) {
      return;
    }
    if (uri == null || uri.isEmpty()) {
      preferenceUriOverride.setSummary(R.string.preference_uri_override_summary_none);
    } else {
      preferenceUriOverride.setSummary(
          getString(R.string.preference_uri_override_summary_given, uri));
    }
  }

  private void populateVersionAndDeviceDetails() {
    final Preference preferenceAndroidVersion = findPreference(KEY_DETAILS_ANDROID_VERSION);
    final String androidVersion =
        getString(
            R.string.preference_details_android_version_summary,
            String.valueOf(Build.VERSION.SDK_INT),
            Build.VERSION.RELEASE);
    if (preferenceAndroidVersion != null) {
      preferenceAndroidVersion.setSummary(androidVersion);
    }

    final Preference preferenceCpuArchitecture = findPreference(KEY_DETAILS_CPU_ARCHITECTURE);
    final String cpuArch = System.getProperty("os.arch");

    final String cpuDetails =
        getString(
            R.string.preference_details_cpu_architecture_summary_after_21,
            cpuArch,
            Arrays.toString(Build.SUPPORTED_ABIS));
    if (preferenceCpuArchitecture != null) {
      preferenceCpuArchitecture.setSummary(cpuDetails);
    }

    final Preference preferenceDeviceName = findPreference(KEY_DETAILS_DEVICE_NAME);
    final String deviceName =
        getString(
            R.string.preference_details_device_name_summary, Build.MANUFACTURER, Build.DEVICE);
    if (preferenceDeviceName != null) {
      preferenceDeviceName.setSummary(deviceName);
    }
  }

  private void showToastText(String text) {
    Context context = getContext();
    if (context != null) {
      Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }
  }

  /** Dialog asking for a new URI override */
  public static class UriOverrideDialog extends DialogFragment
      implements DialogInterface.OnClickListener {

    private @Nullable ImageUriProvider mImageUriProvider;
    private @Nullable EditText mEditText;
    private @Nullable SharedPreferences mSharedPreferences;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      mImageUriProvider = ShowcaseApplication.Companion.getImageUriProvider();
      Context context = getContext();
      if (context != null) {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
      }

      FragmentActivity activity = getActivity();
      if (activity == null) {
        return new AlertDialog.Builder(requireContext()).create();
      }

      final View view =
          activity.getLayoutInflater().inflate(R.layout.dialog_fragment_uri_override, null);

      // setup edit text
      mEditText = view.findViewById(R.id.dialog_uri_override_edittext);
      if (mImageUriProvider != null) {
        final String previousUri = mImageUriProvider.getUriOverride();
        if (!TextUtils.isEmpty(previousUri) && mEditText != null) {
          mEditText.setText(mImageUriProvider.getUriOverride());
        }
      }

      // setup history text view
      final TextView historyTextView = view.findViewById(R.id.dialog_uri_override_history_textview);
      final String historyUri =
          mSharedPreferences != null
              ? mSharedPreferences.getString(KEY_URI_OVERRIDE_HISTORY, null)
              : null;
      if (!TextUtils.isEmpty(historyUri) && historyTextView != null) {
        historyTextView.setText(
            getString(R.string.preference_uri_override_dialog_history, historyUri));
        historyTextView.setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View view) {
                if (mEditText != null) {
                  mEditText.setText(historyUri);
                }
              }
            });
        historyTextView.setVisibility(View.VISIBLE);
      }

      final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
      builder
          .setMessage(R.string.preference_uri_override_dialog_message)
          .setView(view)
          .setPositiveButton(R.string.preference_uri_override_dialog_button_set, this)
          .setNeutralButton(R.string.preference_uri_override_dialog_button_cancel, null)
          .setNegativeButton(R.string.preference_uri_override_dialog_button_remove, this);
      return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
      if (mImageUriProvider == null || mEditText == null) {
        return;
      }
      final String oldUri = mImageUriProvider.getUriOverride();
      final String newUri = mEditText.getText().toString();
      switch (which) {
        case DialogInterface.BUTTON_POSITIVE:
          try {
            if (oldUri != null && mSharedPreferences != null) {
              mSharedPreferences.edit().putString(KEY_URI_OVERRIDE_HISTORY, oldUri).apply();
            }
            mImageUriProvider.setUriOverride(newUri);
          } catch (IllegalArgumentException e) {
            Context context = getContext();
            if (context != null) {
              Toast.makeText(
                      context,
                      getString(
                          R.string.preference_uri_override_dialog_edittext_error_bad_uri,
                          e.getMessage()),
                      Toast.LENGTH_SHORT)
                  .show();
            }
          }
          break;
        case DialogInterface.BUTTON_NEGATIVE:
          if (oldUri != null && mSharedPreferences != null) {
            mSharedPreferences.edit().putString(KEY_URI_OVERRIDE_HISTORY, oldUri).apply();
          }
          mImageUriProvider.setUriOverride(null);

          break;
      }
    }
  }
}
