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

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.BitmapFontLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.StreamUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


/**
 * An asset manager loader for loading {@link MsdfFont}.
 * <p>
 * The loader is basically just a copy of {@link BitmapFontLoader} with extra parameters.
 * It would have been impossible to just make an asset dependency on a BitmapFont without
 * copying it all because both assets have the same name and one would overwrite the other
 * in the asset manager...
 */
public class MsdfFontLoader extends AsynchronousAssetLoader<MsdfFont, MsdfFontLoader.MsdfFontParameter> {

    private BitmapFontData data;


    public MsdfFontLoader(FileHandleResolver resolver) {
        super(resolver);
    }


    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, MsdfFontParameter parameter) {
        Array<AssetDescriptor> deps = new Array<AssetDescriptor>();
        if (parameter != null && parameter.bitmapFontData != null) {
            data = parameter.bitmapFontData;
            return deps;
        }

        data = new BitmapFontData(file, parameter != null && parameter.flip);
        if (parameter != null && parameter.atlasName != null) {
            deps.add(new AssetDescriptor<TextureAtlas>(parameter.atlasName, TextureAtlas.class));
        } else {
            for (int i = 0; i < data.getImagePaths().length; i++) {
                String path = data.getImagePath(i);
                FileHandle resolved = resolve(path);

                TextureLoader.TextureParameter textureParams = new TextureLoader.TextureParameter();

                if (parameter == null) {
                    parameter = new MsdfFontParameter();
                }
                textureParams.genMipMaps = parameter.genMipMaps;
                textureParams.minFilter = parameter.minFilter;
                textureParams.magFilter = parameter.magFilter;

                AssetDescriptor descriptor = new AssetDescriptor<Texture>(resolved, Texture.class, textureParams);
                deps.add(descriptor);
            }
        }

        return deps;
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, MsdfFontParameter parameter) {
        // Nothing to do.
    }

    @Override
    public MsdfFont loadSync(AssetManager manager, String fileName, FileHandle file, MsdfFontParameter parameter) {
        // Create bitmap font
        BitmapFont bitmapFont;
        if (parameter != null && parameter.atlasName != null) {
            TextureAtlas atlas = manager.get(parameter.atlasName, TextureAtlas.class);
            String name = file.sibling(data.imagePaths[0]).nameWithoutExtension();
            TextureAtlas.AtlasRegion region = atlas.findRegion(name);

            if (region == null)
                throw new GdxRuntimeException("Could not find font region "
                        + name + " in atlas " + parameter.atlasName);
            bitmapFont = new BitmapFont(file, region);

        } else {
            int n = data.getImagePaths().length;
            Array<TextureRegion> regs = new Array<TextureRegion>(n);
            for (int i = 0; i < n; i++) {
                regs.add(new TextureRegion(manager.get(data.getImagePath(i), Texture.class)));
            }
            bitmapFont = new BitmapFont(data, regs, true);
        }

        // Get distance range and glyph size from .fnt file or from parameter.
        if (parameter == null) {
            parameter = new MsdfFontParameter();
        }
        float glyphSize = parameter.glyphSize;
        float distanceRange = parameter.distanceRange;
        final BufferedReader reader = new BufferedReader(new InputStreamReader(file.read()), 512);
        try {
            String infoLine = reader.readLine();
            if (glyphSize == 0f) glyphSize = parseBMFontIntAttribute(infoLine, "size");

            String commonLine = reader.readLine();
            if (distanceRange == 0f) distanceRange = parseBMFontIntAttribute(commonLine, "distanceRange");

        } catch (IOException e) {
            throw new GdxRuntimeException("Error loading font file: " + file, e);
        } finally {
            StreamUtils.closeQuietly(reader);
        }

        if (glyphSize <= 0f) throw new GdxRuntimeException("Glyph size must be greater than zero.");
        if (distanceRange <= 0f) throw new GdxRuntimeException("Distance range must be greater than zero.");

        return new MsdfFont(bitmapFont, glyphSize, distanceRange);
    }

    private int parseBMFontIntAttribute(String line, String name) {
        int start = line.indexOf(name + '=');
        if (start == -1) {
            throw new GdxRuntimeException("Required font parameter '" + name
                    + "' not specified in loader parameter or font file.");
        }
        start += name.length() + 1;
        int end = line.indexOf(' ', start);
        if (end == -1) end = line.length();
        return Integer.parseInt(line.substring(start, end));
    }


    public static class MsdfFontParameter extends AssetLoaderParameters<MsdfFont> {
        /**
         * Flips the font vertically if {@code true}.
         * Defaults to {@code false}.
         */
        public boolean flip = false;

        /**
         * Generates mipmaps for the font if {@code true}. Defaults to {@code true}.
         */
        public boolean genMipMaps = true;

        /**
         * The {@link Texture.TextureFilter} to use when scaling down the {@link BitmapFont}.
         * Defaults to {@link Texture.TextureFilter#MipMapLinearNearest}.
         */
        public Texture.TextureFilter minFilter = Texture.TextureFilter.MipMapLinearNearest;

        /**
         * The {@link Texture.TextureFilter} to use when scaling up the {@link BitmapFont}.
         * Defaults to {@link Texture.TextureFilter#Linear}.
         */
        public Texture.TextureFilter magFilter = Texture.TextureFilter.Linear;

        /**
         * Optional {@link BitmapFontData} to be used instead of loading the {@link Texture} directly.
         * Use this if your font is embedded in a {@link Skin}. *
         */
        public BitmapFontData bitmapFontData = null;

        /**
         * The name of the {@link TextureAtlas} to load the {@link BitmapFont} itself from.
         * Optional; if {@code null}, will look for a separate image
         */
        public String atlasName = null;

        /**
         * See {@link MsdfFont#getGlyphSize()}.
         * Glyph size is automatically set from the {@code size} attribute in the info tag of the {@code .fnt} file.
         * Set a value different than zero to override the value defined in the file or if it isn't defined in the file.
         */
        public float glyphSize = 0f;

        /**
         * See {@link MsdfFont#getDistanceRange()}.
         * Distance range may be defined in the {@code .fnt} file with the {@code distanceRange} attribute in common tag.
         * Set a value different than zero to override the value defined in the file or if it isn't defined in the file.
         */
        public float distanceRange = 0f;


        public MsdfFontParameter() {
            // Default constructor.
        }

        public MsdfFontParameter(float glyphSize, float distanceRange) {
            this.glyphSize = glyphSize;
            this.distanceRange = distanceRange;
        }
    }
}