package com.aionemu.geobuilder.loaders;

import com.aionemu.geobuilder.entries.*;
import com.aionemu.geobuilder.utils.PathSanitizer;
import com.aionemu.geobuilder.utils.Vector3;
import com.aionemu.geobuilder.utils.XmlParser;
import org.jdom2.Document;
import org.jdom2.Element;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EntityLoader {

  private EntityLoader() {
  }

  public static List<EntityEntry> loadPlaceables(ByteBuffer mission, Map<String, Short> addresses) throws Exception {
    Document document = XmlParser.parse(mission);
    Element rootNode = document.getRootElement();
    List<Element> entities = rootNode.getChild("Objects").getChildren("Entity");
    List<EntityEntry> entityEntries = new ArrayList<>(entities.size());
    for (Element node : entities) {
      String entityClass = node.getAttributeValue("EntityClass");
      if (entityClass.equalsIgnoreCase("Door")) {
        Element prop = node.getChild("Properties");
        if (!prop.getAttributeValue("object_AnimatedModel").isEmpty()) {
          DoorEntry entry = new DoorEntry();
          DoorEntry entry2 = new DoorEntry();
          entry.entityId = node.getAttribute("EntityId").getIntValue();
          entry2.entityId = entry.entityId;
          loadNameAnglePositionAndScale(node, entry);
          entry2.name = entry.name;
          entry2.angle = entry.angle;
          entry2.pos = entry.pos;
          entry2.scale = entry.scale;
          String mesh = PathSanitizer.sanitize(prop.getAttributeValue("object_AnimatedModel"));
          entry.mesh = mesh;
          entry2.mesh = mesh;
          entry2.suffix = "_state2";
          entry.type = EntryType.DOOR;
          entry2.type = EntryType.DOOR2;
          entityEntries.add(entry);
          entityEntries.add(entry2);
        }
      } else if (entityClass.equalsIgnoreCase("PlaceableObject")
                           || entityClass.equalsIgnoreCase("AbyssArtifacts")
                           || entityClass.equalsIgnoreCase("AbyssShield")
                           || entityClass.equalsIgnoreCase("AbyssDoor")
                           || entityClass.equalsIgnoreCase("tailoring")
                           || entityClass.equalsIgnoreCase("weapon_craft")
                           || entityClass.equalsIgnoreCase("handiwork")
                           || entityClass.equalsIgnoreCase("armor_craft")
                           || entityClass.equalsIgnoreCase("menuisier")
                           || entityClass.equalsIgnoreCase("alchemy")
                           || entityClass.equalsIgnoreCase("cooking")) {
        Element prop = node.getChild("Properties");
        if (!prop.getAttributeValue("fileLadderCGF").isEmpty() && !prop.getAttributeValue("fileLadderCGF").endsWith(".saf")) {
          EntityEntry entry = new EntityEntry();
          entry.entityId = node.getAttribute("EntityId").getIntValue(); // static id
          loadNameAnglePositionAndScale(node, entry);
          entry.mesh = PathSanitizer.sanitize(prop.getAttributeValue("fileLadderCGF"));
          entry.type = EntryType.PLACEABLE;
          entityEntries.add(entry);
        }
      } else if (entityClass.equalsIgnoreCase("BasicEntity")) {
        Element prop = node.getChild("Properties");
        if (!prop.getAttributeValue("object_Model").isEmpty() && !prop.getAttributeValue("object_Model").endsWith(".saf")) {
          EntityEntry entry = new EntityEntry();
          entry.mesh = PathSanitizer.sanitize(prop.getAttributeValue("object_Model"));
          if (entry.mesh.endsWith(".cga")) {
            // TODO: check if there are cgas we should not ignore.
            continue;
          }
          entry.entityId = 0; // basic entites have a static id but they are always spawned, so serverside spawning is not needed
          loadNameAnglePositionAndScale(node, entry);
          String eventType = node.getAttributeValue("EventType");
          if (eventType != null && !eventType.isEmpty()) {
            if (eventType.endsWith("_1")) {
              entry.entityId = 1;
            } else if (eventType.endsWith("_2")) {
              entry.entityId = 2;
            } else if (eventType.endsWith("_4")) {
              entry.entityId = 4;
            } else if (eventType.endsWith("_8")) {
              entry.entityId = 8;
            } else {
              throw new Exception("Unknown event type " + eventType + " for Basic Entity " + node.getAttributeValue("EntityId"));
            }
            entry.type = EntryType.EVENT;
            BrushLstLoader.EVENT_MESHES.add(entry.mesh);
          }
          entityEntries.add(entry);
        }
      } else if (entityClass.equalsIgnoreCase("TownObject")) {
        Element prop = node.getChild("Properties");
        if (!prop.getAttributeValue("object_Model").isEmpty() && !prop.getAttributeValue("object_Model").endsWith(".saf")) {
          TownEntry entry = new TownEntry();
          entry.entityId = node.getAttribute("EntityId").getIntValue(); // static id
          loadNameAnglePositionAndScale(node, entry);
          entry.level = prop.getAttribute("Level").getIntValue();
          entry.townId = prop.getAttribute("TownID").getIntValue();
          entry.mesh = PathSanitizer.sanitize(prop.getAttributeValue("object_Model"));
          entityEntries.add(entry);
        }
      } else if (entityClass.equalsIgnoreCase("HousingBuilding")) {
        Element prop = node.getChild("Properties");
        if (prop != null) {
          HouseEntry entry = new HouseEntry();
          entry.entityId = node.getAttribute("EntityId").getIntValue(); // static id
          loadNameAnglePositionAndScale(node, entry);
          String addressName = prop.getAttributeValue("address_Address");
          entry.address = addressName.isEmpty() ? -1 : addresses.get(addressName);

          Element partsInfo = prop.getChild("PartsInfo");
          if (partsInfo != null) {
            Element build = partsInfo.getChild("Build");
            Element land = partsInfo.getChild("Land");
            if (land != null) {
              String housingobjFence = land.getAttributeValue("housingobjFence");
              if (!housingobjFence.isEmpty()) {
                entry.meshes.add(PathSanitizer.sanitize(housingobjFence));
              }

              String housingobjGarden = land.getAttributeValue("housingobjGarden");
              if (!housingobjGarden.isEmpty()) {
                entry.meshes.add(PathSanitizer.sanitize(housingobjGarden));
              }
            }

            if (build != null) {
              if (prop.getChild("BuildInfo").getAttribute("vectorBuild_Offset") != null) {
                String offsetString = prop.getChild("BuildInfo").getAttributeValue("vectorBuild_Offset");
                if (!offsetString.isEmpty()) {
                  String[] offsetSplitted = offsetString.split(",");
                  float xOffset = Float.parseFloat(offsetSplitted[0]);
                  float yOffset = Float.parseFloat(offsetSplitted[1]);
                  float zOffset = Float.parseFloat(offsetSplitted[2]);
                  if (xOffset != 0f || yOffset != 0f || zOffset != 0f) {
                    entityEntries.add(entry);
                    HouseEntry entry2 = new HouseEntry();
                    entry2.entityId = entry.entityId;
                    entry2.name = entry.name;
                    entry2.angle = entry.angle;
                    entry2.pos = entry.pos;
                    entry2.scale = entry.scale;
                    entry2.address = entry.address;
                    Vector3 offset = Vector3.transform(new Vector3(xOffset, yOffset, zOffset), entry2.getMatrix());
                    entry2.pos = new Vector3(entry2.pos.x + offset.x, entry2.pos.y + offset.y, entry2.pos.z + offset.z);
                    entry = entry2;
                  }
                }
              }
              String housingobjDoor = build.getAttributeValue("housingobjDoor");
              if (!housingobjDoor.isEmpty()) {
                entry.mesh = PathSanitizer.sanitize(housingobjDoor);
                entry.meshes.add(entry.mesh);
              }

              String housingobjFrame = build.getAttributeValue("housingobjFrame");
              if (!housingobjFrame.isEmpty()) {
                entry.meshes.add(PathSanitizer.sanitize(housingobjFrame));
              }

              String housingobjOutWall = build.getAttributeValue("housingobjOutWall");
              if (!housingobjOutWall.isEmpty()) {
                entry.meshes.add(PathSanitizer.sanitize(housingobjOutWall));
              }

              String housingobjRoof = build.getAttributeValue("housingobjRoof");
              if (!housingobjRoof.isEmpty()) {
                entry.meshes.add(PathSanitizer.sanitize(housingobjRoof));
              }

              for (int i = 1; i <= 5; i++) {
                String housingobjInFloor = build.getAttributeValue("housingobjInFloor" + i);
                if (!housingobjInFloor.isEmpty()) {
                  entry.meshes.add(PathSanitizer.sanitize(housingobjInFloor));
                }

                String housingobjInWall = build.getAttributeValue("housingobjInWall" + i);
                if (!housingobjInWall.isEmpty()) {
                  entry.meshes.add(PathSanitizer.sanitize(housingobjInWall));
                }
              }
            }
            entityEntries.add(entry);
          }
        }

      }
    }
    return entityEntries;
  }

  private static void loadNameAnglePositionAndScale(Element node, EntityEntry entry) {
    entry.name = node.getAttributeValue("Name");
    entry.angle = new Vector3();
    String angles = node.getAttributeValue("Angles");
    if (angles != null) {
      String[] values = angles.split(",");
      entry.angle.x = Float.parseFloat(values[0]);
      entry.angle.y = Float.parseFloat(values[1]);
      entry.angle.z = Float.parseFloat(values[2]);
    }
    entry.pos = new Vector3();
    String[] posValues = node.getAttributeValue("Pos").split(",");
    entry.pos.x = Float.parseFloat(posValues[0]);
    entry.pos.y = Float.parseFloat(posValues[1]);
    entry.pos.z = Float.parseFloat(posValues[2]);

    entry.scale = new Vector3(1, 1, 1);
    String scales = node.getAttributeValue("Scale");
    if (scales != null) {
      String[] scaleValues = scales.split(",");
      entry.scale.x = Float.parseFloat(scaleValues[0]);
      entry.scale.y = Float.parseFloat(scaleValues[1]);
      entry.scale.z = Float.parseFloat(scaleValues[2]);
    }
  }
}
