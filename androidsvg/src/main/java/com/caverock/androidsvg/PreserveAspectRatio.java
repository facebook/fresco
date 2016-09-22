/*
   Copyright 2013 Paul LeBeau, Cave Rock Software Ltd.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.caverock.androidsvg;

/**
 * The SVGPositioning class tells the renderer how to scale and position the
 * SVG document in the current viewport.  It is roughly equivalent to the
 * {@code preserveAspectRatio} attribute of an {@code <svg>} element. 
 * <p>
 * In order for scaling to happen, the SVG document must have a viewBox attribute set.
 * For example:
 * 
 * <pre>
 * {@code
 * <svg version="1.1" viewBox="0 0 200 100">
 * }
 * </pre>
 * 
 */
public class PreserveAspectRatio
{
   private Alignment  alignment;
   private Scale      scale;

   /**
    * Draw doucment at its natural position and scale.
    */
   public static final PreserveAspectRatio  UNSCALED = new PreserveAspectRatio(null, null);

   /**
    * Stretch horizontally and vertically to fill the viewport.
    */
   public static final PreserveAspectRatio  STRETCH = new PreserveAspectRatio(Alignment.None, null);

   /**
    * Keep the document's aspect ratio, but scale it so that it fits neatly inside the viewport.
    * <p>
    * The document will be centred in the viewport and may have blank strips at either the top and
    * bottom of the viewport or at the sides. 
    */
   public static final PreserveAspectRatio  LETTERBOX = new PreserveAspectRatio(Alignment.XMidYMid,
           Scale.Meet);

   /**
    * Keep the document's aspect ratio, but scale it so that it fits neatly inside the viewport.
    * <p>
    * The document will be positioned at the top of tall and narrow viewports,
     and at the left of short and wide viewports.
    */
   public static final PreserveAspectRatio  START = new PreserveAspectRatio(Alignment.XMinYMin,
           Scale.Meet);

   /**
    * Keep the document's aspect ratio, but scale it so that it fits neatly inside the viewport.
    * <p>
    * The document will be positioned at the bottom of tall and narrow viewports,
    * and at the right of short and wide viewports.
    */
   public static final PreserveAspectRatio  END = new PreserveAspectRatio(Alignment.XMaxYMax,
           Scale.Meet);

   /**
    * Keep the document's aspect ratio, but scale it so that it fits neatly inside the viewport.
    * <p>
    * The document will be positioned at the top of tall and narrow viewports, and at the centre of
    * short and wide viewports.
    */
   public static final PreserveAspectRatio  TOP = new PreserveAspectRatio(Alignment.XMidYMin,
           Scale.Meet);

   /**
    * Keep the document's aspect ratio, but scale it so that it fits neatly inside the viewport.
    * <p>
    * The document will be positioned at the bottom of tall and narrow viewports,
    * and at the centre of short and wide viewports.
    */
   public static final PreserveAspectRatio  BOTTOM = new PreserveAspectRatio(Alignment.XMidYMax,
           Scale.Meet);

   /**
    * Keep the document's aspect ratio, but scale it so that it fills the entire viewport.
    * This may result in some of the document falling outside the viewport.
    * <p>
    * The document will be positioned so that the centre of the document will always be visible,
    * but the edges of the document may not.
    */
   public static final PreserveAspectRatio  FULLSCREEN = new PreserveAspectRatio(Alignment.XMidYMid,
           Scale.Slice);

   /**
    * Keep the document's aspect ratio, but scale it so that it fills the entire viewport.
    * This may result in some of the document falling outside the viewport.
    * <p>
    * The document will be positioned so that the top left of the document will always be visible,
    * but the right hand or bottom edge may not.
    */
   public static final PreserveAspectRatio  FULLSCREEN_START =
           new PreserveAspectRatio(Alignment.XMinYMin, Scale.Slice);



   /**
    * Determines how the document is to me positioned relative to the viewport (normally the canvas)
    * <p>
    * For the value {@code none}, the document is stretched to fit the viewport dimensions. For all
    * other values, the aspect ratio of the document is kept the same but the document is scaled to
    * fit the viewport. 
    */
   public enum Alignment
   {
      /** Document is stretched to fit both the width and height of the viewport.
       * When using this Alignment value, the value of Scale is not used and will be ignored. */
      None,
      /** Document is positioned at the top left of the viewport. */
      XMinYMin,
      /** Document is positioned at the centre top of the viewport. */
      XMidYMin,
      /** Document is positioned at the top right of the viewport. */
      XMaxYMin,
      /** Document is positioned at the middle left of the viewport. */
      XMinYMid,
      /** Document is centred in the viewport both vertically and horizontally. */
      XMidYMid,
      /** Document is positioned at the middle right of the viewport. */
      XMaxYMid,
      /** Document is positioned at the bottom left of the viewport. */
      XMinYMax,
      /** Document is positioned at the bottom centre of the viewport. */
      XMidYMax,
      /** Document is positioned at the bottom right of the viewport. */
      XMaxYMax
   }


   /**
    * Determine whether the scaled document fills the viewport entirely or is scaled to
    * fill the viewport without overflowing.
    */
   public enum Scale
   {
      /**
       * The document is scaled so that it is as large as possible without overflowing the viewport.
       * There may be blank areas on one or more sides of the document.
       */
      Meet,
      /**
       * The document is scaled so that entirely fills the viewport. That means that some of the
       * document may fall outside the viewport and will not be rendered.
       */
      Slice
   }


   public PreserveAspectRatio(Alignment alignment, Scale scale)
   {
      this.alignment = alignment;
      this.scale = scale;
   }


   /**
    * Returns the alignment value of this instance.
    * @return the alignment
    */
   public Alignment  getAlignment()
   {
      return alignment;
   }


   /**
    * Returns the scale value of this instance.
    * @return the scale
    */
   public Scale  getScale()
   {
      return scale;
   }


   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      PreserveAspectRatio other = (PreserveAspectRatio) obj;
      if (alignment != other.alignment)
         return false;
      if (scale != other.scale)
         return false;
      return true;
   }

}
