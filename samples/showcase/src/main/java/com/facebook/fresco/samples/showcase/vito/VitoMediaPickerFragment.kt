/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.drawee

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment
import com.facebook.fresco.samples.showcase.R
import com.facebook.fresco.samples.showcase.permissions.StoragePermissionHelper.withStoragePermission
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.source.ImageSourceProvider
import com.facebook.fresco.vito.view.VitoView

/** Display images from media pickers. */
class VitoMediaPickerFragment : BaseShowcaseFragment() {
  private lateinit var imageView: ImageView
  private lateinit var imagePath: TextView
  private val callerContext: String = "VitoMediaPickerFragment"
  private val imageOptions: ImageOptions =
      ImageOptions.create()
          .errorRes(R.color.error_color)
          .placeholderRes(R.color.placeholder_color)
          .build()

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View? {
    return inflater.inflate(R.layout.fragment_vito_media_picker, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    imageView = view.findViewById(R.id.image_view)
    imagePath = view.findViewById(R.id.image_path)

    val actionOpenDocumentButton = view.findViewById<View>(R.id.pick_action_open_document)
    actionOpenDocumentButton.setOnClickListener {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.setType("image/*")
        startActivityForResult(intent, REQUEST_CODE_PICK_MEDIA)
      } else {
        Toast.makeText(
                context,
                R.string.drawee_media_picker_action_open_document_not_supported,
                Toast.LENGTH_SHORT,
            )
            .show()
      }
    }
    val actionGetContent = view.findViewById<View>(R.id.pick_action_get_content)
    actionGetContent.setOnClickListener {
      val intent = Intent(Intent.ACTION_GET_CONTENT)
      intent.setType("image/*")
      startActivityForResult(intent, REQUEST_CODE_PICK_MEDIA)
    }
    val actionPickButton = view.findViewById<View>(R.id.pick_action_pick)
    actionPickButton.setOnClickListener {
      withStoragePermission(activity!!) { unit: Unit ->
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_CODE_PICK_MEDIA)
        null
      }
    }
    // Show placeholder
    VitoView.show(ImageSourceProvider.emptySource(), imageOptions, callerContext, imageView)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == REQUEST_CODE_PICK_MEDIA) {
      if (resultCode != Activity.RESULT_OK || data == null) {
        VitoView.show(ImageSourceProvider.emptySource(), imageOptions, callerContext, imageView)
        imagePath.setText(R.string.drawee_media_picker_no_image)
      } else {
        VitoView.show(data.data, imageOptions, callerContext, imageView)
        imagePath.text = data.dataString
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data)
    }
  }

  companion object {
    private const val REQUEST_CODE_PICK_MEDIA = 1
  }
}
