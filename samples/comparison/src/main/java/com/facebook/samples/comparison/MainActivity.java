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

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
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

import java.util.ArrayList;
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

  // These need to be in sync with {@link R.array.image_sources}
  public static final int NETWORK_INDEX = 1;
  public static final int LOCAL_INDEX = 2;

  private static final long STATS_CLOCK_INTERVAL_MS = 1000;
  private static final int DEFAULT_MESSAGE_SIZE = 1024;
  private static final int BYTES_IN_MEGABYTE = 1024 * 1024;

  private static final String EXTRA_ALLOW_ANIMATIONS = "allow_animations";
  private static final String EXTRA_USE_DRAWEE = "use_drawee";
  private static final String EXTRA_CURRENT_ADAPTER_INDEX = "current_adapter_index";
  private static final String EXTRA_CURRENT_SOURCE_ADAPTER_INDEX = "current_source_adapter_index";

  private Handler mHandler;
  private Runnable mStatsClockTickRunnable;

  private TextView mStatsDisplay;
  private Spinner mLoaderSelect;
  private Spinner mSourceSelect;
  private GridView mImageGrid;

  private boolean mUseDrawee;
  private boolean mAllowAnimations;
  private int mCurrentLoaderAdapterIndex;
  private int mCurrentSourceAdapterIndex;

  private ImageListAdapter mCurrentAdapter;
  private PerfListener mPerfListener;

  private List<String> mImageUrls = new ArrayList<>();

  private boolean mUrlsLocal = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    FLog.setMinimumLoggingLevel(FLog.WARN);
    Drawables.init(getResources());

    mAllowAnimations = true;
    mUseDrawee = true;
    mCurrentLoaderAdapterIndex = 0;
    mCurrentSourceAdapterIndex = 0;
    if (savedInstanceState != null) {
      mAllowAnimations = savedInstanceState.getBoolean(EXTRA_ALLOW_ANIMATIONS);
      mUseDrawee = savedInstanceState.getBoolean(EXTRA_USE_DRAWEE);
      mCurrentLoaderAdapterIndex = savedInstanceState.getInt(EXTRA_CURRENT_ADAPTER_INDEX);
      mCurrentSourceAdapterIndex = savedInstanceState.getInt(EXTRA_CURRENT_SOURCE_ADAPTER_INDEX);
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
    mImageGrid = (GridView) findViewById(R.id.image_grid);
    mLoaderSelect = (Spinner) findViewById(R.id.loader_select);
    mLoaderSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        setLoaderAdapter(position);
      }
      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });
    mLoaderSelect.setSelection(mCurrentLoaderAdapterIndex);

    mSourceSelect = (Spinner) findViewById(R.id.source_select);
    mSourceSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        setSourceAdapter(position);
      }
      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });
    mSourceSelect.setSelection(mCurrentSourceAdapterIndex);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(EXTRA_ALLOW_ANIMATIONS, mAllowAnimations);
    outState.putBoolean(EXTRA_USE_DRAWEE, mUseDrawee);
    outState.putInt(EXTRA_CURRENT_ADAPTER_INDEX, mCurrentLoaderAdapterIndex);
    outState.putInt(EXTRA_CURRENT_SOURCE_ADAPTER_INDEX, mCurrentSourceAdapterIndex);
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
    loadUrls();
  }

  @VisibleForTesting
  public void setUseDrawee(boolean useDrawee) {
    mUseDrawee = useDrawee;
    supportInvalidateOptionsMenu();
    setLoaderAdapter(mCurrentLoaderAdapterIndex);
    setSourceAdapter(mCurrentSourceAdapterIndex);
  }

  private void resetAdapter() {
    if (mCurrentAdapter != null) {
      mCurrentAdapter.shutDown();
      mCurrentAdapter = null;
      System.gc();
    }
  }

  private void setLoaderAdapter(int index) {
    FLog.v(TAG, "onImageLoaderSelect: %d", index);
    resetAdapter();

    mCurrentLoaderAdapterIndex = index;
    mPerfListener = new PerfListener();
    switch (index) {
      case FRESCO_INDEX:
      case FRESCO_OKHTTP_INDEX:
        mCurrentAdapter = new FrescoAdapter(
                this,
                R.id.image_grid,
                mPerfListener,
                index == FRESCO_INDEX ?
                        ImagePipelineConfigFactory.getImagePipelineConfig(this) :
                        ImagePipelineConfigFactory.getOkHttpImagePipelineConfig(this));
        break;
      case GLIDE_INDEX:
        mCurrentAdapter = new GlideAdapter(this, R.id.image_grid, mPerfListener);
        break;
      case PICASSO_INDEX:
        mCurrentAdapter = new PicassoAdapter(this, R.id.image_grid, mPerfListener);
        break;
      case UIL_INDEX:
        mCurrentAdapter = new UilAdapter(this, R.id.image_grid, mPerfListener);
        break;
      case VOLLEY_INDEX:
        mCurrentAdapter = mUseDrawee ?
                new VolleyDraweeAdapter(this, R.id.image_grid, mPerfListener) :
                new VolleyAdapter(this, R.id.image_grid, mPerfListener);
        break;
      default:
        mCurrentAdapter = null;
        return;
    }

    mImageGrid.setAdapter(mCurrentAdapter);
    updateAdapter(mImageUrls);

    updateStats();
  }

  private void setSourceAdapter(int index) {
    FLog.v(TAG, "onImageSourceSelect: %d", index);

    mCurrentSourceAdapterIndex = index;
    switch (index) {
      case NETWORK_INDEX:
        mUrlsLocal = false;
        break;
      case LOCAL_INDEX:
        mUrlsLocal = true;
        break;
      default:
        resetAdapter();
        mImageUrls.clear();
        return;
    }

    loadUrls();
    setLoaderAdapter(mCurrentLoaderAdapterIndex);
  }

  private void loadUrls() {
    if (mUrlsLocal) {
      loadLocalUrls();
    } else {
      loadNetworkUrls();
    }
  }

  private void scheduleNextStatsClockTick() {
    mHandler.postDelayed(mStatsClockTickRunnable, STATS_CLOCK_INTERVAL_MS);
  }

  private void cancelNextStatsClockTick() {
    mHandler.removeCallbacks(mStatsClockTickRunnable);
  }

  private void loadNetworkUrls() {
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
                // If the user changes to local images before the call comes back, then this should
                // be ignored
                if (!mUrlsLocal) {
                  mImageUrls = result;
                  updateAdapter(mImageUrls);
                }
              }
            });
  }

  private void loadLocalUrls() {
    Uri externalContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    String[] projection = {MediaStore.Images.Media._ID};

    Cursor cursor = null;
    try {
      cursor = getContentResolver().query(externalContentUri, projection, null, null, null);

      mImageUrls.clear();

      int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);

      String imageId;
      Uri imageUri;
      while (cursor.moveToNext()) {
        imageId = cursor.getString(columnIndex);
        imageUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageId);
        mImageUrls.add(imageUri.toString());
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
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
    // When changing format of output below, make sure to sync "run_comparison.py" as well
    sb.append("Heap: ");
    appendSize(sb, heapMemory);
    sb.append(" Java ");
    appendSize(sb, Debug.getNativeHeapSize());
    sb.append(" native\n");
    appendTime(sb, "Avg wait time: ", mPerfListener.getAverageWaitTime(), "\n");
    appendNumber(sb, "Requests: ", mPerfListener.getOutstandingRequests(), " outsdng ");
    appendNumber(sb, "", mPerfListener.getCancelledRequests(), " cncld\n");
    final String message = sb.toString();
    mStatsDisplay.setText(message);
    FLog.i(TAG, message);
  }

  private static void appendSize(StringBuilder sb, long bytes) {
    String value = String.format(Locale.getDefault(), "%.1f MB", (float) bytes / BYTES_IN_MEGABYTE);
    sb.append(value);
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
