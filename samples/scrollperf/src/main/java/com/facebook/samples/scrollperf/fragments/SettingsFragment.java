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
package com.facebook.samples.scrollperf.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import com.facebook.common.webp.WebpSupportStatus;
import com.facebook.samples.scrollperf.R;
import com.facebook.samples.scrollperf.conf.Const;
import com.facebook.samples.scrollperf.preferences.SizePreferences;
import com.facebook.samples.scrollperf.util.SizeUtil;

/**
 * The Fragment for settings
 */
public class SettingsFragment extends PreferenceFragmentCompat
    implements SharedPreferences.OnSharedPreferenceChangeListener {

  /**
   * The Tag for this Fragment
   */
  public static final String TAG = SettingsFragment.class.getSimpleName();

  private ShowRestartMessageDialog mShowRestartMessageDialog;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(false);
  }

  @Override
  public void onCreatePreferences(Bundle bundle, String s) {
    addPreferencesFromResource(R.xml.preferences);
    getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    // Update summaries
    updateDataSourceSummary(findPreference(Const.DATA_SOURCE_KEY));
    updateInfiniteDataSourceSummary(findPreference(Const.INFINITE_DATA_SOURCE_KEY));
    updateDistinctDataSourceSummary(findPreference(Const.DISTINCT_DATA_SOURCE_KEY));
    updateRecyclerLayoutSummary(findPreference(Const.RECYCLER_LAYOUT_KEY));
    updateReuseOldControllerSummary(findPreference(Const.REUSE_OLD_CONTROLLER_KEY));
    updateRoundedCornersSummary(findPreference(Const.ROUNDED_CORNERS_KEY));
    updateRoundedAsCircleSummary(findPreference(Const.ROUNDED_AS_CIRCLE_KEY));
    updateUsePostprocessorSummary(findPreference(Const.USE_POSTPROCESSOR_KEY));
    updateWhatPostprocessorSummary(findPreference(Const.POSTPROCESSOR_TYPE_KEY));
    updateWhatScaleTypeSummary(findPreference(Const.SCALE_TYPE_KEY));
    updateAutoRotateSummary(findPreference(Const.AUTO_ROTATE_KEY));
    updateRotationAngleSummary(findPreference(Const.FORCED_ROTATION_ANGLE_KEY));
    updateDownsamplingSummary(findPreference(Const.DOWNSAMPLING_KEY));
    updateOverrideSizeSummary(findPreference(Const.OVERRIDE_SIZE_KEY));
    updateDraweeOverlaySummary(findPreference(Const.DRAWEE_OVERLAY_KEY));
    updateBgColorSummary(findPreference(Const.BG_COLOR_KEY));
    updateInstrumentationSummary(findPreference(Const.INSTRUMENTATION_ENABLED_KEY));
    updateNumberOfDecodingThreadSummary(findPreference(Const.DECODING_THREAD_KEY));
    // Set sizes
    SizePreferences widthPreferences =
        (SizePreferences) findPreference(Const.OVERRIDEN_WIDTH_KEY);
    widthPreferences.setSeekBarMaxValue(SizeUtil.DISPLAY_WIDTH);
    SizePreferences heightPreferences =
        (SizePreferences) findPreference(Const.OVERRIDEN_HEIGHT_KEY);
    heightPreferences.setSeekBarMaxValue(SizeUtil.DISPLAY_HEIGHT);
    updateFadeDurationSummary(findPreference(Const.FADE_DURATION_KEY));
    updateDrawBorderSummary(findPreference(Const.DRAW_BORDER_KEY));
    updateDecodeCancellationSummary(findPreference(Const.DECODE_CANCELLATION_KEY));
    // This has no meaning for Android > JELLY_BEAN_MR1 because it already supports WebP
    if (WebpSupportStatus.sIsWebpSupportRequired) {
      updateWebpSupportSummary(findPreference(Const.WEBP_SUPPORT_KEY));
    } else {
      findPreference(Const.WEBP_SUPPORT_KEY).setVisible(false);
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    Preference preference = findPreference(key);
    switch (key) {
      case Const.DATA_SOURCE_KEY:
        updateDataSourceSummary(preference);
        break;
      case Const.RECYCLER_LAYOUT_KEY:
        updateRecyclerLayoutSummary(preference);
        break;
      case Const.GRID_SPAN_COUNT_KEY:
        updateGridRecyclerLayoutSummary();
        break;
      case Const.INFINITE_DATA_SOURCE_KEY:
        updateInfiniteDataSourceSummary(preference);
        break;
      case Const.DISTINCT_DATA_SOURCE_KEY:
        updateDistinctDataSourceSummary(preference);
        break;
      case Const.REUSE_OLD_CONTROLLER_KEY:
        updateReuseOldControllerSummary(preference);
        break;
      case Const.ROUNDED_CORNERS_KEY:
        updateRoundedCornersSummary(preference);
        break;
      case Const.ROUNDED_AS_CIRCLE_KEY:
        updateRoundedAsCircleSummary(preference);
        break;
      case Const.USE_POSTPROCESSOR_KEY:
        updateUsePostprocessorSummary(preference);
        break;
      case Const.POSTPROCESSOR_TYPE_KEY:
        updateWhatPostprocessorSummary(preference);
        break;
      case Const.SCALE_TYPE_KEY:
        updateWhatScaleTypeSummary(preference);
        break;
      case Const.AUTO_ROTATE_KEY:
        updateAutoRotateSummary(preference);
        break;
      case Const.FORCED_ROTATION_ANGLE_KEY:
        updateRotationAngleSummary(preference);
        break;
      case Const.DOWNSAMPLING_KEY:
        updateDownsamplingSummary(preference);
        getShowRestartMessageDialog().show(getChildFragmentManager(), null);
        break;
      case Const.WEBP_SUPPORT_KEY:
        updateWebpSupportSummary(preference);
        getShowRestartMessageDialog().show(getChildFragmentManager(), null);
        break;
      case Const.DECODING_THREAD_KEY:
        updateNumberOfDecodingThreadSummary(preference);
        getShowRestartMessageDialog().show(getChildFragmentManager(), null);
        break;
      case Const.INSTRUMENTATION_ENABLED_KEY:
        updateInstrumentationSummary(preference);
        break;
      case Const.DECODE_CANCELLATION_KEY:
        updateDecodeCancellationSummary(preference);
        getShowRestartMessageDialog().show(getChildFragmentManager(), null);
        break;
      case Const.DRAWEE_OVERLAY_KEY:
        updateDraweeOverlaySummary(preference);
        getShowRestartMessageDialog().show(getChildFragmentManager(), null);
        break;
      case Const.BG_COLOR_KEY:
        updateBgColorSummary(preference);
        break;
      case Const.OVERRIDE_SIZE_KEY:
        updateOverrideSizeSummary(preference);
        break;
      case Const.FADE_DURATION_KEY:
        updateFadeDurationSummary(preference);
        break;
      case Const.DRAW_BORDER_KEY:
        updateDrawBorderSummary(preference);
        break;
    }
  }

  private void updateDataSourceSummary(final Preference preference) {
    updateListPreference(
        getResources(),
        (ListPreference) preference,
        R.array.data_source_summaries);
  }

  private void updateInfiniteDataSourceSummary(final Preference preference) {
    final boolean currentState = updateCheckBoxPreference(
        getResources(),
        (CheckBoxPreference) preference,
        R.string.checked_infinite_data_source_summary,
        R.string.unchecked_infinite_data_source_summary);
    // We disableDistinct Uris if infinite is not enabled
    findPreference(Const.DISTINCT_DATA_SOURCE_KEY).setEnabled(currentState);
  }

  private void updateDistinctDataSourceSummary(final Preference preference) {
    updateCheckBoxPreference(
        getResources(),
        (CheckBoxPreference) preference,
        R.string.checked_distinct_uri_data_source_summary,
        R.string.unchecked_distinct_uri_data_source_summary);
  }

  private void updateRecyclerLayoutSummary(final Preference preference) {
    updateListPreference(
        getResources(),
        (ListPreference) preference,
        R.array.recycler_layout_summaries);
    updateGridRecyclerLayoutSummary();
  }

  private void updateGridRecyclerLayoutSummary() {
    final ListPreference listPreference =
        (ListPreference) findPreference(Const.RECYCLER_LAYOUT_KEY);
    // We have to enable the Grid settings only if the selection is the related on
    final ListPreference gridPreference =
        (ListPreference) findPreference(Const.GRID_SPAN_COUNT_KEY);
    final String value = listPreference.getValue();
    final boolean gridGroupVisible = Const.GRID_RECYCLER_VIEW_LAYOUT_VALUE.equals(value);
    // We update summary
    if (gridGroupVisible) {
      final String spanCountValue = gridPreference.getValue();
      gridPreference.setSummary(
          getString(R.string.label_grid_recycler_span_count_summary, spanCountValue));
    }
    gridPreference.setVisible(gridGroupVisible);
  }

  private void updateReuseOldControllerSummary(final Preference preference) {
    updateCheckBoxPreference(
        getResources(),
        (CheckBoxPreference) preference,
        R.string.checked_reuse_old_controller_summary,
        R.string.unchecked_reuse_old_controller_summary);
  }

  private void updateRoundedCornersSummary(final Preference preference) {
    updateCheckBoxPreference(
        getResources(),
        (CheckBoxPreference) preference,
        R.string.checked_rounded_corners_summary,
        R.string.unchecked_rounded_corners_summary);
  }

  private void updateRoundedAsCircleSummary(final Preference preference) {
    updateCheckBoxPreference(
        getResources(),
        (CheckBoxPreference) preference,
        R.string.checked_rounded_as_circle_summary,
        R.string.unchecked_rounded_as_circle_summary);
  }

  private void updateUsePostprocessorSummary(final Preference preference) {
    updateCheckBoxPreference(
        getResources(),
        (CheckBoxPreference) preference,
        R.string.checked_postprocessor_summary,
        R.string.unchecked_postprocessor_summary);
  }

  private void updateWhatPostprocessorSummary(final Preference preference) {
    updateListPreference(
        getResources(),
        (ListPreference) preference,
        R.array.postprocessor_summaries);
  }

  private void updateBgColorSummary(final Preference preference) {
    updateListPreference(
        getResources(),
        (ListPreference) preference,
        R.array.bg_color_summaries);
  }

  private void updateWhatScaleTypeSummary(final Preference preference) {
    updateListPreference(
        getResources(),
        (ListPreference) preference,
        R.array.scale_type_summaries);
  }

  private void updateDecodeCancellationSummary(final Preference preference) {
    updateCheckBoxPreference(
        getResources(),
        (CheckBoxPreference) preference,
        R.string.checked_decode_cancellation_summary,
        R.string.unchecked_decode_cancellation_summary);
  }

  private void updateWebpSupportSummary(final Preference preference) {
    updateCheckBoxPreference(
        getResources(),
        (CheckBoxPreference) preference,
        R.string.checked_webp_support_summary,
        R.string.unchecked_webp_support_summary);
  }

  private static boolean updateCheckBoxPreference(
      Resources resources,
      CheckBoxPreference preference,
      int checkedSummaryRes,
      int uncheckedSummaryRes) {
    final boolean checkboxState = preference.isChecked();
    if (checkboxState) {
      preference.setSummary(resources.getString(checkedSummaryRes));
    } else {
      preference.setSummary(resources.getString(uncheckedSummaryRes));
    }
    return checkboxState;
  }

  private static void updateListPreference(
      Resources resources,
      ListPreference preference,
      int arrayValuesId) {
    final int valueIndex = preference.findIndexOfValue(preference.getValue());
    final String summary = resources.getStringArray(arrayValuesId)[valueIndex];
    preference.setSummary(summary);
  }

  private void updateAutoRotateSummary(final Preference preference) {
    boolean currentState = updateCheckBoxPreference(
        getResources(),
        (CheckBoxPreference) preference,
        R.string.checked_auto_rotate_summary,
        R.string.unchecked_auto_rotate_summary);
    findPreference(Const.FORCED_ROTATION_ANGLE_KEY).setEnabled(!currentState);
  }

  private void updateRotationAngleSummary(final Preference preference) {
    updateListPreference(
        getResources(),
        (ListPreference) preference,
        R.array.rotation_angle_summaries);
  }

  private void updateDownsamplingSummary(final Preference preference) {
    updateCheckBoxPreference(
        getResources(),
        (CheckBoxPreference) preference,
        R.string.checked_downsampling_summary,
        R.string.unchecked_downsampling_summary);
  }

  private void updateNumberOfDecodingThreadSummary(final Preference preference) {
    final ListPreference listPreference = (ListPreference) preference;
    final int valueIndex = listPreference.findIndexOfValue(listPreference.getValue());
    String summary = getResources().getStringArray(R.array.decoding_thread_summaries)[valueIndex];
    if (valueIndex == 0) {
      summary += Const.NUMBER_OF_PROCESSORS;
    }
    preference.setSummary(summary);
  }

  private void updateDraweeOverlaySummary(final Preference preference) {
    updateCheckBoxPreference(
        getResources(),
        (CheckBoxPreference) preference,
        R.string.checked_drawee_overlay_summary,
        R.string.unchecked_drawee_overlay_summary);
  }

  private void updateInstrumentationSummary(final Preference preference) {
    updateCheckBoxPreference(
        getResources(),
        (CheckBoxPreference) preference,
        R.string.checked_instrumentation_summary,
        R.string.unchecked_instrumentation_summary);
  }

  private void updateOverrideSizeSummary(final Preference preference) {
    boolean currentState = updateCheckBoxPreference(
        getResources(),
        (CheckBoxPreference) preference,
        R.string.checked_auto_size_override,
        R.string.unchecked_auto_size_override);
    findPreference(Const.FORCED_ROTATION_ANGLE_KEY).setEnabled(!currentState);
  }

  private void updateFadeDurationSummary(final Preference preference) {
    updateListPreference(
        getResources(),
        (ListPreference) preference,
        R.array.fade_duration_summaries);
  }

  private void updateDrawBorderSummary(final Preference preference) {
    updateCheckBoxPreference(
        getResources(),
        (CheckBoxPreference) preference,
        R.string.checked_draw_border_summary,
        R.string.unchecked_draw_border_summary);
  }

  private ShowRestartMessageDialog getShowRestartMessageDialog() {
    if (mShowRestartMessageDialog == null) {
      mShowRestartMessageDialog = new ShowRestartMessageDialog();
    }
    return mShowRestartMessageDialog;
  }

  public static class ShowRestartMessageDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      // Use the Builder class for convenient dialog construction
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
