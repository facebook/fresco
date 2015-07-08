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

/**
 * Image sizes supported by Imgur. To download an image of particular size we have
 * to append appropriate letter after the id of required image but before extension name.
 * For example, if we want a "big square" version of "nice-image.jpeg", then
 * we should request "nice-imageb.jpeg".
 */
public enum ImageSize {
  ORIGINAL_IMAGE(""),
  SMALL_SQUARE("s"),
  BIG_SQUARE("b"),
  SMALL_THUMBNAIL("t"),
  MEDIUM_THUMBNAIL("m"),
  LARGE_THUMBNAIL("l"),
  HUGE_THUMBNAIL("h");

  public final String suffix;
  private ImageSize(final String suffix) {
    this.suffix = suffix;
  }
}
