package com.badlogic.gdx.graphics.g2d.msdf;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;

import java.util.Objects;

public class MsdfTextParams {

    private Color shadowColor = new Color();
    private Color innerShadowColor = new Color();
    private Vector2 shadowOffset = new Vector2();
    private float distanceRange;
    private float size;
    private float glyphSize;
    private float weight;
    private boolean shadowClipped;
    private float shadowSmoothing;
    private float innerShadowRange;
    private float shadowIntensity;

    public MsdfTextParams() {}

    public void update(Label.LabelStyle style) {
        shadowColor.set(style.getShadowColor());
        innerShadowColor.set(style.getInnerShadowColor());
        shadowOffset.set(style.getShadowOffset());
        distanceRange = style.font.getDistanceRange();
        size = style.getSize();
        glyphSize = style.font.getGlyphSize();
        weight = style.getWeight();
        shadowClipped = style.isShadowClipped();
        shadowSmoothing = style.getShadowSmoothing();
        innerShadowRange = style.getInnerShadowRange();
        shadowIntensity = style.getShadowIntensity();
    }

    public void update(TextField.TextFieldStyle style, boolean isMessageText) {
        shadowColor.set(style.getShadowColor());
        innerShadowColor.set(style.getInnerShadowColor());
        shadowOffset.set(style.getShadowOffset());
        MsdfFont font = isMessageText ? style.messageFont : style.font;
        distanceRange = font.getDistanceRange();
        size = style.getSize();
        glyphSize = font.getGlyphSize();
        weight = style.getWeight();
        shadowClipped = style.isShadowClipped();
        shadowSmoothing = style.getShadowSmoothing();
        innerShadowRange = style.getInnerShadowRange();
        shadowIntensity = style.getShadowIntensity();
    }

    public void update(MsdfTextParams params) {
        shadowColor.set(params.shadowColor);
        innerShadowColor.set(params.innerShadowColor);
        shadowOffset.set(params.shadowOffset);
        distanceRange = params.distanceRange;
        size = params.size;
        glyphSize = params.glyphSize;
        weight = params.weight;
        shadowClipped = params.shadowClipped;
        shadowSmoothing = params.shadowSmoothing;
        innerShadowRange = params.innerShadowRange;
        shadowIntensity = params.shadowIntensity;
    }

    public Color getShadowColor() {
        return shadowColor;
    }

    public Color getInnerShadowColor() {
        return innerShadowColor;
    }

    public Vector2 getShadowOffset() {
        return shadowOffset;
    }

    public float getDistanceRange() {
        return distanceRange;
    }

    public float getSize() {
        return size;
    }

    public float getWeight() {
        return weight;
    }

    public float getGlyphSize() {
        return glyphSize;
    }

    public boolean isShadowClipped() {
        return shadowClipped;
    }

    public float getShadowSmoothing() {
        return shadowSmoothing;
    }

    public float getInnerShadowRange() {
        return innerShadowRange;
    }

    public float getShadowIntensity() {
        return shadowIntensity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MsdfTextParams that = (MsdfTextParams) o;
        return Float.compare(that.distanceRange, distanceRange) == 0 &&
                Float.compare(that.size, size) == 0 &&
                Float.compare(that.glyphSize, glyphSize) == 0 &&
                Float.compare(that.weight, weight) == 0 &&
                shadowClipped == that.shadowClipped &&
                Float.compare(that.shadowSmoothing, shadowSmoothing) == 0 &&
                Float.compare(that.innerShadowRange, innerShadowRange) == 0 &&
                Float.compare(that.shadowIntensity, shadowIntensity) == 0 &&
                shadowColor.equals(that.shadowColor) &&
                innerShadowColor.equals(that.innerShadowColor) &&
                shadowOffset.equals(that.shadowOffset);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shadowColor,
                innerShadowColor,
                shadowOffset,
                distanceRange,
                size,
                glyphSize,
                weight,
                shadowClipped,
                shadowSmoothing,
                innerShadowRange,
                shadowIntensity);
    }
}
