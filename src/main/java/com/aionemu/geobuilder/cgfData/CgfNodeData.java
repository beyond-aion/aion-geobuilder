package com.aionemu.geobuilder.cgfData;

import com.aionemu.geobuilder.utils.Vector3;

import java.util.List;

public class CgfNodeData {

  public int parentId = -1, chunkId, objectId, materialId, positionControllerId, rotationControllerId, scaleControllerId;
  public CgfMeshData mesh;
  public CgfHelperData helper;
  public float[] transform;
  public float[] rotQuat;
  public Vector3 position, scale;
  public List<CgfNodeData> children;
  public CgfMaterialData material;
  public boolean isGroupHead = false;
}
