package com.aionemu.geobuilder.loaders;

import com.aionemu.geobuilder.entries.BrushEntry;
import com.aionemu.geobuilder.entries.EntryType;
import com.aionemu.geobuilder.utils.DataInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BrushLstLoader {

  public static final Set<String> EVENT_MESHES = ConcurrentHashMap.newKeySet();
  private final List<String> meshFileNames = new ArrayList<>();
  private final List<BrushEntry> meshEntries = new ArrayList<>();

  public void load(byte[] brushLst) throws IOException {
    clear();
    try (DataInputStream stream = new DataInputStream(new ByteArrayInputStream(brushLst))) {

      final byte[] signature = new byte[3];
      stream.readFully(signature);
      if (signature[0] != 0x43 || signature[1] != 0x52 || signature[2] != 0x59) {
        stream.close();
        throw new IOException("[BrushLstLoader] Wrong signature");
      }

      stream.readInt(); //dw1
      final int meshDataBlockSize = stream.readInt();
      final int titlesCount = stream.readInt();
      for (int i = 0; i < titlesCount; i++) {
        final int nameLength = stream.readInt();
        final byte[] nameBytes = new byte[nameLength - 4];
        stream.readFully(nameBytes);
      }

      final int meshInfoCount = stream.readInt();
      final byte[] fileNameBytes = new byte[128];
      float[] fl = new float[6];
      for (int i = 0; i < meshInfoCount; i++) {
        stream.skip(4);
        stream.readFully(fileNameBytes);
        final String fileName = new String(fileNameBytes);
        meshFileNames.add(fileName.toLowerCase().replace('\\', '/').trim());
        stream.skip(4);

        // bounding box
        fl[0] = stream.readFloat(); // x1
        fl[1] = stream.readFloat(); // y1
        fl[2] = stream.readFloat(); // z1
        fl[3] = stream.readFloat(); // x2
        fl[4] = stream.readFloat(); // y2
        fl[5] = stream.readFloat(); // z2
      }

      final int meshDataCount = stream.readInt();
      for (int i = 0; i < meshDataCount; i++) {
        stream.skip(4 * 2);
        final int meshIdx = stream.readInt();
        stream.skip(4 * 3);

        final float[] meshMatrix = new float[3 * 4];
        for (int j = 0; j < meshMatrix.length; j++) {
          meshMatrix[j] = stream.readFloat();
        }

        stream.readByte(); // 100/200
        stream.readByte(); // maybe an angle?
        stream.readByte(); // unk
        stream.readByte(); // unk
        stream.readInt(); // unk
        stream.readInt(); // unk
        stream.readInt(); // 0

        // Server Event Decorations
        // 00 = no decoration/also means normal usage of event service
        // 01 = christmas
        // 02 = halloween
        // 04 = valentine
        // 08 = oversea maid event / brax cafe
        int eventType = stream.readInt();
        final BrushEntry entry = new BrushEntry();
        if (eventType > 0 && eventType <= 8) {
          entry.eventType = (byte) eventType;
          entry.type = EntryType.EVENT;
        } else {
          // unsupported event
        }
        entry.meshIdx = meshIdx;
        entry.matrix = meshMatrix;
        meshEntries.add(entry);
        if (eventType > 0) {
          EVENT_MESHES.add(meshFileNames.get(entry.meshIdx));
        }

        stream.readInt(); // 3 unk
        if (meshDataBlockSize > 16)
          stream.readInt(); // 0 unk
        stream.skip(4 * (meshDataBlockSize - 17));

      }
    }
  }

  public List<String> getMeshFileNames() {
    return (List<String>) ((ArrayList<String>) meshFileNames).clone();
  }

  public List<BrushEntry> getMeshEntries() {
    return (List<BrushEntry>) ((ArrayList<BrushEntry>) meshEntries).clone();
  }

  public void clear() {
    meshEntries.clear();
    meshFileNames.clear();
  }
}
