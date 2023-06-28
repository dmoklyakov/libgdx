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

package com.badlogic.gdx.graphics.g2d;

import static java.lang.Math.max;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Mesh.VertexDataType;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector4;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.UiParams;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Draws batched quads using indices.
 * <p>
 * This is an optimized version of the SpriteBatch that maintains an LFU texture-cache to combine draw calls with different
 * textures effectively.
 * <p>
 * Use this Batch if you frequently utilize more than a single texture between calling {@link#begin()} and {@link#end()}. An
 * example would be if your Atlas is spread over multiple Textures or if you draw with individual Textures.
 *
 * @author mzechner (Original SpriteBatch)
 * @author Nathan Sweet (Original SpriteBatch)
 * @author VaTTeRGeR (TextureArray Extension)
 * @see Batch
 * @see SpriteBatch
 */

public class SpriteBatch implements Batch {

    int idx = 0;
    private final Mesh mesh;
    final float[] vertices;
    private final int VERTEX_SIZE = 2 + 1 + 2; // x, y, color, u, v
    private final int SPRITE_SIZE = 4 * (VERTEX_SIZE + 1 + 1); // 1 is for the label style index, 1 is for ui params index.
    private final int SPRITE_SIZE_TEX_ARRAY = 4 * (VERTEX_SIZE + 1 + 1 + 1); // 1 is for the texture unit index, 1 is for the label style index, 1 is for ui params index.
    /**
     * The maximum number of available texture units for the fragment shader.
     * Also used as "max MSDF params amount" for simplicity.
     */
    private static int maxTextureUnits = -1;
    private static int maxVertexUniformVectors = -1;
    int msdfParamsComponents = 4 * 5;
    private static int maxMsdfParams = -1;
    int uiParamsComponents = 4 * 4;
    private static int maxUiParams = -1;

    /**
     * Textures in use (index: Texture Unit, value: Texture)
     */
    private final Texture[] usedTextures;
    private final Label.LabelStyle[] labelStylesArray;
    private final UiParams[] uiParamsArray;
    private final Vector4[] regionsArray; // Used with uiParamsArray only.
    private int labelStylesCount = 0;
    private int uiParamsCount = 0;

    /**
     * LFU Array (index: Texture Unit Index - value: Access frequency)
     */
    private final int[] usedTexturesLFU;

    /**
     * Gets sent to the fragment shader as an uniform "uniform sampler2d[X] u_textures"
     */
    private final IntBuffer textureUnitIndicesBuffer;

    private float invTexWidth = 0, invTexHeight = 0;

    boolean drawing = false;

    private final Matrix4 transformMatrix = new Matrix4();
    private final Matrix4 projectionMatrix = new Matrix4();
    private final Matrix4 combinedMatrix = new Matrix4();

    private boolean blendingDisabled = false;
    private int blendSrcFunc = GL20.GL_SRC_ALPHA;
    private int blendDstFunc = GL20.GL_ONE_MINUS_SRC_ALPHA;
    private int blendSrcFuncAlpha = GL20.GL_SRC_ALPHA;
    private int blendDstFuncAlpha = GL20.GL_ONE_MINUS_SRC_ALPHA;

    private ShaderProgram shader = null;
    private ShaderProgram customShader = null;

    private static String shaderErrorLog = null;

    public boolean isOwnsShader() {
        return ownsShader;
    }

    public void setOwnsShader(boolean ownsShader) {
        this.ownsShader = ownsShader;
    }

    private boolean ownsShader;
    private final Color color = new Color(1, 1, 1, 1);
    float colorPacked = Color.WHITE_FLOAT_BITS;

    /**
     * Number of render calls since the last {@link #begin()}.
     **/
    public int renderCalls = 0;

    /**
     * Number of rendering calls, ever. Will not be reset unless set manually.
     **/
    public int totalRenderCalls = 0;

    /**
     * The maximum number of sprites rendered in one batch so far.
     **/
    public int maxSpritesInBatch = 0;

    /**
     * The current number of textures in the LFU cache. Gets reset when calling {@link#begin()}
     **/
    private int currentTextureLFUSize = 0;

    /**
     * The current number of texture swaps in the LFU cache. Gets reset when calling {@link#begin()}
     **/
    private int currentTextureLFUSwaps = 0;

    /**
     * Constructs a new TextureArraySpriteBatch with a size of 1000, one buffer, and the default shader.
     *
     * @see SpriteBatch#SpriteBatch(int, ShaderProgram)
     */
    public SpriteBatch() {
        this(1000);
    }

    /**
     * Constructs a TextureArraySpriteBatch with one buffer and the default shader.
     *
     * @see SpriteBatch#SpriteBatch(int, ShaderProgram)
     */
    public SpriteBatch(int size) {
        this(size, null);
    }

    /**
     * Constructs a new TextureArraySpriteBatch. Sets the projection matrix to an orthographic projection with y-axis point
     * upwards, x-axis point to the right and the origin being in the bottom left corner of the screen. The projection will be
     * pixel perfect with respect to the current screen resolution.
     * <p>
     * The defaultShader specifies the shader to use. Note that the names for uniforms for this default shader are different than
     * the ones expect for shaders set with {@link #setShader(ShaderProgram)}.
     *
     * @param size          The max number of sprites in a single batch. Max of 8191.
     * @param defaultShader The default shader to use. This is not owned by the TextureArraySpriteBatch and must be disposed
     *                      separately.
     * @throws IllegalStateException Thrown if the device does not support texture arrays. Make sure to implement a Fallback to
     *                               {@link SpriteBatch} in case Texture Arrays are not supported on a clients device.
     * @See {@link#createDefaultShader()} {@link#getMaxTextureUnits()}
     */
    public SpriteBatch(int size, ShaderProgram defaultShader) throws IllegalStateException {
        // 32767 is max vertex index, so 32767 / 4 vertices per sprite = 8191 sprites max.
        if (size > 8191)
            throw new IllegalArgumentException("Can't have more than 8191 sprites per batch: " + size);

        getMaxTextureUnits();
        getMaxVertexUniformVectors();
        int availableVertexUniformVectors = maxVertexUniformVectors - (maxTextureUnits > 1 ? maxTextureUnits * 4 * 2 : 0); // One for texture units, one for texture sizes.
        maxMsdfParams = availableVertexUniformVectors / msdfParamsComponents / 2; // Not accurate but ok.
        maxUiParams = availableVertexUniformVectors / uiParamsComponents / 2; // Not accurate but ok.

        if (defaultShader == null) {
            shader = createDefaultShader(maxTextureUnits, maxMsdfParams, maxUiParams);
            ownsShader = true;
        } else {
            shader = defaultShader;
            ownsShader = false;
        }

        usedTextures = new Texture[maxTextureUnits];
        usedTexturesLFU = new int[maxTextureUnits];

        // This contains the numbers 0 ... maxTextureUnits - 1. We send these to the shader as an uniform.
        textureUnitIndicesBuffer = BufferUtils.newIntBuffer(maxTextureUnits);
        for (int i = 0; i < maxTextureUnits; i++) {
            textureUnitIndicesBuffer.put(i);
        }
        textureUnitIndicesBuffer.flip();
        labelStylesArray = new Label.LabelStyle[maxMsdfParams];
        uiParamsArray = new UiParams[maxUiParams];
        regionsArray = new Vector4[maxUiParams];
        for (int i = 0; i < maxUiParams; i++) {
            regionsArray[i] = new Vector4();
        }

        VertexDataType vertexDataType = (Gdx.gl30 != null) ? VertexDataType.VertexBufferObjectWithVAO : VertexDataType.VertexArray;

        ArrayList<VertexAttribute> attributes = new ArrayList<>();
        attributes.add(new VertexAttribute(Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"));
        if (maxTextureUnits > 1) {
            attributes.add(new VertexAttribute(Usage.Generic, 1, "a_textureIndex"));
        }
        attributes.add(new VertexAttribute(Usage.Generic, 1, "a_msdfParamsIndex"));
        attributes.add(new VertexAttribute(Usage.Generic, 1, "a_uiParamsIndex"));

        // The vertex data is extended with one float for the texture index and one for MSDF params index.
        mesh = new Mesh(
                vertexDataType,
                false,
                size * 4,
                size * 6,
                attributes.toArray(new VertexAttribute[0])
        );

        projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        vertices = new float[size * (maxTextureUnits > 1 ? SPRITE_SIZE_TEX_ARRAY : SPRITE_SIZE)];

        int len = size * 6;
        short[] indices = new short[len];
        short j = 0;
        for (int i = 0; i < len; i += 6, j += 4) {
            indices[i] = j;
            indices[i + 1] = (short) (j + 1);
            indices[i + 2] = (short) (j + 2);
            indices[i + 3] = (short) (j + 2);
            indices[i + 4] = (short) (j + 3);
            indices[i + 5] = j;
        }

        mesh.setIndices(indices);
    }

    @Override
    public void begin() {
        if (drawing)
            throw new IllegalStateException("TextureArraySpriteBatch.end must be called before begin.");

        renderCalls = 0;

        currentTextureLFUSize = 0;
        currentTextureLFUSwaps = 0;

        Arrays.fill(usedTextures, null);
        Arrays.fill(usedTexturesLFU, 0);

        Gdx.gl.glDepthMask(false);

        if (customShader != null) {
            customShader.bind();
        } else {
            shader.bind();
        }

        setupMatrices();

        drawing = true;
    }

    @Override
    public void end() {
        if (!drawing)
            throw new IllegalStateException("TextureArraySpriteBatch.begin must be called before end.");

        if (idx > 0) flush();

        drawing = false;

        GL20 gl = Gdx.gl;

        gl.glDepthMask(true);

        if (isBlendingEnabled()) {
            gl.glDisable(GL20.GL_BLEND);
        }
    }

    @Override
    public void dispose() {
        mesh.dispose();

        if (ownsShader && shader != null) {
            shader.dispose();
        }
    }

    @Override
    public void setColor(Color tint) {
        color.set(tint);
        colorPacked = tint.toFloatBits();
    }

    @Override
    public void setColor(float r, float g, float b, float a) {
        color.set(r, g, b, a);
        colorPacked = color.toFloatBits();
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public void setPackedColor(float packedColor) {
        Color.abgr8888ToColor(color, packedColor);
        this.colorPacked = packedColor;
    }

    @Override
    public float getPackedColor() {
        return colorPacked;
    }

    @Override
    public void draw(Texture texture, float x, float y, float originX, float originY, float width, float height, float scaleX,
                     float scaleY, float rotation, int srcX, int srcY, int srcWidth, int srcHeight, boolean flipX, boolean flipY) {
        if (!drawing)
            throw new IllegalStateException("TextureArraySpriteBatch.begin must be called before draw.");

        float[] vertices = this.vertices;

        flushIfFull();

        final float ti = activateTexture(texture);
        final float lsi = activateLabelStyle();
        final float upi = activateUiParams(
                srcX * texture.getWidth(),
                srcY * texture.getHeight(),
                srcWidth * texture.getWidth(),
                srcHeight * texture.getHeight()
        );

        // bottom left and top right corner points relative to origin
        final float worldOriginX = x + originX;
        final float worldOriginY = y + originY;
        float fx = -originX;
        float fy = -originY;
        float fx2 = width - originX;
        float fy2 = height - originY;

        // scale
        if (scaleX != 1 || scaleY != 1) {
            fx *= scaleX;
            fy *= scaleY;
            fx2 *= scaleX;
            fy2 *= scaleY;
        }

        // construct corner points, start from top left and go counter clockwise
        final float p1x = fx;
        final float p1y = fy;
        final float p2x = fx;
        final float p2y = fy2;
        final float p3x = fx2;
        final float p3y = fy2;
        final float p4x = fx2;
        final float p4y = fy;

        float x1;
        float y1;
        float x2;
        float y2;
        float x3;
        float y3;
        float x4;
        float y4;

        // rotate
        if (rotation != 0) {
            final float cos = MathUtils.cosDeg(rotation);
            final float sin = MathUtils.sinDeg(rotation);

            x1 = cos * p1x - sin * p1y;
            y1 = sin * p1x + cos * p1y;

            x2 = cos * p2x - sin * p2y;
            y2 = sin * p2x + cos * p2y;

            x3 = cos * p3x - sin * p3y;
            y3 = sin * p3x + cos * p3y;

            x4 = x1 + (x3 - x2);
            y4 = y3 - (y2 - y1);
        } else {
            x1 = p1x;
            y1 = p1y;

            x2 = p2x;
            y2 = p2y;

            x3 = p3x;
            y3 = p3y;

            x4 = p4x;
            y4 = p4y;
        }

        x1 += worldOriginX;
        y1 += worldOriginY;
        x2 += worldOriginX;
        y2 += worldOriginY;
        x3 += worldOriginX;
        y3 += worldOriginY;
        x4 += worldOriginX;
        y4 += worldOriginY;

        float u = srcX * invTexWidth;
        float v = (srcY + srcHeight) * invTexHeight;
        float u2 = (srcX + srcWidth) * invTexWidth;
        float v2 = srcY * invTexHeight;

        if (flipX) {
            float tmp = u;
            u = u2;
            u2 = tmp;
        }

        if (flipY) {
            float tmp = v;
            v = v2;
            v2 = tmp;
        }

        final float color = this.colorPacked;

        vertices[idx++] = x1;
        vertices[idx++] = y1;
        vertices[idx++] = color;
        vertices[idx++] = u;
        vertices[idx++] = v;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;

        vertices[idx++] = x2;
        vertices[idx++] = y2;
        vertices[idx++] = color;
        vertices[idx++] = u;
        vertices[idx++] = v2;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;

        vertices[idx++] = x3;
        vertices[idx++] = y3;
        vertices[idx++] = color;
        vertices[idx++] = u2;
        vertices[idx++] = v2;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;

        vertices[idx++] = x4;
        vertices[idx++] = y4;
        vertices[idx++] = color;
        vertices[idx++] = u2;
        vertices[idx++] = v;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;
    }

    @Override
    public void draw(Texture texture, float x, float y, float width, float height, int srcX, int srcY, int srcWidth,
                     int srcHeight, boolean flipX, boolean flipY) {
        if (!drawing)
            throw new IllegalStateException("TextureArraySpriteBatch.begin must be called before draw.");

        float[] vertices = this.vertices;

        flushIfFull();

        final float ti = activateTexture(texture);
        final float lsi = activateLabelStyle();
        final float upi = activateUiParams(
                srcX * texture.getWidth(),
                srcY * texture.getHeight(),
                srcWidth * texture.getWidth(),
                srcHeight * texture.getHeight()
        );

        float u = srcX * invTexWidth;
        float v = (srcY + srcHeight) * invTexHeight;
        float u2 = (srcX + srcWidth) * invTexWidth;
        float v2 = srcY * invTexHeight;
        final float fx2 = x + width;
        final float fy2 = y + height;

        if (flipX) {
            float tmp = u;
            u = u2;
            u2 = tmp;
        }

        if (flipY) {
            float tmp = v;
            v = v2;
            v2 = tmp;
        }

        float color = this.colorPacked;

        vertices[idx++] = x;
        vertices[idx++] = y;
        vertices[idx++] = color;
        vertices[idx++] = u;
        vertices[idx++] = v;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;

        vertices[idx++] = x;
        vertices[idx++] = fy2;
        vertices[idx++] = color;
        vertices[idx++] = u;
        vertices[idx++] = v2;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;

        vertices[idx++] = fx2;
        vertices[idx++] = fy2;
        vertices[idx++] = color;
        vertices[idx++] = u2;
        vertices[idx++] = v2;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;

        vertices[idx++] = fx2;
        vertices[idx++] = y;
        vertices[idx++] = color;
        vertices[idx++] = u2;
        vertices[idx++] = v;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;
    }

    @Override
    public void draw(Texture texture, float x, float y, int srcX, int srcY, int srcWidth, int srcHeight) {
        if (!drawing)
            throw new IllegalStateException("TextureArraySpriteBatch.begin must be called before draw.");

        float[] vertices = this.vertices;

        flushIfFull();

        final float ti = activateTexture(texture);
        final float lsi = activateLabelStyle();
        final float upi = activateUiParams(
                srcX * texture.getWidth(),
                srcY * texture.getHeight(),
                srcWidth * texture.getWidth(),
                srcHeight * texture.getHeight()
        );

        final float u = srcX * invTexWidth;
        final float v = (srcY + srcHeight) * invTexHeight;
        final float u2 = (srcX + srcWidth) * invTexWidth;
        final float v2 = srcY * invTexHeight;
        final float fx2 = x + srcWidth;
        final float fy2 = y + srcHeight;

        float color = this.colorPacked;

        vertices[idx++] = x;
        vertices[idx++] = y;
        vertices[idx++] = color;
        vertices[idx++] = u;
        vertices[idx++] = v;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;

        vertices[idx++] = x;
        vertices[idx++] = fy2;
        vertices[idx++] = color;
        vertices[idx++] = u;
        vertices[idx++] = v2;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;

        vertices[idx++] = fx2;
        vertices[idx++] = fy2;
        vertices[idx++] = color;
        vertices[idx++] = u2;
        vertices[idx++] = v2;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;

        vertices[idx++] = fx2;
        vertices[idx++] = y;
        vertices[idx++] = color;
        vertices[idx++] = u2;
        vertices[idx++] = v;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;
    }

    @Override
    public void draw(Texture texture, float x, float y, float width, float height, float u, float v, float u2, float v2) {
        if (!drawing)
            throw new IllegalStateException("TextureArraySpriteBatch.begin must be called before draw.");

        float[] vertices = this.vertices;

        flushIfFull();

        final float ti = activateTexture(texture);
        final float lsi = activateLabelStyle();
        final float upi = activateUiParams(
                u * texture.getWidth(),
                v * texture.getHeight(),
                u2 * texture.getWidth(),
                v2 * texture.getHeight()
        );

        final float fx2 = x + width;
        final float fy2 = y + height;

        float color = this.colorPacked;

        vertices[idx++] = x;
        vertices[idx++] = y;
        vertices[idx++] = color;
        vertices[idx++] = u;
        vertices[idx++] = v;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;

        vertices[idx++] = x;
        vertices[idx++] = fy2;
        vertices[idx++] = color;
        vertices[idx++] = u;
        vertices[idx++] = v2;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;

        vertices[idx++] = fx2;
        vertices[idx++] = fy2;
        vertices[idx++] = color;
        vertices[idx++] = u2;
        vertices[idx++] = v2;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;

        vertices[idx++] = fx2;
        vertices[idx++] = y;
        vertices[idx++] = color;
        vertices[idx++] = u2;
        vertices[idx++] = v;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;
    }

    @Override
    public void draw(Texture texture, float x, float y) {
        draw(texture, x, y, texture.getWidth(), texture.getHeight());
    }

    @Override
    public void draw(Texture texture, float x, float y, float width, float height) {
        if (!drawing)
            throw new IllegalStateException("TextureArraySpriteBatch.begin must be called before draw.");

        float[] vertices = this.vertices;

        flushIfFull();

        final float ti = activateTexture(texture);
        final float lsi = activateLabelStyle();
        final float upi = activateUiParams(texture);

        final float fx2 = x + width;
        final float fy2 = y + height;
        final float u = 0;
        final float v = 1;
        final float u2 = 1;
        final float v2 = 0;

        float color = this.colorPacked;

        vertices[idx++] = x;
        vertices[idx++] = y;
        vertices[idx++] = color;
        vertices[idx++] = u;
        vertices[idx++] = v;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;

        vertices[idx++] = x;
        vertices[idx++] = fy2;
        vertices[idx++] = color;
        vertices[idx++] = u;
        vertices[idx++] = v2;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;

        vertices[idx++] = fx2;
        vertices[idx++] = fy2;
        vertices[idx++] = color;
        vertices[idx++] = u2;
        vertices[idx++] = v2;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;

        vertices[idx++] = fx2;
        vertices[idx++] = y;
        vertices[idx++] = color;
        vertices[idx++] = u2;
        vertices[idx++] = v;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;
    }

    @Override
    public void draw(Texture texture, float[] spriteVertices, int offset, int count) {
        if (!drawing) {
            throw new IllegalStateException("TextureArraySpriteBatch.begin must be called before draw.");
        }

        flushIfFull();

        // Assigns a texture unit to this texture, flushing if none is available
        final float ti = (float) activateTexture(texture);
        final float lsi = activateLabelStyle();
        final float upi = activateUiParams(texture);

        // spriteVertexSize is the number of floats an unmodified input vertex consists of,
        // therefore this loop iterates over the vertices stored in parameter spriteVertices.
        for (int srcPos = 0; srcPos < count; srcPos += VERTEX_SIZE) {

            // Copy the vertices
            System.arraycopy(spriteVertices, srcPos, vertices, idx, VERTEX_SIZE);

            // Advance idx by vertex float count
            idx += VERTEX_SIZE;

            // Inject texture unit index and advance idx
            if (maxTextureUnits > 1) {
                vertices[idx++] = ti;
            }
            vertices[idx++] = lsi;
            vertices[idx++] = upi;
        }
    }

    @Override
    public void draw(TextureRegion region, float x, float y) {
        draw(region, x, y, region.getRegionWidth(), region.getRegionHeight());
    }

    @Override
    public void draw(TextureRegion region, float x, float y, float width, float height) {
        if (!drawing)
            throw new IllegalStateException("TextureArraySpriteBatch.begin must be called before draw.");

        float[] vertices = this.vertices;

        flushIfFull();

        final float ti = activateTexture(region.getTexture());
        final float lsi = activateLabelStyle();
        final float upi = activateUiParams(region);

        final float fx2 = x + width;
        final float fy2 = y + height;
        final float u = region.getU();
        final float v = region.getV2();
        final float u2 = region.getU2();
        final float v2 = region.getV();

        float color = this.colorPacked;

        vertices[idx++] = x;
        vertices[idx++] = y;
        vertices[idx++] = color;
        vertices[idx++] = u;
        vertices[idx++] = v;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;

        vertices[idx++] = x;
        vertices[idx++] = fy2;
        vertices[idx++] = color;
        vertices[idx++] = u;
        vertices[idx++] = v2;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;

        vertices[idx++] = fx2;
        vertices[idx++] = fy2;
        vertices[idx++] = color;
        vertices[idx++] = u2;
        vertices[idx++] = v2;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;

        vertices[idx++] = fx2;
        vertices[idx++] = y;
        vertices[idx++] = color;
        vertices[idx++] = u2;
        vertices[idx++] = v;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;
    }

    @Override
    public void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height,
                     float scaleX, float scaleY, float rotation) {
        if (!drawing)
            throw new IllegalStateException("TextureArraySpriteBatch.begin must be called before draw.");

        float[] vertices = this.vertices;

        flushIfFull();

        final float ti = activateTexture(region.getTexture());
        final float lsi = activateLabelStyle();
        final float upi = activateUiParams(region);

        // bottom left and top right corner points relative to origin
        final float worldOriginX = x + originX;
        final float worldOriginY = y + originY;
        float fx = -originX;
        float fy = -originY;
        float fx2 = width - originX;
        float fy2 = height - originY;

        // scale
        if (scaleX != 1 || scaleY != 1) {
            fx *= scaleX;
            fy *= scaleY;
            fx2 *= scaleX;
            fy2 *= scaleY;
        }

        // construct corner points, start from top left and go counter clockwise
        final float p1x = fx;
        final float p1y = fy;
        final float p2x = fx;
        final float p2y = fy2;
        final float p3x = fx2;
        final float p3y = fy2;
        final float p4x = fx2;
        final float p4y = fy;

        float x1;
        float y1;
        float x2;
        float y2;
        float x3;
        float y3;
        float x4;
        float y4;

        // rotate
        if (rotation != 0) {
            final float cos = MathUtils.cosDeg(rotation);
            final float sin = MathUtils.sinDeg(rotation);

            x1 = cos * p1x - sin * p1y;
            y1 = sin * p1x + cos * p1y;
            x2 = cos * p2x - sin * p2y;
            y2 = sin * p2x + cos * p2y;
            x3 = cos * p3x - sin * p3y;
            y3 = sin * p3x + cos * p3y;
            x4 = x1 + (x3 - x2);
            y4 = y3 - (y2 - y1);
        } else {
            x1 = p1x;
            y1 = p1y;
            x2 = p2x;
            y2 = p2y;
            x3 = p3x;
            y3 = p3y;
            x4 = p4x;
            y4 = p4y;
        }

        x1 += worldOriginX;
        y1 += worldOriginY;
        x2 += worldOriginX;
        y2 += worldOriginY;
        x3 += worldOriginX;
        y3 += worldOriginY;
        x4 += worldOriginX;
        y4 += worldOriginY;

        final float u = region.getU();
        final float v = region.getV2();
        final float u2 = region.getU2();
        final float v2 = region.getV();

        float color = this.colorPacked;

        vertices[idx++] = x1;
        vertices[idx++] = y1;
        vertices[idx++] = color;
        vertices[idx++] = u;
        vertices[idx++] = v;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;

        vertices[idx++] = x2;
        vertices[idx++] = y2;
        vertices[idx++] = color;
        vertices[idx++] = u;
        vertices[idx++] = v2;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;

        vertices[idx++] = x3;
        vertices[idx++] = y3;
        vertices[idx++] = color;
        vertices[idx++] = u2;
        vertices[idx++] = v2;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;

        vertices[idx++] = x4;
        vertices[idx++] = y4;
        vertices[idx++] = color;
        vertices[idx++] = u2;
        vertices[idx++] = v;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;
    }

    @Override
    public void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height,
                     float scaleX, float scaleY, float rotation, boolean clockwise) {
        if (!drawing)
            throw new IllegalStateException("TextureArraySpriteBatch.begin must be called before draw.");

        float[] vertices = this.vertices;

        flushIfFull();

        final float ti = activateTexture(region.getTexture());
        final float lsi = activateLabelStyle();
        final float upi = activateUiParams(region);

        // bottom left and top right corner points relative to origin
        final float worldOriginX = x + originX;
        final float worldOriginY = y + originY;
        float fx = -originX;
        float fy = -originY;
        float fx2 = width - originX;
        float fy2 = height - originY;

        // scale
        if (scaleX != 1 || scaleY != 1) {
            fx *= scaleX;
            fy *= scaleY;
            fx2 *= scaleX;
            fy2 *= scaleY;
        }

        // construct corner points, start from top left and go counter clockwise
        final float p1x = fx;
        final float p1y = fy;
        final float p2x = fx;
        final float p2y = fy2;
        final float p3x = fx2;
        final float p3y = fy2;
        final float p4x = fx2;
        final float p4y = fy;

        float x1;
        float y1;
        float x2;
        float y2;
        float x3;
        float y3;
        float x4;
        float y4;

        // rotate
        if (rotation != 0) {
            final float cos = MathUtils.cosDeg(rotation);
            final float sin = MathUtils.sinDeg(rotation);

            x1 = cos * p1x - sin * p1y;
            y1 = sin * p1x + cos * p1y;

            x2 = cos * p2x - sin * p2y;
            y2 = sin * p2x + cos * p2y;

            x3 = cos * p3x - sin * p3y;
            y3 = sin * p3x + cos * p3y;

            x4 = x1 + (x3 - x2);
            y4 = y3 - (y2 - y1);
        } else {
            x1 = p1x;
            y1 = p1y;

            x2 = p2x;
            y2 = p2y;

            x3 = p3x;
            y3 = p3y;

            x4 = p4x;
            y4 = p4y;
        }

        x1 += worldOriginX;
        y1 += worldOriginY;
        x2 += worldOriginX;
        y2 += worldOriginY;
        x3 += worldOriginX;
        y3 += worldOriginY;
        x4 += worldOriginX;
        y4 += worldOriginY;

        float u1, v1, u2, v2, u3, v3, u4, v4;
        if (clockwise) {
            u1 = region.getU2();
            v1 = region.getV2();
            u2 = region.getU();
            v2 = region.getV2();
            u3 = region.getU();
            v3 = region.getV();
            u4 = region.getU2();
            v4 = region.getV();
        } else {
            u1 = region.getU();
            v1 = region.getV();
            u2 = region.getU2();
            v2 = region.getV();
            u3 = region.getU2();
            v3 = region.getV2();
            u4 = region.getU();
            v4 = region.getV2();
        }

        float color = this.colorPacked;

        vertices[idx++] = x1;
        vertices[idx++] = y1;
        vertices[idx++] = color;
        vertices[idx++] = u1;
        vertices[idx++] = v1;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;

        vertices[idx++] = x2;
        vertices[idx++] = y2;
        vertices[idx++] = color;
        vertices[idx++] = u2;
        vertices[idx++] = v2;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;

        vertices[idx++] = x3;
        vertices[idx++] = y3;
        vertices[idx++] = color;
        vertices[idx++] = u3;
        vertices[idx++] = v3;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;

        vertices[idx++] = x4;
        vertices[idx++] = y4;
        vertices[idx++] = color;
        vertices[idx++] = u4;
        vertices[idx++] = v4;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;
    }

    @Override
    public void draw(TextureRegion region, float width, float height, Affine2 transform) {
        if (!drawing)
            throw new IllegalStateException("TextureArraySpriteBatch.begin must be called before draw.");

        float[] vertices = this.vertices;

        flushIfFull();

        final float ti = activateTexture(region.getTexture());
        final float lsi = activateLabelStyle();
        final float upi = activateUiParams(region);

        // construct corner points
        float x1 = transform.m02;
        float y1 = transform.m12;
        float x2 = transform.m01 * height + transform.m02;
        float y2 = transform.m11 * height + transform.m12;
        float x3 = transform.m00 * width + transform.m01 * height + transform.m02;
        float y3 = transform.m10 * width + transform.m11 * height + transform.m12;
        float x4 = transform.m00 * width + transform.m02;
        float y4 = transform.m10 * width + transform.m12;

        float u = region.getU();
        float v = region.getV2();
        float u2 = region.getU2();
        float v2 = region.getV();

        float color = this.colorPacked;

        vertices[idx++] = x1;
        vertices[idx++] = y1;
        vertices[idx++] = color;
        vertices[idx++] = u;
        vertices[idx++] = v;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;

        vertices[idx++] = x2;
        vertices[idx++] = y2;
        vertices[idx++] = color;
        vertices[idx++] = u;
        vertices[idx++] = v2;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;

        vertices[idx++] = x3;
        vertices[idx++] = y3;
        vertices[idx++] = color;
        vertices[idx++] = u2;
        vertices[idx++] = v2;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;

        vertices[idx++] = x4;
        vertices[idx++] = y4;
        vertices[idx++] = color;
        vertices[idx++] = u2;
        vertices[idx++] = v;
        if (maxTextureUnits > 1) { vertices[idx++] = ti; }
        vertices[idx++] = lsi;
        vertices[idx++] = upi;
    }

    /**
     * Flushes if the vertices array cannot hold an additional sprite ((spriteVertexSize + 1) * 4 vertices) anymore.
     */
    private void flushIfFull() {
        // original Sprite attribute size plus one extra float per sprite vertex
        int spriteSize = maxTextureUnits > 1 ? SPRITE_SIZE_TEX_ARRAY : SPRITE_SIZE;
        if (vertices.length - idx < spriteSize + spriteSize / VERTEX_SIZE) {
            flush();
        }
    }

    private void bindLabelStyle(int index, ShaderProgram shader) { // TODO: Use uniform array.
        Label.LabelStyle style = labelStylesArray[index];
        Gdx.gl.glUniform4f(
                shader.fetchUniformLocation("u_msdfParams[" + index + "].color", true),
                style.getFontColor().r,
                style.getFontColor().g,
                style.getFontColor().b,
                style.getFontColor().a
        );
        Gdx.gl.glUniform4f(
                shader.fetchUniformLocation("u_msdfParams[" + index + "].shadowColor", true),
                style.getShadowColor().r,
                style.getShadowColor().g,
                style.getShadowColor().b,
                style.getShadowColor().a
        );
        Gdx.gl.glUniform4f(
                shader.fetchUniformLocation("u_msdfParams[" + index + "].innerShadowColor", true),
                style.getInnerShadowColor().r,
                style.getInnerShadowColor().g,
                style.getInnerShadowColor().b,
                style.getInnerShadowColor().a
        );
        Gdx.gl.glUniform4f(
                shader.fetchUniformLocation("u_msdfParams[" + index + "].data1", true),
                style.getShadowOffset().x,
                style.getShadowOffset().y,
                style.font.getDistanceRange() * style.getSize() / style.font.getGlyphSize(),
                style.getWeight()
        );
        Gdx.gl.glUniform4f(
                shader.fetchUniformLocation("u_msdfParams[" + index + "].data2", true),
                style.isShadowClipped() ? 1f : 0f,
                style.getShadowSmoothing(),
                style.getInnerShadowRange(),
                style.getShadowIntensity()
        );
    }

    private void bindUiParams(int index, ShaderProgram shader) { // TODO: Use uniform array.
        UiParams params = uiParamsArray[index];
        Vector4 region = regionsArray[index];
        Gdx.gl.glUniform4f(
                shader.fetchUniformLocation("u_uiParams[" + index + "].region", true),
                region.x,
                region.y,
                region.z,
                region.w
        );
        Gdx.gl.glUniform4f(
                shader.fetchUniformLocation("u_uiParams[" + index + "].cornerRadii", true),
                params.getAutoCornerRadii(region.z, region.w).z, // bottom right in shader
                params.getAutoCornerRadii(region.z, region.w).y, // top right in shader
                params.getAutoCornerRadii(region.z, region.w).w, // bottom left in shader
                params.getAutoCornerRadii(region.z, region.w).x  // top left in shader
        );
        Gdx.gl.glUniform4f(
                shader.fetchUniformLocation("u_uiParams[" + index + "].borderColor", true),
                params.getBorderColor().r,
                params.getBorderColor().g,
                params.getBorderColor().b,
                params.getBorderColor().a
        );
        Gdx.gl.glUniform4f(
                shader.fetchUniformLocation("u_uiParams[" + index + "].data", true),
                params.getContentScaleForSize(region.z, region.w),
                params.getBorderOutSoftness(),
                params.getBorderThickness(),
                params.getBorderInSoftness()
        );
    }

    private void bindTextureSize(int index, ShaderProgram shader) {
        Texture texture = usedTextures[index];
        Gdx.gl.glUniform2f(
                shader.fetchUniformLocation(maxTextureUnits > 1 ? "u_textureSizes[" + index + "]" : "u_textureSize", true),
                texture != null ? texture.getWidth() : 1f,
                texture != null ? texture.getHeight() : 1f
        );
    }

    @Override
    public void flush() {
        if (idx == 0) return;

        renderCalls++;
        totalRenderCalls++;

        int spritesInBatch = idx / (maxTextureUnits > 1 ? SPRITE_SIZE_TEX_ARRAY : SPRITE_SIZE);
        if (spritesInBatch > maxSpritesInBatch) maxSpritesInBatch = spritesInBatch;
        int count = spritesInBatch * 6;

        // Bind the textures
        for (int i = 0; i < currentTextureLFUSize; i++) {
            usedTextures[i].bind(i);
            bindTextureSize(i, customShader != null ? customShader : shader);
        }
        for (int i = 0; i < labelStylesCount; i++) {
            bindLabelStyle(i, customShader != null ? customShader : shader);
        }
        for (int i = 0; i < uiParamsCount; i++) {
            bindUiParams(i, customShader != null ? customShader : shader);
        }
        Arrays.fill(labelStylesArray, null);
        Arrays.fill(uiParamsArray, null);
        labelStylesCount = 0;
        uiParamsCount = 0;

        // Set TEXTURE0 as active again before drawing.
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);

        Mesh mesh = this.mesh;
        mesh.setVertices(vertices, 0, idx);
        mesh.getIndicesBuffer(true).position(0);
        mesh.getIndicesBuffer(true).limit(count);

        if (blendingDisabled) {
            Gdx.gl.glDisable(GL20.GL_BLEND);
        } else {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            if (blendSrcFunc != -1)
                Gdx.gl.glBlendFuncSeparate(blendSrcFunc, blendDstFunc, blendSrcFuncAlpha, blendDstFuncAlpha);
        }

        if (customShader != null) {
            mesh.render(customShader, GL20.GL_TRIANGLES, 0, count);
        } else {
            mesh.render(shader, GL20.GL_TRIANGLES, 0, count);
        }
        idx = 0;
    }

    /**
     * Assigns Texture units and manages the LFU cache.
     *
     * @param texture The texture that shall be loaded into the cache, if it is not already loaded.
     * @return The texture slot that has been allocated to the selected texture
     */
    private int activateTexture(Texture texture) {
        invTexWidth = 1.0f / texture.getWidth();
        invTexHeight = 1.0f / texture.getHeight();
        // This is our identifier for the textures
        final int textureHandle = texture.getTextureObjectHandle();
        // First try to see if the texture is already cached
        for (int i = 0; i < currentTextureLFUSize; i++) {
            // getTextureObjectHandle() just returns an int,
            // it's fine to call this method instead of caching the value.
            if (textureHandle == usedTextures[i].getTextureObjectHandle()) {
                // Increase the access counter.
                usedTexturesLFU[i]++;
                return i;
            }
        }

        // If a free texture unit is available we just use it
        // If not we have to flush and then throw out the least accessed one.
        if (currentTextureLFUSize < maxTextureUnits) {
            // Put the texture into the next free slot
            usedTextures[currentTextureLFUSize] = texture;
            // Increase the access counter.
            usedTexturesLFU[currentTextureLFUSize]++;
            return currentTextureLFUSize++;
        } else {
            // We have to flush if there is something in the pipeline already,
            // otherwise the texture index of previously rendered sprites gets invalidated
            if (idx > 0) {
                flush();
            }
            int slot = 0;
            int slotVal = usedTexturesLFU[0];
            int max = 0;
            int average = 0;
            // We search for the best candidate for a swap (least accessed) and collect some data
            for (int i = 0; i < maxTextureUnits; i++) {
                final int val = usedTexturesLFU[i];
                max = max(val, max);
                average += val;
                if (val <= slotVal) {
                    slot = i;
                    slotVal = val;
                }
            }

            // The LFU weights will be normalized to the range 0...100
            final int normalizeRange = 100;
            for (int i = 0; i < maxTextureUnits; i++) {
                usedTexturesLFU[i] = usedTexturesLFU[i] * normalizeRange / max;
            }
            average = (average * normalizeRange) / (max * maxTextureUnits);
            // Give the new texture a fair (average) chance of staying.
            usedTexturesLFU[slot] = average;
            usedTextures[slot] = texture;
            // For statistics
            currentTextureLFUSwaps++;
            return slot;
        }
    }

    private int activateLabelStyle() {
        if (currentLabelStyle == null) {
            return -1;
        }
        for (int i = 0; i < labelStylesCount; i++) {
            if (currentLabelStyle.equals(labelStylesArray[i])) {
                currentLabelStyle = null;
                return i;
            }
        }
        if (labelStylesCount >= maxMsdfParams) {
            flush();
        }
        labelStylesArray[labelStylesCount] = currentLabelStyle;
        labelStylesCount++;
        currentLabelStyle = null;
        return labelStylesCount - 1;
    }

    private int activateUiParams(Texture texture) {
        return activateUiParams(0f, 0f, texture.getWidth(), texture.getHeight());
    }

    private int activateUiParams(TextureRegion region) {
        return activateUiParams(
                region.getRegionX(),
                region.getRegionY(),
                region.getRegionWidth(),
                region.getRegionHeight()
        );
    }

    private Vector4 tmpVector4 = new Vector4();
    private int activateUiParams(float srcX, float srcY, float srcWidth, float srcHeight) {
        if (currentUiParams == null || currentUiParams.isDefault()) {
            return -1;
        }
        tmpVector4.set(srcX, srcY, srcWidth, srcHeight);
        for (int i = 0; i < uiParamsCount; i++) {
            if (tmpVector4.equals(regionsArray[i]) && currentUiParams.equals(uiParamsArray[i])) {
                currentUiParams = null;
                return i;
            }
        }
        if (uiParamsCount >= maxUiParams) {
            flush();
        }
        uiParamsArray[uiParamsCount] = currentUiParams;
        regionsArray[uiParamsCount].set(tmpVector4);
        uiParamsCount++;
        currentUiParams = null;
        return uiParamsCount - 1;
    }

    /**
     * @return The number of texture swaps the LFU cache performed since calling {@link#begin()}.
     */
    public int getTextureLFUSwaps() {
        return currentTextureLFUSwaps;
    }

    /**
     * @return The current number of textures in the LFU cache. Gets reset when calling {@link#begin()}.
     */
    public int getTextureLFUSize() {
        return currentTextureLFUSize;
    }

    @Override
    public void disableBlending() {
        if (blendingDisabled) return;
        flush();
        blendingDisabled = true;
    }

    @Override
    public void enableBlending() {
        if (!blendingDisabled) {
            return;
        }
        flush();
        blendingDisabled = false;
    }

    @Override
    public void setBlendFunction(int srcFunc, int dstFunc) {
        setBlendFunctionSeparate(srcFunc, dstFunc, srcFunc, dstFunc);
    }

    @Override
    public void setBlendFunctionSeparate(int srcFuncColor, int dstFuncColor, int srcFuncAlpha, int dstFuncAlpha) {
        if (blendSrcFunc == srcFuncColor && blendDstFunc == dstFuncColor && blendSrcFuncAlpha == srcFuncAlpha
                && blendDstFuncAlpha == dstFuncAlpha) {
            return;
        }

        flush();

        blendSrcFunc = srcFuncColor;
        blendDstFunc = dstFuncColor;
        blendSrcFuncAlpha = srcFuncAlpha;
        blendDstFuncAlpha = dstFuncAlpha;
    }

    @Override
    public int getBlendSrcFunc() {
        return blendSrcFunc;
    }

    @Override
    public int getBlendDstFunc() {
        return blendDstFunc;
    }

    @Override
    public int getBlendSrcFuncAlpha() {
        return blendSrcFuncAlpha;
    }

    @Override
    public int getBlendDstFuncAlpha() {
        return blendDstFuncAlpha;
    }

    @Override
    public boolean isBlendingEnabled() {
        return !blendingDisabled;
    }

    @Override
    public boolean isDrawing() {
        return drawing;
    }

    @Override
    public Matrix4 getProjectionMatrix() {
        return projectionMatrix;
    }

    @Override
    public Matrix4 getTransformMatrix() {
        return transformMatrix;
    }

    @Override
    public void setProjectionMatrix(Matrix4 projection) {
        if (drawing) {
            flush();
        }
        projectionMatrix.set(projection);
        if (drawing) {
            setupMatrices();
        }
    }

    @Override
    public void setTransformMatrix(Matrix4 transform) {
        if (drawing) {
            flush();
        }
        transformMatrix.set(transform);
        if (drawing) {
            setupMatrices();
        }
    }

    private Label.LabelStyle currentLabelStyle = null;

    public void setLabelStyle(Label.LabelStyle style) {
        currentLabelStyle = style;
    }

    private UiParams currentUiParams = null;

    public void setUiParams(UiParams params) {
        currentUiParams = params;
    }

    protected void setupMatrices() {
        combinedMatrix.set(projectionMatrix).mul(transformMatrix);
        if (customShader != null) {
            customShader.setUniformMatrix("u_projTrans", combinedMatrix);
            if (maxTextureUnits > 1) {
                Gdx.gl20.glUniform1iv(customShader.fetchUniformLocation("u_textures", true), maxTextureUnits, textureUnitIndicesBuffer);
            }
        } else {
            shader.setUniformMatrix("u_projTrans", combinedMatrix);
            if (maxTextureUnits > 1) {
                Gdx.gl20.glUniform1iv(shader.fetchUniformLocation("u_textures", true), maxTextureUnits, textureUnitIndicesBuffer);
            }
        }
    }

    /**
     * Sets the shader to be used in a GLES environment. Vertex position attribute is called "a_position", the texture
     * coordinates attribute is called "a_texCoord0", the color attribute is called "a_color", texture unit index is called
     * "texture_index", this needs to be converted to int with int(...) in the fragment shader. See
     * {@link ShaderProgram#POSITION_ATTRIBUTE}, {@link ShaderProgram#COLOR_ATTRIBUTE} and {@link ShaderProgram#TEXCOORD_ATTRIBUTE}
     * which gets "0" appended to indicate the use of the first texture unit. The combined transform and projection matrix is
     * uploaded via a mat4 uniform called "u_projTrans". The texture sampler array is passed via a uniform called "u_textures".
     * <p>
     * Call this method with a null argument to use the default shader.
     * <p>
     * This method will flush the batch before setting the new shader, you can call it in between {@link #begin()} and
     * {@link #end()}.
     *
     * @param shader the {@link ShaderProgram} or null to use the default shader.
     */
    @Override
    public void setShader(ShaderProgram shader) {
        if (shader == customShader) // avoid unnecessary flushing in case we are drawing
            return;
        if (drawing) {
            flush();
        }
        customShader = shader;
        if (drawing) {
            if (customShader != null) {
                customShader.bind();
            } else {
                this.shader.bind();
            }
            setupMatrices();
        }
    }

    @Override
    public ShaderProgram getShader() {
        if (customShader != null) {
            return customShader;
        }
        return shader;
    }

    /**
     * Queries the number of supported textures in a texture array by trying the create the default shader.<br>
     * The first call of this method is very expensive, after that it simply returns a cached value.
     *
     * @return the number of supported textures in a texture array or zero if this feature is unsupported on this device.
     * @see {@link #setShader(ShaderProgram shader)}
     */
    public static int getMaxTextureUnits() {
        if (maxTextureUnits == -1) {
            // Query the number of available texture units and decide on a safe number of texture units to use
            IntBuffer texUnitsQueryBuffer = BufferUtils.newIntBuffer(32);
            Gdx.gl.glGetIntegerv(GL20.GL_MAX_TEXTURE_IMAGE_UNITS, texUnitsQueryBuffer);
            int maxTextureUnitsLocal = texUnitsQueryBuffer.get();
            // Some OpenGL drivers (I'm looking at you, Intel!) do not report the right values,
            // so we take caution and test it first, reducing the number of slots if needed.
            // Will try to find the maximum amount of texture units supported.
            while (maxTextureUnitsLocal > 0) {
                try {
                    ShaderProgram tempProg = createDefaultShader(maxTextureUnitsLocal, 1, 1);
                    tempProg.dispose();
                    break;
                } catch (Exception e) {
                    maxTextureUnitsLocal /= 2;
                    shaderErrorLog = e.getMessage();
                }
            }
            maxTextureUnits = maxTextureUnitsLocal;
        }
        return max(1, maxTextureUnits);
    }

    public static int getMaxVertexUniformVectors() {
        if (maxVertexUniformVectors == -1) {
            // Query the number of available texture units and decide on a safe number of texture units to use
            IntBuffer buffer = BufferUtils.newIntBuffer(32);
            Gdx.gl.glGetIntegerv(GL20.GL_MAX_VERTEX_UNIFORM_VECTORS, buffer);
            maxVertexUniformVectors = buffer.get();
        }
        return maxVertexUniformVectors;
    }

    /** Returns a new instance of the default shader used by TextureArrayMsdfSpriteBatch when no shader is specified. */
    public static ShaderProgram createDefaultShader (int maxTextureUnits, int maxMsdfParams, int maxUiParams) {
        String vertexShader = Gdx.files.classpath("com/badlogic/gdx/graphics/g2d/shaders/sprite_batch.vertex.glsl").readString();
        String fragmentShader = Gdx.files.classpath("com/badlogic/gdx/graphics/g2d/shaders/sprite_batch.fragment.glsl").readString();
        String prependPart1 = Gdx.app.getType() == Application.ApplicationType.Desktop ? "#version 150\n" : "#version 320 es\n";
        String prependPart2 = Gdx.graphics.isGL30Available() ? "#define GLSL3\n" : "";
        String prependPart3 = maxTextureUnits > 1 ? "#define TEXTURE_ARRAY\n#define MAX_TEXTURE_UNITS " + maxTextureUnits + "\n" : "";
        String prependPart4 = "#define MAX_MSDF_PARAMS " + maxMsdfParams + "\n";
        String prependPart5 = "#define MAX_UI_PARAMS " + maxUiParams + "\n";
        String prependText = prependPart1 + prependPart2 + prependPart3 + prependPart4 + prependPart5;
        ShaderProgram.prependVertexCode = prependText;
        ShaderProgram.prependFragmentCode = prependText;

        ShaderProgram shader = new ShaderProgram(vertexShader, fragmentShader);
        if (!shader.isCompiled()) throw new IllegalArgumentException("Error compiling shader: " + shader.getLog());
        ShaderProgram.prependFragmentCode = null;
        ShaderProgram.prependVertexCode = null;
        return shader;
    }
}