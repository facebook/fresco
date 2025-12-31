/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.samples.comparison

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.common.logging.FLog
import com.facebook.common.logging.FLog.minimumLoggingLevel
import com.facebook.samples.comparison.adapters.AQueryAdapter
import com.facebook.samples.comparison.adapters.FrescoAdapter
import com.facebook.samples.comparison.adapters.GlideAdapter
import com.facebook.samples.comparison.adapters.ImageListAdapter
import com.facebook.samples.comparison.adapters.PicassoAdapter
import com.facebook.samples.comparison.adapters.UilAdapter
import com.facebook.samples.comparison.adapters.VolleyAdapter
import com.facebook.samples.comparison.configs.imagepipeline.ImagePipelineConfigFactory
import com.facebook.samples.comparison.instrumentation.PerfListener
import com.facebook.samples.comparison.urlsfetcher.ImageFormat
import com.facebook.samples.comparison.urlsfetcher.ImageSize
import com.facebook.samples.comparison.urlsfetcher.ImageUrlsFetcher
import com.facebook.samples.comparison.urlsfetcher.ImageUrlsRequestBuilder
import java.util.Locale
import kotlin.math.min

class MainActivity : AppCompatActivity() {
  private var mHandler: Handler? = null
  private var mStatsClockTickRunnable: Runnable? = null

  private var mStatsDisplay: TextView? = null
  private var mLoaderSelect: Spinner? = null
  private var mSourceSelect: Spinner? = null

  private var mHasStoragePermissions = false
  private var mRequestedLocalSource = false
  private var mAllowAnimations = false
  private var mCurrentLoaderAdapterIndex = 0
  private var mCurrentSourceAdapterIndex = 0

  private var mCurrentAdapter: ImageListAdapter? = null
  private var mRecyclerView: RecyclerView? = null

  private var mPerfListener: PerfListener? = null

  private var mImageUrls: MutableList<String?> = ArrayList<String?>()

  private var mUrlsLocal = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    mRecyclerView = findViewById<View?>(R.id.image_grid) as RecyclerView
    mRecyclerView!!.setLayoutManager(GridLayoutManager(this, COLS_NUMBER))

    minimumLoggingLevel = FLog.WARN
    Drawables.init(getResources())

    mPerfListener = PerfListener()
    mAllowAnimations = true
    mCurrentLoaderAdapterIndex = 0
    mCurrentSourceAdapterIndex = 0
    if (savedInstanceState != null) {
      mAllowAnimations = savedInstanceState.getBoolean(EXTRA_ALLOW_ANIMATIONS)
      mCurrentLoaderAdapterIndex = savedInstanceState.getInt(EXTRA_CURRENT_ADAPTER_INDEX)
      mCurrentSourceAdapterIndex = savedInstanceState.getInt(EXTRA_CURRENT_SOURCE_ADAPTER_INDEX)
    }

    mHandler = Handler(Looper.getMainLooper())
    mStatsClockTickRunnable =
        object : Runnable {
          override fun run() {
            updateStats()
            scheduleNextStatsClockTick()
          }
        }

    mCurrentAdapter = null

    mStatsDisplay = findViewById<View?>(R.id.stats_display) as TextView
    mLoaderSelect = findViewById<View?>(R.id.loader_select) as Spinner
    mLoaderSelect!!.setOnItemSelectedListener(
        object : AdapterView.OnItemSelectedListener {
          override fun onItemSelected(
              parent: AdapterView<*>?,
              view: View?,
              position: Int,
              id: Long,
          ) {
            setLoaderAdapter(position)
          }

          override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    )
    mLoaderSelect!!.setSelection(mCurrentLoaderAdapterIndex)

    mSourceSelect = findViewById<View?>(R.id.source_select) as Spinner
    mSourceSelect!!.setOnItemSelectedListener(
        object : AdapterView.OnItemSelectedListener {
          override fun onItemSelected(
              parent: AdapterView<*>?,
              view: View?,
              position: Int,
              id: Long,
          ) {
            setSourceAdapter(position)
          }

          override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    )
    mSourceSelect!!.setSelection(mCurrentSourceAdapterIndex)
    mHasStoragePermissions =
        (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED)
  }

  protected override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putBoolean(EXTRA_ALLOW_ANIMATIONS, mAllowAnimations)
    outState.putInt(EXTRA_CURRENT_ADAPTER_INDEX, mCurrentLoaderAdapterIndex)
    outState.putInt(EXTRA_CURRENT_SOURCE_ADAPTER_INDEX, mCurrentSourceAdapterIndex)
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    getMenuInflater().inflate(R.menu.menu_main, menu)
    return true
  }

  override fun onPrepareOptionsMenu(menu: Menu): Boolean {
    menu.findItem(R.id.allow_animations).setChecked(mAllowAnimations)
    return super.onPrepareOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    val id = item.getItemId()

    if (id == R.id.allow_animations) {
      setAllowAnimations(!item.isChecked())
      return true
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onStart() {
    super.onStart()
    updateStats()
    scheduleNextStatsClockTick()
  }

  override fun onStop() {
    super.onStop()
    cancelNextStatsClockTick()
  }

  @VisibleForTesting
  fun setAllowAnimations(allowAnimations: Boolean) {
    mAllowAnimations = allowAnimations
    supportInvalidateOptionsMenu()
    updateAdapter(null)
    loadUrls()
  }

  private fun requestStoragePermissions() {
    if (mHasStoragePermissions) {
      return
    }

    ActivityCompat.requestPermissions(
        this,
        arrayOf<String>(Manifest.permission.READ_EXTERNAL_STORAGE),
        PERMISSION_REQUEST_CODE,
    )
  }

  override fun onRequestPermissionsResult(
      requestCode: Int,
      permissions: Array<String?>,
      grantResults: IntArray,
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    for (i in permissions.indices) {
      if (permissions[i] == Manifest.permission.READ_EXTERNAL_STORAGE) {
        mHasStoragePermissions = grantResults[i] == PackageManager.PERMISSION_GRANTED
        if (mHasStoragePermissions && mRequestedLocalSource) {
          mRequestedLocalSource = false
          setSourceAdapter(LOCAL_INDEX)
        } else if (!mHasStoragePermissions && mRequestedLocalSource) {
          // If the user chose to deny the permission, update the source selection to none for
          // visual consistency.
          mSourceSelect!!.setSelection(NONE_INDEX)
        }
      }
    }
  }

  private fun resetAdapter() {
    if (mCurrentAdapter != null) {
      mCurrentAdapter!!.shutDown()
      mCurrentAdapter = null
      System.gc()
    }
  }

  private fun setLoaderAdapter(index: Int) {
    FLog.v(TAG, "onImageLoaderSelect: %d", index)
    resetAdapter()
    mCurrentLoaderAdapterIndex = index
    mPerfListener = PerfListener()
    when (index) {
      FRESCO_INDEX,
      FRESCO_OKHTTP_INDEX ->
          mCurrentAdapter =
              FrescoAdapter(
                  this,
                  mPerfListener,
                  if (index == FRESCO_INDEX) ImagePipelineConfigFactory.getImagePipelineConfig(this)
                  else ImagePipelineConfigFactory.getOkHttpImagePipelineConfig(this),
              )
      GLIDE_INDEX -> mCurrentAdapter = GlideAdapter(this, mPerfListener)
      PICASSO_INDEX -> mCurrentAdapter = PicassoAdapter(this, mPerfListener)
      UIL_INDEX -> mCurrentAdapter = UilAdapter(this, mPerfListener)
      VOLLEY_INDEX -> mCurrentAdapter = VolleyAdapter(this, mPerfListener)
      AQUERY_INDEX -> mCurrentAdapter = AQueryAdapter(this, mPerfListener)
      else -> {
        mCurrentAdapter = null
        return
      }
    }
    mRecyclerView!!.setAdapter(mCurrentAdapter)

    updateAdapter(mImageUrls)

    updateStats()
  }

  private fun setSourceAdapter(index: Int) {
    FLog.v(TAG, "onImageSourceSelect: %d", index)

    mCurrentSourceAdapterIndex = index
    when (index) {
      NETWORK_INDEX -> mUrlsLocal = false
      LOCAL_INDEX -> mUrlsLocal = true
      else -> {
        resetAdapter()
        mImageUrls.clear()
        return
      }
    }

    loadUrls()
    setLoaderAdapter(mCurrentLoaderAdapterIndex)
  }

  private fun loadUrls() {
    if (mUrlsLocal) {
      if (!mHasStoragePermissions) {
        mRequestedLocalSource = true
        requestStoragePermissions()
        return
      }

      loadLocalUrls()
    } else {
      mRequestedLocalSource = false
      loadNetworkUrls()
    }
  }

  private fun scheduleNextStatsClockTick() {
    mHandler!!.postDelayed(mStatsClockTickRunnable!!, STATS_CLOCK_INTERVAL_MS)
  }

  private fun cancelNextStatsClockTick() {
    mHandler!!.removeCallbacks(mStatsClockTickRunnable!!)
  }

  private fun chooseImageSize(): ImageSize {
    val layoutParams = mRecyclerView!!.getLayoutParams()
    if (layoutParams == null) {
      return ImageSize.LARGE_THUMBNAIL
    }
    val size: Int = calcDesiredSize(this, layoutParams.width, layoutParams.height)
    if (size <= 90) {
      return ImageSize.SMALL_SQUARE
    } else if (size <= 160) {
      return ImageSize.SMALL_THUMBNAIL
    } else if (size <= 320) {
      return ImageSize.MEDIUM_THUMBNAIL
    } else if (size <= 640) {
      return ImageSize.LARGE_THUMBNAIL
    } else if (size <= 1024) {
      return ImageSize.HUGE_THUMBNAIL
    } else {
      return ImageSize.ORIGINAL_IMAGE
    }
  }

  private fun loadNetworkUrls() {
    val url = "https://api.imgur.com/3/gallery/hot/viral/0.json"
    val staticSize = chooseImageSize()
    val builder =
        ImageUrlsRequestBuilder(url)
            .addImageFormat(ImageFormat.JPEG, staticSize)
            .addImageFormat(ImageFormat.PNG, staticSize)
    if (mAllowAnimations) {
      builder.addImageFormat(ImageFormat.GIF, ImageSize.ORIGINAL_IMAGE)
    }
    ImageUrlsFetcher.getImageUrls(
        builder.build(),
        object : ImageUrlsFetcher.Callback {
          override fun onFinish(result: MutableList<String?>) {
            // If the user changes to local images before the call comes back, then this should
            // be ignored
            if (!mUrlsLocal) {
              mImageUrls = result
              updateAdapter(mImageUrls)
            }
          }
        },
    )
  }

  private fun loadLocalUrls() {
    val externalContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf<String?>(MediaStore.Images.Media._ID)
    var cursor: Cursor? = null
    try {
      cursor = getContentResolver().query(externalContentUri, projection, null, null, null)

      mImageUrls.clear()

      val columnIndex = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

      var imageId: String?
      var imageUri: Uri
      while (cursor.moveToNext()) {
        imageId = cursor.getString(columnIndex)
        imageUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageId)
        mImageUrls.add(imageUri.toString())
      }
    } finally {
      if (cursor != null) {
        cursor.close()
      }
    }
  }

  private fun updateAdapter(urls: MutableList<String?>?) {
    if (mCurrentAdapter != null) {
      mCurrentAdapter!!.clear()
      if (urls != null) {
        for (url in urls) {
          mCurrentAdapter!!.addUrl(url)
        }
      }
      mCurrentAdapter!!.notifyDataSetChanged()
    }
  }

  private fun updateStats() {
    val runtime = Runtime.getRuntime()
    val heapMemory = runtime.totalMemory() - runtime.freeMemory()
    val sb: StringBuilder = StringBuilder(DEFAULT_MESSAGE_SIZE)
    // When changing format of output below, make sure to sync "run_comparison.py" as well
    sb.append("Heap: ")
    appendSize(sb, heapMemory)
    sb.append(" Java ")
    appendSize(sb, Debug.getNativeHeapSize())
    sb.append(" native\n")
    appendTime(sb, "Avg wait time: ", mPerfListener!!.getAverageWaitTime(), "\n")
    appendNumber(sb, "Requests: ", mPerfListener!!.getOutstandingRequests(), " outsdng ")
    appendNumber(sb, "", mPerfListener!!.getCancelledRequests(), " cncld\n")
    val message = sb.toString()
    mStatsDisplay!!.setText(message)
    FLog.i(TAG, message)
  }

  val displayHeight: Int
    /** Determines display's height. */
    get() {
      val display = getWindowManager().getDefaultDisplay()
      val size = Point()
      display.getSize(size)
      return size.y
    }

  companion object {
    private const val TAG = "FrescoSample"

    private const val PERMISSION_REQUEST_CODE = 42

    // These need to be in sync with {@link R.array.image_loaders}
    const val FRESCO_INDEX: Int = 1
    const val FRESCO_OKHTTP_INDEX: Int = 2
    const val GLIDE_INDEX: Int = 3
    const val PICASSO_INDEX: Int = 4
    const val UIL_INDEX: Int = 5
    const val VOLLEY_INDEX: Int = 6
    const val AQUERY_INDEX: Int = 7

    // These need to be in sync with {@link R.array.image_sources}
    const val NONE_INDEX: Int = 0
    const val NETWORK_INDEX: Int = 1
    const val LOCAL_INDEX: Int = 2

    private const val COLS_NUMBER = 3

    private const val STATS_CLOCK_INTERVAL_MS: Long = 1000
    private const val DEFAULT_MESSAGE_SIZE = 1024
    private val BYTES_IN_MEGABYTE = 1024 * 1024

    private const val EXTRA_ALLOW_ANIMATIONS = "allow_animations"
    private const val EXTRA_CURRENT_ADAPTER_INDEX = "current_adapter_index"
    private const val EXTRA_CURRENT_SOURCE_ADAPTER_INDEX = "current_source_adapter_index"

    @JvmStatic
    fun calcDesiredSize(context: Context, parentWidth: Int, parentHeight: Int): Int {
      val orientation = context.getResources().getConfiguration().orientation
      val desiredSize =
          if (orientation == Configuration.ORIENTATION_LANDSCAPE) parentHeight / 2
          else parentHeight / 3
      return min(desiredSize.toDouble(), parentWidth.toDouble()).toInt()
    }

    private fun appendSize(sb: StringBuilder, bytes: Long) {
      val value = String.format(Locale.getDefault(), "%.1f MB", bytes.toFloat() / BYTES_IN_MEGABYTE)
      sb.append(value)
    }

    private fun appendTime(sb: StringBuilder, prefix: String?, timeMs: Long, suffix: String?) {
      appendValue(sb, prefix, timeMs.toString() + " ms", suffix)
    }

    private fun appendNumber(sb: StringBuilder, prefix: String?, number: Long, suffix: String?) {
      appendValue(sb, prefix, number.toString() + "", suffix)
    }

    private fun appendValue(sb: StringBuilder, prefix: String?, value: String?, suffix: String?) {
      sb.append(prefix).append(value).append(suffix)
    }
  }
}
