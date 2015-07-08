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

package com.facebook.samples.comparison.urlsfetcher;

import com.facebook.common.internal.Preconditions;

import java.util.Map;
import java.util.HashMap;

/**
 * Builds ImageUrlsRequest.
 *
 * <p> Use addImageFormat to specify what image types you are interested in
 */
public class ImageUrlsRequestBuilder {
  final private String mEndpointUrl;
  Map<ImageFormat, ImageSize> mRequestedImageFormats;

  public ImageUrlsRequestBuilder(final String endpointUrl) {
    mEndpointUrl = Preconditions.checkNotNull(endpointUrl);
    mRequestedImageFormats = new HashMap<>();
  }

  /**
   * Adds imageFormat to the set of image formats you want to download. imageSize specify
   * server-side resize options.
   */
  public ImageUrlsRequestBuilder addImageFormat(ImageFormat imageFormat, ImageSize imageSize) {
    mRequestedImageFormats.put(imageFormat, imageSize);
    return this;
  }

  public ImageUrlsRequest build() {
    ImageUrlsRequest request = new ImageUrlsRequest(mEndpointUrl, mRequestedImageFormats);
    mRequestedImageFormats = null;
    return request;
  }
}
