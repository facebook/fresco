---
id: supported-uris
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
| Resource in app | `res://` | `Resources.openRawResource` |
