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
import com.facebook.samples.scrollperf.util.SizeUtil;

/**
 * We use this class to keep in memory all the information from the Settings. It's a kind of buffer
 * of those information in order to avoid repeated reading
 */
public class Config {

  public final String dataSourceType;

  public final boolean infiniteDataSource;
  public final boolean distinctUriDataSource;

  public final String recyclerLayoutType;
  public final int gridSpanCount;

  public final boolean reuseOldController;

  public final boolean useRoundedCorners;
  public final boolean useRoundedAsCircle;

  public final boolean usePostprocessor;
  public final String postprocessorType;

  public final String scaleType;

  public final boolean rotateUsingMetaData;
  public final int forcedRotationAngle;

  public final boolean downsampling;

  public final boolean overrideSize;
  public final int overridenWidth;
  public final int overridenHeight;

  public final int fadeDurationMs;

  public final boolean decodeCancellation;
  public final boolean webpSupportEnabled;

  public final int decodingThreadCount;
  public final int bgColor;

  public final boolean drawBorder;

  public final boolean draweeOverlayEnabled;
  public final boolean instrumentationEnabled;

  public static Config load(final Context context) {
    final SharedPreferences sharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context);
    return Builder.newBuilder()
        .setDataSourceType(sharedPreferences.getString(
            Const.DATA_SOURCE_KEY,
            context.getString(R.string.value_local_uri)))
        .setInfiniteDataSource(sharedPreferences.getBoolean(
            Const.INFINITE_DATA_SOURCE_KEY,
            false))
        .setDistinctUriDataSource(sharedPreferences.getBoolean(
            Const.DISTINCT_DATA_SOURCE_KEY,
            false))
        .setRecyclerLayoutType(sharedPreferences.getString(
            Const.RECYCLER_LAYOUT_KEY,
            context.getString(R.string.value_recyclerview_recycler_layout)))
        .setReuseOldController(sharedPreferences.getBoolean(
            Const.REUSE_OLD_CONTROLLER_KEY,
            false))
        .setUseRoundedCorners(sharedPreferences.getBoolean(
            Const.ROUNDED_CORNERS_KEY,
            false))
        .setUseRoundedAsCircle(sharedPreferences.getBoolean(
            Const.ROUNDED_AS_CIRCLE_KEY,
            false))
        .setUsePostprocessor(sharedPreferences.getBoolean(
            Const.USE_POSTPROCESSOR_KEY,
            false))
        .setPostprocessorType(sharedPreferences.getString(
            Const.POSTPROCESSOR_TYPE_KEY,
            context.getString(R.string.value_postprocessor_medium)))
        .setScaleType(sharedPreferences.getString(
            Const.SCALE_TYPE_KEY,
            context.getString(R.string.value_scale_type_fit_center)))
        .setRotateUsingMetaData(sharedPreferences.getBoolean(
            Const.AUTO_ROTATE_KEY,
            false))
        .setForcedRotationAngle(Integer.parseInt(sharedPreferences.getString(
            Const.FORCED_ROTATION_ANGLE_KEY,
            "0")))
        .setDownsampling(sharedPreferences.getBoolean(
            Const.DOWNSAMPLING_KEY,
            false))
        .setOverrideSize(sharedPreferences.getBoolean(
            Const.OVERRIDE_SIZE_KEY,
            false))
        .setOverridenWidth(sharedPreferences.getInt(
            Const.OVERRIDEN_WIDTH_KEY,
            SizeUtil.DISPLAY_WIDTH / 2))
        .setOverridenHeight(sharedPreferences.getInt(
            Const.OVERRIDEN_HEIGHT_KEY,
            SizeUtil.DISPLAY_HEIGHT / 2))
        .setFadeDurationMs(Integer.parseInt(sharedPreferences.getString(
            Const.FADE_DURATION_KEY,
            context.getString(R.string.value_fast_fade_duration))))
        .setDrawBorder(sharedPreferences.getBoolean(
            Const.DRAW_BORDER_KEY,
            false))
        .setGridSpanCount(Integer.parseInt(sharedPreferences.getString(
            Const.GRID_SPAN_COUNT_KEY,
            "3")))
        .setDecodeCancellation(sharedPreferences.getBoolean(
            Const.DECODE_CANCELLATION_KEY,
            false))
        .setWebpSupportEnabled(sharedPreferences.getBoolean(
            Const.WEBP_SUPPORT_KEY,
            false))
        .setDraweeOverlayEnabled(sharedPreferences.getBoolean(
            Const.DRAWEE_OVERLAY_KEY,
            false))
        .setInstrumentationEnabled(sharedPreferences.getBoolean(
            Const.INSTRUMENTATION_ENABLED_KEY,
            false))
        .setDecodingThreadCount(Integer.parseInt(sharedPreferences.getString(
            Const.DECODING_THREAD_KEY,
            "0")))
        .setBgColor(Integer.parseInt(sharedPreferences.getString(
            Const.BG_COLOR_KEY,
            "0")))
        .build();
  }

  private Config(Builder builder) {
    this.dataSourceType = builder.mDataSourceType;
    this.recyclerLayoutType = builder.mRecyclerLayoutType;
    this.gridSpanCount = builder.mGridSpanCount;
    this.infiniteDataSource = builder.mInfiniteDataSource;
    this.distinctUriDataSource = builder.mDistinctUriDataSource;
    this.reuseOldController = builder.mReuseOldController;
    this.useRoundedCorners = builder.mUseRoundedCorners;
    this.useRoundedAsCircle = builder.mUseRoundedAsCircle;
    this.usePostprocessor = builder.mUsePostprocessor;
    this.postprocessorType = builder.mPostprocessorType;
    this.scaleType = builder.mScaleType;
    this.rotateUsingMetaData = builder.mRotateUsingMetaData;
    this.forcedRotationAngle = builder.mForcedRotationAngle;
    this.downsampling = builder.mDownsampling;
    this.overrideSize = builder.mOverrideSize;
    this.overridenWidth = builder.mOverridenWidth;
    this.overridenHeight = builder.mOverridenHeight;
    this.fadeDurationMs = builder.mFadeDurationMs;
    this.drawBorder = builder.mDrawBorder;
    this.decodeCancellation = builder.mDecodeCancellation;
    this.webpSupportEnabled = builder.mWebpSupportEnabled;
    this.draweeOverlayEnabled = builder.mDraweeOverlayEnabled;
    this.instrumentationEnabled = builder.mInstrumentationEnabled;
    this.decodingThreadCount = builder.mDecodingThreadCount;
    this.bgColor = builder.mBgColor;
  }

  public static class Builder {

    private String mDataSourceType;
    private boolean mInfiniteDataSource;
    private boolean mDistinctUriDataSource;
    private String mRecyclerLayoutType;
    private int mGridSpanCount;
    private boolean mReuseOldController;
    private boolean mUseRoundedCorners;
    private boolean mUseRoundedAsCircle;
    private boolean mUsePostprocessor;
    private String mPostprocessorType;
    private String mScaleType;
    private boolean mRotateUsingMetaData;
    private int mForcedRotationAngle;
    private boolean mDownsampling;
    private boolean mOverrideSize;
    private int mOverridenWidth;
    private int mOverridenHeight;
    private int mFadeDurationMs;
    private boolean mDecodeCancellation;
    private boolean mWebpSupportEnabled;
    private boolean mDrawBorder;
    private boolean mDraweeOverlayEnabled;
    private boolean mInstrumentationEnabled;
    private int mDecodingThreadCount;
    private int mBgColor;

    private Builder() {
    }

    public static Builder newBuilder() {
      return new Builder();
    }

    public Builder setDataSourceType(String dataSourceType) {
      this.mDataSourceType = dataSourceType;
      return this;
    }

    public Builder setInfiniteDataSource(boolean infiniteDataSource) {
      this.mInfiniteDataSource = infiniteDataSource;
      return this;
    }

    public Builder setDistinctUriDataSource(boolean distinctUriDataSource) {
      this.mDistinctUriDataSource = distinctUriDataSource;
      return this;
    }

    public Builder setRecyclerLayoutType(String recyclerLayoutType) {
      this.mRecyclerLayoutType = recyclerLayoutType;
      return this;
    }

    public Builder setGridSpanCount(int gridSpanCount) {
      this.mGridSpanCount = gridSpanCount;
      return this;
    }

    public Builder setReuseOldController(boolean reuseOldController) {
      this.mReuseOldController = reuseOldController;
      return this;
    }

    public Builder setUseRoundedCorners(boolean useRoundedCorners) {
      this.mUseRoundedCorners = useRoundedCorners;
      return this;
    }

    public Builder setUseRoundedAsCircle(boolean useRoundedAsCircle) {
      this.mUseRoundedAsCircle = useRoundedAsCircle;
      return this;
    }

    public Builder setUsePostprocessor(boolean usePostprocessor) {
      this.mUsePostprocessor = usePostprocessor;
      return this;
    }

    public Builder setPostprocessorType(String postprocessorType) {
      this.mPostprocessorType = postprocessorType;
      return this;
    }

    public Builder setScaleType(String scaleType) {
      this.mScaleType = scaleType;
      return this;
    }

    public Builder setRotateUsingMetaData(boolean rotateUsingMetaData) {
      this.mRotateUsingMetaData = rotateUsingMetaData;
      return this;
    }

    public Builder setForcedRotationAngle(int forcedRotationAngle) {
      this.mForcedRotationAngle = forcedRotationAngle;
      return this;
    }

    public Builder setDecodingThreadCount(int decodingThreadCount) {
      this.mDecodingThreadCount = decodingThreadCount;
      return this;
    }

    public Builder setDownsampling(boolean downsampling) {
      this.mDownsampling = downsampling;
      return this;
    }

    public Builder setOverrideSize(boolean overrideSize) {
      this.mOverrideSize = overrideSize;
      return this;
    }

    public Builder setOverridenWidth(int overridenWidth) {
      this.mOverridenWidth = overridenWidth;
      return this;
    }

    public Builder setOverridenHeight(int overridenHeight) {
      this.mOverridenHeight = overridenHeight;
      return this;
    }

    public Builder setFadeDurationMs(int fadeDurationMs) {
      this.mFadeDurationMs = fadeDurationMs;
      return this;
    }

    public Builder setDecodeCancellation(boolean decodeCancellation) {
      this.mDecodeCancellation = decodeCancellation;
      return this;
    }

    public Builder setWebpSupportEnabled(boolean webpSupportEnabled) {
      this.mWebpSupportEnabled = webpSupportEnabled;
      return this;
    }

    public Builder setDrawBorder(boolean drawBorder) {
      this.mDrawBorder = drawBorder;
      return this;
    }

    public Builder setBgColor(int bgColor) {
      this.mBgColor = bgColor;
      return this;
    }

    public Builder setDraweeOverlayEnabled(boolean draweeOverlayEnabled) {
      this.mDraweeOverlayEnabled = draweeOverlayEnabled;
      return this;
    }

    public Builder setInstrumentationEnabled(boolean instrumentationEnabled) {
      this.mInstrumentationEnabled = instrumentationEnabled;
      return this;
    }

    public Config build() {
      return new Config(this);
    }
  }
}
