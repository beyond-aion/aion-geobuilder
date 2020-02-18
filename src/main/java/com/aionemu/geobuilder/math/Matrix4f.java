/*
 * Copyright (c) 2009-2010 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.aionemu.geobuilder.math;


import com.aionemu.geobuilder.utils.BufferUtils;
import com.aionemu.geobuilder.utils.Quaternion;
import com.aionemu.geobuilder.utils.Vector3;

import java.nio.FloatBuffer;
import java.util.logging.Logger;


/**
 * <code>Matrix4f</code> defines and maintains a 4x4 matrix in row major order.
 * This matrix is intended for use in a translation and rotational capacity. 
 * It provides convenience methods for creating the matrix from a multitude 
 * of sources.
 * 
 * Matrices are stored assuming column vectors on the right, with the translation
 * in the rightmost column. Element numbering is row,column, so m03 is the zeroth
 * row, third column, which is the "x" translation part. This means that the implicit
 * storage order is column major. However, the get() and set() functions on float
 * arrays default to row major order!
 *
 * @author Mark Powell
 * @author Joshua Slack
 */
public final class Matrix4f implements Cloneable {

  private static final Logger logger = Logger.getLogger(Matrix4f.class.getName());

  public float m11, m12, m13, m14;
  public float m21, m22, m23, m24;
  public float m31, m32, m33, m34;
  public float m41, m42, m43, m44;

  public static final Matrix4f IDENTITY = new Matrix4f();

  /**
   * Constructor instantiates a new <code>Matrix</code> that is set to the
   * identity matrix.
   *  
   */
  public Matrix4f() {
    loadIdentity();
  }

  /**
   * constructs a matrix with the given values.
   */
  public Matrix4f(float m11, float m12, float m13, float m14,
          float m21, float m22, float m23, float m24,
          float m31, float m32, float m33, float m34,
          float m41, float m42, float m43, float m44) {

    this.m11 = m11;
    this.m12 = m12;
    this.m13 = m13;
    this.m14 = m14;
    this.m21 = m21;
    this.m22 = m22;
    this.m23 = m23;
    this.m24 = m24;
    this.m31 = m31;
    this.m32 = m32;
    this.m33 = m33;
    this.m34 = m34;
    this.m41 = m41;
    this.m42 = m42;
    this.m43 = m43;
    this.m44 = m44;
  }

  /**
   * Create a new Matrix4f, given data in column-major format.
   *
   * @param array
	 *		An array of 16 floats in column-major format (translation in elements 12, 13 and 14).
   */
  public Matrix4f(float[] array) {
  	set(array, false);
  }

  /**
   * Constructor instantiates a new <code>Matrix</code> that is set to the
   * provided matrix. This constructor copies a given Matrix. If the provided
   * matrix is null, the constructor sets the matrix to the identity.
   * 
   * @param mat
   *            the matrix to copy.
   */
  public Matrix4f(Matrix4f mat) {
    copy(mat);
  }

  /**
   * <code>copy</code> transfers the contents of a given matrix to this
   * matrix. If a null matrix is supplied, this matrix is set to the identity
   * matrix.
   * 
   * @param matrix
   *            the matrix to copy.
   */
  public void copy(Matrix4f matrix) {
    if (null == matrix) {
      loadIdentity();
    } else {
      m11 = matrix.m11;
      m12 = matrix.m12;
      m13 = matrix.m13;
      m14 = matrix.m14;
      m21 = matrix.m21;
      m22 = matrix.m22;
      m23 = matrix.m23;
      m24 = matrix.m24;
      m31 = matrix.m31;
      m32 = matrix.m32;
      m33 = matrix.m33;
      m34 = matrix.m34;
      m41 = matrix.m41;
      m42 = matrix.m42;
      m43 = matrix.m43;
      m44 = matrix.m44;
    }
  }

  /**
   * <code>get</code> retrieves the values of this object into
   * a float array in row-major order.
   * 
   * @param matrix
   *            the matrix to set the values into.
   */
  public void get(float[] matrix) {
    get(matrix, true);
  }

  /**
   * <code>set</code> retrieves the values of this object into
   * a float array.
   * 
   * @param matrix
   *            the matrix to set the values into.
   * @param rowMajor
   *            whether the outgoing data is in row or column major order.
   */
  public void get(float[] matrix, boolean rowMajor) {
    if (matrix.length != 16) throw new IllegalArgumentException(
        "Array must be of size 16.");

    if (rowMajor) {
      matrix[0] = m11;
      matrix[1] = m12;
      matrix[2] = m13;
      matrix[3] = m14;
      matrix[4] = m21;
      matrix[5] = m22;
      matrix[6] = m23;
      matrix[7] = m24;
      matrix[8] = m31;
      matrix[9] = m32;
      matrix[10] = m33;
      matrix[11] = m34;
      matrix[12] = m41;
      matrix[13] = m42;
      matrix[14] = m43;
      matrix[15] = m44;
    } else {
      matrix[0] = m11;
      matrix[4] = m12;
      matrix[8] = m13;
      matrix[12] = m14;
      matrix[1] = m21;
      matrix[5] = m22;
      matrix[9] = m23;
      matrix[13] = m24;
      matrix[2] = m31;
      matrix[6] = m32;
      matrix[10] = m33;
      matrix[14] = m34;
      matrix[3] = m41;
      matrix[7] = m42;
      matrix[11] = m43;
      matrix[15] = m44;
    }
  }

  /**
   * <code>get</code> retrieves a value from the matrix at the given
   * position. If the position is invalid a <code>JmeException</code> is
   * thrown.
   * 
   * @param i
   *            the row index.
   * @param j
   *            the colum index.
   * @return the value at (i, j).
   */
  public float get(int i, int j) {
    switch (i) {
    case 0:
      switch (j) {
      case 0: return m11;
      case 1: return m12;
      case 2: return m13;
      case 3: return m14;
      }
    case 1:
      switch (j) {
      case 0: return m21;
      case 1: return m22;
      case 2: return m23;
      case 3: return m24;
      }
    case 2:
      switch (j) {
      case 0: return m31;
      case 1: return m32;
      case 2: return m33;
      case 3: return m34;
      }
    case 3:
      switch (j) {
      case 0: return m41;
      case 1: return m42;
      case 2: return m43;
      case 3: return m44;
      }
    }

    logger.warning("Invalid matrix index.");
    throw new IllegalArgumentException("Invalid indices into matrix.");
  }

  /**
   * <code>getColumn</code> returns one of three columns specified by the
   * parameter. This column is returned as a float array of length 4.
   * 
   * @param i
   *            the column to retrieve. Must be between 0 and 3.
   * @return the column specified by the index.
   */
  public float[] getColumn(int i) {
    return getColumn(i, null);
  }

  /**
   * <code>getColumn</code> returns one of three columns specified by the
   * parameter. This column is returned as a float[4].
   * 
   * @param i
   *            the column to retrieve. Must be between 0 and 3.
   * @param store
   *            the float array to store the result in. if null, a new one
   *            is created.
   * @return the column specified by the index.
   */
  public float[] getColumn(int i, float[] store) {
    if (store == null) store = new float[4];
    switch (i) {
    case 0:
      store[0] = m11;
      store[1] = m21;
      store[2] = m31;
      store[3] = m41;
      break;
    case 1:
      store[0] = m12;
      store[1] = m22;
      store[2] = m32;
      store[3] = m42;
      break;
    case 2:
      store[0] = m13;
      store[1] = m23;
      store[2] = m33;
      store[3] = m43;
      break;
    case 3:
      store[0] = m14;
      store[1] = m24;
      store[2] = m34;
      store[3] = m44;
      break;
    default:
      logger.warning("Invalid column index.");
      throw new IllegalArgumentException("Invalid column index. " + i);
    }
    return store;
  }

  /**
   * 
   * <code>setColumn</code> sets a particular column of this matrix to that
   * represented by the provided vector.
   * 
   * @param i
   *            the column to set.
   * @param column
   *            the data to set.
   */
  public void setColumn(int i, float[] column) {

    if (column == null) {
      logger.warning("Column is null. Ignoring.");
      return;
    }
    switch (i) {
    case 0:
      m11 = column[0];
      m21 = column[1];
      m31 = column[2];
      m41 = column[3];
      break;
    case 1:
      m12 = column[0];
      m22 = column[1];
      m32 = column[2];
      m42 = column[3];
      break;
    case 2:
      m13 = column[0];
      m23 = column[1];
      m33 = column[2];
      m43 = column[3];
      break;
    case 3:
      m14 = column[0];
      m24 = column[1];
      m34 = column[2];
      m44 = column[3];
      break;
    default:
      logger.warning("Invalid column index.");
      throw new IllegalArgumentException("Invalid column index. " + i);
    }    }

  /**
   * <code>set</code> places a given value into the matrix at the given
   * position. If the position is invalid a <code>JmeException</code> is
   * thrown.
   * 
   * @param i
   *            the row index.
   * @param j
   *            the colum index.
   * @param value
   *            the value for (i, j).
   */
  public void set(int i, int j, float value) {
    switch (i) {
    case 0:
      switch (j) {
      case 0: m11 = value; return;
      case 1: m12 = value; return;
      case 2: m13 = value; return;
      case 3: m14 = value; return;
      }
    case 1:
      switch (j) {
      case 0: m21 = value; return;
      case 1: m22 = value; return;
      case 2: m23 = value; return;
      case 3: m24 = value; return;
      }
    case 2:
      switch (j) {
      case 0: m31 = value; return;
      case 1: m32 = value; return;
      case 2: m33 = value; return;
      case 3: m34 = value; return;
      }
    case 3:
      switch (j) {
      case 0: m41 = value; return;
      case 1: m42 = value; return;
      case 2: m43 = value; return;
      case 3: m44 = value; return;
      }
    }

    logger.warning("Invalid matrix index.");
    throw new IllegalArgumentException("Invalid indices into matrix.");
  }

  /**
   * <code>set</code> sets the values of this matrix from an array of
   * values.
   * 
   * @param matrix
   *            the matrix to set the value to.
   * @throws JmeException
   *             if the array is not of size 16.
   */
  public void set(float[][] matrix) {
    if (matrix.length != 4 || matrix[0].length != 4) { throw new IllegalArgumentException(
        "Array must be of size 16."); }

    m11 = matrix[0][0];
    m12 = matrix[0][1];
    m13 = matrix[0][2];
    m14 = matrix[0][3];
    m21 = matrix[1][0];
    m22 = matrix[1][1];
    m23 = matrix[1][2];
    m24 = matrix[1][3];
    m31 = matrix[2][0];
    m32 = matrix[2][1];
    m33 = matrix[2][2];
    m34 = matrix[2][3];
    m41 = matrix[3][0];
    m42 = matrix[3][1];
    m43 = matrix[3][2];
    m44 = matrix[3][3];
  }

  /**
   * <code>set</code> sets the values of this matrix from another matrix.
   *
   * @param matrix
   *            the matrix to read the value from.
   */
  public Matrix4f set(Matrix4f matrix) {
    m11 = matrix.m11; m12 = matrix.m12; m13 = matrix.m13; m14 = matrix.m14;
    m21 = matrix.m21; m22 = matrix.m22; m23 = matrix.m23; m24 = matrix.m24;
    m31 = matrix.m31; m32 = matrix.m32; m33 = matrix.m33; m34 = matrix.m34;
    m41 = matrix.m41; m42 = matrix.m42; m43 = matrix.m43; m44 = matrix.m44;
    return this;
  }

  /**
   * <code>set</code> sets the values of this matrix from an array of
   * values assuming that the data is rowMajor order;
   * 
   * @param matrix
   *            the matrix to set the value to.
   */
  public void set(float[] matrix) {
    set(matrix, true);
  }

  /**
   * <code>set</code> sets the values of this matrix from an array of
   * values;
   * 
   * @param matrix
   *            the matrix to set the value to.
   * @param rowMajor
   *            whether the incoming data is in row or column major order.
   */
  public void set(float[] matrix, boolean rowMajor) {
    if (matrix.length != 16) throw new IllegalArgumentException(
        "Array must be of size 16.");

    if (rowMajor) {
      m11 = matrix[0];
      m12 = matrix[1];
      m13 = matrix[2];
      m14 = matrix[3];
      m21 = matrix[4];
      m22 = matrix[5];
      m23 = matrix[6];
      m24 = matrix[7];
      m31 = matrix[8];
      m32 = matrix[9];
      m33 = matrix[10];
      m34 = matrix[11];
      m41 = matrix[12];
      m42 = matrix[13];
      m43 = matrix[14];
      m44 = matrix[15];
    } else {
      m11 = matrix[0];
      m12 = matrix[4];
      m13 = matrix[8];
      m14 = matrix[12];
      m21 = matrix[1];
      m22 = matrix[5];
      m23 = matrix[9];
      m24 = matrix[13];
      m31 = matrix[2];
      m32 = matrix[6];
      m33 = matrix[10];
      m34 = matrix[14];
      m41 = matrix[3];
      m42 = matrix[7];
      m43 = matrix[11];
      m44 = matrix[15];
    }
  }

  public Matrix4f transpose() {
    float[] tmp = new float[16];
    get(tmp, true);
    Matrix4f mat = new Matrix4f(tmp);
  	return mat;
  }

  /**
   * <code>transpose</code> locally transposes this Matrix.
   * 
   * @return this object for chaining.
   */
  public Matrix4f transposeLocal() {
    float tmp = m12;
    m12 = m21;
    m21 = tmp;

    tmp = m13;
    m13 = m31;
    m31 = tmp;

    tmp = m14;
    m14 = m41;
    m41 = tmp;

    tmp = m23;
    m23 = m32;
    m32 = tmp;

    tmp = m24;
    m24 = m42;
    m42 = tmp;

    tmp = m34;
    m34 = m43;
    m43 = tmp;

    return this;
  }
  
  
  /**
   * <code>toFloatBuffer</code> returns a FloatBuffer object that contains
   * the matrix data.
   * 
   * @return matrix data as a FloatBuffer.
   */
  public FloatBuffer toFloatBuffer() {
  	return toFloatBuffer(false);
  }

  /**
   * <code>toFloatBuffer</code> returns a FloatBuffer object that contains the
   * matrix data.
   * 
   * @param columnMajor
   *            if true, this buffer should be filled with column major data,
   *            otherwise it will be filled row major.
   * @return matrix data as a FloatBuffer. The position is set to 0 for
   *         convenience.
   */
  public FloatBuffer toFloatBuffer(boolean columnMajor) {
  	FloatBuffer fb = BufferUtils.createFloatBuffer(16);
  	fillFloatBuffer(fb, columnMajor);
  	fb.rewind();
  	return fb;
  }
  
  /**
   * <code>fillFloatBuffer</code> fills a FloatBuffer object with
   * the matrix data.
   * @param fb the buffer to fill, must be correct size
   * @return matrix data as a FloatBuffer.
   */
  public FloatBuffer fillFloatBuffer(FloatBuffer fb) {
  	return fillFloatBuffer(fb, false);
  }

  /**
   * <code>fillFloatBuffer</code> fills a FloatBuffer object with the matrix
   * data.
   * 
   * @param fb
   *            the buffer to fill, starting at current position. Must have
   *            room for 16 more floats.
   * @param columnMajor
   *            if true, this buffer should be filled with column major data,
   *            otherwise it will be filled row major.
   * @return matrix data as a FloatBuffer. (position is advanced by 16 and any
   *         limit set is not changed).
   */
  public FloatBuffer fillFloatBuffer(FloatBuffer fb, boolean columnMajor) {
    if(columnMajor) {
  	    fb.put(m11).put(m21).put(m31).put(m41);
	        fb.put(m12).put(m22).put(m32).put(m42);
	        fb.put(m13).put(m23).put(m33).put(m43);
	        fb.put(m14).put(m24).put(m34).put(m44);
	    } else {
	        fb.put(m11).put(m12).put(m13).put(m14);
	        fb.put(m21).put(m22).put(m23).put(m24);
	        fb.put(m31).put(m32).put(m33).put(m34);
	        fb.put(m41).put(m42).put(m43).put(m44);
	    }
    return fb;
  }

  public void fillFloatArray(float[] f, boolean columnMajor) {
    if(columnMajor) {
      f[ 0] = m11; f[ 1] = m21; f[ 2] = m31; f[ 3] = m41;
      f[ 4] = m12; f[ 5] = m22; f[ 6] = m32; f[ 7] = m42;
      f[ 8] = m13; f[ 9] = m23; f[10] = m33; f[11] = m43;
      f[12] = m14; f[13] = m24; f[14] = m34; f[15] = m44;
	    } else {
      f[ 0] = m11; f[ 1] = m12; f[ 2] = m13; f[ 3] = m14;
      f[ 4] = m21; f[ 5] = m22; f[ 6] = m23; f[ 7] = m24;
      f[ 8] = m31; f[ 9] = m32; f[10] = m33; f[11] = m34;
      f[12] = m41; f[13] = m42; f[14] = m43; f[15] = m44;
	    }
  }
  
  /**
   * <code>readFloatBuffer</code> reads value for this matrix from a FloatBuffer.
   * @param fb the buffer to read from, must be correct size
   * @return this data as a FloatBuffer.
   */
  public Matrix4f readFloatBuffer(FloatBuffer fb) {
  	return readFloatBuffer(fb, false);
  }

  /**
   * <code>readFloatBuffer</code> reads value for this matrix from a FloatBuffer.
   * @param fb the buffer to read from, must be correct size
   * @param columnMajor if true, this buffer should be filled with column
   * 		major data, otherwise it will be filled row major.
   * @return this data as a FloatBuffer.
   */
  public Matrix4f readFloatBuffer(FloatBuffer fb, boolean columnMajor) {
  	
  	if(columnMajor) {
  		m11 = fb.get(); m21 = fb.get(); m31 = fb.get(); m41 = fb.get();
  		m12 = fb.get(); m22 = fb.get(); m32 = fb.get(); m42 = fb.get();
  		m13 = fb.get(); m23 = fb.get(); m33 = fb.get(); m43 = fb.get();
  		m14 = fb.get(); m24 = fb.get(); m34 = fb.get(); m44 = fb.get();
  	} else {
  		m11 = fb.get(); m12 = fb.get(); m13 = fb.get(); m14 = fb.get();
  		m21 = fb.get(); m22 = fb.get(); m23 = fb.get(); m24 = fb.get();
  		m31 = fb.get(); m32 = fb.get(); m33 = fb.get(); m34 = fb.get();
  		m41 = fb.get(); m42 = fb.get(); m43 = fb.get(); m44 = fb.get();
  	}
    return this;
  }

  /**
   * <code>loadIdentity</code> sets this matrix to the identity matrix,
   * namely all zeros with ones along the diagonal.
   *  
   */
  public void loadIdentity() {
    m12 = m13 = m14 = 0.0f;
    m21 = m23 = m24 = 0.0f;
    m31 = m32 = m34 = 0.0f;
    m41 = m42 = m43 = 0.0f;
    m11 = m22 = m33 = m44 = 1.0f;
  }

  public void fromFrustum(float near, float far, float left, float right, float top, float bottom, boolean parallel){
    loadIdentity();
    if (parallel) {
      // scale
      m11 = 2.0f / (right - left);
      //m11 = 2.0f / (bottom - top);
      m22 = 2.0f / (top - bottom);
      m33 = -2.0f / (far - near);
      m44 = 1f;
      
      // translation
      m14 = -(right + left) / (right - left);
      //m31 = -(bottom + top) / (bottom - top);
      m24 = -(top + bottom) / (top - bottom);
      m34 = -(far + near) / (far - near);
    } else {
      m11 = (2.0f * near) / (right - left);
      m22 = (2.0f * near) / (top - bottom);
      m43 = -1.0f;
      m44 = -0.0f;

      // A
      m13 = (right + left) / (right - left);
      
      // B 
      m23 = (top + bottom) / (top - bottom);
      
      // C
      m33 = -(far + near) / (far - near);
      
      // D
      m34 = -(2.0f * far * near) / (far - near);
    }
  }

  /**
   * <code>fromAngleAxis</code> sets this matrix4f to the values specified
   * by an angle and an axis of rotation.  This method creates an object, so
   * use fromAngleNormalAxis if your axis is already normalized.
   * 
   * @param angle
   *            the angle to rotate (in radians).
   * @param axis
   *            the axis of rotation.
   */
  public void fromAngleAxis(float angle, Vector3f axis) {
    Vector3f normAxis = axis.normalize();
    fromAngleNormalAxis(angle, normAxis);
  }

  /**
   * <code>fromAngleNormalAxis</code> sets this matrix4f to the values
   * specified by an angle and a normalized axis of rotation.
   * 
   * @param angle
   *            the angle to rotate (in radians).
   * @param axis
   *            the axis of rotation (already normalized).
   */
  public void fromAngleNormalAxis(float angle, Vector3f axis) {
    zero();
    m44 = 1;

    float fCos = FastMath.cos(angle);
    float fSin = FastMath.sin(angle);
    float fOneMinusCos = ((float)1.0)-fCos;
    float fX2 = axis.x*axis.x;
    float fY2 = axis.y*axis.y;
    float fZ2 = axis.z*axis.z;
    float fXYM = axis.x*axis.y*fOneMinusCos;
    float fXZM = axis.x*axis.z*fOneMinusCos;
    float fYZM = axis.y*axis.z*fOneMinusCos;
    float fXSin = axis.x*fSin;
    float fYSin = axis.y*fSin;
    float fZSin = axis.z*fSin;
    
    m11 = fX2*fOneMinusCos+fCos;
    m12 = fXYM-fZSin;
    m13 = fXZM+fYSin;
    m21 = fXYM+fZSin;
    m22 = fY2*fOneMinusCos+fCos;
    m23 = fYZM-fXSin;
    m31 = fXZM-fYSin;
    m32 = fYZM+fXSin;
    m33 = fZ2*fOneMinusCos+fCos;
  }

  /**
   * <code>mult</code> multiplies this matrix by a scalar.
   * 
   * @param scalar
   *            the scalar to multiply this matrix by.
   */
  public void multLocal(float scalar) {
    m11 *= scalar;
    m12 *= scalar;
    m13 *= scalar;
    m14 *= scalar;
    m21 *= scalar;
    m22 *= scalar;
    m23 *= scalar;
    m24 *= scalar;
    m31 *= scalar;
    m32 *= scalar;
    m33 *= scalar;
    m34 *= scalar;
    m41 *= scalar;
    m42 *= scalar;
    m43 *= scalar;
    m44 *= scalar;
  }
  
  public Matrix4f mult(float scalar) {
  	Matrix4f out = new Matrix4f();
  	out.set(this);
  	out.multLocal(scalar);
  	return out;
  }
  
  public Matrix4f mult(float scalar, Matrix4f store) {
  	store.set(this);
  	store.multLocal(scalar);
  	return store;
  }

  /**
   * <code>mult</code> multiplies this matrix with another matrix. The
   * result matrix will then be returned. This matrix will be on the left hand
   * side, while the parameter matrix will be on the right.
   * 
   * @param in2
   *            the matrix to multiply this matrix by.
   * @return the resultant matrix
   */
  public Matrix4f mult(Matrix4f in2) {
    return mult(in2, null);
  }

  /**
   * <code>mult</code> multiplies this matrix with another matrix. The
   * result matrix will then be returned. This matrix will be on the left hand
   * side, while the parameter matrix will be on the right.
   * 
   * @param in2
   *            the matrix to multiply this matrix by.
   * @param store
   *            where to store the result. It is safe for in2 and store to be
   *            the same object.
   * @return the resultant matrix
   */
  public Matrix4f mult(Matrix4f in2, Matrix4f store) {
    if (store == null) store = new Matrix4f();

    float temp00, temp01, temp02, temp03;
    float temp10, temp11, temp12, temp13;
    float temp20, temp21, temp22, temp23;
    float temp30, temp31, temp32, temp33;

    temp00 = m11 * in2.m11 +
        m12 * in2.m21 +
        m13 * in2.m31 +
        m14 * in2.m41;
    temp01 = m11 * in2.m12 +
        m12 * in2.m22 +
        m13 * in2.m32 +
        m14 * in2.m42;
    temp02 = m11 * in2.m13 +
        m12 * in2.m23 +
        m13 * in2.m33 +
        m14 * in2.m43;
    temp03 = m11 * in2.m14 +
        m12 * in2.m24 +
        m13 * in2.m34 +
        m14 * in2.m44;
    
    temp10 = m21 * in2.m11 +
        m22 * in2.m21 +
        m23 * in2.m31 +
        m24 * in2.m41;
    temp11 = m21 * in2.m12 +
        m22 * in2.m22 +
        m23 * in2.m32 +
        m24 * in2.m42;
    temp12 = m21 * in2.m13 +
        m22 * in2.m23 +
        m23 * in2.m33 +
        m24 * in2.m43;
    temp13 = m21 * in2.m14 +
        m22 * in2.m24 +
        m23 * in2.m34 +
        m24 * in2.m44;

    temp20 = m31 * in2.m11 +
        m32 * in2.m21 +
        m33 * in2.m31 +
        m34 * in2.m41;
    temp21 = m31 * in2.m12 +
        m32 * in2.m22 +
        m33 * in2.m32 +
        m34 * in2.m42;
    temp22 = m31 * in2.m13 +
        m32 * in2.m23 +
        m33 * in2.m33 +
        m34 * in2.m43;
    temp23 = m31 * in2.m14 +
        m32 * in2.m24 +
        m33 * in2.m34 +
        m34 * in2.m44;

    temp30 = m41 * in2.m11 +
        m42 * in2.m21 +
        m43 * in2.m31 +
        m44 * in2.m41;
    temp31 = m41 * in2.m12 +
        m42 * in2.m22 +
        m43 * in2.m32 +
        m44 * in2.m42;
    temp32 = m41 * in2.m13 +
        m42 * in2.m23 +
        m43 * in2.m33 +
        m44 * in2.m43;
    temp33 = m41 * in2.m14 +
        m42 * in2.m24 +
        m43 * in2.m34 +
        m44 * in2.m44;
    
    store.m11 = temp00;  store.m12 = temp01;  store.m13 = temp02;  store.m14 = temp03;
    store.m21 = temp10;  store.m22 = temp11;  store.m23 = temp12;  store.m24 = temp13;
    store.m31 = temp20;  store.m32 = temp21;  store.m33 = temp22;  store.m34 = temp23;
    store.m41 = temp30;  store.m42 = temp31;  store.m43 = temp32;  store.m44 = temp33;
    
    return store;
  }

  /**
   * <code>mult</code> multiplies this matrix with another matrix. The
   * results are stored internally and a handle to this matrix will 
   * then be returned. This matrix will be on the left hand
   * side, while the parameter matrix will be on the right.
   * 
   * @param in2
   *            the matrix to multiply this matrix by.
   * @return the resultant matrix
   */
  public Matrix4f multLocal(Matrix4f in2) {
    return mult(in2, this);
  }

  /**
   * <code>mult</code> multiplies a vector about a rotation matrix. The
   * resulting vector is returned as a new Vector3f.
   * 
   * @param vec
   *            vec to multiply against.
   * @return the rotated vector.
   */
  public Vector3f mult(Vector3f vec) {
    return mult(vec, null);
  }

  /**
   * <code>mult</code> multiplies a vector about a rotation matrix and adds
   * translation. The resulting vector is returned.
   * 
   * @param vec
   *            vec to multiply against.
   * @param store
   *            a vector to store the result in. Created if null is passed.
   * @return the rotated vector.
   */
  public Vector3f mult(Vector3f vec, Vector3f store) {
    if (store == null) store = new Vector3f();
    
    float vx = vec.x, vy = vec.y, vz = vec.z;
    store.x = m11 * vx + m12 * vy + m13 * vz + m14;
    store.y = m21 * vx + m22 * vy + m23 * vz + m24;
    store.z = m31 * vx + m32 * vy + m33 * vz + m34;

    return store;
  }

  /**
   * <code>multNormal</code> multiplies a vector about a rotation matrix, but
   * does not add translation. The resulting vector is returned.
   *
   * @param vec
   *            vec to multiply against.
   * @param store
   *            a vector to store the result in. Created if null is passed.
   * @return the rotated vector.
   */
  public Vector3f multNormal(Vector3f vec, Vector3f store) {
    if (store == null) store = new Vector3f();

    float vx = vec.x, vy = vec.y, vz = vec.z;
    store.x = m11 * vx + m12 * vy + m13 * vz;
    store.y = m21 * vx + m22 * vy + m23 * vz;
    store.z = m31 * vx + m32 * vy + m33 * vz;

    return store;
  }

  /**
   * <code>multNormal</code> multiplies a vector about a rotation matrix, but
   * does not add translation. The resulting vector is returned.
   *
   * @param vec
   *            vec to multiply against.
   * @param store
   *            a vector to store the result in. Created if null is passed.
   * @return the rotated vector.
   */
  public Vector3f multNormalAcross(Vector3f vec, Vector3f store) {
    if (store == null) store = new Vector3f();

    float vx = vec.x, vy = vec.y, vz = vec.z;
    store.x = m11 * vx + m21 * vy + m31 * vz;
    store.y = m12 * vx + m22 * vy + m32 * vz;
    store.z = m13 * vx + m23 * vy + m33 * vz;

    return store;
  }

  /**
   * <code>mult</code> multiplies a vector about a rotation matrix and adds
   * translation. The w value is returned as a result of
   * multiplying the last column of the matrix by 1.0
   * 
   * @param vec
   *            vec to multiply against.
   * @param store
   *            a vector to store the result in. 
   * @return the W value
   */
  public float multProj(Vector3f vec, Vector3f store) {
    float vx = vec.x, vy = vec.y, vz = vec.z;
    store.x = m11 * vx + m12 * vy + m13 * vz + m14;
    store.y = m21 * vx + m22 * vy + m23 * vz + m24;
    store.z = m31 * vx + m32 * vy + m33 * vz + m34;
    return    m41 * vx + m42 * vy + m43 * vz + m44;
  }

  /**
   * <code>mult</code> multiplies a vector about a rotation matrix. The
   * resulting vector is returned.
   * 
   * @param vec
   *            vec to multiply against.
   * @param store
   *            a vector to store the result in.  created if null is passed.
   * @return the rotated vector.
   */
  public Vector3f multAcross(Vector3f vec, Vector3f store) {
    if (null == vec) {
      logger.info("Source vector is null, null result returned.");
      return null;
    }
    if (store == null) store = new Vector3f();
    
    float vx = vec.x, vy = vec.y, vz = vec.z;
    store.x = m11 * vx + m21 * vy + m31 * vz + m41 * 1;
    store.y = m12 * vx + m22 * vy + m32 * vz + m42 * 1;
    store.z = m13 * vx + m23 * vy + m33 * vz + m43 * 1;

    return store;
  }
  
  /**
   * <code>mult</code> multiplies an array of 4 floats against this rotation 
   * matrix. The results are stored directly in the array. (vec4f x mat4f)
   * 
   * @param vec4f
   *            float array (size 4) to multiply against the matrix.
   * @return the vec4f for chaining.
   */
  public float[] mult(float[] vec4f) {
    if (null == vec4f || vec4f.length != 4) {
      logger.warning("invalid array given, must be nonnull and length 4");
      return null;
    }

    float x = vec4f[0], y = vec4f[1], z = vec4f[2], w = vec4f[3];
    
    vec4f[0] = m11 * x + m12 * y + m13 * z + m14 * w;
    vec4f[1] = m21 * x + m22 * y + m23 * z + m24 * w;
    vec4f[2] = m31 * x + m32 * y + m33 * z + m34 * w;
    vec4f[3] = m41 * x + m42 * y + m43 * z + m44 * w;

    return vec4f;
  }

  /**
   * <code>mult</code> multiplies an array of 4 floats against this rotation 
   * matrix. The results are stored directly in the array. (vec4f x mat4f)
   * 
   * @param vec4f
   *            float array (size 4) to multiply against the matrix.
   * @return the vec4f for chaining.
   */
  public float[] multAcross(float[] vec4f) {
    if (null == vec4f || vec4f.length != 4) {
      logger.warning("invalid array given, must be nonnull and length 4");
      return null;
    }

    float x = vec4f[0], y = vec4f[1], z = vec4f[2], w = vec4f[3];
    
    vec4f[0] = m11 * x + m21 * y + m31 * z + m41 * w;
    vec4f[1] = m12 * x + m22 * y + m32 * z + m42 * w;
    vec4f[2] = m13 * x + m23 * y + m33 * z + m43 * w;
    vec4f[3] = m14 * x + m24 * y + m34 * z + m44 * w;

    return vec4f;
  }

  /**
   * Inverts this matrix as a new Matrix4f.
   * 
   * @return The new inverse matrix
   */
  public Matrix4f invert() {
    return invert(null);
  }

  /**
   * Inverts this matrix and stores it in the given store.
   * 
   * @return The store
   */
  public Matrix4f invert(Matrix4f store) {
    if (store == null) store = new Matrix4f();

    float fA0 = m11 * m22 - m12 * m21;
    float fA1 = m11 * m23 - m13 * m21;
    float fA2 = m11 * m24 - m14 * m21;
    float fA3 = m12 * m23 - m13 * m22;
    float fA4 = m12 * m24 - m14 * m22;
    float fA5 = m13 * m24 - m14 * m23;
    float fB0 = m31 * m42 - m32 * m41;
    float fB1 = m31 * m43 - m33 * m41;
    float fB2 = m31 * m44 - m34 * m41;
    float fB3 = m32 * m43 - m33 * m42;
    float fB4 = m32 * m44 - m34 * m42;
    float fB5 = m33 * m44 - m34 * m43;
    float fDet = fA0*fB5-fA1*fB4+fA2*fB3+fA3*fB2-fA4*fB1+fA5*fB0;

    if ( FastMath.abs(fDet) <= 0f )
      throw new ArithmeticException("This matrix cannot be inverted");

    store.m11 = +m22 *fB5 - m23 *fB4 + m24 *fB3;
    store.m21 = -m21 *fB5 + m23 *fB2 - m24 *fB1;
    store.m31 = +m21 *fB4 - m22 *fB2 + m24 *fB0;
    store.m41 = -m21 *fB3 + m22 *fB1 - m23 *fB0;
    store.m12 = -m12 *fB5 + m13 *fB4 - m14 *fB3;
    store.m22 = +m11 *fB5 - m13 *fB2 + m14 *fB1;
    store.m32 = -m11 *fB4 + m12 *fB2 - m14 *fB0;
    store.m42 = +m11 *fB3 - m12 *fB1 + m13 *fB0;
    store.m13 = +m42 *fA5 - m43 *fA4 + m44 *fA3;
    store.m23 = -m41 *fA5 + m43 *fA2 - m44 *fA1;
    store.m33 = +m41 *fA4 - m42 *fA2 + m44 *fA0;
    store.m43 = -m41 *fA3 + m42 *fA1 - m43 *fA0;
    store.m14 = -m32 *fA5 + m33 *fA4 - m34 *fA3;
    store.m24 = +m31 *fA5 - m33 *fA2 + m34 *fA1;
    store.m34 = -m31 *fA4 + m32 *fA2 - m34 *fA0;
    store.m44 = +m31 *fA3 - m32 *fA1 + m33 *fA0;

    float fInvDet = 1.0f/fDet;
    store.multLocal(fInvDet);

    return store;
  }

  /**
   * Inverts this matrix locally.
   * 
   * @return this
   */
  public Matrix4f invertLocal() {

    float fA0 = m11 * m22 - m12 * m21;
    float fA1 = m11 * m23 - m13 * m21;
    float fA2 = m11 * m24 - m14 * m21;
    float fA3 = m12 * m23 - m13 * m22;
    float fA4 = m12 * m24 - m14 * m22;
    float fA5 = m13 * m24 - m14 * m23;
    float fB0 = m31 * m42 - m32 * m41;
    float fB1 = m31 * m43 - m33 * m41;
    float fB2 = m31 * m44 - m34 * m41;
    float fB3 = m32 * m43 - m33 * m42;
    float fB4 = m32 * m44 - m34 * m42;
    float fB5 = m33 * m44 - m34 * m43;
    float fDet = fA0*fB5-fA1*fB4+fA2*fB3+fA3*fB2-fA4*fB1+fA5*fB0;

    if ( FastMath.abs(fDet) <= 0f )
      return zero();

    float f00 = +m22 *fB5 - m23 *fB4 + m24 *fB3;
    float f10 = -m21 *fB5 + m23 *fB2 - m24 *fB1;
    float f20 = +m21 *fB4 - m22 *fB2 + m24 *fB0;
    float f30 = -m21 *fB3 + m22 *fB1 - m23 *fB0;
    float f01 = -m12 *fB5 + m13 *fB4 - m14 *fB3;
    float f11 = +m11 *fB5 - m13 *fB2 + m14 *fB1;
    float f21 = -m11 *fB4 + m12 *fB2 - m14 *fB0;
    float f31 = +m11 *fB3 - m12 *fB1 + m13 *fB0;
    float f02 = +m42 *fA5 - m43 *fA4 + m44 *fA3;
    float f12 = -m41 *fA5 + m43 *fA2 - m44 *fA1;
    float f22 = +m41 *fA4 - m42 *fA2 + m44 *fA0;
    float f32 = -m41 *fA3 + m42 *fA1 - m43 *fA0;
    float f03 = -m32 *fA5 + m33 *fA4 - m34 *fA3;
    float f13 = +m31 *fA5 - m33 *fA2 + m34 *fA1;
    float f23 = -m31 *fA4 + m32 *fA2 - m34 *fA0;
    float f33 = +m31 *fA3 - m32 *fA1 + m33 *fA0;
    
    m11 = f00;
    m12 = f01;
    m13 = f02;
    m14 = f03;
    m21 = f10;
    m22 = f11;
    m23 = f12;
    m24 = f13;
    m31 = f20;
    m32 = f21;
    m33 = f22;
    m34 = f23;
    m41 = f30;
    m42 = f31;
    m43 = f32;
    m44 = f33;

    float fInvDet = 1.0f/fDet;
    multLocal(fInvDet);

    return this;
  }
  
  /**
   * Returns a new matrix representing the adjoint of this matrix.
   * 
   * @return The adjoint matrix
   */
  public Matrix4f adjoint() {
    return adjoint(null);
  }
     
  
  /**
   * Places the adjoint of this matrix in store (creates store if null.)
   * 
   * @param store
   *            The matrix to store the result in.  If null, a new matrix is created.
   * @return store
   */
  public Matrix4f adjoint(Matrix4f store) {
    if (store == null) store = new Matrix4f();

    float fA0 = m11 * m22 - m12 * m21;
    float fA1 = m11 * m23 - m13 * m21;
    float fA2 = m11 * m24 - m14 * m21;
    float fA3 = m12 * m23 - m13 * m22;
    float fA4 = m12 * m24 - m14 * m22;
    float fA5 = m13 * m24 - m14 * m23;
    float fB0 = m31 * m42 - m32 * m41;
    float fB1 = m31 * m43 - m33 * m41;
    float fB2 = m31 * m44 - m34 * m41;
    float fB3 = m32 * m43 - m33 * m42;
    float fB4 = m32 * m44 - m34 * m42;
    float fB5 = m33 * m44 - m34 * m43;

    store.m11 = +m22 *fB5 - m23 *fB4 + m24 *fB3;
    store.m21 = -m21 *fB5 + m23 *fB2 - m24 *fB1;
    store.m31 = +m21 *fB4 - m22 *fB2 + m24 *fB0;
    store.m41 = -m21 *fB3 + m22 *fB1 - m23 *fB0;
    store.m12 = -m12 *fB5 + m13 *fB4 - m14 *fB3;
    store.m22 = +m11 *fB5 - m13 *fB2 + m14 *fB1;
    store.m32 = -m11 *fB4 + m12 *fB2 - m14 *fB0;
    store.m42 = +m11 *fB3 - m12 *fB1 + m13 *fB0;
    store.m13 = +m42 *fA5 - m43 *fA4 + m44 *fA3;
    store.m23 = -m41 *fA5 + m43 *fA2 - m44 *fA1;
    store.m33 = +m41 *fA4 - m42 *fA2 + m44 *fA0;
    store.m43 = -m41 *fA3 + m42 *fA1 - m43 *fA0;
    store.m14 = -m32 *fA5 + m33 *fA4 - m34 *fA3;
    store.m24 = +m31 *fA5 - m33 *fA2 + m34 *fA1;
    store.m34 = -m31 *fA4 + m32 *fA2 - m34 *fA0;
    store.m44 = +m31 *fA3 - m32 *fA1 + m33 *fA0;

    return store;
  }

  /**
   * <code>determinant</code> generates the determinate of this matrix.
   * 
   * @return the determinate
   */
  public float determinant() {
    float fA0 = m11 * m22 - m12 * m21;
    float fA1 = m11 * m23 - m13 * m21;
    float fA2 = m11 * m24 - m14 * m21;
    float fA3 = m12 * m23 - m13 * m22;
    float fA4 = m12 * m24 - m14 * m22;
    float fA5 = m13 * m24 - m14 * m23;
    float fB0 = m31 * m42 - m32 * m41;
    float fB1 = m31 * m43 - m33 * m41;
    float fB2 = m31 * m44 - m34 * m41;
    float fB3 = m32 * m43 - m33 * m42;
    float fB4 = m32 * m44 - m34 * m42;
    float fB5 = m33 * m44 - m34 * m43;
    float fDet = fA0*fB5-fA1*fB4+fA2*fB3+fA3*fB2-fA4*fB1+fA5*fB0;
    return fDet;
  }

  /**
   * Sets all of the values in this matrix to zero.
   * 
   * @return this matrix
   */
  public Matrix4f zero() {
    m11 = m12 = m13 = m14 = 0.0f;
    m21 = m22 = m23 = m24 = 0.0f;
    m31 = m32 = m33 = m34 = 0.0f;
    m41 = m42 = m43 = m44 = 0.0f;
    return this;
  }
  
  public Matrix4f add(Matrix4f mat) {
  	Matrix4f result = new Matrix4f();
  	result.m11 = this.m11 + mat.m11;
  	result.m12 = this.m12 + mat.m12;
  	result.m13 = this.m13 + mat.m13;
  	result.m14 = this.m14 + mat.m14;
  	result.m21 = this.m21 + mat.m21;
  	result.m22 = this.m22 + mat.m22;
  	result.m23 = this.m23 + mat.m23;
  	result.m24 = this.m24 + mat.m24;
  	result.m31 = this.m31 + mat.m31;
  	result.m32 = this.m32 + mat.m32;
  	result.m33 = this.m33 + mat.m33;
  	result.m34 = this.m34 + mat.m34;
  	result.m41 = this.m41 + mat.m41;
  	result.m42 = this.m42 + mat.m42;
  	result.m43 = this.m43 + mat.m43;
  	result.m44 = this.m44 + mat.m44;
  	return result;
  }

  /**
   * <code>add</code> adds the values of a parameter matrix to this matrix.
   * 
   * @param mat
   *            the matrix to add to this.
   */
  public void addLocal(Matrix4f mat) {
    m11 += mat.m11;
    m12 += mat.m12;
    m13 += mat.m13;
    m14 += mat.m14;
    m21 += mat.m21;
    m22 += mat.m22;
    m23 += mat.m23;
    m24 += mat.m24;
    m31 += mat.m31;
    m32 += mat.m32;
    m33 += mat.m33;
    m34 += mat.m34;
    m41 += mat.m41;
    m42 += mat.m42;
    m43 += mat.m43;
    m44 += mat.m44;
  }
  
  public Vector3f toTranslationVector() {
    return new Vector3f(m14, m24, m34);
  }
  
  public void toTranslationVector(Vector3f vector) {
    vector.set(m14, m24, m34);
  }
  
  public Matrix3f toRotationMatrix() {
    return new Matrix3f(m11, m12, m13, m21, m22, m23, m31, m32, m33);
    
  }
  
  public void toRotationMatrix(Matrix3f mat) {
    mat.m00 = m11;
    mat.m01 = m12;
    mat.m02 = m13;
    mat.m10 = m21;
    mat.m11 = m22;
    mat.m12 = m23;
    mat.m20 = m31;
    mat.m21 = m32;
    mat.m22 = m33;
    
  }

  public void setRotationMatrix(Matrix3f mat) {
    this.m11 = mat.m00;
    this.m12 = mat.m01;
    this.m13 = mat.m02;
    this.m21 = mat.m10;
    this.m22 = mat.m11;
    this.m23 = mat.m12;
    this.m31 = mat.m20;
    this.m32 = mat.m21;
    this.m33 = mat.m22;
    
  }
  
  public void setScale(float x, float y, float z){
    m11 *= x;
    m22 *= y;
    m33 *= z;
  }

  public void setScale(Vector3f scale){
    m11 *= scale.x;
    m22 *= scale.y;
    m33 *= scale.z;
  }

  /**
   * <code>setTranslation</code> will set the matrix's translation values.
   * 
   * @param translation
   *            the new values for the translation.
   * @throws JmeException
   *             if translation is not size 3.
   */
  public void setTranslation(float[] translation) {
    if (translation.length != 3) { throw new IllegalArgumentException(
        "Translation size must be 3."); }
    m14 = translation[0];
    m24 = translation[1];
    m34 = translation[2];
  }

  /**
   * <code>setTranslation</code> will set the matrix's translation values.
   * 
   * @param x
   *            value of the translation on the x axis
   * @param y
   *            value of the translation on the y axis
   * @param z
   *            value of the translation on the z axis
   */
  public void setTranslation(float x, float y, float z) {
    m14 = x;
    m24 = y;
    m34 = z;
  }

  /**
   * <code>setTranslation</code> will set the matrix's translation values.
   *
   * @param translation
   *            the new values for the translation.
   */
  public void setTranslation(Vector3f translation) {
    m14 = translation.x;
    m24 = translation.y;
    m34 = translation.z;
  }

  /**
   * <code>setInverseTranslation</code> will set the matrix's inverse
   * translation values.
   * 
   * @param translation
   *            the new values for the inverse translation.
   * @throws JmeException
   *             if translation is not size 3.
   */
  public void setInverseTranslation(float[] translation) {
    if (translation.length != 3) { throw new IllegalArgumentException(
        "Translation size must be 3."); }
    m14 = -translation[0];
    m24 = -translation[1];
    m34 = -translation[2];
  }

  /**
   * <code>angleRotation</code> sets this matrix to that of a rotation about
   * three axes (x, y, z). Where each axis has a specified rotation in
   * degrees. These rotations are expressed in a single <code>Vector3f</code>
   * object.
   * 
   * @param angles
   *            the angles to rotate.
   */
  public void angleRotation(Vector3f angles) {
    float angle;
    float sr, sp, sy, cr, cp, cy;

    angle = (angles.z * FastMath.DEG_TO_RAD);
    sy = FastMath.sin(angle);
    cy = FastMath.cos(angle);
    angle = (angles.y * FastMath.DEG_TO_RAD);
    sp = FastMath.sin(angle);
    cp = FastMath.cos(angle);
    angle = (angles.x * FastMath.DEG_TO_RAD);
    sr = FastMath.sin(angle);
    cr = FastMath.cos(angle);

    // matrix = (Z * Y) * X
    m11 = cp * cy;
    m21 = cp * sy;
    m31 = -sp;
    m12 = sr * sp * cy + cr * -sy;
    m22 = sr * sp * sy + cr * cy;
    m32 = sr * cp;
    m13 = (cr * sp * cy + -sr * -sy);
    m23 = (cr * sp * sy + -sr * cy);
    m33 = cr * cp;
    m14 = 0.0f;
    m24 = 0.0f;
    m34 = 0.0f;
  }

  /**
   * <code>setInverseRotationRadians</code> builds an inverted rotation from
   * Euler angles that are in radians.
   * 
   * @param angles
   *            the Euler angles in radians.
   * @throws JmeException
   *             if angles is not size 3.
   */
  public void setInverseRotationRadians(float[] angles) {
    if (angles.length != 3) { throw new IllegalArgumentException(
        "Angles must be of size 3."); }
    double cr = FastMath.cos(angles[0]);
    double sr = FastMath.sin(angles[0]);
    double cp = FastMath.cos(angles[1]);
    double sp = FastMath.sin(angles[1]);
    double cy = FastMath.cos(angles[2]);
    double sy = FastMath.sin(angles[2]);

    m11 = (float) (cp * cy);
    m21 = (float) (cp * sy);
    m31 = (float) (-sp);

    double srsp = sr * sp;
    double crsp = cr * sp;

    m12 = (float) (srsp * cy - cr * sy);
    m22 = (float) (srsp * sy + cr * cy);
    m32 = (float) (sr * cp);

    m13 = (float) (crsp * cy + sr * sy);
    m23 = (float) (crsp * sy - sr * cy);
    m33 = (float) (cr * cp);
  }

  /**
   * <code>setInverseRotationDegrees</code> builds an inverted rotation from
   * Euler angles that are in degrees.
   * 
   * @param angles
   *            the Euler angles in degrees.
   * @throws JmeException
   *             if angles is not size 3.
   */
  public void setInverseRotationDegrees(float[] angles) {
    if (angles.length != 3) { throw new IllegalArgumentException(
        "Angles must be of size 3."); }
    float vec[] = new float[3];
    vec[0] = (angles[0] * FastMath.RAD_TO_DEG);
    vec[1] = (angles[1] * FastMath.RAD_TO_DEG);
    vec[2] = (angles[2] * FastMath.RAD_TO_DEG);
    setInverseRotationRadians(vec);
  }

  /**
   * 
   * <code>inverseTranslateVect</code> translates a given Vector3f by the
   * translation part of this matrix.
   * 
   * @param vec
   *            the Vector3f data to be translated.
   * @throws JmeException
   *             if the size of the Vector3f is not 3.
   */
  public void inverseTranslateVect(float[] vec) {
    if (vec.length != 3) { throw new IllegalArgumentException(
        "vec must be of size 3."); }

    vec[0] = vec[0] - m14;
    vec[1] = vec[1] - m24;
    vec[2] = vec[2] - m34;
  }

  /**
   * 
   * <code>inverseTranslateVect</code> translates a given Vector3f by the
   * translation part of this matrix.
   * 
   * @param data
   *            the Vector3f to be translated.
   * @throws JmeException
   *             if the size of the Vector3f is not 3.
   */
  public void inverseTranslateVect(Vector3f data) {
    data.x -= m14;
    data.y -= m24;
    data.z -= m34;
  }

  /**
   * 
   * <code>inverseTranslateVect</code> translates a given Vector3f by the
   * translation part of this matrix.
   * 
   * @param data
   *            the Vector3f to be translated.
   * @throws JmeException
   *             if the size of the Vector3f is not 3.
   */
  public void translateVect(Vector3f data) {
    data.x += m14;
    data.y += m24;
    data.z += m34;
  }

  /**
   * 
   * <code>inverseRotateVect</code> rotates a given Vector3f by the rotation
   * part of this matrix.
   * 
   * @param vec
   *            the Vector3f to be rotated.
   */
  public void inverseRotateVect(Vector3f vec) {
    float vx = vec.x, vy = vec.y, vz = vec.z;

    vec.x = vx * m11 + vy * m21 + vz * m31;
    vec.y = vx * m12 + vy * m22 + vz * m32;
    vec.z = vx * m13 + vy * m23 + vz * m33;
  }
  
  public void rotateVect(Vector3f vec) {
    float vx = vec.x, vy = vec.y, vz = vec.z;

    vec.x = vx * m11 + vy * m12 + vz * m13;
    vec.y = vx * m21 + vy * m22 + vz * m23;
    vec.z = vx * m31 + vy * m32 + vz * m33;
  }

  /**
   * <code>toString</code> returns the string representation of this object.
   * It is in a format of a 4x4 matrix. For example, an identity matrix would
   * be represented by the following string. com.jme.math.Matrix3f <br>[<br>
   * 1.0  0.0  0.0  0.0 <br>
   * 0.0  1.0  0.0  0.0 <br>
   * 0.0  0.0  1.0  0.0 <br>
   * 0.0  0.0  0.0  1.0 <br>]<br>
   * 
   * @return the string representation of this object.
   */
  @Override
  public String toString() {
    StringBuilder result = new StringBuilder("Matrix4f\n[\n");
    result.append(" ");
    result.append(m11);
    result.append("  ");
    result.append(m12);
    result.append("  ");
    result.append(m13);
    result.append("  ");
    result.append(m14);
    result.append(" \n");
    result.append(" ");
    result.append(m21);
    result.append("  ");
    result.append(m22);
    result.append("  ");
    result.append(m23);
    result.append("  ");
    result.append(m24);
    result.append(" \n");
    result.append(" ");
    result.append(m31);
    result.append("  ");
    result.append(m32);
    result.append("  ");
    result.append(m33);
    result.append("  ");
    result.append(m34);
    result.append(" \n");
    result.append(" ");
    result.append(m41);
    result.append("  ");
    result.append(m42);
    result.append("  ");
    result.append(m43);
    result.append("  ");
    result.append(m44);
    result.append(" \n]");
    return result.toString();
  }

  /**
   * 
   * <code>hashCode</code> returns the hash code value as an integer and is
   * supported for the benefit of hashing based collection classes such as
   * Hashtable, HashMap, HashSet etc.
   * 
   * @return the hashcode for this instance of Matrix4f.
   * @see Object#hashCode()
   */
  @Override
  public int hashCode() {
    int hash = 37;
    hash = 37 * hash + Float.floatToIntBits(m11);
    hash = 37 * hash + Float.floatToIntBits(m12);
    hash = 37 * hash + Float.floatToIntBits(m13);
    hash = 37 * hash + Float.floatToIntBits(m14);

    hash = 37 * hash + Float.floatToIntBits(m21);
    hash = 37 * hash + Float.floatToIntBits(m22);
    hash = 37 * hash + Float.floatToIntBits(m23);
    hash = 37 * hash + Float.floatToIntBits(m24);

    hash = 37 * hash + Float.floatToIntBits(m31);
    hash = 37 * hash + Float.floatToIntBits(m32);
    hash = 37 * hash + Float.floatToIntBits(m33);
    hash = 37 * hash + Float.floatToIntBits(m34);

    hash = 37 * hash + Float.floatToIntBits(m41);
    hash = 37 * hash + Float.floatToIntBits(m42);
    hash = 37 * hash + Float.floatToIntBits(m43);
    hash = 37 * hash + Float.floatToIntBits(m44);

    return hash;
  }
  
  /**
   * are these two matrices the same? they are is they both have the same mXX values.
   *
   * @param o
   *            the object to compare for equality
   * @return true if they are equal
   */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Matrix4f) || o == null) {
      return false;
    }

    if (this == o) {
      return true;
    }

    Matrix4f comp = (Matrix4f) o;
    if (Float.compare(m11,comp.m11) != 0) return false;
    if (Float.compare(m12,comp.m12) != 0) return false;
    if (Float.compare(m13,comp.m13) != 0) return false;
    if (Float.compare(m14,comp.m14) != 0) return false;

    if (Float.compare(m21,comp.m21) != 0) return false;
    if (Float.compare(m22,comp.m22) != 0) return false;
    if (Float.compare(m23,comp.m23) != 0) return false;
    if (Float.compare(m24,comp.m24) != 0) return false;

    if (Float.compare(m31,comp.m31) != 0) return false;
    if (Float.compare(m32,comp.m32) != 0) return false;
    if (Float.compare(m33,comp.m33) != 0) return false;
    if (Float.compare(m34,comp.m34) != 0) return false;

    if (Float.compare(m41,comp.m41) != 0) return false;
    if (Float.compare(m42,comp.m42) != 0) return false;
    if (Float.compare(m43,comp.m43) != 0) return false;
    if (Float.compare(m44,comp.m44) != 0) return false;

    return true;
  }
  
  public Class<? extends Matrix4f> getClassTag() {
    return this.getClass();
  }

  /**
   * @return true if this matrix is identity
   */
  public boolean isIdentity() {
    return 
    (m11 == 1 && m12 == 0 && m13 == 0 && m14 == 0) &&
    (m21 == 0 && m22 == 1 && m23 == 0 && m24 == 0) &&
    (m31 == 0 && m32 == 0 && m33 == 1 && m34 == 0) &&
    (m41 == 0 && m42 == 0 && m43 == 0 && m44 == 1);
  }

  /**
   * Apply a scale to this matrix.
   * 
   * @param scale
   *            the scale to apply
   */
  public void scale(Vector3f scale) {
    m11 *= scale.getX();
    m21 *= scale.getX();
    m31 *= scale.getX();
    m41 *= scale.getX();
    m12 *= scale.getY();
    m22 *= scale.getY();
    m32 *= scale.getY();
    m42 *= scale.getY();
    m13 *= scale.getZ();
    m23 *= scale.getZ();
    m33 *= scale.getZ();
    m43 *= scale.getZ();
  }

  /**
   * Apply a scale to this matrix.
   * 
   * @param scale
   *            the scale to apply
   */
  public void scale(float scale) {
    m11 *= scale;
    m21 *= scale;
    m31 *= scale;
    m41 *= scale;
    m12 *= scale;
    m22 *= scale;
    m32 *= scale;
    m42 *= scale;
    m13 *= scale;
    m23 *= scale;
    m33 *= scale;
    m43 *= scale;
  }

  static boolean equalIdentity(Matrix4f mat) {
		if (Math.abs(mat.m11 - 1) > 1e-4) return false;
		if (Math.abs(mat.m22 - 1) > 1e-4) return false;
		if (Math.abs(mat.m33 - 1) > 1e-4) return false;
		if (Math.abs(mat.m44 - 1) > 1e-4) return false;

		if (Math.abs(mat.m12) > 1e-4) return false;
		if (Math.abs(mat.m13) > 1e-4) return false;
		if (Math.abs(mat.m14) > 1e-4) return false;

		if (Math.abs(mat.m21) > 1e-4) return false;
		if (Math.abs(mat.m23) > 1e-4) return false;
		if (Math.abs(mat.m24) > 1e-4) return false;

		if (Math.abs(mat.m31) > 1e-4) return false;
		if (Math.abs(mat.m32) > 1e-4) return false;
		if (Math.abs(mat.m34) > 1e-4) return false;

		if (Math.abs(mat.m41) > 1e-4) return false;
		if (Math.abs(mat.m42) > 1e-4) return false;
		if (Math.abs(mat.m43) > 1e-4) return false;

		return true;
  }
  
  @Override
  public Matrix4f clone() {
    try {
      return (Matrix4f) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new AssertionError(); // can not happen
    }
  }

  public Matrix4f createScale(Vector3 scales) {
    Matrix4f result = new Matrix4f();
    result.m11 = scales.x;
    result.m12 = 0.0f;
    result.m13 = 0.0f;
    result.m14 = 0.0f;
    result.m21 = 0.0f;
    result.m22 = scales.y;
    result.m23 = 0.0f;
    result.m24 = 0.0f;
    result.m31 = 0.0f;
    result.m32 = 0.0f;
    result.m33 = scales.z;
    result.m34 = 0.0f;
    result.m41 = 0.0f;
    result.m42 = 0.0f;
    result.m43 = 0.0f;
    result.m44 = 1f;

    return result;
  }

  public static Matrix4f createFromQuaternion(Quaternion quaternion) {
    Matrix4f result = new Matrix4f();
    float num1 = quaternion.x * quaternion.x;
    float num2 = quaternion.y * quaternion.y;
    float num3 = quaternion.z * quaternion.z;
    float num4 = quaternion.x * quaternion.y;
    float num5 = quaternion.z * quaternion.w;
    float num6 = quaternion.z * quaternion.x;
    float num7 = quaternion.y * quaternion.w;
    float num8 = quaternion.y * quaternion.z;
    float num9 = quaternion.x * quaternion.w;
    result.m11 = (float) (1.0 - 2.0 * ((double) num2 + (double) num3));
    result.m12 = (float) (2.0 * ((double) num4 + (double) num5));
    result.m13 = (float) (2.0 * ((double) num6 - (double) num7));
    result.m14 = 0.0f;
    result.m21 = (float) (2.0 * ((double) num4 - (double) num5));
    result.m22 = (float) (1.0 - 2.0 * ((double) num3 + (double) num1));
    result.m23 = (float) (2.0 * ((double) num8 + (double) num9));
    result.m24 = 0.0f;
    result.m31 = (float) (2.0 * ((double) num6 + (double) num7));
    result.m32 = (float) (2.0 * ((double) num8 - (double) num9));
    result.m33 = (float) (1.0 - 2.0 * ((double) num2 + (double) num1));
    result.m34 = 0.0f;
    result.m41 = 0.0f;
    result.m42 = 0.0f;
    result.m43 = 0.0f;
    result.m44 = 1f;

    return result;
  }

  public static Matrix4f createTranslation(Vector3 vector) {
    Matrix4f result = new Matrix4f();
    result.m11 = 1f;
    result.m12 = 0.0f;
    result.m13 = 0.0f;
    result.m14 = 0.0f;
    result.m21 = 0.0f;
    result.m22 = 1f;
    result.m23 = 0.0f;
    result.m24 = 0.0f;
    result.m31 = 0.0f;
    result.m32 = 0.0f;
    result.m33 = 1f;
    result.m34 = 0.0f;
    result.m41 = vector.x;
    result.m42 = vector.y;
    result.m43 = vector.z;
    result.m44 = 1f;

    return result;
  }

  public static Matrix4f createFromYawPitchRoll(float yaw, float pitch, float roll) {
    Quaternion result = Quaternion.createFromYawPitchRoll(yaw, pitch, roll);
    return createFromQuaternion(result);
  }

  public static Matrix4f createRotationMatrix(float rotX, float rotY, float rotZ) {
    Matrix4f rotaX = createRotationAroundX(rotX);
    Matrix4f rotaY = createRotationAroundY(rotY);
    Matrix4f rotaZ = createRotationAroundZ(rotZ);
    return rotaX.mult(rotaY).mult(rotaZ);
  }

  private static Matrix4f createRotationAroundZ(float angle) {
    Matrix4f matrix = new Matrix4f();
    double radians = angle * ((float) Math.PI / 180f);
    float num1 = (float) Math.cos(radians);
    float num2 = (float) Math.sin(radians);
    matrix.m11 = num1;
    matrix.m12 = num2;
    matrix.m21 = -num2;
    matrix.m22 = num1;
    return matrix;
  }

  private static Matrix4f createRotationAroundY(float angle) {
    Matrix4f matrix = new Matrix4f();
    double radians = angle * ((float) Math.PI / 180f);
    float num1 = (float) Math.cos(radians);
    float num2 = (float) Math.sin(radians);
    matrix.m11 = num1;
    matrix.m13 = -num2;
    matrix.m31 = num2;
    matrix.m33 = num1;
    return matrix;
  }

  private static Matrix4f createRotationAroundX(float angle) {
    Matrix4f matrix = new Matrix4f();
    double radians = angle * ((float) Math.PI / 180f);
    float num1 = (float) Math.cos(radians);
    float num2 = (float) Math.sin(radians);
    matrix.m22 = num1;
    matrix.m23 = num2;
    matrix.m32 = -num2;
    matrix.m33 = num1;
    return matrix;
  }

}

