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
package com.facebook.fresco.samples.showcase.vito

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment
import com.facebook.fresco.samples.showcase.R
import com.facebook.fresco.samples.showcase.common.SpinnerUtils.setupWithList
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider
import com.facebook.fresco.vito.litho.FrescoVitoImage
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.litho.ComponentContext
import com.facebook.litho.LithoView
import kotlinx.android.synthetic.main.fragment_vito_image_options_config.*

/** Experimental Fresco Vito fragment that allows to configure ImageOptions via a simple UI.  */
class FrescoVitoLithoImageOptionsConfigFragment : BaseShowcaseFragment() {

    private val imageOptionsBuilder = ImageOptions.create().autoPlay(true)

    private var currentUri: Uri? = null
    private var componentContext: ComponentContext? = null
    private var lithoView: LithoView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_vito_image_options_config, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        currentUri = sampleUris().createSampleUri(ImageUriProvider.ImageSize.M)
        componentContext = ComponentContext(context)

        lithoView = LithoView.create(componentContext, createImage(imageOptionsBuilder.build(), currentUri))
        container.addView(lithoView)

        spinner_rounding.setupWithList(VitoSpinners.roundingOptions) {
            refresh(imageOptionsBuilder.round(it))
        }
        spinner_border.setupWithList(VitoSpinners.borderOptions) {
            refresh(imageOptionsBuilder.borders(it))
        }
        spinner_scale_type.setupWithList(VitoSpinners.scaleTypes) {
            refresh(imageOptionsBuilder.scale(it.first).focusPoint(it.second))
        }
        spinner_image_format.setupWithList(VitoSpinners.imageFormats) {
            refresh(uri = sampleUris().create(it))
        }

        spinner_color_filter.setupWithList(VitoSpinners.colorFilters) {
            refresh(imageOptionsBuilder.colorFilter(it))
        }

        spinner_placeholder.setupWithList(VitoSpinners.placeholderOptions) {
            refresh(it(imageOptionsBuilder))
        }
        spinner_postprocessor.setupWithList(VitoSpinners.postprocessorOptions) {
            refresh(it(imageOptionsBuilder))
        }
        spinner_rotation.setupWithList(VitoSpinners.rotationOptions) {
            refresh(imageOptionsBuilder.rotate(it))
        }
    }

    override fun getTitleId() = R.string.vito_litho_image_options_config

    private fun refresh(builder: ImageOptions.Builder = imageOptionsBuilder, uri: Uri? = currentUri) {
        currentUri = uri
        lithoView?.setComponentAsync(createImage(builder.build(), uri))
    }

    private fun createImage(imageOptions: ImageOptions, uri: Uri?) = FrescoVitoImage.create(componentContext)
            .uri(uri)
            .imageOptions(imageOptions)
            .build()
}
