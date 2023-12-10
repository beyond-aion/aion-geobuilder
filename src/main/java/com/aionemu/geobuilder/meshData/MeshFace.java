package com.aionemu.geobuilder.meshData;

public class MeshFace implements Comparable<MeshFace> {

  public int v0, v1, v2;

  public boolean isDegenerate() {
    return v0 == v1 || v0 == v2 || v1 == v2;
  }

  public void sort() {
    int temp;
    if (v1 < v0) {
      temp = v0;
      v0 = v1;
      v1 = temp;
    }
    if (v2 < v1) {
      temp = v1;
      v1 = v2;
      v2 = temp;
    }
    if (v1 < v0) {
      temp = v0;
      v0 = v1;
      v1 = temp;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MeshFace meshFace = (MeshFace) o;
    return (v0 == meshFace.v0 && v1 == meshFace.v1 && v2 == meshFace.v2)
                   || (v0 == meshFace.v0 && v1 == meshFace.v2 && v2 == meshFace.v1)
                   || (v0 == meshFace.v1 && v1 == meshFace.v0 && v2 == meshFace.v2)
                   || (v0 == meshFace.v1 && v1 == meshFace.v2 && v2 == meshFace.v0)
                   || (v0 == meshFace.v2 && v1 == meshFace.v0 && v2 == meshFace.v1)
                   || (v0 == meshFace.v2 && v1 == meshFace.v1 && v2 == meshFace.v0);
  }

  @Override
  public int hashCode() {
    return v0 + v1 + v2;
  }

  @Override
  public int compareTo(MeshFace face) {
    if (v0 != face.v0) {
      return Integer.compare(v0, face.v0);
    }
    if (v1 != face.v1) {
      return Integer.compare(v1, face.v1);
    }
    if (v2 != face.v2) {
      return Integer.compare(v2, face.v2);
    }
    return 0;
  }
}
