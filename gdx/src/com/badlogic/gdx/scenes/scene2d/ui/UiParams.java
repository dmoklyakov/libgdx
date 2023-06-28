package com.badlogic.gdx.scenes.scene2d.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector4;

import java.util.Objects;

public class UiParams {
    private final Vector4 cornerRadii = new Vector4(0f, 0f, 0f, 0f); // px: [topLeft, topRight, bottomRight, bottomLeft].
    private final Color borderColor = new Color(Color.CLEAR);
    private float contentScale = 1f;
    private float borderOutSoftness = 0f; // px.
    private float borderThickness = 0f; // px.
    private float borderInSoftness = 0f; // px.
    private boolean autoScaleContent = true;
    private boolean circleCorners = false;

    private final Vector4 tmpVector = new Vector4();

    public boolean isDefault() {
        return cornerRadii.x == 0f && cornerRadii.y == 0f && cornerRadii.z == 0f && cornerRadii.w == 0f &&
                (borderColor.equals(Color.CLEAR) || borderThickness == 0f) &&
                (contentScale == 1f || autoScaleContent) &&
                borderOutSoftness == 0f &&
                borderInSoftness == 0f &&
                !circleCorners;
    }

    public Vector4 getCornerRadii() {
        return cornerRadii;
    }

    public Vector4 getAutoCornerRadii(float width, float height) {
        if (circleCorners) {
            float radius = Math.min(width, height) / 2f;
            return tmpVector.set(radius, radius, radius, radius);
        } else {
            return cornerRadii;
        }
    }

    public void setCornerRadius(float radius) {
        circleCorners = false;
        cornerRadii.x = radius;
        cornerRadii.y = radius;
        cornerRadii.z = radius;
        cornerRadii.w = radius;
    }

    public void setCornerRadii(float topLeft, float topRight, float bottomRight, float bottomLeft) {
        circleCorners = false;
        cornerRadii.x = topLeft;
        cornerRadii.y = topRight;
        cornerRadii.z = bottomRight;
        cornerRadii.w = bottomLeft;
    }

    public void setCornerRadii(Vector4 cornerRadii) {
        circleCorners = false;
        this.cornerRadii.set(cornerRadii);
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(float r, float g, float b, float a) {
        borderColor.set(r, g, b, a);
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor.set(borderColor);
    }

    public float getContentScale() {
        return contentScale;
    }

    public float getContentScaleForSize(float width, float height) {
        if (autoScaleContent) {
            float minSide = Math.min(width, height);
            return (minSide - borderThickness * 2f - borderOutSoftness * 2f) / minSide;
        } else {
            return contentScale;
        }
    }

    public void setContentScale(float contentScale) {
        autoScaleContent = false;
        this.contentScale = contentScale;
    }

    public float getBorderOutSoftness() {
        return borderOutSoftness;
    }

    public void setBorderOutSoftness(float borderOutSoftness) {
        this.borderOutSoftness = borderOutSoftness;
    }

    public float getBorderThickness() {
        return borderThickness;
    }

    public void setBorderThickness(float borderThickness) {
        this.borderThickness = borderThickness;
    }

    public float getBorderInSoftness() {
        return borderInSoftness;
    }

    public void setBorderInSoftness(float borderInSoftness) {
        this.borderInSoftness = borderInSoftness;
    }

    public boolean isAutoScaleContent() {
        return autoScaleContent;
    }

    public void setAutoScaleContent(boolean autoScaleContent) {
        this.autoScaleContent = autoScaleContent;
    }

    public boolean isCircleCorners() {
        return circleCorners;
    }

    public void setCircleCorners(boolean circleCorners) {
        this.circleCorners = circleCorners;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UiParams uiParams = (UiParams) o;
        return Float.compare(uiParams.contentScale, contentScale) == 0 && Float.compare(uiParams.borderOutSoftness, borderOutSoftness) == 0 && Float.compare(uiParams.borderThickness, borderThickness) == 0 && Float.compare(uiParams.borderInSoftness, borderInSoftness) == 0 && autoScaleContent == uiParams.autoScaleContent && circleCorners == uiParams.circleCorners && cornerRadii.equals(uiParams.cornerRadii) && borderColor.equals(uiParams.borderColor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cornerRadii, borderColor, contentScale, borderOutSoftness, borderThickness, borderInSoftness, autoScaleContent, circleCorners);
    }
}
