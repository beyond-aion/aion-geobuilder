package com.aionemu.geobuilder.loaders;

import com.aionemu.geobuilder.CollisionIntention;
import com.aionemu.geobuilder.cgfData.*;
import com.aionemu.geobuilder.meshData.MeshData;
import com.aionemu.geobuilder.meshData.MeshFace;
import com.aionemu.geobuilder.utils.DataInputStream;
import com.aionemu.geobuilder.utils.Quaternion;
import com.aionemu.geobuilder.utils.Vector3;
import com.aionemu.geobuilder.math.Matrix4f;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CgfLoader {

  private static final Map<String, Integer> materialNamesAndIds = new HashMap<>();
  private static final Set<Integer> materialIntentionIds = new HashSet<>();
  private List<CgfChunkHeader> chunkHeaders = new ArrayList<>();
  private Map<Integer, CgfMaterialData> materialData = new HashMap<>();
  private List<CgfNodeData> nodes = new ArrayList<>();
  private List<Integer> materialIdx = new ArrayList<>();
  private String[] boneNames;
  private List<float[]> boneInitialPos = new ArrayList<>();
  private List<CgfBoneAnimData> bones = new ArrayList<>();
  private HashMap<Integer, CgfBoneMeshData> boneMeshes = new HashMap<>();

  static {
    materialNamesAndIds.put("mat_default", 0);
    materialNamesAndIds.put("mat_nowalk_obstacle0", 1);
    materialNamesAndIds.put("mat_nowalk_obstacle1", 2);
    materialNamesAndIds.put("mat_nowalk_obstacle2", 3);
    materialNamesAndIds.put("mat_nowalk_obstacle3", 4);
    materialNamesAndIds.put("mat_nowalk_obstacle4", 5);
    materialNamesAndIds.put("mat_walk_obstacle1", 6);
    materialNamesAndIds.put("mat_walk_obstacle2", 7);
    materialNamesAndIds.put("mat_walk_obstacle3", 8);
    materialNamesAndIds.put("mat_walk_obstacle4", 9);
    materialNamesAndIds.put("mat_nobreathing", 10);
    materialNamesAndIds.put("mat_abyss_castle_shield", 11);
    materialNamesAndIds.put("mat_lava", 12);
    materialNamesAndIds.put("mat_passby_dmg_shield", 13);
    materialNamesAndIds.put("mat_ab1_light_start", 14);
    materialNamesAndIds.put("mat_ab1_dark_start", 15);
    materialNamesAndIds.put("mat_ab1_flamemoon", 16);
    materialNamesAndIds.put("mat_grass", 20);
    materialNamesAndIds.put("mat_sand", 21);
    materialNamesAndIds.put("mat_dirt", 22);
    materialNamesAndIds.put("mat_pavement", 23);
    materialNamesAndIds.put("mat_wood", 24);
    materialNamesAndIds.put("mat_stone_tough", 25);
    materialNamesAndIds.put("mat_stone_marble", 26);
    materialNamesAndIds.put("mat_pebble", 27);
    materialNamesAndIds.put("mat_metal_plate", 28);
    materialNamesAndIds.put("mat_metal_wirenet", 29);
    materialNamesAndIds.put("mat_fabric", 30);
    materialNamesAndIds.put("mat_leaves", 31);
    materialNamesAndIds.put("mat_water", 32);
    materialNamesAndIds.put("mat_water_deep", 33);
    materialNamesAndIds.put("mat_magic_circle", 34);
    materialNamesAndIds.put("mat_flesh", 35);
    materialNamesAndIds.put("mat_sand_wet", 36);
    materialNamesAndIds.put("mat_under_water", 37);
    materialNamesAndIds.put("mat_mob_insect", 38);
    materialNamesAndIds.put("mat_mob_reptile", 39);
    materialNamesAndIds.put("mat_mob_rotten", 40);
    materialNamesAndIds.put("mat_mob_hard", 41);
    materialNamesAndIds.put("mat_mob_wood", 42);
    materialNamesAndIds.put("mat_mob_orc", 43);
    materialNamesAndIds.put("mat_mob_boss", 44);
    materialNamesAndIds.put("mat_e3item_book", 45);
    materialNamesAndIds.put("mat_item_wood", 46);
    materialNamesAndIds.put("foot_2leg_small", 47);
    materialNamesAndIds.put("foot_2leg_medium", 48);
    materialNamesAndIds.put("foot_2leg_big", 49);
    materialNamesAndIds.put("foot_4leg_small", 50);
    materialNamesAndIds.put("foot_4leg_medium", 51);
    materialNamesAndIds.put("foot_4leg_big", 52);
    materialNamesAndIds.put("foot_reptile_small", 53);
    materialNamesAndIds.put("foot_reptile_medium", 54);
    materialNamesAndIds.put("foot_reptile_big", 55);
    materialNamesAndIds.put("foot_flying", 56);
    materialNamesAndIds.put("foot_insect", 57);
    materialNamesAndIds.put("foot_etc", 58);
    materialNamesAndIds.put("mat_snow", 59);
    materialNamesAndIds.put("mat_fire", 60);
    materialNamesAndIds.put("mat_cond_fire", 61);
    materialNamesAndIds.put("mat_weather_cond_fire", 62);
    materialNamesAndIds.put("mat_time_cond_fire", 63);
    materialNamesAndIds.put("mat_sword_s", 64);
    materialNamesAndIds.put("mat_sword_m", 65);
    materialNamesAndIds.put("mat_sword_h", 66);
    materialNamesAndIds.put("mat_mace_s", 67);
    materialNamesAndIds.put("mat_mace_m", 68);
    materialNamesAndIds.put("mat_mace_h", 69);
    materialNamesAndIds.put("mat_dagger_s", 70);
    materialNamesAndIds.put("mat_dagger_m", 71);
    materialNamesAndIds.put("mat_dagger_h", 72);
    materialNamesAndIds.put("mat_orb_s", 73);
    materialNamesAndIds.put("mat_orb_m", 74);
    materialNamesAndIds.put("mat_orb_h", 75);
    materialNamesAndIds.put("mat_book_s", 76);
    materialNamesAndIds.put("mat_book_m", 77);
    materialNamesAndIds.put("mat_book_h", 78);
    materialNamesAndIds.put("mat_2hsword_s", 79);
    materialNamesAndIds.put("mat_2hsword_m", 80);
    materialNamesAndIds.put("mat_2hsword_h", 81);
    materialNamesAndIds.put("mat_polearm_s", 82);
    materialNamesAndIds.put("mat_polearm_m", 83);
    materialNamesAndIds.put("mat_polearm_h", 84);
    materialNamesAndIds.put("mat_staff_s", 85);
    materialNamesAndIds.put("mat_staff_m", 86);
    materialNamesAndIds.put("mat_staff_h", 87);
    materialNamesAndIds.put("mat_bow_s", 88);
    materialNamesAndIds.put("mat_bow_m", 89);
    materialNamesAndIds.put("mat_bow_h", 90);
    materialNamesAndIds.put("mat_hp_regen", 91);
    materialNamesAndIds.put("mat_poison_recovery_a", 92);
    materialNamesAndIds.put("mat_poison_recovery_b", 93);
    materialNamesAndIds.put("mat_gold", 94);
    materialNamesAndIds.put("mat_deep_sand", 95);
    materialNamesAndIds.put("mat_swamp", 96);
    materialNamesAndIds.put("mat_strong_lava", 97);
    materialNamesAndIds.put("foot_2leg_shulack", 98);
    materialNamesAndIds.put("mat_Test_Material1", 99);
    materialNamesAndIds.put("mat_Test_Material2", 100);
    materialNamesAndIds.put("mat_Test_Material3", 101);
    materialNamesAndIds.put("mat_poison", 102);
    materialNamesAndIds.put("mat_Medium_lava", 103);
    materialNamesAndIds.put("mat_housing_type1", 104);
    materialNamesAndIds.put("mat_housing_type2", 105);
    materialNamesAndIds.put("mat_housing_type3", 106);
    materialNamesAndIds.put("mat_drana", 107);
    materialNamesAndIds.put("mat_acidheal", 108);
    materialNamesAndIds.put("foot_2leg_Pet", 109);
    materialNamesAndIds.put("foot_4leg_Pet", 110);
    materialNamesAndIds.put("mat_Swamp_Arena", 111);
    materialNamesAndIds.put("mat_mud_Arena", 112);
    materialNamesAndIds.put("mat_water_damage_arena", 113);
    materialNamesAndIds.put("mat_housing_spa", 114);
    materialNamesAndIds.put("mat_dispel_corn", 115);
    materialNamesAndIds.put("mat_dispel_starturtle", 116);
    materialNamesAndIds.put("mat_dispel_starfish", 117);
    materialNamesAndIds.put("mat_must_die", 118);
    materialNamesAndIds.put("mat_rainwater_Arena", 119);
    materialNamesAndIds.put("mat_oditonite_Arena", 120);
    materialNamesAndIds.put("mat_default_obstacle_1", 121);
    materialNamesAndIds.put("mat_default_obstacle_2", 122);
    materialNamesAndIds.put("mat_default_obstacle_3", 123);
    materialNamesAndIds.put("mat_default_obstacle_4", 124);
    materialNamesAndIds.put("mat_id_01", 125);
    materialNamesAndIds.put("mat_id_02", 126);
    materialNamesAndIds.put("mat_id_03", 127);
    materialNamesAndIds.put("foot_drakan_F_heel", 128);
    materialNamesAndIds.put("foot_drakan_M_boots", 129);
    materialNamesAndIds.put("foot_drakan_bare", 130);
    materialNamesAndIds.put("foot_robot", 131);
    materialNamesAndIds.put("mat_rainwater_6vs6Boss", 132);
    materialNamesAndIds.put("foot_npc", 133);
    materialNamesAndIds.put("mat_Medium_lava_LDF5", 134);
    materialNamesAndIds.put("mat_ab1_buildup_op_light", 135);
    materialNamesAndIds.put("mat_ab1_buildup_op_dark", 136);
    materialNamesAndIds.put("mat_poison_dmg1", 139);
    materialNamesAndIds.put("mat_poison_die", 140);
    materialNamesAndIds.put("mat_eresukigal_dmg", 141);
    materialNamesAndIds.put("mat_FFA_Hide", 142);

    materialIntentionIds.add(11);
    materialIntentionIds.add(12);
    materialIntentionIds.add(13);
    materialIntentionIds.add(14);
    materialIntentionIds.add(15);
    materialIntentionIds.add(16);
    materialIntentionIds.add(60);
    materialIntentionIds.add(61);
    materialIntentionIds.add(62);
    materialIntentionIds.add(63);
    materialIntentionIds.add(91);
    materialIntentionIds.add(92);
    materialIntentionIds.add(93);
    materialIntentionIds.add(97);
    materialIntentionIds.add(99);
    materialIntentionIds.add(100);
    materialIntentionIds.add(101);
    materialIntentionIds.add(103);
    materialIntentionIds.add(107);
    materialIntentionIds.add(108);
    materialIntentionIds.add(111);
    materialIntentionIds.add(113);
    materialIntentionIds.add(114);
    materialIntentionIds.add(115);
    materialIntentionIds.add(116);
    materialIntentionIds.add(117);
    materialIntentionIds.add(118);
    // skip skill obstacles
    //materialIntentionIds.add(121);
    //materialIntentionIds.add(122);
    //materialIntentionIds.add(123);
    //materialIntentionIds.add(124);
    materialIntentionIds.add(125);
    materialIntentionIds.add(126);
    materialIntentionIds.add(127);
    materialIntentionIds.add(132);
    materialIntentionIds.add(134);

    //new 7.0
    materialIntentionIds.add(135);
    materialIntentionIds.add(136);
    materialIntentionIds.add(139);
    materialIntentionIds.add(140);
    materialIntentionIds.add(141);
    materialIntentionIds.add(142);
  }

  public void load(InputStream inputStream) throws IOException {
    this.load(inputStream, true);
  }

  public void load(InputStream inputStream, boolean loadBones) throws IOException {
    clear();
    try (DataInputStream stream = new DataInputStream(inputStream)) {
      final byte[] signature = new byte[8];
      stream.readFully(signature);
      if (signature[0] != 0x4E || signature[1] != 0x43 || signature[2] != 0x41 || signature[3] != 0x69 || signature[4] != 0x6F || signature[5] != 0x6E
          || signature[6] != 0x00 || signature[7] != 0x00) { // NCAion
        throw new IOException("Wrong signature");
      }
      final int fileType = stream.readInt();
      if (fileType == 0xFFFF0001)
        throw new IOException("TODO: Animation Data");
      if (fileType != 0xFFFF0000) // geom data
        throw new IOException("Wrong filetype");
      stream.skip(4); // chunk version

      final int tableOffset = stream.readInt();
      position(stream, tableOffset); // move to chunks table
      final int chunksCount = stream.readInt();
      List<CgfNodeData> flatNodes = new ArrayList<>();
      for (int i = 0; i < chunksCount; i++) {
        CgfChunkHeader header = CgfChunkHeader.read(stream);
        chunkHeaders.add(header);
      }

      for (int i = 0; i < chunkHeaders.size(); i++) {
        if (chunkHeaders.get(i).chunkType == 0xCCCC000C) { // Material
          materialData.put(i, loadMaterial(chunkHeaders.get(i), stream));
          if (materialData.get(i).matType != 2) {
            materialIdx.add(i);
          }
        }
      }

      for (CgfChunkHeader chunkHeader : chunkHeaders) {
        if (chunkHeader.chunkType == 0xCCCC000B) { // Node
          flatNodes.add(loadNodeData(chunkHeader, stream));
        }
      }

      for (CgfNodeData node : flatNodes) {
        if (node.parentId != -1) {
          for (CgfNodeData nodeData : flatNodes) {
            if (nodeData.chunkId == node.parentId) {
              if (nodeData.children == null) {
                nodeData.children = new ArrayList<>();
              }
              nodeData.children.add(node);
              break;
            }
          }
        } else { // node is top level
          nodes.add(node);
        }
      }

      if (loadBones) {
        for (CgfChunkHeader chunkHeader : chunkHeaders) {
          if (chunkHeader.chunkType == 0xCCCC0005) { // BoneNameList
            loadBoneNameList(chunkHeader, stream);
          } else if (chunkHeader.chunkType == 0xCCCC0012) { // BoneInitialPos
            loadBoneInitialPos(chunkHeader, stream);
          } else if (chunkHeader.chunkType == 0xCCCC000F) { // BoneMesh
            CgfBoneMeshData cgfBoneMeshData = loadBoneMeshData(chunkHeader, stream);
            boneMeshes.put(chunkHeader.chunkId, cgfBoneMeshData);
          }
        }

        for (CgfChunkHeader chunkHeader : chunkHeaders) {
          if (chunkHeader.chunkType == 0xCCCC0003) { // BoneAnimChunk
            CgfBoneAnimData data = loadBoneAnimData(chunkHeader, stream);
            if (data != null) {
              bones.add(data);
            }
          }
        }
      }
    }
  }

  private void position(InputStream byteStream, long position) throws IOException {
    byteStream.reset();
    byteStream.skip(position);
  }

  private CgfBoneAnimData loadBoneAnimData(CgfChunkHeader header, DataInputStream stream) throws IOException {
    position(stream, header.chunkOffset);

    stream.readInt(); // chunk type
    stream.readInt(); // chunk version
    stream.readInt(); // chunk offset
    stream.readInt(); // this chunk id

    int numChildren = stream.readInt();
    CgfBoneAnimData result = new CgfBoneAnimData();
    List<CgfBoneEntity> boneEntities = new ArrayList<>();
    for (int i = 0; i < numChildren; i++) {
      int boneId = stream.readInt();
      int parentBoneId = stream.readInt();
      int childrenSize = stream.readInt();
      int nameCrc32 = stream.readInt(); // unsigned value
      byte[] nameBytes = new byte[32];
      stream.readFully(nameBytes);
      int meshId = stream.readInt();
      int flags = stream.readInt();
      stream.skip(5 * 3 * 4); // skip vector3s: min, max, spring_angle, spring_tension and damping
      stream.skip(3 * 3 * 4); // skip frame matrix3x3

      CgfBoneEntity entity = new CgfBoneEntity();
      entity.chunkdId = boneId;
      entity.parentChunkId = parentBoneId;
      entity.matrix = boneInitialPos.get(i);
      if (meshId >= 0) {
        entity.mesh = boneMeshes.get(meshId);
        if (entity.mesh != null) {
          if (boneNames != null && boneNames.length > i) {
            entity.mesh.name = boneNames[i];
          }
        }
      }
      if (entity.mesh != null) {
        if (!entity.mesh.name.toLowerCase().contains("box") && !entity.mesh.name.toLowerCase().startsWith("fx")) {
          boneEntities.add(entity);
        }
      }
    }
    if (boneEntities.size() > 0) {
      result.bones = boneEntities;
      return result;
    }
    return null;
  }

  private CgfBoneMeshData loadBoneMeshData(CgfChunkHeader header, DataInputStream stream) throws IOException {
    position(stream, header.chunkOffset);
    stream.skip(4 * 5); // skip header
    int verticesCount = stream.readInt();
    stream.skip(4); // skip uvs count
    int indicesCount = stream.readInt();
    stream.skip(4); // skip vertAnim reference

    CgfBoneMeshData result = new CgfBoneMeshData();
    result.vertices = new ArrayList<>();
    result.indices = new ArrayList<>();
    for (int i = 0; i < verticesCount; i++) {
      Vector3 vec = new Vector3();
      vec.x = stream.readFloat() / 100f;
      vec.y = stream.readFloat() / 100f;
      vec.z = stream.readFloat() / 100f;
      result.vertices.add(vec);
      stream.skip(4 * 3); // skip normal
    }

    for (int i = 0; i < indicesCount; i++) {
      MeshFace meshFace = new MeshFace();
      meshFace.v0 = stream.readInt();
      meshFace.v1 = stream.readInt();
      meshFace.v2 = stream.readInt();

      int matIdx = stream.readInt();
      if (isMaterialCollideable(materialData.get(materialIdx.get(matIdx))) || isMaterialIntention(materialData.get(materialIdx.get(matIdx)).materialId)) {
        result.indices.add(meshFace);
      }
      stream.skip(4); //skip smoothing group
    }
    return result;
  }

  private void loadBoneInitialPos(CgfChunkHeader header, DataInputStream stream) throws IOException {
    position(stream, header.chunkOffset);
    int meshRef = stream.readInt();
    int numBones = stream.readInt();
    for (int i = 0; i < numBones; i++) {
      float[] matrix = new float[3 * 4];
      matrix[0] = stream.readFloat();
      matrix[1] = stream.readFloat();
      matrix[2] = stream.readFloat();
      matrix[3] = stream.readFloat();
      matrix[4] = stream.readFloat();
      matrix[5] = stream.readFloat();
      matrix[6] = stream.readFloat();
      matrix[7] = stream.readFloat();
      matrix[8] = stream.readFloat();
      matrix[9] = stream.readFloat();
      matrix[10] = stream.readFloat();
      matrix[11] = stream.readFloat();
      boneInitialPos.add(i, matrix);
    }
  }

  private void loadBoneNameList(CgfChunkHeader header, DataInputStream stream) throws IOException {
    position(stream, header.chunkOffset);

    int nameCount = stream.readInt();
    String[] names = new String[nameCount];
    int i = 0;
    List<Byte> array = new ArrayList<Byte>();
    while (true) {
      if (i >= nameCount) {
        break;
      }
      byte read = stream.readByte();
      if (read != 0) {
        array.add(read);
      } else {
        byte[] nameBytes = new byte[array.size()];
        for (int j = 0; j < array.size(); j++) {
          nameBytes[j] = array.get(j);
        }
        names[i] = new String(nameBytes, StandardCharsets.UTF_8);
        array.clear();
        i++;
      }
    }
    this.boneNames = names;
  }

  private CgfMaterialData loadMaterial(CgfChunkHeader header,DataInputStream stream) throws IOException {
    position(stream, header.chunkOffset);

    CgfMaterialData result = new CgfMaterialData();
    stream.skip(4 * 4); // skip header
    byte[] nameBytes = new byte[128];
    stream.readFully(nameBytes);
    String name = new String(nameBytes, StandardCharsets.UTF_8).trim();
    result.matType = stream.readInt();

    int multiCount = 0;
    int tmp = stream.readInt();
    // if type == 2: next int is multicount, if its 1 next int is a color
    if (result.matType == 2) {
      multiCount = tmp;
    }
    // if matType == 1, next chunk is texture/shader info
    // if matType == 2, next chunk is zeros
    stream.skip(67 * 4 + 128 + 263 * 4 + 128 + 204 * 4);
    result.matFlags = stream.readInt();
    String matName = name;
    if (name.contains("/")) {
      String[] nameSplitted = name.split("/");
      matName = nameSplitted[nameSplitted.length - 1];
      if (matName.contains("\u0005")) {
        String[] matNameSplitted = matName.split("\u0005");
        matName = matNameSplitted[0];
      }
    }
    matName = matName.replaceAll("[^0-9a-zA-Z_]", "");
    result.materialId = getMaterialIdFor(matName);
    float collision = stream.readFloat();
    if (collision != 0f && collision != 1f) {
      throw new IOException("expected 0.0 or 1.0 for collision flag but found: " + collision);
    }
    if (collision == 1f) { // TODO: this might be wrong, materialIds 6-9 are not physical ingame yet we mark them?
      result.isCollision = true;
    }
    stream.readFloat();
    stream.readFloat();

    if (result.matType == 2 && multiCount > 0) {
      result.multiMaterialIds = new ArrayList<>();
    }
    for (int i = 0; i < multiCount; i++) {
      result.multiMaterialIds.add(stream.readInt());
    }
    return result;
  }

  public static int getMaterialIdFor(String matName) {
    return materialNamesAndIds.getOrDefault(matName, -1);
  }

  private CgfNodeData loadNodeData(CgfChunkHeader header, DataInputStream stream) throws IOException {
    position(stream, header.chunkOffset);

    CgfNodeData result = new CgfNodeData();
    int headerSize = 4 * 3;
    stream.skip( headerSize); // skip header
    result.chunkId = stream.readInt();
    byte[] nameBytes = new byte[64];
    stream.readFully(nameBytes);
    result.objectId = stream.readInt();
    long curPos = header.chunkOffset + headerSize + 4 + nameBytes.length + 4;
    result.mesh = loadMeshData(result.objectId, stream);
    result.helper = loadHelperData(result.objectId, stream);
    if (result.mesh == null && result.helper == null) {
      throw new IOException("Expected either a mesh or helper but found none");
    }
    position(stream, curPos);
    result.parentId = stream.readInt();
    stream.readInt(); // number of children
    result.materialId = stream.readInt();

    if (result.materialId != -1) {
      result.material = materialData.get(result.materialId);
    }
    boolean isGroupHead = stream.readBoolean();
    boolean isGroupMember = stream.readBoolean();
    result.isGroupHead = isGroupHead;
    stream.readShort(); // unk
    result.transform = new float[16];
    for (int i = 0; i < 16; i++) {
      result.transform[i] = stream.readFloat();
    }

    result.position = new Vector3();
    result.position.x = stream.readFloat();
    result.position.y = stream.readFloat();
    result.position.z = stream.readFloat();

    result.rotQuat = new float[4];
    result.rotQuat[0] = stream.readFloat();
    result.rotQuat[1] = stream.readFloat();
    result.rotQuat[2] = stream.readFloat();
    result.rotQuat[3] = stream.readFloat();

    result.scale = new Vector3();
    result.scale.x = stream.readFloat();
    result.scale.y = stream.readFloat();
    result.scale.z = stream.readFloat();

    result.positionControllerId = stream.readInt();
    result.rotationControllerId = stream.readInt();
    result.scaleControllerId = stream.readInt();

    return result;
  }

  private CgfMeshData loadMeshData(int objectId, DataInputStream stream) throws IOException {
    if (chunkHeaders.get(objectId).chunkType != 0xCCCC0000) {
      return null;
    }
    position(stream, chunkHeaders.get(objectId).chunkOffset);
    stream.skip(4 * 5); // skip header
    int verticesCount = stream.readInt();
    stream.skip(4); // skip uvs count
    int indicesCount = stream.readInt();
    stream.skip(4); // skip vertAnim reference

    CgfMeshData result = new CgfMeshData();
    result.vertices = new ArrayList<>();
    result.indices = new HashMap<>(); //ArrayList<>();
    for (int i = 0; i < verticesCount; i++) {
      Vector3 vec = new Vector3();
      vec.x = stream.readFloat() / 100f;
      vec.y = stream.readFloat() / 100f;
      vec.z = stream.readFloat() / 100f;
      result.vertices.add(vec);
      stream.skip(4 * 3); // skip normal
    }

    for (int i = 0; i < indicesCount; i++) {
      MeshFace meshFace = new MeshFace();
      meshFace.v0 = stream.readInt();
      meshFace.v1 = stream.readInt();
      meshFace.v2 = stream.readInt();

      int matIdx = stream.readInt();
      CgfMaterialData material = materialData.get(materialIdx.get(matIdx));
      if (isMaterialCollideable(material)) {
        if (isMaterialIntention(material.materialId)) {
          if (!result.indices.containsKey(matIdx)) {
            result.indices.put(matIdx, new ArrayList<>());
          }
          result.indices.get(matIdx).add(meshFace);
        } else {
          if (!result.indices.containsKey(0)) {
            result.indices.put(0, new ArrayList<>());
          }
          result.indices.get(0).add(meshFace);
        }
      } else if (isMaterialIntention(material.materialId)) {
        if (!result.indices.containsKey(matIdx)) {
          result.indices.put(matIdx, new ArrayList<>());
        }
        result.indices.get(matIdx).add(meshFace);
      }
      stream.skip(4); //skip smoothing group
    }
    return result;
  }

  private CgfHelperData loadHelperData(int objectId, DataInputStream stream) throws IOException {
    if (chunkHeaders.get(objectId).chunkType != 0xCCCC0001) {
      return null;
    }
    position(stream, chunkHeaders.get(objectId).chunkOffset);

    stream.skip(4 * 4); // skip header

    CgfHelperData result = new CgfHelperData();
    result.helperType = stream.readInt();
    Vector3 pos = new Vector3();
    pos.x = stream.readFloat();
    pos.y = stream.readFloat();
    pos.z = stream.readFloat();
    result.position = pos;

    return result;
  }


  public void clear() {
    chunkHeaders.clear();
    materialData.clear();
    nodes.clear();
    materialIdx.clear();
    boneNames = null;
    boneInitialPos.clear();
    bones.clear();
    boneMeshes.clear();
  }

  public void traverseNodes(List<MeshData> meshes) {
    traverseNodesInternal(0, meshes, nodes, new Vector3(0, 0, 0), new Vector3(1, 1, 1), null);
    for (CgfBoneAnimData bone : bones) {
      if (bone.bones != null && bone.bones.size() > 0) {
        traverseBonesInternal(0, meshes, bone.bones, new Vector3(0, 0, 0), new Vector3(1, 1, 1));
      }
    }
  }

  private void traverseBonesInternal(int recursiveDepth, List<MeshData> meshes, List<CgfBoneEntity> bones, Vector3 objOrigin, Vector3 parentScale) {
    for (CgfBoneEntity entity : bones) {
      if (null == entity) {
        continue;
      }
      if (entity.matrix == null) {
        continue;
      }
      Matrix4f matrix = new Matrix4f(entity.matrix[0], entity.matrix[1], entity.matrix[2], 0f,
        entity.matrix[3], entity.matrix[4], entity.matrix[5], 0f,
        entity.matrix[6], entity.matrix[7], entity.matrix[8], 0f,
        entity.matrix[9]/100f, entity.matrix[10]/100f, entity.matrix[11]/100f, 1f);

      if (entity.mesh != null) {
        MeshData meshData = new MeshData();
        meshData.vertices = new ArrayList<>(entity.mesh.vertices.size());
        meshData.vertices = transform(entity.mesh.vertices, matrix);
        meshData.indices = entity.mesh.indices;
        meshes.add(meshData);
      }
    }
  }

  private void traverseNodesInternal(int recursiveDepth, List<MeshData> meshes, List<CgfNodeData> nodes, Vector3 objOrigin, Vector3 parentScale, Matrix4f parentMatrix) {
    for (CgfNodeData node : nodes) {

      Vector3 locPos = new Vector3(node.position.x + objOrigin.x, node.position.y + objOrigin.y, node.position.z + objOrigin.z); //node.position;
      if (node.helper != null) {
        if (node.helper.helperType == 1) { // type = Dummy
          if (node.children == null) {
            continue;
          }
          float[] t2 = node.transform;
          Matrix4f tMatrix = new Matrix4f(t2[0], t2[1], t2[2], t2[3],
            t2[4], t2[5], t2[6], t2[7],
            t2[8], t2[9], t2[10], t2[11],
            0, 0, 0, 1f);
          if (node.isGroupHead) {
            locPos = new Vector3(locPos.x / node.scale.x, locPos.y / node.scale.y, locPos.z / node.scale.z);
          }
          traverseNodesInternal(recursiveDepth + 1, meshes, node.children, locPos, node.scale, tMatrix);
        }
        continue;
      }

      float[] t = node.transform;
      Matrix4f mat = new Matrix4f(t[0], t[1], t[2], t[3],
        t[4], t[5], t[6], t[7],
        t[8], t[9], t[10], t[11],
        locPos.x/100f, locPos.y/100f, locPos.z/100f, 1f);
      if (parentMatrix != null) {
        mat = mat.mult(parentMatrix);
        mat = mat.mult(Matrix4f.IDENTITY);
      }

      if (node.mesh.vertices.size() > 0 && node.mesh.indices.size() > 0) {
        if (isCollideable(node) || getCollidableMaterialId(node) > 0) {
          for (Map.Entry<Integer, ArrayList<MeshFace>> face : node.mesh.indices.entrySet()) {
            MeshData meshData = new MeshData();
            meshData.vertices = new ArrayList<>(node.mesh.vertices.size());
            meshData.vertices = transform(node.mesh.vertices, mat);
            meshData.indices = face.getValue();

            //isMaterialIntention(materialData.get(materialIdx.get(matIdx)).materialId)
            CgfMaterialData matData = materialData.get(materialIdx.get(face.getKey()));
            if (isMaterialIntention(matData.materialId)) {
              if (!isMaterialCollideable(matData) || matData.materialId >= 14 && matData.materialId <= 16) { // exception for abyss core & abyss bases
                meshData.collisionIntention = 0;
              }
              meshData.collisionIntention |= CollisionIntention.MATERIAL.getId();
              meshData.materialId = matData.materialId;
            }
            if (meshData.materialId >= 1 && meshData.materialId <= 9) {
              meshData.collisionIntention |= CollisionIntention.WALK.getId();
            }
            meshes.add(meshData);
          }
        }
      }
    }
  }

  public int getCollidableMaterialId(CgfNodeData node) {
    if (node.material == null) {
      return 0;
    }
    return getCollidableMaterialId(node.material);
  }

  public int getCollidableMaterialId(CgfMaterialData material) {
    if (isMaterialIntention(material.materialId)) {
      return material.materialId;
    }
    if (material.multiMaterialIds != null) {
      for (int matNodeId : material.multiMaterialIds) {
        if (matNodeId == -1) {
          continue;
        }
        CgfMaterialData matData = materialData.get(matNodeId);
        if (matData.matType == 0) { // unk
          continue;
        }
        if (matData.matType == 2) {
          int mId = getCollidableMaterialId(matData);
          if (mId > 0) {
            return mId;
          }
        } else if (matData.matType != 1) {
          throw new RuntimeException("Unhandled matType: " + matData.matType);
        }
        if (isMaterialIntention(matData.materialId)) {
          return matData.materialId;
        }
      }
    }
    return 0;
  }

  public static boolean isMaterialIntention(int matId) {
    return materialIntentionIds.contains(matId);
  }

  public boolean isCollideable(CgfNodeData node ) {
    if (node.material == null) {
      if (node.isGroupHead && node.children != null && node.children.size() > 0) {
        for (CgfNodeData child : node.children) {
          if (isCollideable(child)) {
            return true;
          }
        }
      }
      return false;
    }
    return isMaterialCollideable(node.material);
  }

  public boolean isMaterialCollideable(CgfMaterialData material) {
    if (material.isCollision) {
      return true;
    }
    if (material.multiMaterialIds != null) {
      for (int matId : material.multiMaterialIds) {
        if (matId == -1) {
          continue;
        }
        CgfMaterialData matData = materialData.get(matId);
        if (matData.matType == 0) { // unk
          continue;
        }
        if (matData.matType == 2) {
          if (isMaterialCollideable(matData)) {
            return true;
          }
        } else if (matData.matType != 1) {
          throw new RuntimeException("Unhandled matType: " + matData.matType);
        }
        if (matData.isCollision) {
          return true;
        }
      }
    }
    return false;
  }


  private List<Vector3> transform(List<Vector3> source, Matrix4f m) {
    List<Vector3> result = new ArrayList<>(source.size());
    for (int i = 0; i < source.size(); i++) {
      Vector3 vec = source.get(i);
      result.add(new Vector3((float) ((double) vec.x * (double) m.m11 + (double) vec.y * (double) m.m21 + (double) vec.z * (double) m.m31 + (double) m.m41),
        (float) ((double) vec.x * (double) m.m12 + (double) vec.y * (double) m.m22 + (double) vec.z * (double) m.m32 + (double) m.m42),
        (float) ((double) vec.x * (double) m.m13 + (double) vec.y * (double) m.m23 + (double) vec.z * (double) m.m33 + (double) m.m43)));
    }
    return result;
  }

  // creates a new cgf at the specified time in ticks.
  // uses the controllers to modify the original transforms.
  // this loads exact keyframe values, curves are not interpolated.
  public CgfLoader cloneAtTime(int time, InputStream inputStream) throws IOException {
    // TODO - check loop type.
    // TODO - check time is not greater than global range - need to load timing chunk.
    // TODO - validate keyframe start times are ascending and within global range.
    // TODO - validate controller type. TBC3 for pos, scale, TBCQ for rot, others unexpected...
    // TODO - validate cga vs cgf... some doors have .cgf extension...
    CgfLoader clone = new CgfLoader();
    clone.load(inputStream);
    try (DataInputStream stream = new DataInputStream(inputStream)) {
      for (CgfNodeData node : clone.nodes) {
        if (!isCollideable(node)) {
          continue;
        }
        if (node.positionControllerId != -1) {
          List<CgfControllerData> cd = clone.getControllerData(node.positionControllerId, 9, stream); // TBC3
          for (int i = cd.size() - 1; i >=0; i--) {
            if (time >= cd.get(i).time) {
              CgfControllerData d = cd.get(i);
              Vector3 pos = new Vector3(d.params[0], d.params[1], d.params[2]);
              node.position = pos;
              break;
            }
          }
        }

        if (node.rotationControllerId != -1) {
          List<CgfControllerData> cd = clone.getControllerData(node.rotationControllerId, 10, stream); // TBCQ
          // BUG: doors in theo lab have nodes with broken rotations.
          // see librarydoor_04d controller=12 values: {0, 45 deg, 45 deg, 180 deg}
          // they appear in a 1-key set, so ignoring those for now.

          /*
            Quaternion rot = new Quaternion(node.rotQuat[0], node.rotQuat[1], node.rotQuat[2], node.rotQuat[3]);
            int curTime = 0;
            for (int i = 0; i < cd.size(); i++) {
              CgfControllerData d = cd.get(i);
              if (!(Math.abs(d.params[0] - 1) < 0.00001 &&
                  Math.abs(d.params[1]) < 0.00001 &&
                  Math.abs(d.params[2]) < 0.00001 &&
                  Math.abs(d.params[3]) < 0.00001)) {
                rot = Quaternion.mult(rot, Quaternion.createFromAxisAngle(new Vector3(d.params[0], d.params[1], d.params[2]), d.params[3]));
              }
              if ((i < cd.size() - 1 && time <= curTime + cd.get(i).time) || i == cd.size() - 1) {
                node.rotQuat[0] = rot.x;
                node.rotQuat[1] = rot.y;
                node.rotQuat[2] = rot.z;
                node.rotQuat[3] = rot.w;
                break;
              }
            }
            */
          for (int i = cd.size() - 1; i >= 0; i--) {
            if (time >= cd.get(i).time) {
              CgfControllerData d = cd.get(i);
              Quaternion quat = Quaternion.createFromAxisAngle(new Vector3(d.params[0], d.params[1], d.params[2]), d.params[3]);
              node.rotQuat[0] = quat.x;
              node.rotQuat[1] = quat.y;
              node.rotQuat[2] = quat.z;
              node.rotQuat[3] = quat.w;
              break;
            }
          }
        }

        if (node.scaleControllerId != -1) {
          List<CgfControllerData> cd = clone.getControllerData(node.scaleControllerId, 9, stream); // TBC3
          for (CgfControllerData d : cd) {
            if (d.params[0] != 1 || d.params[1] != 1 || d.params[2] != 1) {
              node.scale.x *= d.params[0];
              node.scale.y *= d.params[1];
              node.scale.z *= d.params[2];
            }
          }
        }
      }
    }
    return clone;
  }

  /**
   * ControllerType:
   * NONE = 0,
   * CRYBONE = 1,
   * LINEAR1 = 2,
   * LINEAR3 = 3,
   * LINEARQ = 4,
   * BEZIER1 = 5,
   * BEZIER3 = 6,
   * BEZIERQ = 7,
   * // TCB = tension-continuity-bias. after value is 5 floats: t,c,b,ein,eout
   * TCB1 = 8,  // 1 float
   * TCB3 = 9,  // 3 values xyz, 1+8*4 bytes per frame
   * TCBQ = 10, // 4 value rotation, 1+9*4 bytes per frame
   * BSPLINE2O = 11, //2-byte fixed values, open
   * BSPLINE1O = 12, //1-byte fixed values, open
   * BSPLINE2C = 13, //2-byte fixed values, closed
   * BSPLINE1C = 14, //1-byte fixed values, closed
   * CONST = 15
   */
  private List<CgfControllerData> getControllerData(int idx, int type, DataInputStream stream) throws IOException {
    if (idx < 0 || idx > chunkHeaders.size()) {
      throw new IndexOutOfBoundsException();
    }
    if (chunkHeaders.get(idx).chunkType != 0xCCCC000D) { // ChunkType Controller
      return null;
    }
    position(stream, chunkHeaders.get(idx).chunkOffset);
    stream.skip(4 * 4); // skip header
    int controllerType = stream.readInt();
    if (controllerType != type && (controllerType != 6 && type == 9)) { // 9 = TCB3
      throw new IOException("Unexpected controller type. Found: " + controllerType + " expected: " + type);
    }
    int numKeys = stream.readInt();
    stream.skip(4); // flags
    int controllerId = stream.readInt();

    if (numKeys < 1) {
      throw new IOException("Unexpected key amount.");
    }
    if (controllerId != idx) {
      throw new IOException("Unexpected controller id. Found: " + controllerId + " expected: " + idx);
    }

    List<CgfControllerData> result = new ArrayList<>();
    for (int i = 0; i < numKeys; i++) {
      CgfControllerData data = new CgfControllerData();
      data.time = stream.readInt();
      // TODO - each key type uses a different struct. this is good enough for now...
      float[] params = new float[9];
      for (int j = 0; j < 8; j++) {
        params[j] = stream.readFloat();
      }
      if (controllerType == 10) { // TCBQ
        params[8] = stream.readFloat();
      }
      data.params = params;
      result.add(data);
    }
    return result;
  }
}
