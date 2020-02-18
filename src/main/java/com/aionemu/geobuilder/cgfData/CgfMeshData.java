package com.aionemu.geobuilder.cgfData;

import com.aionemu.geobuilder.meshData.MeshFace;
import com.aionemu.geobuilder.utils.Vector3;

import java.util.ArrayList;
import java.util.HashMap;

public class CgfMeshData {
  public ArrayList<Vector3> vertices;
  public HashMap<Integer, ArrayList<MeshFace>> indices;
}
