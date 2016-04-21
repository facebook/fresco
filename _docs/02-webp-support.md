---
docid: webp-support
title: Webp images
layout: docs
permalink: /docs/webp-support.html
prev: closeable-references.html
next: troubleshooting.html
---

Android added webp support in version 4.0 and improved it in 4.2.1:

* 4.0+ (Ice Cream Sandwich): basic webp support
* 4.2.1+ (Jelly Beam MR1): support for transparency and losless wepb

Fresco handles webp images by default if the OS supports it. So you can use webp with 4.0+ and trasparency and losless webps from 4.2.1.

Fresco also supports webp for older OS versions. The only thing you need to do is add the `webpsupport` library to your dependencies. So if you want to use webps on Gingerbread just add the following line to your gradle build file:

```
dependencies {
  // your app's other dependencies
  compile 'com.facebook.fresco:webpsupport:{{site.current_version}}'
}
```
