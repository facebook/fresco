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

/**
 * Encapsulates url and set of image types together with corresponding
 * resizing options.
 */
public class ImageUrlsRequest {
  final private String mEndpointUrl;
  Map<ImageFormat, ImageSize> mRequestedImageFormats;

  ImageUrlsRequest(final String endpointUrl, Map<ImageFormat, ImageSize> requestedTypes) {
    mEndpointUrl = Preconditions.checkNotNull(endpointUrl);
    mRequestedImageFormats = Preconditions.checkNotNull(requestedTypes);
  }

  public String getEndpointUrl() {
    return mEndpointUrl;
  }

  public ImageSize getImageSize(ImageFormat imageFormat) {
    return mRequestedImageFormats.get(imageFormat);
  }
}
