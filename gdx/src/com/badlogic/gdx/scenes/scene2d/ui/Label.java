/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.badlogic.gdx.scenes.scene2d.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.msdf.MsdfFont;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.StringBuilder;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;


/** A text label, with optional word wrapping.
 * <p>
 * The preferred size of the label is determined by the actual text bounds, unless {@link #setWrap(boolean) word wrap} is enabled.
 * @author Nathan Sweet */
public class Label extends Widget {
	static protected final Color tempColor = new Color();
	static private final GlyphLayout prefSizeLayout = new GlyphLayout();

	private LabelStyle style;
	private final GlyphLayout layout = new GlyphLayout();
	private float prefWidth, prefHeight;
	private final StringBuilder text = new StringBuilder();
	private int intValue = Integer.MIN_VALUE;
	protected BitmapFontCache cache;
	private int labelAlign = Align.left;
	private int lineAlign = Align.left;
	private boolean wrap;
	private float lastPrefHeight;
	private boolean prefSizeInvalid = true;
	private float fontScaleX = 1, fontScaleY = 1;
	private boolean fontScaleChanged = false;
	private @Null String ellipsis;

	public Label (@Null CharSequence text, Skin skin) {
		this(text, skin.get(LabelStyle.class));
	}

	public Label (@Null CharSequence text, Skin skin, String styleName) {
		this(text, skin.get(styleName, LabelStyle.class));
	}

	/** Creates a label, using a {@link LabelStyle} that has a BitmapFont with the specified name from the skin and the specified
	 * color. */
	public Label (@Null CharSequence text, Skin skin, String fontName, Color color) {
		this(text, new LabelStyle(skin.getMsdfFont(fontName), color));
	}

	/** Creates a label, using a {@link LabelStyle} that has a BitmapFont with the specified name and the specified color from the
	 * skin. */
	public Label (@Null CharSequence text, Skin skin, String fontName, String colorName) {
		this(text, new LabelStyle(skin.getMsdfFont(fontName), skin.getColor(colorName)));
	}

	public Label (@Null CharSequence text, LabelStyle style) {
		if (text != null) this.text.append(text);
		style = new LabelStyle(style);
		setStyle(style);
		if (text != null && text.length() > 0) setSize(getPrefWidth(), getPrefHeight());
	}

	public void setStyle (LabelStyle style) {
		if (style == null) throw new IllegalArgumentException("style cannot be null.");
		if (style.font == null) throw new IllegalArgumentException("Missing LabelStyle font.");
		this.style = style;
		setFontScale(style.getSize() / style.font.getGlyphSize());
		for (TextureRegion region : style.font.getBitmapFont().getRegions().items) {
			region.getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		}

		cache = style.font.newFontCache();
		invalidateHierarchy();
	}

	public void setFont(MsdfFont font) {
		if (font == null) throw new IllegalArgumentException("Missing font.");
		this.style.font = font;
		setFontScale(style.getSize() / style.font.getGlyphSize());
		cache = font.newFontCache();
		invalidateHierarchy();
	}

	/** Returns the label's style. Modifying the returned style may not have an effect until {@link #setStyle(LabelStyle)} is
	 * called. */
	public LabelStyle getStyle () {
		return style;
	}

	@Override
	public void setColor(Color color) {
		style.setFontColor(color);
	}

	@Override
	public void setColor(float r, float g, float b, float a) {
		style.setFontColor(r, g, b, a);
	}

	@Override
	public Color getColor() {
		return style.getFontColor();
	}

	/** Sets the text to the specified integer value. If the text is already equivalent to the specified value, a string is not
	 * allocated.
	 * @return true if the text was changed. */
	public boolean setText (int value) {
		if (this.intValue == value) return false;
		text.clear();
		text.append(value);
		intValue = value;
		invalidateHierarchy();
		return true;
	}

	/** @param newText If null, "" will be used. */
	public void setText (@Null CharSequence newText) {
		if (newText == null) {
			if (text.length == 0) return;
			text.clear();
		} else if (newText instanceof StringBuilder) {
			if (text.equals(newText)) return;
			text.clear();
			text.append((StringBuilder)newText);
		} else {
			if (textEquals(newText)) return;
			text.clear();
			text.append(newText);
		}
		intValue = Integer.MIN_VALUE;
		invalidateHierarchy();
	}

	public boolean textEquals (CharSequence other) {
		int length = text.length;
		char[] chars = text.chars;
		if (length != other.length()) return false;
		for (int i = 0; i < length; i++)
			if (chars[i] != other.charAt(i)) return false;
		return true;
	}

	public StringBuilder getText () {
		return text;
	}

	public void invalidate () {
		super.invalidate();
		prefSizeInvalid = true;
	}

	private void scaleAndComputePrefSize () {
		BitmapFont font = cache.getFont();
		float oldScaleX = font.getScaleX();
		float oldScaleY = font.getScaleY();
		if (fontScaleChanged) font.getData().setScale(fontScaleX, fontScaleY);

		computePrefSize(Label.prefSizeLayout);

		if (fontScaleChanged) font.getData().setScale(oldScaleX, oldScaleY);
	}

	protected void computePrefSize (GlyphLayout layout) {
		prefSizeInvalid = false;
		if (wrap && ellipsis == null) {
			float width = getWidth();
			if (style.background != null) {
				width = Math.max(width, style.background.getMinWidth()) - style.background.getPaddingOutLeft()
					- style.background.getPaddingOutRight();
			}
			layout.setText(cache.getFont(), text, Color.WHITE, width, Align.left, true);
		} else
			layout.setText(cache.getFont(), text);
		prefWidth = layout.width;
		prefHeight = layout.height;
	}

	public void layout () {
		BitmapFont font = cache.getFont();
		float oldScaleX = font.getScaleX();
		float oldScaleY = font.getScaleY();
		if (fontScaleChanged) font.getData().setScale(fontScaleX, fontScaleY);

		boolean wrap = this.wrap && ellipsis == null;
		if (wrap) {
			float prefHeight = getPrefHeight();
			if (prefHeight != lastPrefHeight) {
				lastPrefHeight = prefHeight;
				invalidateHierarchy();
			}
		}

		float width = getWidth(), height = getHeight();
		Drawable background = style.background;
		float x = 0, y = 0;
		if (background != null) {
			x = background.getPaddingOutLeft();
			y = background.getPaddingOutBottom();
			width -= background.getPaddingOutLeft() + background.getPaddingOutRight();
			height -= background.getPaddingOutBottom() + background.getPaddingOutTop();
		}

		GlyphLayout layout = this.layout;
		float textWidth, textHeight;
		if (wrap || text.indexOf("\n") != -1) {
			// If the text can span multiple lines, determine the text's actual size so it can be aligned within the label.
			layout.setText(font, text, 0, text.length, Color.WHITE, width, lineAlign, wrap, ellipsis);
			textWidth = layout.width;
			textHeight = layout.height;

			if ((labelAlign & Align.left) == 0) {
				if ((labelAlign & Align.right) != 0)
					x += width - textWidth;
				else
					x += (width - textWidth) / 2;
			}
		} else {
			textWidth = width;
			textHeight = font.getData().capHeight;
		}

		if ((labelAlign & Align.top) != 0) {
			y += cache.getFont().isFlipped() ? 0 : height - textHeight;
			y += style.font.getDescent();
		} else if ((labelAlign & Align.bottom) != 0) {
			y += cache.getFont().isFlipped() ? height - textHeight : 0;
			y -= style.font.getDescent();
		} else {
			y += (height - textHeight) / 2;
		}
		if (!cache.getFont().isFlipped()) y += textHeight;

		layout.setText(font, text, 0, text.length, Color.WHITE, textWidth, lineAlign, wrap, ellipsis);
		cache.setText(layout, x, y);

		if (fontScaleChanged) font.getData().setScale(oldScaleX, oldScaleY);
	}

	public void draw (Batch batch, float parentAlpha) {
		validate();
		Color color = tempColor.set(getColor());
		color.a *= parentAlpha;
		if (style.background != null) {
			batch.setColor(color.r, color.g, color.b, color.a);
			style.background.draw(batch, getX(), getY(), getWidth(), getHeight());
		}
		cache.tint(color);
		cache.setPosition(getX(), getY());
		if (batch instanceof SpriteBatch) {
			((SpriteBatch) batch).setLabelStyle(style);
		}
		cache.draw(batch);
		batch.setShader(null);
	}

	public float getPrefWidth () {
		if (wrap) return 0;
		if (prefSizeInvalid) scaleAndComputePrefSize();
		float width = prefWidth;
		Drawable background = style.background;
		if (background != null)
			width = Math.max(width + background.getPaddingOutLeft() + background.getPaddingOutRight(), background.getMinWidth());
		return width;
	}

	public float getPrefHeight () {
		if (prefSizeInvalid) scaleAndComputePrefSize();
		float descentScaleCorrection = 1;
		if (fontScaleChanged) descentScaleCorrection = fontScaleY / style.font.getScaleY();
		float height = prefHeight - style.font.getDescent() * descentScaleCorrection * 2;
		Drawable background = style.background;
		if (background != null)
			height = Math.max(height + background.getPaddingOutTop() + background.getPaddingOutBottom(), background.getMinHeight());
		return height;
	}

	public GlyphLayout getGlyphLayout () {
		return layout;
	}

	/** If false, the text will only wrap where it contains newlines (\n). The preferred size of the label will be the text bounds.
	 * If true, the text will word wrap using the width of the label. The preferred width of the label will be 0, it is expected
	 * that something external will set the width of the label. Wrapping will not occur when ellipsis is enabled. Default is false.
	 * <p>
	 * When wrap is enabled, the label's preferred height depends on the width of the label. In some cases the parent of the label
	 * will need to layout twice: once to set the width of the label and a second time to adjust to the label's new preferred
	 * height. */
	public void setWrap (boolean wrap) {
		this.wrap = wrap;
		invalidateHierarchy();
	}

	public boolean getWrap () {
		return wrap;
	}

	public int getLabelAlign () {
		return labelAlign;
	}

	public int getLineAlign () {
		return lineAlign;
	}

	/** @param alignment Aligns all the text within the label (default left center) and each line of text horizontally (default
	 *           left).
	 * @see Align */
	public void setAlignment (int alignment) {
		setAlignment(alignment, alignment);
	}

	/** @param labelAlign Aligns all the text within the label (default left center).
	 * @param lineAlign Aligns each line of text horizontally (default left).
	 * @see Align */
	public void setAlignment (int labelAlign, int lineAlign) {
		this.labelAlign = labelAlign;

		if ((lineAlign & Align.left) != 0)
			this.lineAlign = Align.left;
		else if ((lineAlign & Align.right) != 0)
			this.lineAlign = Align.right;
		else
			this.lineAlign = Align.center;

		invalidate();
	}

	public void setFontSize(float size) {
		style.size = size;
		setFontScale(style.getSize() / style.font.getGlyphSize());
	}

	public void setFontScale (float fontScale) {
		setFontScale(fontScale, fontScale);
	}

	public void setFontScale (float fontScaleX, float fontScaleY) {
		fontScaleChanged = true;
		this.fontScaleX = fontScaleX;
		this.fontScaleY = fontScaleY;
		invalidateHierarchy();
	}

	public float getFontScaleX () {
		return fontScaleX;
	}

	public void setFontScaleX (float fontScaleX) {
		fontScaleChanged = true;
		this.fontScaleX = fontScaleX;
		invalidateHierarchy();
	}

	public float getFontScaleY () {
		return fontScaleY;
	}

	public void setFontScaleY (float fontScaleY) {
		fontScaleChanged = true;
		this.fontScaleY = fontScaleY;
		invalidateHierarchy();
	}

	/** When non-null the text will be truncated "..." if it does not fit within the width of the label. Wrapping will not occur
	 * when ellipsis is enabled. Default is false. */
	public void setEllipsis (@Null String ellipsis) {
		this.ellipsis = ellipsis;
	}

	/** When true the text will be truncated "..." if it does not fit within the width of the label. Wrapping will not occur when
	 * ellipsis is true. Default is false. */
	public void setEllipsis (boolean ellipsis) {
		if (ellipsis)
			this.ellipsis = "...";
		else
			this.ellipsis = null;
	}

	/** Allows subclasses to access the cache in {@link #draw(Batch, float)}. */
	protected BitmapFontCache getBitmapFontCache () {
		return cache;
	}

	public String toString () {
		String name = getName();
		if (name != null) return name;
		String className = getClass().getName();
		int dotIndex = className.lastIndexOf('.');
		if (dotIndex != -1) className = className.substring(dotIndex + 1);
		return (className.indexOf('$') != -1 ? "Label " : "") + className + ": " + text;
	}

	/** The style for a label, see {@link Label}.
	 * @author Nathan Sweet */
	static public class LabelStyle {
		private Color fontColor = new Color(Color.WHITE);
		private @Null Drawable background;
		public MsdfFont font;


		public static final float WEIGHT_LIGHT = -0.1f;
		public static final float WEIGHT_REGULAR = 0f;
		public static final float WEIGHT_BOLD = 0.1f;


		/**
		 * The font name for this font style. Must not be null.
		 * Used by {@link Label} to get a {@link MsdfFont} from a skin by name.
		 */
		@NotNull
		private String fontName = "default";

		/**
		 * The font size in pixels.
		 */
		private float size = 32f;

		/**
		 * The font weight, from -0.5 to 0.5. Higher values result in thicker fonts.
		 * The resulting effect depends on {@link MsdfFont#getDistanceRange()} and values
		 * near -0.5 and 0.5 will most always produce rendering artifacts.
		 * 0 should always look the most like the original font.
		 */
		private float weight = WEIGHT_REGULAR;

		/**
		 * Whether to clip shadow under the glyph. When the glyph is
		 * drawn in a translucent color, shadow will appear behind it if not clipped.
		 */
		private boolean shadowClipped = false;

		/**
		 * The color of the outer shadow, can be translucent.
		 * Use transparent for no shadow.
		 */
		@NotNull
		private Color shadowColor = new Color();

		/**
		 * The drawn shadow offset in pixels, relative to the size of glyph in the font image.
		 * Offset is in a Y positive down coordinate system.
		 * Placing the shadow too far from the glyph can create 2 issues:
		 * <ul>
		 * <li>Other glyphs of the texture atlas may appear on the sides. Increasing the padding
		 * value when generating the font can prevent it.</li>
		 * <li>Some parts of the shadow may be cut by the next glyph drawn.</li>
		 * </ul>
		 */
		@NotNull
		private Vector2 shadowOffset = new Vector2(2f, 2f);

		/**
		 * Defines the smoothess of the shadow edges. Value should be between 0 to 0.5.
		 * A value of 0 looks rough because it doesn't have antialiasing.
		 */
		private float shadowSmoothing = 0.1f;


		/**
		 * The color of the inner shadow, can be translucent.
		 * Use transparent for no shadow.
		 */
		@NotNull
		private Color innerShadowColor = new Color();

		/**
		 * The inner shadow range, from 0 to 0.5.
		 */
		private float innerShadowRange = 0.3f;

		/**
		 * The intensity of the shadow.
		 */
		private float shadowIntensity = 1f;


		public LabelStyle() {
			// Default constructor
		}

		public LabelStyle(LabelStyle style) {
			font = style.font;
			fontName = style.fontName;
			size = style.size;
			weight = style.weight;
			shadowClipped = style.shadowClipped;
			shadowColor = style.shadowColor.cpy();
			shadowOffset = style.shadowOffset.cpy();
			shadowSmoothing = style.shadowSmoothing;
			innerShadowColor = style.innerShadowColor.cpy();
			innerShadowRange = style.innerShadowRange;
			fontColor = style.fontColor == null ? Color.WHITE.cpy() : style.fontColor.cpy();
			background = style.background;
			shadowIntensity = style.shadowIntensity;
		}

		public LabelStyle (MsdfFont font, @Null Color fontColor) {
			this.font = font;
			this.fontColor.set(fontColor);
		}


		@NotNull
		public String getFontName() {
			return fontName;
		}

		public LabelStyle setFontName(@NotNull String fontName) {
			this.fontName = fontName;
			return this;
		}

		public float getSize() {
			return size;
		}

		public LabelStyle setSize(float size) {
			this.size = size;
			return this;
		}

		public float getWeight() {
			return weight;
		}

		public LabelStyle setWeight(float weight) {
			this.weight = weight;
			return this;
		}

		public boolean isShadowClipped() {
			return shadowClipped;
		}

		public LabelStyle setShadowClipped(boolean shadowClipped) {
			this.shadowClipped = shadowClipped;
			return this;
		}

		@NotNull
		public Color getShadowColor() {
			return shadowColor;
		}

		public LabelStyle setShadowColor(@NotNull Color shadowColor) {
			//noinspection ConstantConditions
			if (shadowColor == null) throw new NullPointerException("Shadow color cannot be null.");

			this.shadowColor = shadowColor;
			return this;
		}

		@NotNull
		public Vector2 getShadowOffset() {
			return shadowOffset;
		}

		public LabelStyle setShadowOffset(@NotNull Vector2 shadowOffset) {
			//noinspection ConstantConditions
			if (shadowColor == null) throw new NullPointerException("Shadow offset cannot be null.");

			this.shadowOffset = shadowOffset;
			return this;
		}

		public float getShadowSmoothing() {
			return shadowSmoothing;
		}

		public LabelStyle setShadowSmoothing(float shadowSmoothing) {
			this.shadowSmoothing = shadowSmoothing;
			return this;
		}

		@NotNull
		public Color getInnerShadowColor() {
			return innerShadowColor;
		}

		public LabelStyle setInnerShadowColor(@NotNull Color innerShadowColor) {
			this.innerShadowColor = innerShadowColor;
			return this;
		}

		public float getInnerShadowRange() {
			return innerShadowRange;
		}

		public LabelStyle setInnerShadowRange(float innerShadowRange) {
			this.innerShadowRange = innerShadowRange;
			return this;
		}

		public Color getFontColor() {
			return fontColor;
		}

		public LabelStyle setFontColor(Color fontColor) {
			this.fontColor.set(fontColor);
			return this;
		}

		public LabelStyle setFontColor(float r, float g, float b, float a) {
			this.fontColor.set(r, g, b, a);
			return this;
		}

		public Drawable getBackground() {
			return background;
		}

		public LabelStyle setBackground(Drawable background) {
			this.background = background;
			return this;
		}

		public MsdfFont getFont() {
			return font;
		}

		public LabelStyle setFont(MsdfFont font) {
			this.font = font;
			return this;
		}

		public float getShadowIntensity() {
			return shadowIntensity;
		}

		public void setShadowIntensity(float shadowIntensity) {
			this.shadowIntensity = shadowIntensity;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			LabelStyle that = (LabelStyle) o;

			if (Float.compare(that.size, size) != 0) return false;
			if (Float.compare(that.weight, weight) != 0) return false;
			if (shadowClipped != that.shadowClipped) return false;
			if (Float.compare(that.shadowSmoothing, shadowSmoothing) != 0) return false;
			if (Float.compare(that.innerShadowRange, innerShadowRange) != 0) return false;
			if (Float.compare(that.shadowIntensity, shadowIntensity) != 0) return false;
			if (!fontColor.equals(that.fontColor)) return false;
			if (!Objects.equals(background, that.background))
				return false;
			if (!font.equals(that.font)) return false;
			if (!fontName.equals(that.fontName)) return false;
			if (!shadowColor.equals(that.shadowColor)) return false;
			if (!shadowOffset.equals(that.shadowOffset)) return false;
			return innerShadowColor.equals(that.innerShadowColor);
		}

		@Override
		public int hashCode() {
			int result = fontColor.hashCode();
			result = 31 * result + (background != null ? background.hashCode() : 0);
			result = 31 * result + font.hashCode();
			result = 31 * result + fontName.hashCode();
			result = 31 * result + (size != +0.0f ? Float.floatToIntBits(size) : 0);
			result = 31 * result + (weight != +0.0f ? Float.floatToIntBits(weight) : 0);
			result = 31 * result + (shadowClipped ? 1 : 0);
			result = 31 * result + shadowColor.hashCode();
			result = 31 * result + shadowOffset.hashCode();
			result = 31 * result + (shadowSmoothing != +0.0f ? Float.floatToIntBits(shadowSmoothing) : 0);
			result = 31 * result + innerShadowColor.hashCode();
			result = 31 * result + (innerShadowRange != +0.0f ? Float.floatToIntBits(innerShadowRange) : 0);
			result = 31 * result + (shadowIntensity != +0.0f ? Float.floatToIntBits(shadowIntensity) : 0);
			return result;
		}
	}
}
