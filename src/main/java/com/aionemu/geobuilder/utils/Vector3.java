package com.aionemu.geobuilder.utils;

import com.aionemu.geobuilder.math.Matrix4f;

public class Vector3 {

  public float x, y, z;

  public Vector3(float x, float y, float z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public Vector3() {}

  @Override
  public String toString() {
    return "{X:" + x + ", Y:" + y + ", Z:" + z + "}";
  }


  public static Vector3 transform(Vector3 position, Matrix4f matrix)
  {
    Vector3 result = new Vector3();
    float num = (((position.x * matrix.m11) + (position.y * matrix.m21)) + (position.z * matrix.m31)) + matrix.m41;
    float num2 = (((position.x * matrix.m12) + (position.y * matrix.m22)) + (position.z * matrix.m32)) + matrix.m42;
    float num3 = (((position.x * matrix.m13) + (position.y * matrix.m23)) + (position.z * matrix.m33)) + matrix.m43;
    result.x = num;
    result.y = num2;
    result.z = num3;
    return result;
  }
}
