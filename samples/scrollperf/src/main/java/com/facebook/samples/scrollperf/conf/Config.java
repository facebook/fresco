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
package com.facebook.samples.scrollperf.conf;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

import com.facebook.samples.scrollperf.R;

/**
 * We use this class to keep in memory all the information from the Settings. It's a kind of
 * buffer of those information in order to avoid repeated reading
 */
public class Config {

  public final String mDataSourceType;

  public final boolean mInfiniteDataSource;
  public final boolean mDistinctUriDataSource;

  public final String mRecyclerLayoutType;

  public final boolean mReuseOldController;

  public static Config load(final Context context) {
    // We read the DataSource type
    final SharedPreferences sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context);
    // Data Source key
    final String dataSourceKey = context.getString(R.string.key_data_source);
    final String dataSourceType = sharedPreferences.getString(
            dataSourceKey,
            context.getString(R.string.value_local_uri));
    // Infinite Data Source
    final String infiniteDataSourceKey = context.getString(R.string.key_infinite_data_source);
    final boolean infiniteDataSource = sharedPreferences.getBoolean(
            infiniteDataSourceKey,
            false);
    // Distinct Uri
    final String distinctUriDataSourceKey = context
      .getString(R.string.key_distinct_uri_data_source);
    final boolean distinctUriDataSource = sharedPreferences.getBoolean(
            distinctUriDataSourceKey,
            false);
    // The Recycler Layout key
    final String recyclerLayoutKey = context.getString(R.string.key_recycler_layout);
    final String recyclerLayoutType = sharedPreferences.getString(
            recyclerLayoutKey,
            context.getString(R.string.value_listview_recycler_layout));
    // Reuse Old Controller
    final String reuseOldControllerKey = context.getString(R.string.key_reuse_old_controller);
    final boolean reuseOldController = sharedPreferences.getBoolean(
            reuseOldControllerKey,
            false);
    return new Config(
      dataSourceType,
      recyclerLayoutType,
      infiniteDataSource,
      distinctUriDataSource,
      reuseOldController);
  }

  private Config(
      final String dataSourceType,
      final String recyclerLayoutType,
      final boolean infiniteDataSource,
      final boolean distinctUriDataSource,
      final boolean reuseOldController
      ) {
    mDataSourceType = dataSourceType;
    mRecyclerLayoutType = recyclerLayoutType;
    mInfiniteDataSource = infiniteDataSource;
    mDistinctUriDataSource = distinctUriDataSource;
    mReuseOldController = reuseOldController;
  }
}
