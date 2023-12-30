package com.aionemu.geobuilder.loaders;

import com.aionemu.geobuilder.LevelData;
import com.aionemu.geobuilder.entries.ObjectEntry;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class ObjectsLstLoader {

  private ObjectsLstLoader() {
  }

  public static void load(ByteBuffer buffer, LevelData level) throws Exception {
    int signature = buffer.getInt();
    if (signature != 0x10) {
      throw new IOException("objects.lst has wrong signature");
    }
    int mapXSize = level.terrain.heightmapXSize * level.terrain.heightmapUnitSize;
    int mapYSize = level.terrain.heightmapYSize * level.terrain.heightmapUnitSize;
    level.objectMeshData.objectEntries = new ArrayList<>();
    while (buffer.remaining() > 0) {
      ObjectEntry entry = new ObjectEntry();
      entry.x = Short.toUnsignedInt(buffer.getShort()) * mapXSize / (0xFFFF + 1f);
      entry.y = Short.toUnsignedInt(buffer.getShort()) * mapYSize / (0xFFFF + 1f);
      entry.z = Short.toUnsignedInt(buffer.getShort()) * mapXSize / (0xFFFF + 1f);
      entry.meshIndex = Byte.toUnsignedInt(buffer.get());
      buffer.get(); // values 0 and 255
      entry.scale = buffer.getFloat();
      entry.rotZ = Byte.toUnsignedInt(buffer.get()) * 360 / (0xFF + 1f);
      entry.rotY = Byte.toUnsignedInt(buffer.get()) * 360 / (0xFF + 1f);
      entry.rotX = Byte.toUnsignedInt(buffer.get()) * 360 / (0xFF + 1f);
      buffer.get();

      level.objectMeshData.objectEntries.add(entry);
    }
  }
}
