package com.aionemu.geobuilder.meshData;

import com.aionemu.geobuilder.utils.Vector3;

import java.util.List;
import java.util.Objects;

public class MeshData {

  public List<Vector3> vertices;
  public List<MeshFace> faces;

  public int materialId = 0;
  public int collisionIntention = CollisionIntention.PHYSICAL.getId();

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MeshData m = (MeshData) o;
    return materialId == m.materialId && collisionIntention == m.collisionIntention && vertices.equals(m.vertices) && faces.equals(m.faces);
  }

  @Override
  public int hashCode() {
    return Objects.hash(vertices.size(), faces.size(), materialId, collisionIntention);
  }
}
