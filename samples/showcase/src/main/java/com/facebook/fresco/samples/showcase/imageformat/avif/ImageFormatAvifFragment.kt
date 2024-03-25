package com.facebook.fresco.samples.showcase.imageformat.avif

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment
import com.facebook.fresco.samples.showcase.R

class ImageFormatAvifFragment : BaseShowcaseFragment() {
  override fun onCreateView(
          inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_format_avif, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    val draweeAvifStatic = view.findViewById<SimpleDraweeView>(R.id.drawee_view_avif_static)
    draweeAvifStatic.setImageURI(sampleUris().createAvifStaticUri())
  }
}
