---
docid: shared-transitions
title: Shared Transitions
layout: docs
permalink: /docs/shared-transitions.html
prev: wrap-content.html
next: using-other-network-layers.html
---

## Use ChangeBounds, not ChangeImageTransform

Android 5.0 (Lollipop) introduced [shared element transitions](http://developer.android.com/training/material/animations.html#Transitions), allowing apps to share a View between multiple Activities and define a transition between them.

You can define your transitions in XML. There is a transform called ChangeImageTransform which captures an ImageView's matrix and animates it during the transition. This will not work in Fresco, which has its own set of matrices to scale with.

Fortunately there is an easy workaround. Just use the [ChangeBounds](http://developer.android.com/reference/android/transition/ChangeBounds.html) transition instead. This animates the changes in the layout *bounds*. Fresco will automatically adjust the scaling matrix as you update the bounds, so your animation will appear exactly as you want it.