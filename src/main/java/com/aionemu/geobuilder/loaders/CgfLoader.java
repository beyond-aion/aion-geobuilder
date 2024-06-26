package com.aionemu.geobuilder.loaders;

import com.aionemu.geobuilder.cgfData.*;
import com.aionemu.geobuilder.meshData.CollisionIntention;
import com.aionemu.geobuilder.meshData.MeshData;
import com.aionemu.geobuilder.meshData.MeshFace;
import com.aionemu.geobuilder.pakaccessor.PakFile;
import com.aionemu.geobuilder.utils.Matrix4f;
import com.aionemu.geobuilder.utils.Quaternion;
import com.aionemu.geobuilder.utils.Vector3;
import com.aionemu.geobuilder.utils.XmlParser;
import org.jdom2.Element;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

/**
 * File types:<br>
 * .cgf (Crytek Geometry Format)<br>
 * The .cgf file is created in the 3D application and contain geometry data (grouped triangles, vertex attributes like tangent space or vertex color, optional physics data, and optional spherical harmonics data).<br>
 * <br>
 * .cga (Crytek Geometry Animation)<br>
 * The .cga file is a pivot based animated hard body geometry data. It only supports directly linked objects and does not support skeleton based animation (bone animation) with weighted vertices. Works together with .anm files, which are not used in the Aion client, as all .cga files contain only a single animation.<br>
 * <br>
 * <a href="https://web.archive.org/web/20131104034748/http://docs.cryengine.com:80/display/SDKDOC3/Art+Asset+File+Types">Source</a>
 */
public class CgfLoader {

  private static final byte[] SIGNATURE = "NCAion\0\0".getBytes();
  private static final Map<String, Integer> materialNamesAndIds = new HashMap<>();
  private static final Set<Integer> useSkillMaterialIds = new HashSet<>();
  private final List<CgfChunkHeader> chunkHeaders = new ArrayList<>();
  private final Map<Integer, CgfMaterialData> materialDataByChunkId = new HashMap<>();
  private final List<CgfNodeData> nodes = new ArrayList<>();
  private final List<Integer> materialIdx = new ArrayList<>();
  private String[] boneNames;
  private final List<float[]> boneInitialPos = new ArrayList<>();
  private final List<CgfBoneAnimData> bones = new ArrayList<>();
  private final HashMap<Integer, CgfBoneMeshData> boneMeshes = new HashMap<>();

  public static int loadMaterials(Path clientPath) throws IOException {
    try (PakFile pakFile = PakFile.open(clientPath.resolve("Data/Material/Material.pak"))) {
      Element rootElement = XmlParser.parse(pakFile.unpak("materials.xml")).getRootElement();
      for (Element material : rootElement.getChildren("material")) {
        String materialName = material.getChildText("material_name");
        int materialId = Integer.parseInt(material.getChildText("id"));
        if (materialNamesAndIds.putIfAbsent(materialName, materialId) != null)
          throw new IllegalArgumentException(materialName + " is already registered");
        String materialSkillName = material.getChildText("skill_name");
        if (materialSkillName != null || materialName.equalsIgnoreCase("mat_abyss_castle_shield"))
          useSkillMaterialIds.add(materialId);
      }
    }
    if (useSkillMaterialIds.isEmpty()) {
      throw new IllegalStateException("Found no skill materials (materials.xml structure changed?)");
    }
    return materialNamesAndIds.size();
  }

  public static int getMaterialId(String matName) {
    return materialNamesAndIds.getOrDefault(matName, -1);
  }

  public void load(ByteBuffer bb) throws IOException {
    load(bb, true);
  }

  /**
   * <a href="https://github.com/niftools/pyffi/blob/7f4404dbb8cf832dadd4b3150819340b8764f9b0/pyffi/formats/cgf/cgf.xml">File format info</a>
   */
  public void load(ByteBuffer bb, boolean loadBones) throws IOException {
    clear();
    byte[] signature = new byte[SIGNATURE.length];
    bb.get(signature);
    if (!Arrays.equals(SIGNATURE, signature))
      throw new IOException("Wrong signature: " + new String(signature));
    int fileType = bb.getInt();
    if (fileType == 0xFFFF0001)
      throw new IOException("Loading animation data is currently not implemented");
    if (fileType != 0xFFFF0000) // geom data
      throw new IOException("Unsupported file type " + fileType);
    int fileVersion = bb.getInt();
    if (fileVersion != 0x750)
      throw new IOException("Unsupported file version " + fileVersion);
    int tableOffset = bb.getInt();
    bb.position(tableOffset); // move to chunks table
    int chunksCount = bb.getInt();
    List<CgfNodeData> flatNodes = new ArrayList<>();
    for (int i = 0; i < chunksCount; i++) {
      CgfChunkHeader header = CgfChunkHeader.read(bb);
      chunkHeaders.add(header);
    }

    for (CgfChunkHeader chunkHeader : chunkHeaders) {
      if (chunkHeader.chunkType == 0xCCCC000C) { // Material
        CgfMaterialData material = loadMaterial(chunkHeader, bb);
        materialDataByChunkId.put(chunkHeader.chunkId, material);
        if (material.matType != 2) { // skip multiMaterialIds since face indices only reference single materials
          materialIdx.add(chunkHeader.chunkId);
        }
      }
    }

    for (CgfChunkHeader chunkHeader : chunkHeaders) {
      if (chunkHeader.chunkType == 0xCCCC000B) { // Node
        flatNodes.add(loadNodeData(chunkHeader, bb));
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
          loadBoneNameList(chunkHeader, bb);
        } else if (chunkHeader.chunkType == 0xCCCC0012) { // BoneInitialPos
          loadBoneInitialPos(chunkHeader, bb);
        } else if (chunkHeader.chunkType == 0xCCCC000F) { // BoneMesh
          CgfBoneMeshData cgfBoneMeshData = loadBoneMeshData(chunkHeader, bb);
          boneMeshes.put(chunkHeader.chunkId, cgfBoneMeshData);
        }
      }

      for (CgfChunkHeader chunkHeader : chunkHeaders) {
        if (chunkHeader.chunkType == 0xCCCC0003) { // BoneAnimChunk
          CgfBoneAnimData data = loadBoneAnimData(chunkHeader, bb);
          if (data != null) {
            bones.add(data);
          }
        }
      }
    }
  }

  private CgfBoneAnimData loadBoneAnimData(CgfChunkHeader header, ByteBuffer bb) {
    bb.position(header.chunkOffset);

    bb.getInt(); // chunk type
    bb.getInt(); // chunk version
    bb.getInt(); // chunk offset
    bb.getInt(); // this chunk id

    int numChildren = bb.getInt();
    CgfBoneAnimData result = new CgfBoneAnimData();
    List<CgfBoneEntity> boneEntities = new ArrayList<>();
    for (int i = 0; i < numChildren; i++) {
      int boneId = bb.getInt();
      int parentBoneId = bb.getInt();
      int childrenSize = bb.getInt();
      int nameCrc32 = bb.getInt(); // unsigned value
      byte[] nameBytes = new byte[32];
      bb.get(nameBytes);
      int meshId = bb.getInt();
      int flags = bb.getInt();
      bb.position(bb.position() + 5 * 3 * 4); // skip vector3s: min, max, spring_angle, spring_tension and damping
      bb.position(bb.position() + 3 * 3 * 4); // skip frame matrix3x3

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
          String meshNameLowercase = entity.mesh.name.toLowerCase();
          if (!meshNameLowercase.startsWith("fx") && !meshNameLowercase.contains("box")) {
            boneEntities.add(entity);
          }
        }
      }
    }
    if (boneEntities.size() > 0) {
      result.bones = boneEntities;
      return result;
    }
    return null;
  }

  private CgfBoneMeshData loadBoneMeshData(CgfChunkHeader header, ByteBuffer bb) {
    bb.position(header.chunkOffset);
    bb.position(bb.position() + 4 * 5); // skip header
    int verticesCount = bb.getInt();
    bb.position(bb.position() + 4); // skip uvs count
    int indicesCount = bb.getInt();
    bb.position(bb.position() + 4); // skip vertAnim reference

    CgfBoneMeshData result = new CgfBoneMeshData();
    result.vertices = new ArrayList<>();
    result.indices = new ArrayList<>();
    for (int i = 0; i < verticesCount; i++) {
      Vector3 vec = new Vector3();
      vec.x = bb.getFloat() / 100f;
      vec.y = bb.getFloat() / 100f;
      vec.z = bb.getFloat() / 100f;
      result.vertices.add(vec);
      bb.position(bb.position() + 4 * 3); // skip normal
    }

    for (int i = 0; i < indicesCount; i++) {
      MeshFace meshFace = new MeshFace();
      meshFace.v0 = bb.getInt();
      meshFace.v1 = bb.getInt();
      meshFace.v2 = bb.getInt();

      int matIdx = bb.getInt();
      CgfMaterialData material = materialDataByChunkId.get(materialIdx.get(matIdx));
      if (isMaterialCollideable(material) || isUseSkillMaterial(material.materialId)) {
        result.indices.add(meshFace);
      }
      bb.position(bb.position() + 4); //skip smoothing group
    }
    return result;
  }

  private void loadBoneInitialPos(CgfChunkHeader header, ByteBuffer bb) {
    bb.position(header.chunkOffset);
    int meshRef = bb.getInt();
    int numBones = bb.getInt();
    for (int i = 0; i < numBones; i++) {
      float[] matrix = new float[3 * 4];
      for (int j = 0; j < matrix.length; j++) {
        matrix[j] = bb.getFloat();
      }
      boneInitialPos.add(i, matrix);
    }
  }

  private void loadBoneNameList(CgfChunkHeader header, ByteBuffer bb) {
    bb.position(header.chunkOffset);

    int nameCount = bb.getInt();
    String[] names = new String[nameCount];
    int i = 0;
    List<Byte> array = new ArrayList<>();
    while (i < nameCount) {
      byte read = bb.get();
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

  private CgfMaterialData loadMaterial(CgfChunkHeader header, ByteBuffer bb) throws IOException {
    bb.position(header.chunkOffset);

    CgfMaterialData result = new CgfMaterialData();
    bb.position(bb.position() + 4 * 4); // skip header
    byte[] nameBytes = new byte[128];
    bb.get(nameBytes);
    String name = new String(nameBytes, StandardCharsets.UTF_8);
    name = name.substring(0, name.indexOf('\0'));
    result.matType = bb.getInt();

    int multiCount = 0;
    int tmp = bb.getInt();
    // if type == 2: next int is multicount, if its 1 next int is a color
    if (result.matType == 2) {
      multiCount = tmp;
    }
    // if matType == 1, next chunk is texture/shader info
    // if matType == 2, next chunk is zeros
    bb.position(bb.position() + 67 * 4 + 128 + 263 * 4 + 128 + 204 * 4);
    result.matFlags = bb.getInt();
    int splitIndex = name.lastIndexOf('/');
    String matName = name;
    if (splitIndex != -1) {
      matName = name.substring(splitIndex + 1);
    }
    result.materialId = getMaterialId(matName);
    float collision = bb.getFloat();
    if (collision != 0f && collision != 1f) {
      throw new IOException("expected 0.0 or 1.0 for collision flag but found: " + collision);
    }
    if (collision == 1f) { // TODO: this might be wrong, materialIds 6-9 are not physical ingame yet we mark them?
      result.isCollision = true;
    }
    bb.getFloat();
    bb.getFloat();

    if (multiCount > 0) {
      result.multiMaterialChunkIds = new int[multiCount];
      for (int i = 0; i < multiCount; i++) {
        result.multiMaterialChunkIds[i] = bb.getInt();
      }
    }
    return result;
  }

  private CgfNodeData loadNodeData(CgfChunkHeader header, ByteBuffer bb) throws IOException {
    bb.position(header.chunkOffset);

    CgfNodeData result = new CgfNodeData();
    int headerSize = 4 * 3;
    bb.position(bb.position() + headerSize); // skip header
    result.chunkId = bb.getInt();
    byte[] nameBytes = new byte[64];
    bb.get(nameBytes);
    result.objectId = bb.getInt();
    int curPos = bb.position();
    result.mesh = loadMeshData(result.objectId, bb);
    result.helper = loadHelperData(result.objectId, bb);
    if (result.mesh == null && result.helper == null) {
      throw new IOException("Expected either a mesh or helper but found none");
    }
    bb.position(curPos);
    result.parentId = bb.getInt();
    bb.getInt(); // number of children
    int materialChunkId = bb.getInt();
    if (materialChunkId != -1) {
      result.material = materialDataByChunkId.get(materialChunkId);
    }
    result.isGroupHead = bb.get() != 0;
    boolean isGroupMember = bb.get() != 0;
    bb.getShort(); // unk
    result.transform = new float[16];
    for (int i = 0; i < 16; i++) {
      result.transform[i] = bb.getFloat();
    }

    result.position = new Vector3();
    result.position.x = bb.getFloat();
    result.position.y = bb.getFloat();
    result.position.z = bb.getFloat();

    result.rotQuat = new float[4];
    result.rotQuat[0] = bb.getFloat();
    result.rotQuat[1] = bb.getFloat();
    result.rotQuat[2] = bb.getFloat();
    result.rotQuat[3] = bb.getFloat();

    result.scale = new Vector3();
    result.scale.x = bb.getFloat();
    result.scale.y = bb.getFloat();
    result.scale.z = bb.getFloat();

    result.positionControllerId = bb.getInt();
    result.rotationControllerId = bb.getInt();
    result.scaleControllerId = bb.getInt();

    return result;
  }

  private CgfMeshData loadMeshData(int objectId, ByteBuffer bb) {
    if (chunkHeaders.get(objectId).chunkType != 0xCCCC0000) {
      return null;
    }
    bb.position(chunkHeaders.get(objectId).chunkOffset);
    bb.position(bb.position() + 4 * 5); // skip header
    int verticesCount = bb.getInt();
    bb.position(bb.position() + 4); // skip uvs count
    int indicesCount = bb.getInt();
    bb.position(bb.position() + 4); // skip vertAnim reference

    CgfMeshData result = new CgfMeshData();
    result.vertices = new ArrayList<>(verticesCount);
    result.indices = new HashMap<>();
    for (int i = 0; i < verticesCount; i++) {
      Vector3 vec = new Vector3();
      vec.x = bb.getFloat() / 100f;
      vec.y = bb.getFloat() / 100f;
      vec.z = bb.getFloat() / 100f;
      result.vertices.add(vec);
      bb.position(bb.position() + 4 * 3); // skip normal
    }

    for (int i = 0; i < indicesCount; i++) {
      MeshFace meshFace = new MeshFace();
      meshFace.v0 = bb.getInt();
      meshFace.v1 = bb.getInt();
      meshFace.v2 = bb.getInt();

      int matIdx = bb.getInt();
      CgfMaterialData material = materialDataByChunkId.get(materialIdx.get(matIdx));
      if (isMaterialCollideable(material) || isUseSkillMaterial(material.materialId)) {
        if (isUseSkillMaterial(material.materialId) || material.materialId >= 6 && material.materialId <= 9) {
          result.indices.computeIfAbsent(matIdx, k -> new ArrayList<>()).add(meshFace);
        } else {
          result.indices.computeIfAbsent(0, k -> new ArrayList<>()).add(meshFace);
        }
      }
      bb.position(bb.position() + 4); //skip smoothing group
    }
    return result;
  }

  private CgfHelperData loadHelperData(int objectId, ByteBuffer bb) {
    if (chunkHeaders.get(objectId).chunkType != 0xCCCC0001) {
      return null;
    }
    bb.position(chunkHeaders.get(objectId).chunkOffset);

    bb.position(bb.position() + 4 * 4); // skip header

    CgfHelperData result = new CgfHelperData();
    result.helperType = bb.getInt();
    Vector3 pos = new Vector3();
    pos.x = bb.getFloat();
    pos.y = bb.getFloat();
    pos.z = bb.getFloat();
    result.position = pos;

    return result;
  }


  public void clear() {
    chunkHeaders.clear();
    materialDataByChunkId.clear();
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
      for (CgfBoneEntity entity : bone.bones) {
        Matrix4f matrix = new Matrix4f(entity.matrix[0], entity.matrix[1], entity.matrix[2], 0f,
          entity.matrix[3], entity.matrix[4], entity.matrix[5], 0f,
          entity.matrix[6], entity.matrix[7], entity.matrix[8], 0f,
          entity.matrix[9] / 100f, entity.matrix[10] / 100f, entity.matrix[11] / 100f, 1f);

        if (entity.mesh != null) {
          MeshData meshData = new MeshData();
          meshData.vertices = transform(entity.mesh.vertices, matrix);
          meshData.faces = entity.mesh.indices;
          meshes.add(meshData);
        }
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
        locPos.x / 100f, locPos.y / 100f, locPos.z / 100f, 1f);
      if (parentMatrix != null) {
        mat = mat.mult(parentMatrix);
        mat = mat.mult(Matrix4f.IDENTITY);
      }

      if (node.mesh.indices.size() > 0 && (isCollideable(node) || getCollidableMaterialId(node) > 0)) {
        List<Vector3> vertices = transform(node.mesh.vertices, mat);
        for (Map.Entry<Integer, ArrayList<MeshFace>> face : node.mesh.indices.entrySet()) {
          MeshData meshData = new MeshData();
          meshData.vertices = vertices;
          meshData.faces = face.getValue();

          CgfMaterialData matData = materialDataByChunkId.get(materialIdx.get(face.getKey()));
          if (isUseSkillMaterial(matData.materialId)) {
            if (!isMaterialCollideable(matData) || matData.materialId >= 14 && matData.materialId <= 16) { // exception for abyss core & abyss bases
              meshData.collisionIntention = 0;
            }
            meshData.collisionIntention |= CollisionIntention.MATERIAL.getId();
            meshData.materialId = matData.materialId;
          }
          if (matData.materialId >= 1 && matData.materialId <= 9) {
            meshData.collisionIntention = 0;
            if (matData.materialId <= 5) {
              meshData.collisionIntention |= CollisionIntention.PHYSICAL_SEE_THROUGH.getId(); // players and npcs cant move but see through
            }
            meshData.collisionIntention |= CollisionIntention.WALK.getId(); // npcs cannot walk through
          }
          meshes.add(meshData);
        }
      }
    }
  }

  public int getCollidableMaterialId(CgfNodeData node) {
    return node.material == null ? 0 : getCollidableMaterialId(node.material);
  }

  public int getCollidableMaterialId(CgfMaterialData material) {
    if (isUseSkillMaterial(material.materialId)) {
      return material.materialId;
    }
    if (material.multiMaterialChunkIds != null) {
      for (int chunkId : material.multiMaterialChunkIds) {
        if (chunkId == -1) {
          continue;
        }
        CgfMaterialData matData = materialDataByChunkId.get(chunkId);
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
        if (isUseSkillMaterial(matData.materialId)) {
          return matData.materialId;
        }
      }
    }
    return 0;
  }

  public static boolean isUseSkillMaterial(int matId) {
    return useSkillMaterialIds.contains(matId);
  }

  public boolean isCollideable(CgfNodeData node) {
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
    if (material.multiMaterialChunkIds != null) {
      for (int chunkId : material.multiMaterialChunkIds) {
        if (chunkId == -1) {
          continue;
        }
        CgfMaterialData matData = materialDataByChunkId.get(chunkId);
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
    for (Vector3 vec : source) {
      result.add(new Vector3((float) ((double) vec.x * (double) m.m11 + (double) vec.y * (double) m.m21 + (double) vec.z * (double) m.m31 + (double) m.m41),
        (float) ((double) vec.x * (double) m.m12 + (double) vec.y * (double) m.m22 + (double) vec.z * (double) m.m32 + (double) m.m42),
        (float) ((double) vec.x * (double) m.m13 + (double) vec.y * (double) m.m23 + (double) vec.z * (double) m.m33 + (double) m.m43)));
    }
    return result;
  }

  // creates a new cgf at the specified time in ticks.
  // uses the controllers to modify the original transforms.
  // this loads exact keyframe values, curves are not interpolated.
  public CgfLoader cloneAtTime(int time, ByteBuffer bb) throws IOException {
    // TODO - check loop type.
    // TODO - check time is not greater than global range - need to load timing chunk.
    // TODO - validate keyframe start times are ascending and within global range.
    // TODO - validate controller type. TBC3 for pos, scale, TBCQ for rot, others unexpected...
    // TODO - validate cga vs cgf... some doors have .cgf extension...
    CgfLoader clone = new CgfLoader();
    bb.position(0);
    clone.load(bb);
    for (CgfNodeData node : clone.nodes) {
      if (!isCollideable(node)) {
        continue;
      }
      if (node.positionControllerId != -1) {
        List<CgfControllerData> cd = clone.getControllerData(node.positionControllerId, 9, bb); // TBC3
        for (int i = cd.size() - 1; i >= 0; i--) {
          if (time >= cd.get(i).time) {
            CgfControllerData d = cd.get(i);
            Vector3 pos = new Vector3(d.params[0], d.params[1], d.params[2]);
            node.position = pos;
            break;
          }
        }
      }

      if (node.rotationControllerId != -1) {
        List<CgfControllerData> cd = clone.getControllerData(node.rotationControllerId, 10, bb); // TBCQ
        // FIXME doors in theo lab have nodes with broken rotations.
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
        List<CgfControllerData> cd = clone.getControllerData(node.scaleControllerId, 9, bb); // TBC3
        for (CgfControllerData d : cd) {
          if (d.params[0] != 1 || d.params[1] != 1 || d.params[2] != 1) {
            node.scale.x *= d.params[0];
            node.scale.y *= d.params[1];
            node.scale.z *= d.params[2];
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
  private List<CgfControllerData> getControllerData(int idx, int type, ByteBuffer bb) throws IOException {
    if (idx < 0 || idx > chunkHeaders.size()) {
      throw new IndexOutOfBoundsException();
    }
    if (chunkHeaders.get(idx).chunkType != 0xCCCC000D) { // ChunkType Controller
      return null;
    }
    bb.position(chunkHeaders.get(idx).chunkOffset);
    bb.position(bb.position() + 4 * 4); // skip header
    int controllerType = bb.getInt();
    if (controllerType != type && (controllerType != 6 && type == 9)) { // 9 = TCB3
      throw new IOException("Unexpected controller type. Found: " + controllerType + " expected: " + type);
    }
    int numKeys = bb.getInt();
    bb.position(bb.position() + 4); // flags
    int controllerId = bb.getInt();

    if (numKeys < 1) {
      throw new IOException("Unexpected key amount.");
    }
    if (controllerId != idx) {
      throw new IOException("Unexpected controller id. Found: " + controllerId + " expected: " + idx);
    }

    List<CgfControllerData> result = new ArrayList<>();
    for (int i = 0; i < numKeys; i++) {
      CgfControllerData data = new CgfControllerData();
      data.time = bb.getInt();
      // TODO - each key type uses a different struct. this is good enough for now...
      float[] params = new float[9];
      for (int j = 0; j < 8; j++) {
        params[j] = bb.getFloat();
      }
      if (controllerType == 10) { // TCBQ
        params[8] = bb.getFloat();
      }
      data.params = params;
      result.add(data);
    }
    return result;
  }
}
