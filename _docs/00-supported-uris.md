---
docid: supported-uris
title: Supported URIs
layout: docs
permalink: /docs/supported-uris.html
prev: concepts.html
next: using-drawees-xml.html
---

Fresco supports images in a variety of locations.

Fresco does **not** accept relative URIs. All URIs must be absolute and must include the scheme.

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
Note: Only image resources can be used with the image pipeline (e.g. a PNG image). Other resource types such as Strings or XML Drawables make no sense in the context of the image pipeline and so cannot be supported by definition. One potentially confusing case is drawable declared in XML (e.g. ShapeDrawable). Important thing to note is that this is **not** an image. If you want to display an XML drawable as the main image, then set it as a [placeholder](using-drawees-code.html#change_placeholder) and use the `null` uri.
