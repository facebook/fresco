/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.misc

import android.graphics.drawable.Drawable
import android.util.Log
import com.facebook.drawee.backends.pipeline.info.ImageOriginUtils
import com.facebook.fresco.ui.common.DimensionsInfo
import com.facebook.fresco.vito.listener.ImageListener
import com.facebook.imagepipeline.image.ImageInfo

class DebugImageListener(val log: (String) -> Unit = { Log.d("DebugImageListener", it) }) : ImageListener {

    override fun onSubmit(id: Long, callerContext: Any?) {
        log("onSubmit: id=$id, cc=$callerContext")
    }

    override fun onPlaceholderSet(id: Long, placeholder: Drawable?) {
        log("onPlaceholderSet: id=$id, placeholder=$placeholder")
    }

    override fun onFinalImageSet(id: Long, imageOrigin: Int, imageInfo: ImageInfo?, drawable: Drawable?) {
        log("onFinalImageSet: id=$id, origin=${ImageOriginUtils.toString(imageOrigin)}, imageInfo=$imageInfo, drawable=$drawable")
    }

    override fun onIntermediateImageSet(id: Long, imageInfo: ImageInfo?) {
        log("onIntermediateImageSet: id=$id, imageInfo=$imageInfo")
    }

    override fun onIntermediateImageFailed(id: Long, throwable: Throwable?) {
        log("onIntermediateImageFailed: id=$id, throwable=$throwable")
    }

    override fun onFailure(id: Long, error: Drawable?, throwable: Throwable?) {
        log("onFailure: id=$id, errorDrawable=$error, throwable=$throwable")
    }

    override fun onRelease(id: Long) {
        log("onRelease: id=$id")
    }

    override fun onImageDrawn(id: String?, imageInfo: ImageInfo?, dimensionsInfo: DimensionsInfo?) {
        log("onImageDrawn: id=$id, imageInfo=$imageInfo, dimensionsInfo=$dimensionsInfo")
    }
}
