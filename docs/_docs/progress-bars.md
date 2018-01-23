---
docid: progress-bars
title: Progress Bars
layout: docs
permalink: /docs/progress-bars.html
---

The easiest way to set a progress bar in your application is to use the [ProgressBarDrawable](../javadoc/reference/com/facebook/drawee/drawable/ProgressBarDrawable.html) class when building a hierarchy:

```java
.setProgressBarImage(new ProgressBarDrawable())
```

This shows the progress bar as a dark blue rectangle along the bottom of the Drawee.

### Defining your own progress bar

If you wish to customize your own progress indicator, be aware that in order for it to accurately reflect progress while loading, it needs to override the [Drawable.onLevelChange](http://developer.android.com/reference/android/graphics/drawable/Drawable.html#onLevelChange\(int\)) method:

```java
class CustomProgressBar extends Drawable {
   @Override
   protected boolean onLevelChange(int level) {
     // level is on a scale of 0-10,000
     // where 10,000 means fully downloaded

     // your app's logic to change the drawable's
     // appearance here based on progress
   }
}
```

### Example

The Fresco showcase app has a [DraweeHierarchyFragment](https://github.com/facebook/fresco/blob/master/samples/showcase/src/main/java/com/facebook/fresco/samples/showcase/drawee/DraweeHierarchyFragment.java) that demonstrates using a progress bar drawable.

<video controls="" autoplay="">
  <source src="/static/videos/01-progress-bars.mp4" type="video/mp4">
</video>
