---
id: gotchas
title: Gotchas
layout: docs
permalink: /docs/gotchas.html
prev: writing-custom-views.html
next: intro-image-pipeline.html
---

#### Don't downcast

It is tempting to downcast objects returns by Fresco classes into actual objects that appear to give you greater control. At best, this will result in fragile code that gets broken next release; at worst, it will lead to very subtle bugs.

#### Don't use getTopLevelDrawable

`DraweeHierarchy.getTopLevelDrawable()` should **only** be used by DraweeViews. Client code should almost never interact with it. 

The sole exception is [custom views](writing-custom-views.html). Even there, the top-level drawable should never be downcast. We may change the actual type of the drawable in future releases.

#### Don't re-use DraweeHierarchies

Never call ```DraweeView.setHierarchy``` with the same argument on two different views. Hierarchies are made up of Drawables, and Drawables on Android cannot be shared among multiple views.

#### Don't use Drawables in more than one DraweeHierarchy

This is for the same reason as the above. Drawables cannot be shared in multiple views.

You are completely free, of course, to use the same resourceID in multiple hierarchies and views. Android will create a separate instance of each Drawable for each view.

#### Don't set images directly on a DraweeView

Currently ```DraweeView``` is a subclass of Android's ImageView. This has various methods to set an image (such as setImageBitmap, setImageDrawable)

If you set an image directly, you will completely lose your ```DraweeHierarchy```, and will not get any results from the image pipeline.

#### Don't use ImageView attributes with DraweeView

Any XML attribute of ImageView not found in [View](http://developer.android.com/reference/android/view/View.html) will not work on a DraweeView. We plan to remove them entirely in a future release.
