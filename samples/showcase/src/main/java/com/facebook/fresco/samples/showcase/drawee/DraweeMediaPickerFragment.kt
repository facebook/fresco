/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.drawee

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment
import com.facebook.fresco.samples.showcase.R
import com.facebook.fresco.samples.showcase.common.ToggleAnimationClickListener
import com.facebook.fresco.samples.showcase.permissions.StoragePermissionHelper.withStoragePermission

/** Display images from media pickers. */
class DraweeMediaPickerFragment : BaseShowcaseFragment() {
  private var simpleDraweeView: SimpleDraweeView? = null
  private var imagePath: TextView? = null

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_drawee_media_picker, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    simpleDraweeView = view.findViewById<View>(R.id.drawee_view) as SimpleDraweeView
    imagePath = view.findViewById<View>(R.id.image_path) as TextView
    simpleDraweeView!!.setOnClickListener(ToggleAnimationClickListener(simpleDraweeView))
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
                Toast.LENGTH_SHORT)
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
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == REQUEST_CODE_PICK_MEDIA) {
      if (resultCode != Activity.RESULT_OK) {
        simpleDraweeView!!.setImageURI(null as Uri?)
        imagePath!!.setText(R.string.drawee_media_picker_no_image)
      } else {
        simpleDraweeView!!.setImageURI(data!!.data)
        imagePath!!.text = data.dataString
      }
    } else {
      super.onActivityResult(requestCode, resultCode, data)
    }
  }

  companion object {
    private const val REQUEST_CODE_PICK_MEDIA = 1
  }
}
