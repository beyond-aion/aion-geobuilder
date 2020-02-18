package com.aionemu.geobuilder.meshData;

import com.aionemu.geobuilder.utils.Vector3;

import java.util.List;

public class MeshData {

  public List<Vector3> vertices;
  public List<MeshFace> indices;

  public int materialId= 0;
  public int collisionIntention = 1;
}
