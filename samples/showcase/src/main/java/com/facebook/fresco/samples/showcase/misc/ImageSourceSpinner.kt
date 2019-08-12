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
package com.facebook.fresco.samples.showcase.misc

import android.net.Uri
import android.widget.Spinner
import com.facebook.fresco.samples.showcase.common.SpinnerUtils.setupWithCallbacks

object ImageSourceSpinner {

    @JvmOverloads
    fun Spinner.setup(
            imageUriProvider: ImageUriProvider,
            callback: (List<@JvmSuppressWildcards Uri>) -> Unit,
            numEntries: Int = 256) {
        setupWithCallbacks(
                listOf(
                        "Small images" to {
                            callback.invoke(
                                    imageUriProvider.getRandomSampleUris(
                                            ImageUriProvider.ImageSize.S, numEntries))
                        },
                        "Large images" to {
                            callback.invoke(
                                    imageUriProvider.getRandomSampleUris(
                                            ImageUriProvider.ImageSize.M, numEntries))
                        },
                        "Media" to {
                            callback.invoke(imageUriProvider.getMediaStoreUris(context))
                        },
                        "Empty list" to { callback.invoke(emptyList()) }
                )

        )
    }
}