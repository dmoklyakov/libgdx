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
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasSprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.UiParams;

import org.jetbrains.annotations.NotNull;

/** Drawable for a {@link TextureRegion}.
 * @author Nathan Sweet */
public class TextureRegionDrawable extends BaseDrawable implements TransformDrawable {
	private TextureRegion region;

	@NotNull
	private UiParams uiParams = new UiParams();

	/** Creates an uninitialized TextureRegionDrawable. The texture region must be set before use. */
	public TextureRegionDrawable () {
	}

	public TextureRegionDrawable (Texture texture) {
		setRegion(new TextureRegion(texture));
	}

	public TextureRegionDrawable (TextureRegion region) {
		setRegion(region);
	}

	public TextureRegionDrawable (TextureRegionDrawable drawable) {
		super(drawable);
		setRegion(drawable.region);
	}

	protected void updateUiParams(Batch batch) {
		if (batch instanceof SpriteBatch) {
			((SpriteBatch) batch).setUiParams(uiParams);
		}
	}

	public void draw (Batch batch, float x, float y, float width, float height) {
		updateUiParams(batch);
		batch.draw(region,
				x + getPaddingInLeft(),
				y + getPaddingInTop(),
				width - getPaddingInLeft() - getPaddingInRight(),
				height - getPaddingInTop() - getPaddingInBottom()
		);
	}

	public void draw (Batch batch, float x, float y, float originX, float originY, float width, float height, float scaleX,
		float scaleY, float rotation) {
		updateUiParams(batch);
		batch.draw(region,
				x + getPaddingInLeft(),
				y + getPaddingInTop(),
				originX - getPaddingInLeft(),
				originY - getPaddingInTop(),
				width - getPaddingInLeft() - getPaddingInRight(),
				height - getPaddingInTop() - getPaddingInBottom(),
				scaleX,
				scaleY,
				rotation
		);
	}

	public void setRegion (TextureRegion region) {
		this.region = region;
		if (region != null) {
			setMinWidth(region.getRegionWidth());
			setMinHeight(region.getRegionHeight());
		}
	}

	public TextureRegion getRegion () {
		return region;
	}

	/** Creates a new drawable that renders the same as this drawable tinted the specified color. */
	public Drawable tint (Color tint) {
		Sprite sprite;
		if (region instanceof AtlasRegion)
			sprite = new AtlasSprite((AtlasRegion)region);
		else
			sprite = new Sprite(region);
		sprite.setColor(tint);
		sprite.setSize(getMinWidth(), getMinHeight());
		SpriteDrawable drawable = new SpriteDrawable(sprite);
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
