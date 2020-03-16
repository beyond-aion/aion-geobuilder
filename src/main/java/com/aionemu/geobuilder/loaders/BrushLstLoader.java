package com.aionemu.geobuilder.loaders;

import com.aionemu.geobuilder.entries.BrushEntry;
import com.aionemu.geobuilder.entries.EntryType;
import com.aionemu.geobuilder.meshData.BrushLstMeshData;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BrushLstLoader {

  public static final Set<String> EVENT_MESHES = ConcurrentHashMap.newKeySet();

  public static BrushLstMeshData load(ByteBuffer brushLst) throws IOException {
    BrushLstMeshData meshData = new BrushLstMeshData();

    byte[] signature = new byte[3];
    brushLst.get(signature);
    if (signature[0] != 0x43 || signature[1] != 0x52 || signature[2] != 0x59) {
      throw new IOException("[BrushLstLoader] Wrong signature");
    }

    brushLst.getInt(); //dw1
    int meshDataBlockSize = brushLst.getInt();
    int titlesCount = brushLst.getInt();
    for (int i = 0; i < titlesCount; i++) {
      int nameLength = brushLst.getInt();
      byte[] nameBytes = new byte[nameLength - 4];
      brushLst.get(nameBytes);
    }

    int meshInfoCount = brushLst.getInt();
    byte[] fileNameBytes = new byte[128];
    meshData.meshFileNames = new ArrayList<>(meshInfoCount);
    for (int i = 0; i < meshInfoCount; i++) {
      brushLst.position(brushLst.position() + 4);
      brushLst.get(fileNameBytes);
      String fileName = new String(fileNameBytes);
      meshData.meshFileNames.add(fileName.toLowerCase().replace('\\', '/').trim());
      brushLst.position(brushLst.position() + 4);

      // bounding box
      brushLst.getFloat(); // x1
      brushLst.getFloat(); // y1
      brushLst.getFloat(); // z1
      brushLst.getFloat(); // x2
      brushLst.getFloat(); // y2
      brushLst.getFloat(); // z2
    }

    int meshDataCount = brushLst.getInt();
    meshData.brushEntries = new ArrayList<>(meshDataCount);
    for (int i = 0; i < meshDataCount; i++) {
      brushLst.position(brushLst.position() + 4 * 2);
      int meshIdx = brushLst.getInt();
      brushLst.position(brushLst.position() + 4 * 3);

      float[] meshMatrix = new float[3 * 4];
      for (int j = 0; j < meshMatrix.length; j++) {
        meshMatrix[j] = brushLst.getFloat();
      }

      brushLst.get(); // 100/200
      brushLst.get(); // maybe an angle?
      brushLst.get(); // unk
      brushLst.get(); // unk
      brushLst.getInt(); // unk
      brushLst.getInt(); // unk
      brushLst.getInt(); // 0

      // Server Event Decorations
      // 00 = no decoration/also means normal usage of event service
      // 01 = christmas
      // 02 = halloween
      // 04 = valentine
      // 08 = oversea maid event / brax cafe
      // 16, 32, 64, 128 are test IDs on map 900020000 Test_Basic
      int eventType = brushLst.getInt();
      if (eventType < 0 || eventType > 0xFF) {
        throw new IOException("Out of range event type " + eventType);
      }
      BrushEntry entry = new BrushEntry();
      entry.eventType = (byte) eventType;
      entry.type = EntryType.EVENT;
      entry.meshIdx = meshIdx;
      entry.matrix = meshMatrix;
      meshData.brushEntries.add(entry);
      if (eventType > 0) {
        EVENT_MESHES.add(meshData.meshFileNames.get(entry.meshIdx));
      }

      brushLst.getInt(); // 3 unk
      if (meshDataBlockSize > 16)
        brushLst.getInt(); // 0 unk
      brushLst.position(brushLst.position() + 4 * (meshDataBlockSize - 17));

    }
    return meshData;
  }
}
