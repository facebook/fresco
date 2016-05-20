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
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.facebook.samples.scrollperf.R;

/**
 * The Fragment for settings
 */
public class SettingsFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {

  /**
   * The Tag for this Fragment
   */
  public static final String TAG = SettingsFragment.class.getSimpleName();

  private String mDataSourceKey;
  private String mRecyclerLayoutKey;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(false);
  }

  @Override
  public void onCreatePreferences(Bundle bundle, String s) {
    addPreferencesFromResource(R.xml.preferences);
    getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    // We read the keys
    mDataSourceKey = getString(R.string.key_data_source);
    mRecyclerLayoutKey = getString(R.string.key_recycler_layout);
    // Update summaries
    updateDataSourceSummary(findPreference(mDataSourceKey));
    updateRecyclerLayoutSummary(findPreference(mRecyclerLayoutKey));
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    Preference preference = findPreference(key);
    if (mDataSourceKey.equals(key)) {
      updateDataSourceSummary(preference);
    } else if (mRecyclerLayoutKey.equals(key)) {
      updateRecyclerLayoutSummary(preference);
    }
  }

  private void updateDataSourceSummary(final Preference preference) {
    ListPreference dataSourcePreference = (ListPreference) preference;
    final int valueIndex = dataSourcePreference.findIndexOfValue(dataSourcePreference.getValue());
    final String summary = getResources().getStringArray(R.array.data_source_summaries)[valueIndex];
    preference.setSummary(summary);
  }

  private void updateRecyclerLayoutSummary(final Preference preference) {
    ListPreference recyclerLayoutPreference = (ListPreference) preference;
    final int valueIndex = recyclerLayoutPreference
        .findIndexOfValue(recyclerLayoutPreference.getValue());
    final String summary = getResources()
        .getStringArray(R.array.recycler_layout_summaries)[valueIndex];
    preference.setSummary(summary);
  }
}
