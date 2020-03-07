package com.aionemu.geobuilder.loaders;

import com.aionemu.geobuilder.entries.EntryType;
import com.aionemu.geobuilder.entries.DoorEntry;
import com.aionemu.geobuilder.entries.EntityClass;
import com.aionemu.geobuilder.entries.EntityEntry;
import com.aionemu.geobuilder.entries.HouseEntry;
import com.aionemu.geobuilder.utils.Vector3;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EntityLoader {

  private List<EntityEntry> entityEntries = new ArrayList<>();
  private List<String> placeableEntityFileNames = new ArrayList<>();
  private List<String> basicEntityFileNames = new ArrayList<>();
  private List<String> townEntityFileNames = new ArrayList<>();
  private List<String> houseEntityFileNames = new ArrayList<>();
  private List<String> houseDoorEntityFileNames = new ArrayList<>();
  private List<String> doorEntityFileNames = new ArrayList<>();

  public void loadPlaceables(byte[] mission, Map<String, Integer> addresses) throws Exception {
    clear();
    Document document = new SAXBuilder().build(new ByteArrayInputStream(mission));
    Element rootNode = document.getRootElement();
    List<Element> entities = rootNode.getChild("Objects").getChildren("Entity");
    for (Element node : entities) {
      if (node.getAttributeValue("EntityClass").equalsIgnoreCase("Door")) {
        Element prop = node.getChild("Properties");
        if (prop != null && prop.getAttribute("object_AnimatedModel") != null && !prop.getAttributeValue("object_AnimatedModel").isEmpty()) {
          DoorEntry entry = new DoorEntry();
          DoorEntry entry2 = new DoorEntry();
          entry.entityId = Integer.parseInt(node.getAttributeValue("EntityId"));
          entry2.entityId = entry.entityId;
          entry.name = node.getAttributeValue("Name");
          entry2.name = entry.name;
          Vector3 angle = new Vector3();
          String angleValue = node.getAttributeValue("Angles");
          if (angleValue != null) {
            String[] values = angleValue.split(",");
            angle.x = Float.parseFloat(values[0]);
            angle.y = Float.parseFloat(values[1]);
            angle.z = Float.parseFloat(values[2]);
          }
          entry.angle = angle;
          entry2.angle = entry.angle;
          Vector3 pos = new Vector3();
          String[] posValues = node.getAttributeValue("Pos").split(",");
          pos.x = Float.parseFloat(posValues[0]);
          pos.y = Float.parseFloat(posValues[1]);
          pos.z = Float.parseFloat(posValues[2]);
          entry.pos = pos;
          entry2.pos = entry.pos;
          Vector3 scale = new Vector3(1, 1, 1);
          if (node.getAttributeValue("Scale") != null) {
            String[] scaleValues = node.getAttributeValue("Scale").split(",");
            scale.x = Float.parseFloat(scaleValues[0]);
            scale.y = Float.parseFloat(scaleValues[1]);
            scale.z = Float.parseFloat(scaleValues[2]);
          }
          entry.scale = scale;
          entry2.scale = entry.scale;
          String mesh = prop.getAttributeValue("object_AnimatedModel").toLowerCase().replace('\\', '/').trim();
          entry.mesh = mesh;
          entry2.mesh = mesh;
          entry2.suffix = "_state2";
          entry.entityClass = EntityClass.DOOR;
          entry2.entityClass = entry.entityClass;
          entry.type = EntryType.DOOR;
          entry2.type = EntryType.DOOR2;
          entityEntries.add(entry);
          entityEntries.add(entry2);
          if (!doorEntityFileNames.contains(mesh)) {
            doorEntityFileNames.add(mesh);
          }
        }
      } else if (node.getAttributeValue("EntityClass").equalsIgnoreCase("PlaceableObject")
                           || node.getAttributeValue("EntityClass").equalsIgnoreCase("AbyssArtifacts")
                           || node.getAttributeValue("EntityClass").equalsIgnoreCase("AbyssShield")
                           || node.getAttributeValue("EntityClass").equalsIgnoreCase("AbyssDoor")
                           || node.getAttributeValue("EntityClass").equalsIgnoreCase("tailoring")
                           || node.getAttributeValue("EntityClass").equalsIgnoreCase("weapon_craft")
                           || node.getAttributeValue("EntityClass").equalsIgnoreCase("handiwork")
                           || node.getAttributeValue("EntityClass").equalsIgnoreCase("armor_craft")
                           || node.getAttributeValue("EntityClass").equalsIgnoreCase("menuisier")
                           || node.getAttributeValue("EntityClass").equalsIgnoreCase("alchemy")
                           || node.getAttributeValue("EntityClass").equalsIgnoreCase("cooking")) {
        Element prop = node.getChild("Properties");
        if (prop != null && prop.getAttribute("fileLadderCGF") != null && !prop.getAttributeValue("fileLadderCGF").isEmpty() &&
            !prop.getAttributeValue("fileLadderCGF").endsWith(".saf")) {
          EntityEntry entry = new EntityEntry();
          entry.entityId = Integer.parseInt(node.getAttributeValue("EntityId")); // static id
          entry.name = node.getAttributeValue("Name");
          Vector3 angle = new Vector3();
          String angleValue = node.getAttributeValue("Angles");
          if (angleValue != null) {
            String[] values = angleValue.split(",");
            angle.x = Float.parseFloat(values[0]);
            angle.y = Float.parseFloat(values[1]);
            angle.z = Float.parseFloat(values[2]);
          }
          entry.angle = angle;
          Vector3 pos = new Vector3();
          String[] posValues = node.getAttributeValue("Pos").split(",");
          pos.x = Float.parseFloat(posValues[0]);
          pos.y = Float.parseFloat(posValues[1]);
          pos.z = Float.parseFloat(posValues[2]);
          entry.pos = pos;

          Vector3 scale = new Vector3(1, 1, 1);
          if (node.getAttributeValue("Scale") != null) {
            String[] scaleValues = node.getAttributeValue("Scale").split(",");
            scale.x = Float.parseFloat(scaleValues[0]);
            scale.y = Float.parseFloat(scaleValues[1]);
            scale.z = Float.parseFloat(scaleValues[2]);
          }
          entry.scale = scale;
          entry.mesh = prop.getAttributeValue("fileLadderCGF").toLowerCase().replace('\\', '/').trim();
          entry.entityClass = EntityClass.PLACEABLE;
          entry.type = EntryType.PLACEABLE;
          entityEntries.add(entry);
          if (!placeableEntityFileNames.contains(entry.mesh)) {
            placeableEntityFileNames.add(entry.mesh);
          }
        }
      } else if (node.getAttributeValue("EntityClass").equalsIgnoreCase("BasicEntity")) {
        Element prop = node.getChild("Properties");
        if (prop != null && prop.getAttribute("object_Model") != null && !prop.getAttributeValue("object_Model").isEmpty() &&
            !prop.getAttributeValue("object_Model").endsWith(".saf")) {
          EntityEntry entry = new EntityEntry();
          entry.mesh = prop.getAttributeValue("object_Model").toLowerCase().replace('\\', '/').trim();
          if (entry.mesh.toLowerCase().endsWith(".cga")) {
            // TODO: check if there are cgas we should not ignore.
            continue;
          }
          entry.entityId = 0; // basic entites have a static id but they are always spawned, so serverside spawning is not needed
          entry.name = node.getAttributeValue("Name");
          Vector3 angle = new Vector3();
          String angleValue = node.getAttributeValue("Angles");
          if (angleValue != null) {
            String[] values = angleValue.split(",");
            angle.x = Float.parseFloat(values[0]);
            angle.y = Float.parseFloat(values[1]);
            angle.z = Float.parseFloat(values[2]);
          }
          entry.angle = angle;
          Vector3 pos = new Vector3();
          String[] posValues = node.getAttributeValue("Pos").split(",");
          pos.x = Float.parseFloat(posValues[0]);
          pos.y = Float.parseFloat(posValues[1]);
          pos.z = Float.parseFloat(posValues[2]);
          entry.pos = pos;

          Vector3 scale = new Vector3(1, 1, 1);
          if (node.getAttributeValue("Scale") != null) {
            String[] scaleValues = node.getAttributeValue("Scale").split(",");
            scale.x = Float.parseFloat(scaleValues[0]);
            scale.y = Float.parseFloat(scaleValues[1]);
            scale.z = Float.parseFloat(scaleValues[2]);
          }
          if (node.getAttribute("EventType") != null && !node.getAttributeValue("EventType").isEmpty()) {
            String eventType = node.getAttributeValue("EventType");
            entry.type = EntryType.EVENT;
            if (eventType.endsWith("_1")) {
              entry.entityId = 1;
            } else if (eventType.endsWith("_2")) {
              entry.entityId = 2;
            } else if (eventType.endsWith("_4")) {
              entry.entityId = 4;
            } else if (eventType.endsWith("_8")) {
              entry.entityId = 8;
            } else {
              System.out.println(". . . Unknown Event [" + node.getAttributeValue("EventType") + "]for Basic Entity with entityId: " + entry.entityId);
              break;
            }
            entry.type = EntryType.EVENT;
            BrushLstLoader.EVENT_MESHES.add(entry.mesh);
          }
          entry.scale = scale;
          entry.entityClass = EntityClass.BASIC;
          entityEntries.add(entry);
          if (!basicEntityFileNames.contains(entry.mesh)) {
            basicEntityFileNames.add(entry.mesh);
          }
        }
      } else if (node.getAttributeValue("EntityClass").equalsIgnoreCase("TownObject")) {
        Element prop = node.getChild("Properties");
        if (prop != null && prop.getAttribute("object_Model") != null && !prop.getAttributeValue("object_Model").isEmpty() &&
            !prop.getAttributeValue("object_Model").endsWith(".saf")) {
          EntityEntry entry = new EntityEntry();
          entry.entityId = Integer.parseInt(node.getAttributeValue("EntityId")); // static id
          entry.name = node.getAttributeValue("Name");
          Vector3 angle = new Vector3();
          String angleValue = node.getAttributeValue("Angles");
          if (angleValue != null) {
            String[] values = angleValue.split(",");
            angle.x = Float.parseFloat(values[0]);
            angle.y = Float.parseFloat(values[1]);
            angle.z = Float.parseFloat(values[2]);
          }
          entry.angle = angle;
          Vector3 pos = new Vector3();
          String[] posValues = node.getAttributeValue("Pos").split(",");
          pos.x = Float.parseFloat(posValues[0]);
          pos.y = Float.parseFloat(posValues[1]);
          pos.z = Float.parseFloat(posValues[2]);
          entry.pos = pos;

          Vector3 scale = new Vector3(1, 1, 1);
          if (node.getAttributeValue("Scale") != null) {
            String[] scaleValues = node.getAttributeValue("Scale").split(",");
            scale.x = Float.parseFloat(scaleValues[0]);
            scale.y = Float.parseFloat(scaleValues[1]);
            scale.z = Float.parseFloat(scaleValues[2]);
          }
          entry.scale = scale;
          entry.level = Integer.parseInt(prop.getAttributeValue("Level"));
          entry.startLevel = Integer.parseInt(prop.getAttributeValue("StartLevel"));
          entry.townId = Integer.parseInt(prop.getAttributeValue("TownID"));
          entry.mesh = prop.getAttributeValue("object_Model").toLowerCase().replace('\\', '/').trim();
          entry.entityClass = EntityClass.TOWN_OBJECT;
          entry.type = EntryType.TOWN;
          entityEntries.add(entry);
          if (!townEntityFileNames.contains(entry.mesh)) {
            townEntityFileNames.add(entry.mesh);
          }
          for (int i = entry.startLevel; i <= 5; i++) {
            EntityEntry entry2 = new EntityEntry();
            entry2.mesh = entry.mesh.replace("01.cgf", "0" + i + ".cgf");
            if (entry2.mesh.equalsIgnoreCase(entry.mesh)) {
              continue;
            }
            entry2.entityId = entry.entityId;
            entry2.name = entry.name;
            entry2.angle = entry.angle;
            entry2.pos = entry.pos;
            entry2.scale = entry.scale;
            entry2.entityClass = entry.entityClass;
            entry2.level = i;
            entry2.startLevel = entry.startLevel;
            entry2.townId = entry.townId;
            entityEntries.add(entry2);
            if (!townEntityFileNames.contains(entry2.mesh)) {
              townEntityFileNames.add(entry2.mesh);
            }
          }
        }
      } else if (node.getAttributeValue("EntityClass").equalsIgnoreCase("HousingBuilding")) {
        Element prop = node.getChild("Properties");
        if (prop != null) {
          HouseEntry entry = new HouseEntry();
          entry.entityId = Integer.parseInt(node.getAttributeValue("EntityId")); // static id
          entry.name = node.getAttributeValue("Name");
          Vector3 angle = new Vector3();
          String angleValue = node.getAttributeValue("Angles");
          if (angleValue != null) {
            String[] values = angleValue.split(",");
            angle.x = Float.parseFloat(values[0]);
            angle.y = Float.parseFloat(values[1]);
            angle.z = Float.parseFloat(values[2]);
          }
          entry.angle = angle;
          Vector3 pos = new Vector3();
          String[] posValues = node.getAttributeValue("Pos").split(",");
          pos.x = Float.parseFloat(posValues[0]);
          pos.y = Float.parseFloat(posValues[1]);
          pos.z = Float.parseFloat(posValues[2]);
          entry.pos = pos;

          Vector3 scale = new Vector3(1, 1, 1);
          if (node.getAttributeValue("Scale") != null) {
            String[] scaleValues = node.getAttributeValue("Scale").split(",");
            scale.x = Float.parseFloat(scaleValues[0]);
            scale.y = Float.parseFloat(scaleValues[1]);
            scale.z = Float.parseFloat(scaleValues[2]);
          }
          entry.scale = scale;
          entry.entityClass = EntityClass.HOUSE;
          entry.address = addresses.getOrDefault(prop.getAttributeValue("address_Address"), -1);

          Element partsInfo = prop.getChild("PartsInfo");
          if (partsInfo != null) {
            Element build = partsInfo.getChild("Build");
            Element land = partsInfo.getChild("Land");
            if (land != null) {
              if (land.getAttribute("housingobjFence") != null) {
                String val = land.getAttributeValue("housingobjFence").toLowerCase().replace('\\', '/').trim();
                ;
                if (!val.isEmpty()) {
                  entry.meshes.add(val);
                  if (!houseEntityFileNames.contains(val)) {
                    houseEntityFileNames.add(val);
                  }
                }
              } else {
                System.out.println("house has no fence: " + entry.entityId);
              }

              if (land.getAttribute("housingobjGarden") != null) {
                String val = land.getAttributeValue("housingobjGarden").toLowerCase().replace('\\', '/').trim();
                ;
                if (!val.isEmpty()) {
                  entry.meshes.add(val);
                  if (!houseEntityFileNames.contains(val)) {
                    houseEntityFileNames.add(val);
                  }
                }
              } else {
                System.out.println("house has no garden: " + entry.entityId);
              }
            }

            if (build != null) {
              if (prop.getChild("BuildInfo") != null && prop.getChild("BuildInfo").getAttribute("vectorBuild_Offset") != null) {
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
                    entry2.entityClass = entry.entityClass;
                    Vector3 offset = Vector3.transform(new Vector3(xOffset, yOffset, zOffset), entry2.getMatrix());
                    entry2.pos = new Vector3(entry2.pos.x + offset.x, entry2.pos.y + offset.y, entry2.pos.z + offset.z);
                    entry = entry2;
                  }
                }
              }
              if (build.getAttribute("housingobjDoor") != null) {
                String val = build.getAttributeValue("housingobjDoor").toLowerCase().replace('\\', '/').trim();
                ;
                if (!val.isEmpty()) {
                  entry.mesh = val;
                  entry.meshes.add(val);
                  if (!houseDoorEntityFileNames.contains(val)) {
                    houseDoorEntityFileNames.add(val);
                  }
                } else {
                  System.out.println("house has no door: " + entry.entityId);
                }
              }

              if (build.getAttribute("housingobjFrame") != null) {
                String val = build.getAttributeValue("housingobjFrame").toLowerCase().replace('\\', '/').trim();
                ;
                if (!val.isEmpty()) {
                  entry.meshes.add(val);
                  if (!houseEntityFileNames.contains(val)) {
                    houseEntityFileNames.add(val);
                  }
                }
              } else {
                System.out.println("house has no frame: " + entry.entityId);
              }

              if (build.getAttribute("housingobjOutWall") != null) {
                String val = build.getAttributeValue("housingobjOutWall").toLowerCase().replace('\\', '/').trim();
                ;
                if (!val.isEmpty()) {
                  entry.meshes.add(val);
                  if (!houseEntityFileNames.contains(val)) {
                    houseEntityFileNames.add(val);
                  }
                }
              } else {
                System.out.println("house has no out wall: " + entry.entityId);
              }

              if (build.getAttribute("housingobjRoof") != null) {
                String val = build.getAttributeValue("housingobjRoof").toLowerCase().replace('\\', '/').trim();
                ;
                if (!val.isEmpty()) {
                  entry.meshes.add(val);
                  if (!houseEntityFileNames.contains(val)) {
                    houseEntityFileNames.add(val);
                  }
                }
              } else {
                System.out.println("house has no roof: " + entry.entityId);
              }

              for (int i = 1; i < 6; i++) {
                if (build.getAttribute("housingobjInFloor" + i) != null) {
                  String val = build.getAttributeValue("housingobjInFloor" + i).toLowerCase().replace('\\', '/').trim();
                  ;
                  if (!val.isEmpty()) {
                    entry.meshes.add(val);
                    if (!houseEntityFileNames.contains(val)) {
                                            houseEntityFileNames.add(val);
                    }
                  }
                } else {
                  System.out.println("house has no in floor" + i + ": " + entry.entityId);
                }

                if (build.getAttribute("housingobjInWall" + i) != null) {
                  String val = build.getAttributeValue("housingobjInWall" + i).toLowerCase().replace('\\', '/').trim();
                  ;
                  if (!val.isEmpty()) {
                    entry.meshes.add(val);
                    if (!houseEntityFileNames.contains(val)) {
                                            houseEntityFileNames.add(val);
                    }
                  }
                } else {
                  System.out.println("house has no in wall" + i + ": " + entry.entityId);
                }
              }
            }
            entityEntries.add(entry);
          }
        }

      }
    }
  }

  public void clear() {
    entityEntries.clear();
    placeableEntityFileNames.clear();
    basicEntityFileNames.clear();
    townEntityFileNames.clear();
    houseEntityFileNames.clear();
    doorEntityFileNames.clear();
  }

  public List<String> getEntityFileNames(EntityClass entityClass) {
    switch (entityClass) {
      case BASIC:
        return (List<String>) ((ArrayList) basicEntityFileNames).clone();
      case PLACEABLE:
        return (List<String>) ((ArrayList) placeableEntityFileNames).clone();
      case TOWN_OBJECT:
        return (List<String>) ((ArrayList) townEntityFileNames).clone();
      case HOUSE:
        return (List<String>) ((ArrayList) houseEntityFileNames).clone();
      case HOUSE_DOOR:
        return (List<String>) ((ArrayList) houseDoorEntityFileNames).clone();
      case DOOR:
        return (List<String>) ((ArrayList) doorEntityFileNames).clone();
      default:
        return null;
    }
  }

  public List<EntityEntry> getEntityEntries() {
    return (List<EntityEntry>) ((ArrayList<EntityEntry>) entityEntries).clone();
  }

}
