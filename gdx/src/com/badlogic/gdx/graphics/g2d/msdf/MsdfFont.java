/*
 * Copyright 2019 Nicolas Maltais
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.badlogic.gdx.graphics.g2d.msdf;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFontCache;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

import org.jetbrains.annotations.NotNull;


/**
 * Simple wrapper around {@link BitmapFont} to provide values
 * for the glyph size and distance range needed for rendering.
 */
public final class MsdfFont implements Disposable {

    public static final float WEIGHT_LIGHT = -0.1f;
    public static final float WEIGHT_REGULAR = 0f;
    public static final float WEIGHT_BOLD = 0.1f;

    /**
     * The underlying bitmap font, never null.
     */
    private BitmapFont bitmapFont;

    /**
     * The glyphs size in the font texture atlas.
     * This is needed to correctly set the font scale when drawing text.
     * This corresponds to the {@code -s, --font-size} argument
     * in <a href="https://github.com/soimy/msdf-bmfont-xml">msdf-bmfont-xml</a>.
     */
    private float glyphSize;

    /**
     * The range in pixels around the glyphs used to encore the distance field data.
     * This corresponds to the {@code -r, --distance-range} argument
     * in <a href="https://github.com/soimy/msdf-bmfont-xml">msdf-bmfont-xml</a>.
     */
    private float distanceRange;

    public MsdfFont() {}


    /**
     * Create a font from a .fnt file and a .png image file with the same name.
     */
    public MsdfFont(@NotNull FileHandle fontFile, float glyphSize, float distanceRange) {
        this(fontFile, fontFile.sibling(fontFile.nameWithoutExtension() + ".png"),
                glyphSize, distanceRange);
    }

    /**
     * Create a font from a .fnt file and an image file.
     */
    public MsdfFont(@NotNull FileHandle fontFile, @NotNull FileHandle fontRegionFile,
                    float glyphSize, float distanceRange) {
        this(fontFile, getFontRegionFromFile(fontRegionFile), glyphSize, distanceRange);
    }

    /**
     * Create a font from a .fnt file and a texture region.
     */
    public MsdfFont(@NotNull FileHandle fontFile, @NotNull TextureRegion fontRegion,
                    float glyphSize, float distanceRange) {
        this(new BitmapFont(fontFile, fontRegion), glyphSize, distanceRange);
    }

    public MsdfFont(@NotNull FileHandle fontFile, @NotNull FileHandle imageFile,
                    float glyphSize, float distanceRange, boolean flip) {
        this(new BitmapFont(fontFile, imageFile, flip), glyphSize, distanceRange);
    }

    /**
     * Create a font from a bitmap font.
     */
    public MsdfFont(@NotNull BitmapFont bitmapFont, float glyphSize, float distanceRange) {
        //noinspection ConstantConditions
        if (bitmapFont == null) throw new NullPointerException("Font cannot be null");
        this.bitmapFont = bitmapFont;
        this.glyphSize = glyphSize;
        this.distanceRange = distanceRange;
    }


    @NotNull
    public BitmapFont getBitmapFont() {
        return bitmapFont;
    }

    public float getGlyphSize() {
        return glyphSize;
    }

    public float getDistanceRange() {
        return distanceRange;
    }

    @Override
    public void dispose() {
        bitmapFont.dispose();
    }

    private static TextureRegion getFontRegionFromFile(FileHandle file) {
        Texture texture = new Texture(file, Pixmap.Format.RGBA8888, true);
        texture.setFilter(Texture.TextureFilter.MipMapLinearNearest, Texture.TextureFilter.Linear);
        return new TextureRegion(texture);
    }

    public float getAscent() {
    	return bitmapFont.getAscent();
    }

    public float getDescent() {
    	return bitmapFont.getDescent();
    }

    public float getScaleX() {
    	return bitmapFont.getScaleX();
    }

    public float getScaleY() {
    	return bitmapFont.getScaleY();
    }

    public float getCapHeight() {
    	return bitmapFont.getCapHeight();
    }

    public BitmapFont.BitmapFontData getData() {
    	return bitmapFont.getData();
    }

    public void setColor(Color color) {
    	bitmapFont.setColor(color);
    }

    public void setColor (float r, float g, float b, float a) {
        bitmapFont.setColor(r, g, b, a);
    }


    public GlyphLayout draw (Batch batch, CharSequence str, float x, float y) {
        return getBitmapFont().draw(batch, str, x, y);
    }

    public GlyphLayout draw (Batch batch, CharSequence str, float x, float y, float targetWidth, int halign, boolean wrap) {
        return getBitmapFont().draw(batch, str, x, y, targetWidth, halign, wrap);
    }

    public GlyphLayout draw (Batch batch, CharSequence str, float x, float y, int start, int end, float targetWidth, int halign,
                             boolean wrap) {
        return getBitmapFont().draw(batch, str, x, y, start, end, targetWidth, halign, wrap);
    }

    public GlyphLayout draw (Batch batch, CharSequence str, float x, float y, int start, int end, float targetWidth, int halign,
                             boolean wrap, String truncate) {
        return getBitmapFont().draw(batch, str, x, y, start, end, targetWidth, halign, wrap, truncate);
    }

    public void draw (Batch batch, GlyphLayout layout, float x, float y) {
        getBitmapFont().draw(batch, layout, x, y);
    }

    public float getLineHeight() {
    	return bitmapFont.getLineHeight();
    }

    public BitmapFontCache newFontCache() {
    	return bitmapFont.newFontCache();
    }

    public boolean isFlipped() {
    	return bitmapFont.isFlipped();
    }

    public boolean usesIntegerPositions() {
    	return bitmapFont.usesIntegerPositions();
    }

    public TextureRegion getRegion() {
    	return bitmapFont.getRegion();
    }

    @Override
    public String toString() {
        return "MsdfFont{" +
                "bitmapFont=" + bitmapFont +
                ", glyphSize=" + glyphSize +
                ", distanceRange=" + distanceRange +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MsdfFont msdfFont = (MsdfFont) o;

        if (Float.compare(msdfFont.glyphSize, glyphSize) != 0) return false;
        if (Float.compare(msdfFont.distanceRange, distanceRange) != 0) return false;
        return bitmapFont.equals(msdfFont.bitmapFont);
    }

    @Override
    public int hashCode() {
        int result = bitmapFont.hashCode();
        result = 31 * result + (glyphSize != +0.0f ? Float.floatToIntBits(glyphSize) : 0);
        result = 31 * result + (distanceRange != +0.0f ? Float.floatToIntBits(distanceRange) : 0);
        return result;
    }
}