package com.aionemu.geobuilder.loaders;

import com.aionemu.geobuilder.entries.ObjectEntry;
import com.aionemu.geobuilder.meshData.ObjectMeshData;
import com.aionemu.geobuilder.utils.PathSanitizer;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class ObjectsLstLoader {

  private ObjectsLstLoader() {
  }

  public static ObjectMeshData loadLevelData(ByteBuffer levelData, ByteBuffer objects) throws Exception {
    ObjectMeshData objectMeshData = new ObjectMeshData();
    // read client maps
    Document document = new SAXBuilder().build(new ByteArrayInputStream(levelData.array()));
    Element rootNode = document.getRootElement();
    List<Element> vegetation = rootNode.getChildren("Vegetation").get(0).getChildren();

    objectMeshData.meshFiles = new ArrayList<>(vegetation.size());
    for (Element element : vegetation) {
      objectMeshData.meshFiles.add(PathSanitizer.sanitize(element.getAttributeValue("FileName")));
    }
    int w = Integer.parseInt(rootNode.getChild("LevelInfo").getAttributeValue("HeightmapXSize"));
    int h = Integer.parseInt(rootNode.getChild("LevelInfo").getAttributeValue("HeightmapYSize"));
    loadEntries(objects, w, h, objectMeshData);
    return objectMeshData;
  }

  private static void loadEntries(ByteBuffer objects, int w, int h, ObjectMeshData objectMeshData) throws Exception {
    objectMeshData.objectEntries = new ArrayList<>();

    int signature = objects.getInt();
    if (signature != 0x10) {
      throw new IOException("objects.lst has wrong signature");
    }
    if (w <= 0 || h <= 0) {
      throw new Exception("Map width and height are 0 or less.");
    }
    if (w != h) {
      throw new Exception("Map width and height don't match.");
    }

    float magic = 32768.0f / w;
    while (objects.remaining() > 0) {
      int xPos = Short.toUnsignedInt(objects.getShort());
      int yPos = Short.toUnsignedInt(objects.getShort());
      int zPos = Short.toUnsignedInt(objects.getShort());
      int objectId = Byte.toUnsignedInt(objects.get());
      int unk = objects.get(); // values 0 and 255
      float scale = objects.getFloat();
      int rotZ = Byte.toUnsignedInt(objects.get());
      int rotY = Byte.toUnsignedInt(objects.get());
      int rotX = Byte.toUnsignedInt(objects.get());
      byte unk2 = objects.get();

      ObjectEntry entry = new ObjectEntry();
      entry.x = xPos / magic;
      entry.y = yPos / magic;
      entry.z = zPos / magic;
      entry.scale = scale;
      entry.rotX = rotX * 360 / 255f;
      entry.rotY = rotY * 360 / 255f;
      entry.rotZ = rotZ * 360 / 255f;
      entry.objectId = objectId;

      objectMeshData.objectEntries.add(entry);
    }
  }
}
