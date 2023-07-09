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

package com.badlogic.gdx.scenes.scene2d.utils;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.reflect.ClassReflection;

/** Drawable that stores the size information but doesn't draw anything.
 * @author Nathan Sweet */
public class BaseDrawable implements Drawable {
	private @Null String name;
	private float paddingOutLeft, paddingOutRight, paddingOutTop, paddingOutBottom, minWidth, minHeight;
	private float paddingInLeft, paddingInTop, paddingInRight, paddingInBottom;

	public BaseDrawable () {
	}

	/** Creates a new empty drawable with the same sizing information as the specified drawable. */
	public BaseDrawable (Drawable drawable) {
		if (drawable instanceof BaseDrawable) name = ((BaseDrawable)drawable).getName();
		paddingOutLeft = drawable.getPaddingOutLeft();
		paddingOutTop = drawable.getPaddingOutTop();
		paddingOutRight = drawable.getPaddingOutRight();
		paddingOutBottom = drawable.getPaddingOutBottom();
		paddingInLeft = drawable.getPaddingInLeft();
		paddingInTop = drawable.getPaddingInTop();
		paddingInRight = drawable.getPaddingInRight();
		paddingInBottom = drawable.getPaddingInBottom();
		minWidth = drawable.getMinWidth();
		minHeight = drawable.getMinHeight();
	}

	public void draw (Batch batch, float x, float y, float width, float height) {
	}

	public float getPaddingOutLeft() {
		return paddingOutLeft;
	}

	public void setPaddingOutLeft(float paddingOutLeft) {
		this.paddingOutLeft = paddingOutLeft;
	}

	public float getPaddingOutTop() {
		return paddingOutTop;
	}

	public void setPaddingOutTop(float paddingOutTop) {
		this.paddingOutTop = paddingOutTop;
	}

	public float getPaddingOutRight() {
		return paddingOutRight;
	}

	public void setPaddingOutRight(float paddingOutRight) {
		this.paddingOutRight = paddingOutRight;
	}

	public float getPaddingOutBottom() {
		return paddingOutBottom;
	}

	public void setPaddingOutBottom(float paddingOutBottom) {
		this.paddingOutBottom = paddingOutBottom;
	}

	public void setOutPaddings(float left, float top, float right, float bottom) {
		setPaddingOutLeft(left);
		setPaddingOutTop(top);
		setPaddingOutRight(right);
		setPaddingOutBottom(bottom);
	}

	public void setOutPaddings(float padding) {
		setPaddingOutLeft(padding);
		setPaddingOutTop(padding);
		setPaddingOutRight(padding);
		setPaddingOutBottom(padding);
	}

	public float getPaddingInLeft() {
		return paddingInLeft;
	}

	public void setPaddingInLeft(float paddingInLeft) {
		this.paddingInLeft = paddingInLeft;
	}

	public float getPaddingInTop() {
		return paddingInTop;
	}

	public void setPaddingInTop(float paddingInTop) {
		this.paddingInTop = paddingInTop;
	}

	public float getPaddingInRight() {
		return paddingInRight;
	}

	public void setPaddingInRight(float paddingInRight) {
		this.paddingInRight = paddingInRight;
	}

	public float getPaddingInBottom() {
		return paddingInBottom;
	}

	public void setPaddingInBottom(float paddingInBottom) {
		this.paddingInBottom = paddingInBottom;
	}

	public void setInPaddings(float left, float top, float right, float bottom) {
		setPaddingInLeft(left);
		setPaddingInTop(top);
		setPaddingInRight(right);
		setPaddingInBottom(bottom);
	}

	public void setInPaddings(float padding) {
		setPaddingInLeft(padding);
		setPaddingInTop(padding);
		setPaddingInRight(padding);
		setPaddingInBottom(padding);
	}

	public float getMinWidth () {
		return minWidth;
	}

	public void setMinWidth (float minWidth) {
		this.minWidth = minWidth;
	}

	public float getMinHeight () {
		return minHeight;
	}

	public void setMinHeight (float minHeight) {
		this.minHeight = minHeight;
	}

	public void setMinSize (float minWidth, float minHeight) {
		setMinWidth(minWidth);
		setMinHeight(minHeight);
	}

	public @Null String getName () {
		return name;
	}

	public void setName (@Null String name) {
		this.name = name;
	}

	public @Null String toString () {
		if (name == null) return ClassReflection.getSimpleName(getClass());
		return name;
	}
}
