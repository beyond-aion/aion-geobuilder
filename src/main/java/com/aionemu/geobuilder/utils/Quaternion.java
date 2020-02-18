package com.aionemu.geobuilder.utils;

public class Quaternion {
  public float x, y, z, w;

  public Quaternion(float x, float y, float z, float w) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.w = w;
  }

  public static Quaternion createFromYawPitchRoll(float yaw, float pitch, float roll) {
    float num1 = roll * 0.5f;
    float num2 = pitch * 0.5f;
    float num3 = yaw * 0.5f;
    float num4 = (float) Math.sin(num1);
    float num5 = (float) Math.cos(num1);
    float num6 = (float) Math.sin(num2);
    float num7 = (float) Math.cos(num2);
    float num8 = (float) Math.sin(num3);
    float num9 = (float) Math.cos(num3);
    float x = (float) ((double) num9 * (double) num6 * (double) num5 + (double) num8 * (double) num7 * (double) num4);
    float y = (float) ((double) num8 * (double) num7 * (double) num5 - (double) num9 * (double) num6 * (double) num4);
    float z = (float) ((double) num9 * (double) num7 * (double) num4 - (double) num8 * (double) num6 * (double) num5);
    float w = (float) ((double) num9 * (double) num7 * (double) num5 + (double) num8 * (double) num6 * (double) num4);
    return new Quaternion(x, y, z, w);
  }

  public static Quaternion createFromAxisAngle(Vector3 axis, float angle) {
    float num1 = angle * 0.5f;
    float num2 = (float) Math.sin(num1);
    float w = (float) Math.cos(num1);
    return new Quaternion(axis.x * num2, axis.y * num2, axis.z * num2, w );
  }

  public static Quaternion mult(Quaternion quaternion1, Quaternion quaternion2) {
    float x1 = quaternion1.x;
    float y1 = quaternion1.y;
    float z1 = quaternion1.z;
    float w1 = quaternion1.w;
    float x2 = quaternion2.x;
    float y2 = quaternion2.y;
    float z2 = quaternion2.z;
    float w2 = quaternion2.w;
    float num1 = (float) ((double) y1 * (double) z2 - (double) z1 * (double) y2);
    float num2 = (float) ((double) z1 * (double) x2 - (double) x1 * (double) z2);
    float num3 = (float) ((double) x1 * (double) y2 - (double) y1 * (double) x2);
    float num4 = (float) ((double) x1 * (double) x2 + (double) y1 * (double) y2 + (double) z1 * (double) z2);
    float x = (float) ((double) x1 * (double) w2 + (double) x2 * (double) w1) + num1;
    float y = (float) ((double) y1 * (double) w2 + (double) y2 * (double) w1) + num2;
    float z = (float) ((double) z1 * (double) w2 + (double) z2 * (double) w1) + num3;
    float w = w1 * w2 - num4;
    return new Quaternion(x, y, z, w);
  }
}
