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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasSprite;
import com.badlogic.gdx.scenes.scene2d.ui.UiParams;

import org.jetbrains.annotations.NotNull;

/** Drawable for a {@link Sprite}.
 * @author Nathan Sweet */
public class SpriteDrawable extends BaseDrawable implements TransformDrawable {
	private Sprite sprite;

	private UiParams uiParams = new UiParams();

	/** Creates an uninitialized SpriteDrawable. The sprite must be set before use. */
	public SpriteDrawable () {
	}

	public SpriteDrawable (Sprite sprite) {
		setSprite(sprite);
	}

	public SpriteDrawable (SpriteDrawable drawable) {
		super(drawable);
		setSprite(drawable.sprite);
	}

	public void draw (Batch batch, float x, float y, float width, float height) {
		Color spriteColor = sprite.getColor();
		float oldColor = spriteColor.toFloatBits();
		sprite.setColor(spriteColor.mul(batch.getColor()));

		sprite.setRotation(0);
		sprite.setScale(1, 1);
		sprite.setBounds(x + getPaddingInLeft(),
				y + getPaddingInTop(),
				width - getPaddingInLeft() - getPaddingInRight(),
				height - getPaddingInTop() - getPaddingInBottom()
		);
		sprite.draw(batch);

		sprite.setPackedColor(oldColor);
	}

	protected void updateUiParams(Batch batch) {
		if (batch instanceof SpriteBatch) {
			((SpriteBatch) batch).setUiParams(uiParams);
		}
	}

	public void draw (Batch batch, float x, float y, float originX, float originY, float width, float height, float scaleX,
		float scaleY, float rotation) {
		updateUiParams(batch);

		Color spriteColor = sprite.getColor();
		float oldColor = spriteColor.toFloatBits();
		sprite.setColor(spriteColor.mul(batch.getColor()));

		sprite.setOrigin(originX - getPaddingInLeft(), originY - getPaddingInTop());
		sprite.setRotation(rotation);
		sprite.setScale(scaleX, scaleY);
		sprite.setBounds(x + getPaddingInLeft(),
				y + getPaddingInTop(),
				width - getPaddingInLeft() - getPaddingInRight(),
				height - getPaddingInTop() - getPaddingInBottom()
		);
		sprite.draw(batch);

		sprite.setPackedColor(oldColor);
	}

	public void setSprite (Sprite sprite) {
		this.sprite = sprite;
		setMinWidth(sprite.getWidth());
		setMinHeight(sprite.getHeight());
	}

	public Sprite getSprite () {
		return sprite;
	}

	/** Creates a new drawable that renders the same as this drawable tinted the specified color. */
	public SpriteDrawable tint (Color tint) {
		Sprite newSprite;
		if (sprite instanceof AtlasSprite)
			newSprite = new AtlasSprite((AtlasSprite)sprite);
		else
			newSprite = new Sprite(sprite);
		newSprite.setColor(tint);
		newSprite.setSize(getMinWidth(), getMinHeight());
		SpriteDrawable drawable = new SpriteDrawable(newSprite);
		drawable.setPaddingOutLeft(getPaddingOutLeft());
		drawable.setPaddingOutTop(getPaddingOutTop());
		drawable.setPaddingOutRight(getPaddingOutRight());
		drawable.setPaddingOutBottom(getPaddingOutBottom());
		drawable.setPaddingInLeft(getPaddingInLeft());
		drawable.setPaddingInTop(getPaddingInTop());
		drawable.setPaddingInRight(getPaddingInRight());
		drawable.setPaddingInBottom(getPaddingInBottom());
		return drawable;
	}

	public UiParams getUiParams() {
		return uiParams;
	}

	public void setUiParams(@NotNull UiParams uiParams) {
		this.uiParams = uiParams;
	}
}
