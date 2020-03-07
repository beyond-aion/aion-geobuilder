package com.aionemu.geobuilder.meshData;

import com.aionemu.geobuilder.entries.ObjectEntry;

import java.util.List;

public class ObjectMeshData {

  public List<String> meshFiles;
  public List<ObjectEntry> objectEntries; // ObjectEntry.objectId is the index for its name in meshFiles list
}
