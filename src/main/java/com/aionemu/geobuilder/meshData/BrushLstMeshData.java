package com.aionemu.geobuilder.meshData;

import com.aionemu.geobuilder.entries.BrushEntry;

import java.util.List;

public class BrushLstMeshData {

  public List<String> meshFileNames;
  public List<BrushEntry> brushEntries; // BrushEntry.meshIdx is the index for its name in meshFiles list
}
