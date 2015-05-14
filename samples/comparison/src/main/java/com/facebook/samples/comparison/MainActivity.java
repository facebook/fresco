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

package com.facebook.samples.comparison;

import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.logging.FLog;
import com.facebook.samples.comparison.adapters.FrescoAdapter;
import com.facebook.samples.comparison.adapters.GlideAdapter;
import com.facebook.samples.comparison.adapters.ImageListAdapter;
import com.facebook.samples.comparison.adapters.PicassoAdapter;
import com.facebook.samples.comparison.adapters.UilAdapter;
import com.facebook.samples.comparison.adapters.VolleyAdapter;
import com.facebook.samples.comparison.adapters.VolleyDraweeAdapter;
import com.facebook.samples.comparison.configs.imagepipeline.ImagePipelineConfigFactory;
import com.facebook.samples.comparison.instrumentation.PerfListener;
import com.facebook.samples.comparison.urlsfetcher.ImageFormat;
import com.facebook.samples.comparison.urlsfetcher.ImageSize;
import com.facebook.samples.comparison.urlsfetcher.ImageUrlsFetcher;
import com.facebook.samples.comparison.urlsfetcher.ImageUrlsRequestBuilder;

import java.util.List;
import java.util.Locale;

public class MainActivity extends ActionBarActivity {

  private static final String TAG = "FrescoSample";

  // These need to be in sync with {@link R.array.image_loaders}
  public static final int FRESCO_INDEX = 1;
  public static final int FRESCO_OKHTTP_INDEX = 2;
  public static final int GLIDE_INDEX = 3;
  public static final int PICASSO_INDEX = 4;
  public static final int UIL_INDEX = 5;
  public static final int VOLLEY_INDEX = 6;

  private static final long STATS_CLOCK_INTERVAL_MS = 1000;
  private static final int DEFAULT_MESSAGE_SIZE = 1024;
  private static final int BYTES_IN_MEGABYTE = 1024 * 1024;

  private static final String EXTRA_ALLOW_ANIMATIONS = "allow_animations";
  private static final String EXTRA_USE_DRAWEE = "use_drawee";
  private static final String EXTRA_CURRENT_ADAPTER_INDEX = "current_adapter_index";

  private Handler mHandler;
  private Runnable mStatsClockTickRunnable;

  private TextView mStatsDisplay;
  private Spinner mLoaderSelect;
  private ListView mImageList;

  private boolean mUseDrawee;
  private boolean mAllowAnimations;
  private int mCurrentAdapterIndex;

  private ImageListAdapter mCurrentAdapter;
  private PerfListener mPerfListener;

  private List<String> mImageUrls;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    FLog.setMinimumLoggingLevel(FLog.WARN);
    Drawables.init(getResources());

    mAllowAnimations = true;
    mUseDrawee = true;
    mCurrentAdapterIndex = 0;
    if (savedInstanceState != null) {
      mAllowAnimations = savedInstanceState.getBoolean(EXTRA_ALLOW_ANIMATIONS);
      mUseDrawee = savedInstanceState.getBoolean(EXTRA_USE_DRAWEE);
      mCurrentAdapterIndex = savedInstanceState.getInt(EXTRA_CURRENT_ADAPTER_INDEX);
    }

    mHandler = new Handler(Looper.getMainLooper());
    mStatsClockTickRunnable = new Runnable() {
        @Override
        public void run() {
          updateStats();
          scheduleNextStatsClockTick();
        }
      };

    mCurrentAdapter = null;
    mPerfListener = new PerfListener();

    mStatsDisplay = (TextView) findViewById(R.id.stats_display);
    mImageList = (ListView) findViewById(R.id.image_list);
    mLoaderSelect = (Spinner) findViewById(R.id.loader_select);
    mLoaderSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        setAdapter(position);
      }
      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });
    mLoaderSelect.setSelection(mCurrentAdapterIndex);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(EXTRA_ALLOW_ANIMATIONS, mAllowAnimations);
    outState.putBoolean(EXTRA_USE_DRAWEE, mUseDrawee);
    outState.putInt(EXTRA_CURRENT_ADAPTER_INDEX, mCurrentAdapterIndex);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    menu.findItem(R.id.allow_animations).setChecked(mAllowAnimations);
    menu.findItem(R.id.use_drawee).setChecked(mUseDrawee);
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();

    if (id == R.id.allow_animations) {
      setAllowAnimations(!item.isChecked());
      return true;
    }

    if (id == R.id.use_drawee) {
      setUseDrawee(!item.isChecked());
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onStart() {
    super.onStart();
    updateStats();
    scheduleNextStatsClockTick();
  }

  protected void onStop() {
    super.onStop();
    cancelNextStatsClockTick();
  }

  @VisibleForTesting
  public void setAllowAnimations(boolean allowAnimations) {
    mAllowAnimations = allowAnimations;
    supportInvalidateOptionsMenu();
    updateAdapter(null);
    reloadUrls();
  }

  @VisibleForTesting
  public void setUseDrawee(boolean useDrawee) {
    mUseDrawee = useDrawee;
    supportInvalidateOptionsMenu();
    setAdapter(mCurrentAdapterIndex);
  }

  private void setAdapter(int index) {
    FLog.w(TAG, "onImageLoaderSelect: %d", index);
    if (mCurrentAdapter != null) {
      mCurrentAdapter.shutDown();
      mCurrentAdapter = null;
      System.gc();
    }

    mCurrentAdapterIndex = index;
    mPerfListener = new PerfListener();
    switch (index) {
      case FRESCO_INDEX:
      case FRESCO_OKHTTP_INDEX:
        mCurrentAdapter = new FrescoAdapter(
                this,
                R.id.image_list,
                mPerfListener,
                index == FRESCO_INDEX ?
                    ImagePipelineConfigFactory.getImagePipelineConfig(this) :
                    ImagePipelineConfigFactory.getOkHttpImagePipelineConfig(this));
        break;
      case GLIDE_INDEX:
        mCurrentAdapter = new GlideAdapter(this, R.id.image_list, mPerfListener);
        break;
      case PICASSO_INDEX:
        mCurrentAdapter = new PicassoAdapter(this, R.id.image_list, mPerfListener);
        break;
      case UIL_INDEX:
        mCurrentAdapter = new UilAdapter(this, R.id.image_list, mPerfListener);
        break;
      case VOLLEY_INDEX:
        mCurrentAdapter = mUseDrawee ?
            new VolleyDraweeAdapter(this, R.id.image_list, mPerfListener) :
            new VolleyAdapter(this, R.id.image_list, mPerfListener);
        break;
      default:
        mCurrentAdapter = null;
        return;
    }

    mImageList.setAdapter(mCurrentAdapter);

    if (mImageUrls != null && !mImageUrls.isEmpty()) {
      updateAdapter(mImageUrls);
    } else {
      reloadUrls();
    }

    updateStats();
  }

  private void scheduleNextStatsClockTick() {
    mHandler.postDelayed(mStatsClockTickRunnable, STATS_CLOCK_INTERVAL_MS);
  }

  private void cancelNextStatsClockTick() {
    mHandler.removeCallbacks(mStatsClockTickRunnable);
  }

  private void reloadUrls() {
    String url = "https://api.imgur.com/3/gallery/hot/viral/0.json";
    ImageUrlsRequestBuilder builder = new ImageUrlsRequestBuilder(url)
        .addImageFormat(
            ImageFormat.JPEG,
            ImageSize.LARGE_THUMBNAIL)
        .addImageFormat(
            ImageFormat.PNG,
            ImageSize.LARGE_THUMBNAIL);
    if (mAllowAnimations) {
      builder.addImageFormat(
          ImageFormat.GIF,
          ImageSize.ORIGINAL_IMAGE);
    }
    ImageUrlsFetcher.getImageUrls(
        builder.build(),
        new ImageUrlsFetcher.Callback() {
          @Override
          public void onFinish(List<String> result) {
            mImageUrls = result;
            updateAdapter(mImageUrls);
          }
        });
  }

  private void updateAdapter(List<String> urls) {
    if (mCurrentAdapter != null) {
      mCurrentAdapter.clear();
      if (urls != null) {
        for (String url : urls) {
          mCurrentAdapter.add(url);
        }
      }
      mCurrentAdapter.notifyDataSetChanged();
    }
  }

  private void updateStats() {
    final Runtime runtime = Runtime.getRuntime();
    final long heapMemory = runtime.totalMemory() - runtime.freeMemory();
    final StringBuilder sb = new StringBuilder(DEFAULT_MESSAGE_SIZE);
    // When changing format of output below, make sure to sync "scripts/test_runner.py" as well.
    appendSize(sb, "Java heap size:          ", heapMemory, "\n");
    appendSize(sb, "Native heap size:        ", Debug.getNativeHeapSize(), "\n");
    appendTime(sb, "Average photo wait time: ", mPerfListener.getAverageWaitTime(), "\n");
    appendNumber(sb, "Outstanding requests:    ", mPerfListener.getOutstandingRequests(), "\n");
    appendNumber(sb, "Cancelled requests:      ", mPerfListener.getCancelledRequests(), "\n");
    final String message = sb.toString();
    mStatsDisplay.setText(message);
    FLog.i(TAG, message);
  }

  private static void appendSize(StringBuilder sb, String prefix, long bytes, String suffix) {
    String value = String.format(Locale.getDefault(), "%.2f", (float) bytes / BYTES_IN_MEGABYTE);
    appendValue(sb, prefix, value + " MB", suffix);
  }

  private static void appendTime(StringBuilder sb, String prefix, long timeMs, String suffix) {
    appendValue(sb, prefix, timeMs + " ms", suffix);
  }

  private static void appendNumber(StringBuilder sb, String prefix, long number, String suffix) {
    appendValue(sb, prefix, number + "", suffix);
  }

  private static void appendValue(StringBuilder sb, String prefix, String value, String suffix) {
    sb.append(prefix).append(value).append(suffix);
  }
}
