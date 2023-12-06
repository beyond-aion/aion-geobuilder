package com.aionemu.geobuilder.loaders;

import com.aionemu.geobuilder.entries.BrushEntry;
import com.aionemu.geobuilder.entries.EntryType;
import com.aionemu.geobuilder.meshData.BrushLstMeshData;
import com.aionemu.geobuilder.utils.PathSanitizer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BrushLstLoader {

  public static final Set<String> EVENT_MESHES = ConcurrentHashMap.newKeySet();
  private static final byte[] SIGNATURE = "CRY".getBytes();

  private BrushLstLoader() {
  }

  public static BrushLstMeshData load(ByteBuffer brushLst) throws IOException {
    BrushLstMeshData meshData = new BrushLstMeshData();

    byte[] signature = new byte[SIGNATURE.length];
    brushLst.get(signature);
    if (!Arrays.equals(SIGNATURE, signature)) {
      throw new IOException("[BrushLstLoader] Wrong signature: " + new String(signature));
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
      String fileName = PathSanitizer.sanitize(new String(fileNameBytes));
      meshData.meshFileNames.add(fileName);
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
      BrushEntry entry = new BrushEntry();
      brushLst.position(brushLst.position() + 4 * 2);
      entry.meshIndex = brushLst.getInt();
      brushLst.position(brushLst.position() + 4 * 3);

      entry.matrix = new float[3 * 4];
      for (int j = 0; j < entry.matrix.length; j++) {
        entry.matrix[j] = brushLst.getFloat();
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
      if (eventType > 0) {
        entry.eventType = (byte) eventType;
        entry.type = EntryType.EVENT;
        EVENT_MESHES.add(meshData.meshFileNames.get(entry.meshIndex));
      }
      meshData.brushEntries.add(entry);
      brushLst.position(brushLst.position() + 4 * (meshDataBlockSize - 15));
    }
    return meshData;
  }
}
