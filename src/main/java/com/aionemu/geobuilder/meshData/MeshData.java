package com.aionemu.geobuilder.meshData;

import com.aionemu.geobuilder.utils.Vector3;

import java.util.*;

public class MeshData {

  public List<Vector3> vertices = Collections.emptyList();
  public List<MeshFace> faces = Collections.emptyList();
  private int maxFaceVertexIndex = -1;

  public int materialId = 0;
  public int collisionIntention = CollisionIntention.PHYSICAL.getId();

  public int getMaxFaceVertexIndex() {
    if (maxFaceVertexIndex == -1) {
      for (MeshFace meshFace : faces)
        maxFaceVertexIndex = Math.max(maxFaceVertexIndex, Math.max(meshFace.v0, Math.max(meshFace.v1, meshFace.v2)));
    }
    return maxFaceVertexIndex;
  }

  public int getSize() {
    return vertices.size() * 3 * 4 + faces.size() * 3 * (getMaxFaceVertexIndex() > 0xFF ? 2 : 1) + 4 + 4;
  }

  public void compact() {
    int[] uniqueIndices = new int[vertices.size()];
    Map<Vector3, Integer> unique = new HashMap<>();
    for (int i = 0; i < vertices.size(); i++) {
      int index = i;
      uniqueIndices[i] = unique.computeIfAbsent(vertices.get(i), k -> index);
    }
    Set<MeshFace> newFaces = new LinkedHashSet<>(faces.size()); // ordered set for repeatable file hashes
    for (MeshFace face : faces) {
      face.v0 = uniqueIndices[face.v0];
      face.v1 = uniqueIndices[face.v1];
      face.v2 = uniqueIndices[face.v2];
      if (!face.isDegenerate()) {
        newFaces.add(face);
      }
    }
    BitSet oldIndices = new BitSet(vertices.size());
    List<Vector3> newVertices = new ArrayList<>();
    for (MeshFace face : newFaces) {
      face.v0 = addUniqueVertexAndGetIndex(face.v0, oldIndices, uniqueIndices, newVertices);
      face.v1 = addUniqueVertexAndGetIndex(face.v1, oldIndices, uniqueIndices, newVertices);
      face.v2 = addUniqueVertexAndGetIndex(face.v2, oldIndices, uniqueIndices, newVertices);
    }
    vertices = newVertices;
    faces = new ArrayList<>(newFaces);
    maxFaceVertexIndex = vertices.size() - 1; // all vertices are referenced by faces now
  }

  private int addUniqueVertexAndGetIndex(int oldIndex, BitSet oldIndices, int[] newIndices, List<Vector3> newVertices) {
    if (!oldIndices.get(oldIndex)) {
      oldIndices.set(oldIndex);
      newIndices[oldIndex] = newVertices.size();
      newVertices.add(vertices.get(oldIndex));
    }
    return newIndices[oldIndex];
  }

  public void sort() {
    sortVertices();
    sortFaces();
  }

  private void sortVertices() {
    Map<Vector3, Integer> oldIndexByVertex = new HashMap<>();
    for (int i = 0; i < vertices.size(); i++) {
      oldIndexByVertex.put(vertices.get(i), i);
    }
    vertices.sort(null);
    int[] newIndices = new int[vertices.size()];
    for (int i = 0; i < vertices.size(); i++) {
      newIndices[oldIndexByVertex.get(vertices.get(i))] = i;
    }
    for (MeshFace face : faces) {
      face.v0 = newIndices[face.v0];
      face.v1 = newIndices[face.v1];
      face.v2 = newIndices[face.v2];
    }
  }

  private void sortFaces() {
    faces.forEach(MeshFace::sort);
    faces.sort(null);
  }

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
