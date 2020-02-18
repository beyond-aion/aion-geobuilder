package com.aionemu.geobuilder.loaders;

import com.aionemu.geobuilder.entries.DoorEntry;
import com.aionemu.geobuilder.utils.Vector3;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DoorLoader {

  private List<String> doorFileNames = new ArrayList<>();
  private List<DoorEntry> doorEntries = new ArrayList<>();

  public static List<String> list = new ArrayList<>();
  public void loadDoors(final File missionFile) throws Exception {
    clear();
    final SAXBuilder builder = new SAXBuilder();
    try {
      final Document document = builder.build(missionFile);
      final Element rootNode = document.getRootElement();
      final List<?> entities = rootNode.getChild("Objects").getChildren("Entity");
      for (Object obj : entities) {
        final Element node = (Element) obj;
        if (node.getAttributeValue("EntityClass").equals("Door")) { // doors with keys
          DoorEntry door = new DoorEntry();
          door.entityId = Integer.parseInt(node.getAttributeValue("EntityId")); // static id
          door.name = node.getAttributeValue("Name");
          Vector3 angle = new Vector3();
          String angleValue = node.getAttributeValue("Angles");
          if (angleValue != null) {
            String[] values = angleValue.split(",");
            angle.x = Float.parseFloat(values[0]);
            angle.y = Float.parseFloat(values[1]);
            angle.z = Float.parseFloat(values[2]);
          }
          door.angle = angle;
          Vector3 pos = new Vector3();
          String[] posValues = node.getAttributeValue("Pos").split(",");
          pos.x = Float.parseFloat(posValues[0]);
          pos.y = Float.parseFloat(posValues[1]);
          pos.z = Float.parseFloat(posValues[2]);
          door.pos = pos;


        } /*else if (node.getAttributeValue("EntityClass").equals("PlaceableObject")) {
          Element prop = node.getChild("Properties");
          if (prop != null && prop.getAttribute("fileLadderCGF") != null && prop.getAttributeValue("fileLadderCGF").contains("\\door")) { // doors triggered by event
            DoorEntry door = new DoorEntry();
            door.type = 1;
            door.entityId = Integer.parseInt(node.getAttributeValue("EntityId")); // static id
            door.name = node.getAttributeValue("Name");
            Vector3 angle = new Vector3();
            String angleValue = node.getAttributeValue("Angles");
            if (angleValue != null) {
              String[] values = angleValue.split(",");
              angle.x = Float.parseFloat(values[0]);
              angle.y = Float.parseFloat(values[1]);
              angle.z = Float.parseFloat(values[2]);
            }
            door.angle = angle;
            Vector3 pos = new Vector3();
            String[] posValues = node.getAttributeValue("Pos").split(",");
            pos.x = Float.parseFloat(posValues[0]);
            pos.y = Float.parseFloat(posValues[1]);
            pos.z = Float.parseFloat(posValues[2]);
            door.pos = pos;
            door.animationModel = prop.getAttributeValue("fileLadderCGF").toLowerCase().replace('\\', '/').trim();
            doorEntries.add(door);
            if (!doorFileNames.contains(door.animationModel)) {
              doorFileNames.add(door.animationModel);
            }
          }
        }
        */
      }
    } catch (Exception e) {
      System.err.println(e.toString());
      e.printStackTrace();
    }
  }

  public void clear() {
    doorFileNames.clear();
    doorEntries.clear();
  }

  public List<String> getDoorFileNames() {
    return (List<String>) ((ArrayList) doorFileNames).clone();
  }

  public List<DoorEntry> getDoorEntries() {
    return (List<DoorEntry>) ((ArrayList<DoorEntry>) doorEntries).clone();
  }
}
