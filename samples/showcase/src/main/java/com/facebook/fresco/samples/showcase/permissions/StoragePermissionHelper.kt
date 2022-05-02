/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.permissions

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object StoragePermissionHelper {

  private const val PERMISSION_REQUEST_CODE = 42

  private var pendingAction: (Unit) -> Unit = {}

  /**
   * Execute a given action iff the storage permission is granted. Returns true if storage
   * permissions were requested, false otherwise
   */
  fun withStoragePermission(activity: Activity, action: (Unit) -> Unit = {}) {
    if (ContextCompat.checkSelfPermission(activity, READ_EXTERNAL_STORAGE) !=
        PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(
          activity, arrayOf(READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
      pendingAction = action
    } else {
      action(Unit)
    }
  }

  /**
   * Utility method to execute a pending action when the storage permission was requested using this
   * class.
   */
  fun onRequestPermissionsResult(
      context: Context,
      requestCode: Int,
      permissions: Array<out String>,
      grantResults: IntArray
  ) {
    if (requestCode != PERMISSION_REQUEST_CODE) {
      return
    }

    for ((i, permission) in permissions.withIndex()) {
      if (permission != READ_EXTERNAL_STORAGE) {
        continue
      }
      if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
        pendingAction(Unit)
        pendingAction = {}
      } else {
        Toast.makeText(
                context,
                "This sample app needs storage permissions to load images",
                Toast.LENGTH_LONG)
            .show()
      }
      return
    }
  }
}
