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

  public final String dataSourceType;

  public final boolean infiniteDataSource;
  public final boolean distinctUriDataSource;

  public final String recyclerLayoutType;

  public final boolean reuseOldController;

  public final boolean useRoundedCorners;
  public final boolean useRoundedAsCircle;

  public final boolean usePostprocessor;
  public final String postprocessorType;

  public final String scaleType;

  public static Config load(final Context context) {
    final SharedPreferences sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context);
    final String dataSourceType = sharedPreferences.getString(
            Const.DATA_SOURCE_KEY,
            context.getString(R.string.value_local_uri));
    final boolean infiniteDataSource = sharedPreferences.getBoolean(
            Const.INFINITE_DATA_SOURCE_KEY,
            false);
    final boolean distinctUriDataSource = sharedPreferences.getBoolean(
            Const.DISTINCT_DATA_SOURCE_KEY,
            false);
    final String recyclerLayoutType = sharedPreferences.getString(
            Const.RECYCLER_LAYOUT_KEY,
            context.getString(R.string.value_recyclerview_recycler_layout));
    final boolean reuseOldController = sharedPreferences.getBoolean(
            Const.REUSE_OLD_CONTROLLER_KEY,
            false);
    final boolean useRoundedCorners = sharedPreferences.getBoolean(
            Const.ROUNDED_CORNERS_KEY,
            false);
    final boolean useRoundedAsCircle = sharedPreferences.getBoolean(
            Const.ROUNDED_AS_CIRCLE_KEY,
            false);
    final boolean usePostprocessor = sharedPreferences.getBoolean(
            Const.USE_POSTPROCESSOR_KEY,
            false);
    final String postprocessorType = sharedPreferences.getString(
            Const.POSTPROCESSOR_TYPE_KEY,
            context.getString(R.string.value_postprocessor_medium));
    final String scaleType = sharedPreferences.getString(
            Const.SCALE_TYPE_KEY,
            context.getString(R.string.value_scale_type_fit_center));
    return new Config(
      dataSourceType,
      recyclerLayoutType,
      infiniteDataSource,
      distinctUriDataSource,
      reuseOldController,
      useRoundedCorners,
      useRoundedAsCircle,
      usePostprocessor,
      postprocessorType,
      scaleType);
  }

  private Config(
      final String dataSourceType,
      final String recyclerLayoutType,
      final boolean infiniteDataSource,
      final boolean distinctUriDataSource,
      final boolean reuseOldController,
      final boolean useRoundedCorners,
      final boolean useRoundedAsCircle,
      final boolean usePostprocessor,
      final String postprocessorType,
      final String scaleType) {
    this.dataSourceType = dataSourceType;
    this.recyclerLayoutType = recyclerLayoutType;
    this.infiniteDataSource = infiniteDataSource;
    this.distinctUriDataSource = distinctUriDataSource;
    this.reuseOldController = reuseOldController;
    this.useRoundedCorners = useRoundedCorners;
    this.useRoundedAsCircle = useRoundedAsCircle;
    this.usePostprocessor = usePostprocessor;
    this.postprocessorType = postprocessorType;
    this.scaleType = scaleType;
  }
}
