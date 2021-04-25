package com.aionemu.geobuilder.meshData;

import java.util.Objects;

public class MeshFace {
  public int v0, v1, v2;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MeshFace meshFace = (MeshFace) o;
    return v0 == meshFace.v0 && v1 == meshFace.v1 && v2 == meshFace.v2;
  }

  @Override
  public int hashCode() {
    return Objects.hash(v0, v1, v2);
  }
}
