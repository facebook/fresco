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

package com.facebook.samples.kotlin

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.Executors
import java.util.concurrent.Future

class MainActivity : AppCompatActivity() {
  companion object {
    private const val GRID_COLUMN_COUNT = 3
    private const val PERMISSION_REQUEST_CODE = 42
    private const val FLIPPER_INDEX_RECYCLER_VIEW = 1

    private val executor = Executors.newSingleThreadExecutor()
  }

  private val dataSource = MediaStoreData()
  private var loadPhotosFuture: Future<out Any>? = null
  private lateinit var imageAdapter: ImageAdapter

  override fun onCreate(savedInstance: Bundle?) {
    super.onCreate(savedInstance)
    setContentView(R.layout.activity_main)

    imageAdapter = ImageAdapter(
        ColorDrawable(ContextCompat.getColor(this, R.color.load_placeholder)),
        ColorDrawable(ContextCompat.getColor(this, R.color.load_fail)),
        resources.displayMetrics.widthPixels / GRID_COLUMN_COUNT)

    with(recycler_view) {
      layoutManager = GridLayoutManager(
          context,
          GRID_COLUMN_COUNT,
          GridLayoutManager.VERTICAL,
          false)
      adapter = imageAdapter
    }
  }

  override fun onResume() {
    super.onResume()
    if (!requestStoragePermissionsIfNeeded()) {
      loadPhotos()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    loadPhotosFuture?.cancel(true)
  }

  override fun onRequestPermissionsResult(requestCode: Int,
                                          permissions: Array<out String>,
                                          grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode != PERMISSION_REQUEST_CODE) {
      return
    }

    for ((i, permission) in permissions.withIndex()) {
      if (permission != Manifest.permission.READ_EXTERNAL_STORAGE) {
        continue
      }
      if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
        loadPhotos()
      } else {
        Toast.makeText(this, R.string.permission_denied_toast, Toast.LENGTH_LONG).show()
      }
      return
    }
  }

  /**
   * Returns true if storage permissions were requested because they are needed, false otherwise
   */
  private fun requestStoragePermissionsIfNeeded(): Boolean {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
        PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(
          this,
          arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
          PERMISSION_REQUEST_CODE)
      return true
    }

    return false
  }

  private fun loadPhotos() {
    loadPhotosFuture?.cancel(true)
    loadPhotosFuture = executor.submit {
      val uris = dataSource.loadPhotoUris(this)
      runOnUiThread {
        imageAdapter.setUris(uris)
        view_flipper.displayedChild = FLIPPER_INDEX_RECYCLER_VIEW
      }
    }
  }
}