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

package com.badlogic.gdx.math;

import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.NumberUtils;

/** Encapsulates a 4D vector. Allows chaining operations by returning a reference to itself in all modification methods.
 * @author badlogicgames@gmail.com */
public class Vector4 {
	/** the x-component of this vector **/
	public float x;
	/** the y-component of this vector **/
	public float y;
	/** the z-component of this vector **/
	public float z;
	/** the w-component of this vector **/
	public float w;

	public final static Vector4 X = new Vector4(1, 0, 0, 0);
	public final static Vector4 Y = new Vector4(0, 1, 0, 0);
	public final static Vector4 Z = new Vector4(0, 0, 1, 0);
	public final static Vector4 W = new Vector4(0, 0, 0, 1);
	public final static Vector4 Zero = new Vector4(0, 0, 0, 0);

	/** Constructs a vector at (0,0,0,0) */
	public Vector4() {
	}

	/** Creates a vector with the given components
	 * @param x The x-component
	 * @param y The y-component
	 * @param z The z-component
	 * @param w The w-component */
	public Vector4(float x, float y, float z, float w) {
		this.set(x, y, z, w);
	}

	/** Creates a vector from the given vector
	 * @param vector The vector */
	public Vector4(final Vector4 vector) {
		this.set(vector);
	}

	/** Creates a vector from the given array. The array must have at least 4 elements.
	 *
	 * @param values The array */
	public Vector4(final float[] values) {
		this.set(values[0], values[1], values[2], values[3]);
	}

	/** Creates a vector from the given vector, z-component and w-component
	 *
	 * @param vector The vector
	 * @param z The z-component 
	 * @param w The w-component */
	public Vector4(final Vector2 vector, float z, float w) {
		this.set(vector.x, vector.y, z, w);
	}

	/** Creates a vector from the given vector and w-component
	 *
	 * @param vector The vector
	 * @param w The w-component */
	public Vector4(final Vector3 vector, float w) {
		this.set(vector.x, vector.y, vector.z, w);
	}

	/** Sets the vector to the given components
	 *
	 * @param x The x-component
	 * @param y The y-component
	 * @param z The z-component
	 * @return this vector for chaining */
	public Vector4 set (float x, float y, float z, float w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
		return this;
	}

	public Vector4 set (final Vector4 vector) {
		return this.set(vector.x, vector.y, vector.z, vector.w);
	}

	/** Sets the components from the array. The array must have at least 3 elements
	 *
	 * @param values The array
	 * @return this vector for chaining */
	public Vector4 set (final float[] values) {
		return this.set(values[0], values[1], values[2], values[3]);
	}

	/** Sets the components of the given vector and z-component
	 *
	 * @param vector The vector
	 * @param z The z-component
	 * @param w The w-component
	 * @return This vector for chaining */
	public Vector4 set (final Vector2 vector, float z, float w) {
		return this.set(vector.x, vector.y, z, w);
	}

	/** Sets the components of the given vector and z-component
	 *
	 * @param vector The vector
	 * @param w The w-component
	 * @return This vector for chaining */
	public Vector4 set (final Vector3 vector, float w) {
		return this.set(vector.x, vector.y, vector.z, w);
	}

	public Vector4 cpy () {
		return new Vector4(this);
	}

	public Vector4 add (final Vector4 vector) {
		return this.add(vector.x, vector.y, vector.z, vector.w);
	}

	/** Adds the given vector to this component
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @param z The z-component of the other vector
	 * @param w The w-component of the other vector
	 * @return This vector for chaining. */
	public Vector4 add (float x, float y, float z, float w) {
		return this.set(this.x + x, this.y + y, this.z + z, this.w + w);
	}

	/** Adds the given value to all three components of the vector.
	 *
	 * @param values The value
	 * @return This vector for chaining */
	public Vector4 add (float values) {
		return this.set(this.x + values, this.y + values, this.z + values, this.w + values);
	}

	public Vector4 sub (final Vector4 a_vec) {
		return this.sub(a_vec.x, a_vec.y, a_vec.z, a_vec.w);
	}

	/** Subtracts the other vector from this vector.
	 *
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @param z The z-component of the other vector
	 * @param w The w-component of the other vector
	 * @return This vector for chaining */
	public Vector4 sub (float x, float y, float z, float w) {
		return this.set(this.x - x, this.y - y, this.z - z, this.w - w);
	}

	/** Subtracts the given value from all components of this vector
	 *
	 * @param value The value
	 * @return This vector for chaining */
	public Vector4 sub (float value) {
		return this.set(this.x - value, this.y - value, this.z - value, this.w - value);
	}

	public Vector4 scl (float scalar) {
		return this.set(this.x * scalar, this.y * scalar, this.z * scalar, this.w * scalar);
	}

	public Vector4 scl (final Vector4 other) {
		return this.set(x * other.x, y * other.y, z * other.z, w * other.w);
	}

	/** Scales this vector by the given values
	 * @param vx X value
	 * @param vy Y value
	 * @param vz Z value
	 * @param vw W value
	 * @return This vector for chaining */
	public Vector4 scl (float vx, float vy, float vz, float vw) {
		return this.set(this.x * vx, this.y * vy, this.z * vz, this.w * vw);
	}

	public Vector4 mulAdd (Vector4 vec, float scalar) {
		this.x += vec.x * scalar;
		this.y += vec.y * scalar;
		this.z += vec.z * scalar;
		this.w += vec.w * scalar;
		return this;
	}

	public Vector4 mulAdd (Vector4 vec, Vector4 mulVec) {
		this.x += vec.x * mulVec.x;
		this.y += vec.y * mulVec.y;
		this.z += vec.z * mulVec.z;
		this.w += vec.w * mulVec.w;
		return this;
	}

	/** @return The euclidean length */
	public static float len (final float x, final float y, final float z, final float w) {
		return (float)Math.sqrt(x * x + y * y + z * z + w * w);
	}

	public float len () {
		return (float)Math.sqrt(x * x + y * y + z * z + w * w);
	}

	/** @return The squared euclidean length */
	public static float len2 (final float x, final float y, final float z, final float w) {
		return x * x + y * y + z * z + w * w;
	}

	public float len2 () {
		return x * x + y * y + z * z + w * w;
	}

	/** @param vector The other vector
	 * @return Whether this and the other vector are equal */
	public boolean idt (final Vector4 vector) {
		return x == vector.x && y == vector.y && z == vector.z && w == vector.w;
	}

	/** @return The euclidean distance between the two specified vectors */
	public static float dst (final float x1, final float y1, final float z1, final float x2, final float y2, final float z2, final float w1, final float w2) {
		final float a = x2 - x1;
		final float b = y2 - y1;
		final float c = z2 - z1;
		final float d = w2 - w1;
		return (float)Math.sqrt(a * a + b * b + c * c + d * d);
	}

	public float dst (final Vector4 vector) {
		final float a = vector.x - x;
		final float b = vector.y - y;
		final float c = vector.z - z;
		final float d = vector.w - w;
		return (float)Math.sqrt(a * a + b * b + c * c + d * d);
	}

	/** @return the distance between this point and the given point */
	public float dst (float x, float y, float z, float w) {
		final float a = x - this.x;
		final float b = y - this.y;
		final float c = z - this.z;
		final float d = w - this.w;
		return (float)Math.sqrt(a * a + b * b + c * c + d * d);
	}

	/** @return the squared distance between the given points */
	public static float dst2 (final float x1, final float y1, final float z1, final float x2, final float y2, final float z2, final float w1, final float w2) {
		final float a = x2 - x1;
		final float b = y2 - y1;
		final float c = z2 - z1;
		final float d = w2 - w1;
		return a * a + b * b + c * c + d * d;
	}

	public float dst2 (Vector4 point) {
		final float a = point.x - x;
		final float b = point.y - y;
		final float c = point.z - z;
		final float d = point.w - w;
		return a * a + b * b + c * c + d * d;
	}

	/** Returns the squared distance between this point and the given point
	 * @param x The x-component of the other point
	 * @param y The y-component of the other point
	 * @param z The z-component of the other point
	 * @param w The w-component of the other point
	 * @return The squared distance */
	public float dst2 (float x, float y, float z, float w) {
		final float a = x - this.x;
		final float b = y - this.y;
		final float c = z - this.z;
		final float d = w - this.w;
		return a * a + b * b + c * c + d * d;
	}

	public Vector4 nor () {
		final float len2 = this.len2();
		if (len2 == 0f || len2 == 1f) return this;
		return this.scl(1f / (float)Math.sqrt(len2));
	}

	/** @return The dot product between the two vectors */
	public static float dot (float x1, float y1, float z1, float x2, float y2, float z2, float w1, float w2) {
		return x1 * x2 + y1 * y2 + z1 * z2 + w1 * w2;
	}

	public float dot (final Vector4 vector) {
		return x * vector.x + y * vector.y + z * vector.z + w * vector.w;
	}

	/** Returns the dot product between this and the given vector.
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @param z The z-component of the other vector
	 * @param w The w-component of the other vector
	 * @return The dot product */
	public float dot (float x, float y, float z, float w) {
		return this.x * x + this.y * y + this.z * z + this.w * w;
	}

	public boolean isUnit () {
		return isUnit(0.000000001f);
	}

	public boolean isUnit (final float margin) {
		return Math.abs(len2() - 1f) < margin;
	}

	public boolean isZero () {
		return x == 0 && y == 0 && z == 0 && w == 0;
	}

	public boolean isZero (final float margin) {
		return len2() < margin;
	}

	public boolean isPerpendicular (Vector4 vector) {
		return MathUtils.isZero(dot(vector));
	}

	public boolean isPerpendicular (Vector4 vector, float epsilon) {
		return MathUtils.isZero(dot(vector), epsilon);
	}

	public boolean hasSameDirection (Vector4 vector) {
		return dot(vector) > 0;
	}

	public boolean hasOppositeDirection (Vector4 vector) {
		return dot(vector) < 0;
	}

	public Vector4 lerp (final Vector4 target, float alpha) {
		x += alpha * (target.x - x);
		y += alpha * (target.y - y);
		z += alpha * (target.z - z);
		w += alpha * (target.w - w);
		return this;
	}

	public Vector4 interpolate (Vector4 target, float alpha, Interpolation interpolator) {
		return lerp(target, interpolator.apply(0f, 1f, alpha));
	}

	/** Spherically interpolates between this vector and the target vector by alpha which is in the range [0,1]. The result is
	 * stored in this vector.
	 *
	 * @param target The target vector
	 * @param alpha The interpolation coefficient
	 * @return This vector for chaining. */
	public Vector4 slerp (final Vector4 target, float alpha) {
		final float dot = dot(target);
		// If the inputs are too close for comfort, simply linearly interpolate.
		if (dot > 0.9995 || dot < -0.9995) return lerp(target, alpha);

		// theta0 = angle between input vectors
		final float theta0 = (float)Math.acos(dot);
		// theta = angle between this vector and result
		final float theta = theta0 * alpha;

		final float st = (float)Math.sin(theta);
		final float tx = target.x - x * dot;
		final float ty = target.y - y * dot;
		final float tz = target.z - z * dot;
		final float tw = target.w - w * dot;
		final float l2 = tx * tx + ty * ty + tz * tz + tw * tw;
		final float dl = st * ((l2 < 0.0001f) ? 1f : 1f / (float)Math.sqrt(l2));

		return scl((float)Math.cos(theta)).add(tx * dl, ty * dl, tz * dl, tw * dl).nor();
	}

	/** Converts this {@code Vector4} to a string in the format {@code (x,y,z,w)}.
	 * @return a string representation of this object. */
	public String toString () {
		return "(" + x + "," + y + "," + z + "," + w + ")";
	}

	/** Sets this {@code Vector4} to the value represented by the specified string according to the format of {@link #toString()}.
	 * @param v the string.
	 * @return this vector for chaining */
	public Vector4 fromString (String v) {
		int s0 = v.indexOf(',', 1);
		int s1 = v.indexOf(',', s0 + 1);
		int s2 = v.indexOf(',', s1 + 1);
		if (s0 != -1 && s1 != -1 && s2 != -1 && v.charAt(0) == '(' && v.charAt(v.length() - 1) == ')') {
			try {
				float x = Float.parseFloat(v.substring(1, s0));
				float y = Float.parseFloat(v.substring(s0 + 1, s1));
				float z = Float.parseFloat(v.substring(s1 + 1, s2));
				float w = Float.parseFloat(v.substring(s2 + 1, v.length() - 1));
				return this.set(x, y, z, w);
			} catch (NumberFormatException ex) {
				// Throw a GdxRuntimeException
			}
		}
		throw new GdxRuntimeException("Malformed Vector4: " + v);
	}

	public Vector4 limit (float limit) {
		return limit2(limit * limit);
	}

	public Vector4 limit2 (float limit2) {
		float len2 = len2();
		if (len2 > limit2) {
			scl((float)Math.sqrt(limit2 / len2));
		}
		return this;
	}

	public Vector4 setLength (float len) {
		return setLength2(len * len);
	}

	public Vector4 setLength2 (float len2) {
		float oldLen2 = len2();
		return (oldLen2 == 0 || oldLen2 == len2) ? this : scl((float)Math.sqrt(len2 / oldLen2));
	}

	public Vector4 clamp (float min, float max) {
		final float len2 = len2();
		if (len2 == 0f) return this;
		float max2 = max * max;
		if (len2 > max2) return scl((float)Math.sqrt(max2 / len2));
		float min2 = min * min;
		if (len2 < min2) return scl((float)Math.sqrt(min2 / len2));
		return this;
	}

	public int hashCode () {
		final int prime = 31;
		int result = 1;
		result = prime * result + NumberUtils.floatToIntBits(x);
		result = prime * result + NumberUtils.floatToIntBits(y);
		result = prime * result + NumberUtils.floatToIntBits(z);
		result = prime * result + NumberUtils.floatToIntBits(w);
		return result;
	}

	public boolean equals (Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Vector4 other = (Vector4)obj;
		if (NumberUtils.floatToIntBits(x) != NumberUtils.floatToIntBits(other.x)) return false;
		if (NumberUtils.floatToIntBits(y) != NumberUtils.floatToIntBits(other.y)) return false;
		if (NumberUtils.floatToIntBits(z) != NumberUtils.floatToIntBits(other.z)) return false;
		if (NumberUtils.floatToIntBits(w) != NumberUtils.floatToIntBits(other.w)) return false;
		return true;
	}

	public boolean epsilonEquals (final Vector4 other, float epsilon) {
		if (other == null) return false;
		if (Math.abs(other.x - x) > epsilon) return false;
		if (Math.abs(other.y - y) > epsilon) return false;
		if (Math.abs(other.z - z) > epsilon) return false;
		if (Math.abs(other.w - w) > epsilon) return false;
		return true;
	}

	/** Compares this vector with the other vector, using the supplied epsilon for fuzzy equality testing.
	 * @return whether the vectors are the same. */
	public boolean epsilonEquals (float x, float y, float z, float w, float epsilon) {
		if (Math.abs(x - this.x) > epsilon) return false;
		if (Math.abs(y - this.y) > epsilon) return false;
		if (Math.abs(z - this.z) > epsilon) return false;
		if (Math.abs(w - this.w) > epsilon) return false;
		return true;
	}

	/** Compares this vector with the other vector using MathUtils.FLOAT_ROUNDING_ERROR for fuzzy equality testing
	 *
	 * @param other other vector to compare
	 * @return true if vector are equal, otherwise false */
	public boolean epsilonEquals (final Vector4 other) {
		return epsilonEquals(other, MathUtils.FLOAT_ROUNDING_ERROR);
	}

	/** Compares this vector with the other vector using MathUtils.FLOAT_ROUNDING_ERROR for fuzzy equality testing
	 *
	 * @param x x component of the other vector to compare
	 * @param y y component of the other vector to compare
	 * @param z z component of the other vector to compare
	 * @param w w component of the other vector to compare
	 * @return true if vector are equal, otherwise false */
	public boolean epsilonEquals (float x, float y, float z, float w) {
		return epsilonEquals(x, y, z, w, MathUtils.FLOAT_ROUNDING_ERROR);
	}

	public Vector4 setZero () {
		this.x = 0;
		this.y = 0;
		this.z = 0;
		this.w = 0;
		return this;
	}
}
