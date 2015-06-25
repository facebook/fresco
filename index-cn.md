---
layout: default-cn
title: Fresco 中文说明
id: home
hero: true
---

# 关于 Fresco

Fresco 是一个强大的图片加载组件。

Fresco 中设计有一个叫做 *image pipeline* 的模块。它负责从网络，从本地文件系统，本地资源加载图片。为了最大限度节省空间和CPU时间，它含有3级缓存设计（2级内存，1级文件）。

Fresco 中设计有一个叫做 *Drawees* 模块，方便地显示loading图，当图片不再显示在屏幕上时，及时地释放内存和空间占用。

Fresco 支持 Android2.3(API level 9) 及其以上系统。

# 特性

## 内存管理

解压后的图片，即Android中的`Bitmap`，占用大量的内存。大的内存占用势必引发更加频繁的GC。在5.0以下，GC将会显著地引发界面卡顿。

在5.0以下系统，Fresco将图片放到一个特别的内存区域。当然，在图片不显示的时候，占用的内存会自动被释放。这会使得APP更加流畅，减少因图片内存占用而引发的OOM。

Fresco 在低端机器上表现一样出色，你再也不用因图片内存占用而思前想后。

## 图片的渐进式呈现

渐进式的JPEG图片格式已经流行数年了，渐进式图片格式先呈现大致的图片轮廓，然后随着图片下载的继续，呈现逐渐清晰的图片，这对于移动设备，尤其是慢网络有极大的利好，可带来更好的用户体验。

Android 本身的图片库不支持此格式，但是Fresco支持。使用时，和往常一样，仅仅需要提供一个图片的URI即可，剩下的事情，Fresco会处理。

## Gif图和WebP格式

是的，支持加载Gif图，支持WebP格式。

### 图像的呈现

Fresco 的 Drawees 设计，带来一些有用的特性：

* 自定义居中焦点(对人脸等图片显示非常有帮助)
* 圆角图，当然圆圈也行。
* 下载失败之后，点击重现下载
* 自定义占位图，自定义overlay, 或者进度条
* 指定用户按压时的overlay

## 图像的加载

Fresco 的 image pipeline 设计，允许用户在多方面控制图片的加载：

* 为同一个图片指定不同的远程路径，或者使用已经存在本地缓存中的图片
* 先显示一个低解析度的图片，等高清图下载完之后再显示高清图
* 加载完成回调通知
* 对于本地图，如有EXIF缩略图，在大图加载完成之前，可先显示缩略图
* 缩放或者旋转图片
* 处理已下载的图片
* WebP 支持

# 了解更多

* [相关博客](https://code.facebook.com/posts/366199913563917): Fresco的发布
* [下载](docs-cn/index.html) Fresco
* [文档](docs-cn/getting-started.html)
* [GitHub](https://github.com/facebook/fresco)的源码
