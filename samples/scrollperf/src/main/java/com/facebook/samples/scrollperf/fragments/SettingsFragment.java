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

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.facebook.samples.scrollperf.R;
import com.facebook.samples.scrollperf.conf.Const;

/**
 * The Fragment for settings
 */
public class SettingsFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {

  /**
   * The Tag for this Fragment
   */
  public static final String TAG = SettingsFragment.class.getSimpleName();

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
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    Preference preference = findPreference(key);
    if (Const.DATA_SOURCE_KEY.equals(key)) {
      updateDataSourceSummary(preference);
    } else if (Const.RECYCLER_LAYOUT_KEY.equals(key)) {
      updateRecyclerLayoutSummary(preference);
    } else if (Const.INFINITE_DATA_SOURCE_KEY.equals(key)) {
      updateInfiniteDataSourceSummary(preference);
    } else if (Const.DISTINCT_DATA_SOURCE_KEY.equals(key)) {
      updateDistinctDataSourceSummary(preference);
    } else if (Const.REUSE_OLD_CONTROLLER_KEY.equals(key)) {
      updateReuseOldControllerSummary(preference);
    } else if (Const.ROUNDED_CORNERS_KEY.equals(key)) {
      updateRoundedCornersSummary(preference);
    } else if (Const.ROUNDED_AS_CIRCLE_KEY.equals(key)) {
      updateRoundedAsCircleSummary(preference);
    }
  }

  private void updateDataSourceSummary(final Preference preference) {
    ListPreference dataSourcePreference = (ListPreference) preference;
    final int valueIndex = dataSourcePreference.findIndexOfValue(dataSourcePreference.getValue());
    final String summary = getResources().getStringArray(R.array.data_source_summaries)[valueIndex];
    preference.setSummary(summary);
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
    ListPreference recyclerLayoutPreference = (ListPreference) preference;
    final int valueIndex = recyclerLayoutPreference
        .findIndexOfValue(recyclerLayoutPreference.getValue());
    final String summary = getResources()
        .getStringArray(R.array.recycler_layout_summaries)[valueIndex];
    preference.setSummary(summary);
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

  private void updateRoundedAsCircleSummary(final Preference preference) {
    CheckBoxPreference roundedAsCirclePreference = (CheckBoxPreference) preference;
    final boolean roundedAsCircle = roundedAsCirclePreference.isChecked();
    if (roundedAsCircle) {
      preference.setSummary(getResources().getString(R.string.checked_rounded_as_circle_enabled_summary));
    } else {
      preference.setSummary(getResources().getString(R.string.unchecked_rounded_as_circle_enabled_summary));
    }
  }
}
