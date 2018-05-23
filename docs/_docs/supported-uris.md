---
docid: supported-uris
title: Supported URIs
layout: docs
permalink: /docs/supported-uris.html
---

Fresco supports images in a variety of locations. Fresco does **not** accept relative URIs. All URIs must be absolute and must include the scheme.

These are the URI schemes accepted:


| Type | Scheme | Fetch method used
| ---------------- | ------- | ------------- |
| File on network | `http://,` `https://` | `HttpURLConnection` or [network layer](using-other-network-layers.html) |
| File on device | `file://` | `FileInputStream` |
| Content provider | `content://` | `ContentResolver` |
| Asset in app | `asset://` | `AssetManager` |
| Resource in app | `res://` as in `res:///12345` | `Resources.openRawResource` |
| Data in URI | `data:mime/type;base64,` | Following [data URI spec](http://tools.ietf.org/html/rfc2397) (UTF-8 only) |

<br/>
Note: Only image resources can be used with the image pipeline (e.g. a PNG image). Other resource types such as Strings or XML Drawables make no sense in the context of the image pipeline and so cannot be supported by definition. One potentially confusing case is drawable declared in XML (e.g. ShapeDrawable). Important thing to note is that this is **not** an image. If you want to display an XML drawable as the main image, then set it as a [placeholder](placeholder-failure-retry.html) and use the `null` uri.

### Sample: Loading an URI

For a sample that just loads an URI see the `DraweeSimpleFragment` in the showcase app: [DraweeSimpleFragment.java](https://github.com/facebook/fresco/blob/master/samples/showcase/src/main/java/com/facebook/fresco/samples/showcase/drawee/DraweeSimpleFragment.java)

![A simple URI sample](/static/images/docs/01-using-simpledraweeview-sample.png)

### Sample: Loading a Local File

For a sample on how to correctly load user-selected files (e.g. using the `content://` URI) see the `DraweeMediaPickerFragment` in the showcase app: [DraweeMediaPickerFragment.java](https://github.com/facebook/fresco/blob/master/samples/showcase/src/main/java/com/facebook/fresco/samples/showcase/drawee/DraweeMediaPickerFragment.java)

![A sample with local files](/static/images/docs/01-supported-uris-sample-local-file.png)

### Sample: Loading a Data URI

The Fresco showcase app has a [ImageFormatDataUriFragment](https://github.com/facebook/fresco/blob/master/samples/showcase/src/main/java/com/facebook/fresco/samples/showcase/imageformat/datauri/ImageFormatDataUriFragment.java) that demonstrates using placeholder, failure and retry images.

![A data URI sample](/static/images/docs/01-supported-uris-sample-data-uri.png)

### More

**Tip:** You can override the displayed image URI in many samples in the showcase app by using the *URI Override* option in the global settings:

![The URI override setting](/static/images/docs/01-supported-uris-sample-override.png)
