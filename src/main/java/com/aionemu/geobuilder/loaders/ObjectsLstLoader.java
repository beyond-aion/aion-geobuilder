package com.aionemu.geobuilder.loaders;

import com.aionemu.geobuilder.entries.ObjectEntry;
import com.aionemu.geobuilder.utils.DataInputStream;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ObjectsLstLoader {

  private FileInputStream fileStream = null;

  private final List<ObjectEntry> objectEntries = new ArrayList<>();
  private final List<String> vegetationFileNames = new ArrayList<>();

  private void loadEntries(String path, int w, int h) throws Exception {
    fileStream = new FileInputStream(path);
    final DataInputStream stream = new DataInputStream(fileStream);

    int signature = stream.readInt();
    if (signature != 0x10) {
      throw new IOException("ojbects.lst has wrong signature");
    }
    if (w <= 0 || h <= 0) {
      throw new Exception("Map width and height are 0 or less.");
    }
    if (w != h) {
      throw new Exception("Map width and height don't match.");
    }

    float magic = 32768.0f / w;
    while (stream.available() > 0) {
      int xPos = Short.toUnsignedInt(stream.readShort());
      int yPos = Short.toUnsignedInt(stream.readShort());
      int zPos = Short.toUnsignedInt(stream.readShort());
      int objectId = Byte.toUnsignedInt(stream.readByte());
      int unk = stream.readByte(); // values 0 and 255
      float scale = stream.readFloat();
      int rotZ = Byte.toUnsignedInt(stream.readByte());
      int rotY = Byte.toUnsignedInt(stream.readByte());
      int rotX = Byte.toUnsignedInt(stream.readByte());
      byte unk2 = stream.readByte();

      ObjectEntry entry = new ObjectEntry();
      entry.x = xPos / magic;
      entry.y = yPos / magic;
      entry.z = zPos / magic;
      entry.scale = scale;
      entry.rotX = rotX * 360 / 255f;
      entry.rotY = rotY * 360 / 255f;
      entry.rotZ = rotZ * 360 / 255f;
      entry.objectId = objectId;

      objectEntries.add(entry);
    }
  }

  public void loadLevelData(final File levelDataFile, String objectsFilePath) throws Exception {
    clear();
    final SAXBuilder builder = new SAXBuilder();
    // read client maps
    try {
      final Document document = builder.build(levelDataFile);
      final Element rootNode = document.getRootElement();
      final List<?> objects = rootNode.getChildren("Vegetation").get(0).getChildren();

      for (Object obj : objects) {
        final Element node = (Element) obj;
        vegetationFileNames.add(node.getAttributeValue("FileName").toLowerCase().replace('\\', '/').trim());
      }
      int w = Integer.parseInt(rootNode.getChild("LevelInfo").getAttributeValue("HeightmapXSize"));
      int h = Integer.parseInt(rootNode.getChild("LevelInfo").getAttributeValue("HeightmapYSize"));
      loadEntries(objectsFilePath, w, h);
    } catch (Exception e) {
      System.err.println(e.toString());
      e.printStackTrace();
    }
  }

  public void clear() {
    if (fileStream != null) {
      try {
        fileStream.close();
      } catch (IOException e) {
        System.err.println(e.toString());
        e.printStackTrace();
      }
    }
    objectEntries.clear();
    vegetationFileNames.clear();
  }

  public List<ObjectEntry> getObjectEntries() {
    return (List<ObjectEntry>) ((ArrayList<ObjectEntry>) objectEntries).clone();
  }

  public List<String> getVegetationFileNames() {
    return (List<String>) ((ArrayList<String>) vegetationFileNames).clone();
  }
}
