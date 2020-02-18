package com.aionemu.geobuilder.entries;

import com.aionemu.geobuilder.utils.Vector3;
import com.aionemu.geobuilder.math.Matrix4f;

public class EntityEntry {

  public EntityClass entityClass;
  public int entityId, level, startLevel, townId;
  public String name;
  public Vector3 angle, pos, scale;
  public String mesh;
  public EntryType type = EntryType.NONE;

  public Matrix4f getMatrix() {
    float pitch = angle.x * ((float) Math.PI / 180f);
    float yaw = angle.y * ((float) Math.PI / 180f);
    float roll = angle.z * ((float) Math.PI / 180f);
    Matrix4f pitchMatrix = new Matrix4f(1f, 0f, 0f, 0f,
        0f, (float)Math.cos(pitch), (float) Math.sin(pitch), 0f,
        0f, (float)Math.sin(pitch) * -1, (float) Math.cos(pitch), 0f,
        0f, 0f, 0f, 1f);

    Matrix4f yawMatrix = new Matrix4f((float) Math.cos(yaw), 0f, (float) Math.sin(yaw) * - 1, 0f,
        0f, 1f, 0f, 0f,
        (float) Math.sin(yaw), 0f, (float) Math.cos(yaw), 0f,
        0f, 0f, 0f, 1f);

    Matrix4f rollMatrix = new Matrix4f((float) Math.cos(roll), (float) Math.sin(roll), 0f, 0f,
        (float) Math.sin(roll) * -1, (float) Math.cos(roll), 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f);

    return yawMatrix.mult(pitchMatrix).mult(rollMatrix);
    //return Matrix4f.createFromYawPitchRoll(yaw, pitch, roll);
  }
}
