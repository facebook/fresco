---
id: supported-uris
title: 지원하는 URIs
layout: docs-kr
permalink: /docs-kr/supported-uris.html
prev: concepts.html
next: using-drawees-xml.html
---
Fresco는 다양한 환경의 이미지를 지원합니다.

Fresco는 **상대경로 URI는 지원하지 않습니다**. 모든 URI는 절대경로거나 scheme을 포함해야 합니다.

아레는 지원되는 URI scheme 종류입니다:

| Type | Scheme | Fetch method used
| --------------- | ------- | ------------- |
| 네트워크의 파일 | `http://,` `https://` | `HttpURLConnection` 혹은 [네트워크 레이어](using-other-network-layers.html) |
| 단말의 파일 | `file://` | `FileInputStream` |
| 컨텐트 프로비더 | `content://` | `ContentResolver` |
| 앱의 Asset | `asset://` | `AssetManager` |
| 앱의 리소스 | `res://` | `Resources.openRawResource` |


이미지 리소스는 오직 이미지 파이프라인으로만 사용됩니다(e.g a PNG 이미지). String이나 XML Drawable 같은 다른 리소스 타잎은 이미지 파이프라인의 컨텍스트와 일치하지 않고 따라서 정의에 의해 사용될 수 없습니다. 잠재적인 혼란스러운 케이스는 XML에 선언된 drawable입니다(e.g. ShapeDrawable). 기억해야 할 중요한 것은 이것이 **이미지가 아니라는 것입니다**. 만약 XML drawable을 주요 이미지로 화면에 표시하고 싶다면, 대기이미지 - [placeholder](using-drawees-code.html#change_placeholder)-나 `null` uri를 지정하세요.
