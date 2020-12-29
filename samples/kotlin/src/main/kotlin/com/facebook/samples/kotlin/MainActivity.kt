/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.kotlin

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlinx.android.synthetic.main.activity_main.*

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

    imageAdapter =
        ImageAdapter(
            ColorDrawable(ContextCompat.getColor(this, R.color.load_placeholder)),
            ColorDrawable(ContextCompat.getColor(this, R.color.load_fail)),
            resources.displayMetrics.widthPixels / GRID_COLUMN_COUNT)

    with(recycler_view) {
      layoutManager =
          GridLayoutManager(context, GRID_COLUMN_COUNT, GridLayoutManager.VERTICAL, false)
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

  override fun onRequestPermissionsResult(
      requestCode: Int,
      permissions: Array<out String>,
      grantResults: IntArray
  ) {
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

  /** Returns true if storage permissions were requested because they are needed, false otherwise */
  private fun requestStoragePermissionsIfNeeded(): Boolean {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
        PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(
          this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
      return true
    }

    return false
  }

  private fun loadPhotos() {
    loadPhotosFuture?.cancel(true)
    loadPhotosFuture =
        executor.submit {
          val uris = dataSource.loadPhotoUris(this)
          runOnUiThread {
            imageAdapter.setUris(uris)
            view_flipper.displayedChild = FLIPPER_INDEX_RECYCLER_VIEW
          }
        }
  }
}
