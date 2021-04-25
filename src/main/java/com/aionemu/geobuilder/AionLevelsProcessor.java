package com.aionemu.geobuilder;

import com.aionemu.geobuilder.entries.*;
import com.aionemu.geobuilder.loaders.BrushLstLoader;
import com.aionemu.geobuilder.loaders.CgfLoader;
import com.aionemu.geobuilder.loaders.EntityLoader;
import com.aionemu.geobuilder.loaders.ObjectsLstLoader;
import com.aionemu.geobuilder.meshData.MeshData;
import com.aionemu.geobuilder.meshData.MeshFace;
import com.aionemu.geobuilder.pakaccessor.PakFile;
import com.aionemu.geobuilder.utils.BinaryXmlDecoder;
import com.aionemu.geobuilder.utils.Matrix4f;
import com.aionemu.geobuilder.utils.Vector3;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FileConverter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.*;
import java.util.stream.Collectors;

public class AionLevelsProcessor {

  @Parameter(description = "<client path>", converter = FileConverter.class, required = true)
  protected File clientPath;

  @Parameter(names = "-lvl", description = "Level-Id to generate geodata for e.g. 110010000. default: all levels. ", order = 1)
  protected String levelId = null;

  @Parameter(names = "-w", description = "Path to server world_maps.xml or client WorldId.xml. default: client WorldId.xml", order = 2)
  protected File worldIdPath;

  @Parameter(names = "-o", description = "Path to the output folder", order = 3)
  protected File outPath = new File("./out");

  @Parameter(names = "-v", description = "Activate verbose logging", order = 4)
  protected boolean verbose;

  private static final Logger log = Logger.getLogger("GeoBuilder");
  private static final Set<String> ignoredCgfs = new HashSet<>();

  static {
    System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tT [%4$s] %5$s%6$s%n");
    log.setUseParentHandlers(false);
    StreamHandler consoleHandler = new StreamHandler(System.out, new SimpleFormatter() {
      @Override
      public String format(LogRecord record) {
        boolean isTemporaryLine = record.getMessage().endsWith("\r");
        String msg = super.format(record);
        if (isTemporaryLine) // remove newline (cursor will be at the start and next print will override this message)
          msg = msg.substring(0, msg.length() - System.lineSeparator().length());
        if (record.getLevel() == Level.SEVERE)
          return "\u001B[31m" + msg + "\u001b[0m";
        if (record.getLevel() == Level.WARNING)
          return "\u001B[33m" + msg + "\u001b[0m";
        if (record.getLevel().intValue() < Level.INFO.intValue())
          return "\u001B[37m" + msg + "\u001b[0m";
        return msg;
      }
    }) {
      @Override
      public synchronized void publish(LogRecord record) {
        super.publish(record);
        flush();
      }
    };
    consoleHandler.setLevel(Level.ALL);
    log.addHandler(consoleHandler);

    ignoredCgfs.add("objects/npc/level_object/idyun_bridge/idyun_bridge_01a.cga");
    ignoredCgfs.add("levels/common/light/structures/props/exterior/pr_l_caldron.cgf");
  }

  private static final int H32_POINT_SIZE = 3;

  private final Set<String> requiredCgfs = ConcurrentHashMap.newKeySet();
  private final Set<String> requiredDoorCgas = ConcurrentHashMap.newKeySet();

  private final Set<String> processedCgfs = ConcurrentHashMap.newKeySet();
  private final List<String> missingCgfs = new ArrayList<>();
  private final Set<String> emptyCgfs = ConcurrentHashMap.newKeySet();

  protected void process() {
    log.setLevel(verbose ? Level.ALL : Level.INFO);
    long time = System.currentTimeMillis();
    try {
      log.info("Generating available levels list …");
      List<LevelData> levels = findLevelsToProcess();
      if (!levels.isEmpty()) {
        if (!outPath.mkdirs())
          Files.list(outPath.toPath()).filter(p -> p.toString().endsWith(".geo") || p.toString().endsWith(".mesh")).forEach(p -> p.toFile().delete());

        log.info("Generating available house addresses …");
        Map<String, Integer> houseAdresses = generateHouseAddressList();

        log.info("Processing levels …");
        processLevels(levels, houseAdresses);

        log.info("Collecting mesh file paths …");
        List<File> meshPaks = collectMeshFilePaths();

        log.info("Generating mesh file …");
        createMeshes(outPath, meshPaks);

        log.info("Generating geo files …");
        createGeoFiles(outPath, levels);
      }
    } catch (Throwable t) {
      log.log(Level.SEVERE, "", t);
    }
    time = (System.currentTimeMillis() - time) / 1000;
    log.info("Processing time: " + (time / 60) + "m " + (time % 60) + "s");
  }

  private List<LevelData> findLevelsToProcess() throws JDOMException, IOException {
    // Read world_maps.xml or WorldId.xml and find Levels to process
    InputStream worldIdFile = worldIdPath == null ? getClientWorldIdFile() : new FileInputStream(worldIdPath);
    String worldIdXml = worldIdPath == null ? "client WorldId.xml" : worldIdPath.getName();

    Path levelsRootFolder = Paths.get(clientPath.getPath(), "Levels");
    Map<String, File> levelsByName = Files.list(levelsRootFolder)
        .map(Path::toFile)
        .filter(file -> file.isDirectory() && !file.getName().equalsIgnoreCase("common"))
        .collect(Collectors.toMap(file -> file.getName().toLowerCase(), file -> file));
    log.fine("Found " + levelsByName.size() + " levels in " + levelsRootFolder);

    // read client maps
    Document document = new SAXBuilder().build(worldIdFile);
    Element rootNode = document.getRootElement();
    boolean clientXml = rootNode.getName().equalsIgnoreCase("world_id");
    List<Element> worldIdXmlLevels = clientXml ? rootNode.getChildren("data") : rootNode.getChildren("map");
    log.fine("Validating " + worldIdXmlLevels.size() + " levels referenced in " + worldIdXml + " …");

    List<LevelData> levels = new ArrayList<>();
    // buggy client files are normal, only log warning when we generate from server file or -lvl parameter
    Level logLevel = this.levelId == null && clientXml ? Level.FINE : Level.WARNING;
    for (Element element : worldIdXmlLevels) {
      String levelId = element.getAttributeValue("id");
      String levelName = clientXml ? element.getText() : element.getAttributeValue("cName");
      File levelClientFolder = levelsByName.remove(levelName.toLowerCase());
      if (this.levelId == null || this.levelId.equalsIgnoreCase(levelId)) {
        if (levelClientFolder == null) {
          log.log(logLevel, "Ignoring " + levelName + ": Folder missing in " + levelsRootFolder);
        } else {
          LevelData level = new LevelData(levelId, levelName, levelClientFolder);
          if (level.hasPak())
            levels.add(level);
          else
            log.log(logLevel, "Ignoring " + levelName + ": Level.pak missing");
        }
      }
    }
    if (!levelsByName.isEmpty()) {
      log.fine(levelsByName.size() + " levels from " + levelsRootFolder + " were not referenced in " + worldIdXml + ": " + levelsByName.values().stream().map(File::getName).sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.joining(", ")));
    }
    String num = this.levelId == null && clientXml ? levels.size() + "" : levels.size() + "/" + worldIdXmlLevels.size();
    log.info("Found " + num + " processable level" + (levels.size() == 1 ? "" : "s"));
    return levels;
  }

  private Map<String, Integer> generateHouseAddressList() throws JDOMException, IOException {
    Document document = new SAXBuilder().build(getClientHouseAddressFile());
    Element rootNode = document.getRootElement();
    Map<String, Integer> addressIdsByName = new HashMap<>();
    for (Element address : rootNode.getChildren("client_housing_address")) {
      String name = address.getChildText("name");
      int id = Integer.parseInt(address.getChildText("id"));
      if (addressIdsByName.putIfAbsent(name, id) != null)
        log.warning("Duplicate house name in client_housing_adress.xml: " + name);
    }
    log.info("Found " + addressIdsByName.size() + " house addresses");
    return addressIdsByName;
  }

  private InputStream getClientWorldIdFile() throws IOException {
    File worldIdPakFile = new File(clientPath, "Data/World/World.pak");
    try (PakFile pakFile = PakFile.open(worldIdPakFile)) {
      return new ByteArrayInputStream(decodeXml(pakFile.unpak("worldid.xml").array()));
    }
  }

  private InputStream getClientHouseAddressFile() throws IOException {
    File housePakFile = new File(clientPath, "Data/Housing/Housing.pak");
    try (PakFile pakFile = PakFile.open(housePakFile)) {
      return new ByteArrayInputStream(decodeXml(pakFile.unpak("client_housing_address.xml").array()));
    }
  }

  private void processLevels(List<LevelData> levels, Map<String, Integer> houseAddresses) {
    levels.parallelStream().forEach(level -> {
      if (parseLevelPak(level, houseAddresses))
        requiredCgfs.addAll(level.getAllMeshFileNames());
      log.info(level + ": Done\r");
    });
    log.info("Found " + requiredCgfs.size() + " mesh references in " + levels.size() + " level" + (levels.size() == 1 ? "" : "s"));
    requiredCgfs.removeAll(ignoredCgfs);
  }

  private boolean parseLevelPak(LevelData level, Map<String, Integer> houseAddresses) {
    log.fine(level + ": [Level.pak] Extracting data …\r");
    ByteBuffer brushLst, objectsLst, mission, landMapH32, levelData;
    String levelName = level.levelName.toLowerCase();
    boolean needsBrushLst = levelName.contains("test") || levelName.contains("system_basic") || levelName.endsWith("prison");
    try (PakFile pakFile = PakFile.open(level.clientLevelPakFile)) {
      brushLst = pakFile.unpak("brush.lst");
      if (brushLst == null)
        log.log(needsBrushLst ? Level.FINE : Level.WARNING, level + ": " + level.clientLevelPakFile + " does not contain brush.lst");
      levelData = pakFile.unpak("leveldata.xml");
      if (levelData == null)
        log.warning(level + ": " + level.clientLevelPakFile + " does not contain leveldata.xml");
      objectsLst = pakFile.unpak("objects.lst");
      if (objectsLst == null)
        log.fine(level + ": " + level.clientLevelPakFile + " does not contain objects.lst");
      mission = pakFile.unpak("mission_mission0.xml");
      if (mission == null)
        log.warning(level + ": " + level.clientLevelPakFile + " does not contain mission_mission0.xml");
      landMapH32 = pakFile.unpak("terrain/land_map.h32");
      if (landMapH32 == null)
        log.warning(level + ": " + level.clientLevelPakFile + " does not contain land_map.h32");
    } catch (Exception e) {
      log.log(Level.SEVERE, "Cannot process " + level.clientLevelPakFile, e);
      return false;
    }
    if (landMapH32 != null) {
      level.landMapH32 = landMapH32;
      level.terrainMaterials = parseTerrainMaterials(levelData, level);
    }
    if (brushLst != null)
      parseBrushLst(brushLst, level);
    if (objectsLst != null)
      parseObjects(objectsLst, levelData, level);
    if (mission != null)
      parseEntities(mission, houseAddresses, level);

    log.fine(level + ": [Level.pak] Done");
    return true;
  }

  private byte[] parseTerrainMaterials(ByteBuffer levelData, LevelData level) {
    log.fine(level + ": [leveldata.xml] Processing …\r");
    byte[] materialIds = null;
    try {
      Document document = new SAXBuilder().build(new ByteArrayInputStream(levelData.array()));
      Element rootNode = document.getRootElement();
      List<Element> objects = rootNode.getChildren("SurfaceTypes").get(0).getChildren();
      materialIds = new byte[objects.size()];
      for (int i = 0; i < objects.size(); i++) {
        String material = objects.get(i).getAttributeValue("Material").trim();
        int matId = CgfLoader.getMaterialIdFor(material);
        if (matId > 0xFF)
          throw new IllegalArgumentException("Encountered out of range material ID " + matId + " for " + material);
        materialIds[i] = CgfLoader.isMaterialIntention(matId) ? (byte) matId : 0;
      }
    } catch (Exception e) {
      log.log(Level.SEVERE, level + ": Error parsing leveldata.xml", e);
    }
    log.fine(level + ": [leveldata.xml] Done");
    return materialIds == null || materialIds.length == 0 ? null : materialIds;
  }

  private void parseBrushLst(ByteBuffer brushLst, LevelData level) {
    log.fine(level + ": [brush.lst] Processing …\r");
    try {
      level.brushMeshData = BrushLstLoader.load(brushLst);
    } catch (Exception e) {
      log.log(Level.SEVERE, level + ": Error parsing brush.lst", e);
    }
    log.fine(level + ": [brush.lst] Done");
  }

  private void parseObjects(ByteBuffer objects, ByteBuffer levelData, LevelData level) {
    log.fine(level + ": [objects.lst] Processing …\r");
    try {
      level.objectMeshData = ObjectsLstLoader.loadLevelData(levelData, objects);
    } catch (Exception e) {
      log.log(Level.SEVERE, level + ": Error parsing objects.lst", e);
    }
    log.fine(level + ": [objects.lst] Done");
  }

  private void parseEntities(ByteBuffer mission, Map<String, Integer> houseAddresses, LevelData level) {
    log.fine(level + ": [mission_mission0.xml] Processing entities …\r");
    try {
      EntityLoader entityLoader = new EntityLoader();
      entityLoader.loadPlaceables(mission, houseAddresses);
      level.entityEntries = entityLoader.getEntityEntries();
      requiredDoorCgas.addAll(entityLoader.getEntityFileNames(EntityClass.DOOR));
    } catch (Exception e) {
      log.log(Level.SEVERE, level + ": Error parsing mission_mission0.xml", e);
    }
    log.fine(level + ": [mission_mission0.xml] Done");
  }


  private List<File> collectMeshFilePaths() throws IOException {
    List<File> clientMeshFiles = findMeshPaks("/Levels/common", "/Levels/idabpro", "/Objects");

    if (clientMeshFiles.isEmpty())
      log.info("There are no mesh archives in the Aion client.");
    else
      log.info("Found " + clientMeshFiles.size() + " mesh archives");

    return clientMeshFiles;
  }

  private List<File> findMeshPaks(String... rootFolders) throws IOException {
    List<File> meshPaks = new ArrayList<>();
    for (String relativeFolderPath : rootFolders) {
      File folder = new File(clientPath, relativeFolderPath);
      if (!folder.isDirectory())
        throw new FileNotFoundException(folder + " doesn't exist or is not a folder path.");
      Files.walk(folder.toPath()).forEach(path -> {
        File file = path.toFile();
        if (file.isFile() && (file.getName().matches(".+_Mesh.*\\.pak") || file.getName().equalsIgnoreCase("idabpro.pak"))) {
          log.fine("Found " + file.getPath());
          meshPaks.add(file);
        }
      });
    }
    return meshPaks;
  }

  private void createMeshes(File outputFolder, List<File> meshPaks) throws IOException {
    if (requiredCgfs.isEmpty()) {
      log.info("No referenced meshes, skipping");
      return;
    }
    AtomicInteger totalMeshes = new AtomicInteger(requiredCgfs.size());
    Map<String, List<MeshData>> availableMeshes = new ConcurrentHashMap<>();
    meshPaks.parallelStream().forEach(meshPakFile -> {
      if (requiredCgfs.isEmpty())
        return;
      try (PakFile pakFile = PakFile.open(meshPakFile, clientPath)) {
        processCgfFiles(pakFile, availableMeshes, totalMeshes);
      } catch (Exception e) {
        log.log(Level.SEVERE, "", e);
      }
    });
    missingCgfs.addAll(requiredCgfs);
    int processedCount = processedCgfs.size() + missingCgfs.size();
    if (processedCount != totalMeshes.get()) // should only happen on parsing/processing error
      log.warning("Only " + processedCount + " of " + totalMeshes + " meshes have been successfully processed!");
    log.info("Found " + availableMeshes.size() + " valid meshes of " + totalMeshes + " total (skipped " + emptyCgfs.size() + " empty and " + missingCgfs.size() + " missing ones)");

    Map<List<MeshData>, String> uniqueMeshes = availableMeshes.entrySet().stream()
        .sorted(Map.Entry.comparingByKey()) // sort to generate .mesh files with deterministic, comparable hashes
        .collect(Collectors.groupingBy(Map.Entry::getValue, LinkedHashMap::new, Collectors.mapping(Map.Entry::getKey, Collectors.joining("|"))));
    int duplicateCount = availableMeshes.size() - uniqueMeshes.size();
    log.info("Writing " + uniqueMeshes.size() + " unique meshes (" + duplicateCount + " duplicates have been merged) …");

    File meshFile = new File(outputFolder, "models.mesh");
    try (DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(meshFile)))) {
      uniqueMeshes.forEach((data, paths) -> writeMeshes(paths, data, stream));
    }
    log.info("Created " + meshFile.getCanonicalPath());
    if (!missingCgfs.isEmpty()) {
      missingCgfs.sort(String.CASE_INSENSITIVE_ORDER);
      log.warning(missingCgfs.size() + " missing CGFs: " + missingCgfs);
    }
  }

  private void createGeoFiles(File outputFolder, List<LevelData> levels) throws IOException {
    AtomicInteger i = new AtomicInteger();
    levels.parallelStream().forEach(level -> {
      try {
        createGeoFile(outputFolder, level);
        log.info("[" + i.incrementAndGet() + "/" + levels.size() + "] " + level.levelName + ": " + level.levelId + ".geo Done\r");
      } catch (Exception e) {
        log.log(Level.SEVERE, "Error generating " + level.levelId + ".geo", e);
      }
    });
    log.info("Created " + i + " geo file" + (i.get() == 1 ? "" : "s") + " under " + outputFolder.getCanonicalPath());
    int notGenerated = levels.size() - i.get();
    if (notGenerated != 0)
      log.warning(notGenerated + " file" + (notGenerated == 1 ? "" : "s") + " could be generated.");
  }

  private byte[] decodeXml(byte[] xmlContent) throws IOException {
    try (ByteArrayInputStream inputStream = new ByteArrayInputStream(xmlContent); ByteArrayOutputStream outputStream = new ByteArrayOutputStream(xmlContent.length)) {
      BinaryXmlDecoder.Decode(inputStream, outputStream);
      return outputStream.toByteArray();
    }
  }

  private void createGeoFile(File outputFolder, LevelData level) throws IOException {
    File geoFile = new File(outputFolder, level.levelId + ".geo");
    try (DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(geoFile)))) {
      short z = 0;
      boolean isAllSameZ = true;
      // terrain
      if (level.landMapH32 != null) {
        ByteBuffer terrainData = ByteBuffer.allocate(level.landMapH32.capacity());
        for (int i = 0; i < level.landMapH32.capacity(); i += H32_POINT_SIZE) {
          if (z != (z = level.landMapH32.getShort()) && i > 0)
            isAllSameZ = false;
          int materialIndex = level.landMapH32.get() & 0xFF;
          boolean isTerrainCutout = materialIndex == 0x3F;
          terrainData.putShort(isTerrainCutout ? Short.MIN_VALUE : z);
          terrainData.put(isTerrainCutout || level.terrainMaterials == null ? 0 : level.terrainMaterials[materialIndex]);
        }
        if (level.landMapH32.remaining() > 0) {
          throw new IOException("Terraindata for " + geoFile + " was not fully read.");
        }
        if (!isAllSameZ) {
          stream.writeByte(1); // terrain mesh
          stream.writeInt(terrainData.capacity() / H32_POINT_SIZE); // terrain data count
          stream.write(terrainData.array());
        }
      }
      if (isAllSameZ) {
        stream.writeByte(0); // basic terrain (same height everywhere) or no real terrain below map (z = 0, mesh based instances)
        stream.writeShort(z);
      }

      // brushes
      if (level.brushMeshData != null) {
        for (BrushEntry entry : level.brushMeshData.brushEntries) {
          String meshFileName = level.brushMeshData.meshFileNames.get(entry.meshIdx);
          if (shouldSkip(meshFileName, level))
            continue;
          byte[] meshFileNameBytes = meshFileName.getBytes(StandardCharsets.US_ASCII);
          stream.writeShort(meshFileNameBytes.length);
          stream.write(meshFileNameBytes);
          float[] matrix = entry.matrix;
          // pos vector
          for (int i = 0; i < 3; i++) { // 3, 7, 11
            stream.writeFloat(matrix[i * 4 + 3]);
          }
          // orientation matrix
          for (int i = 0; i < 3; i++) { // 0, 1, 2; 4, 5, 6; 8, 9, 10
            stream.writeFloat(matrix[i * 4]);
            stream.writeFloat(matrix[i * 4 + 1]);
            stream.writeFloat(matrix[i * 4 + 2]);
          }
          // scale, always 1 in brushList
          stream.writeFloat(1); // x
          stream.writeFloat(1); // y
          stream.writeFloat(1); // z
          stream.writeByte(entry.type.getId());
          stream.writeShort(entry.eventType);
          stream.writeByte(0);
        }
      }

      // vegetation
      if (level.objectMeshData != null) {
        for (ObjectEntry entry : level.objectMeshData.objectEntries) {
          String meshFileName = level.objectMeshData.meshFiles.get(entry.objectId);
          if (shouldSkip(meshFileName, level))
            continue;
          byte[] meshFileNameBytes = meshFileName.getBytes(StandardCharsets.US_ASCII);
          stream.writeShort(meshFileNameBytes.length);
          stream.write(meshFileNameBytes);
          // pos
          stream.writeFloat(entry.x);
          stream.writeFloat(entry.y);
          stream.writeFloat(entry.z);
          // transform
          Matrix4f matrix = Matrix4f.createRotationMatrix(entry.rotX, entry.rotY, entry.rotZ);
          stream.writeFloat(matrix.m11);
          stream.writeFloat(matrix.m21);
          stream.writeFloat(matrix.m31);
          stream.writeFloat(matrix.m12);
          stream.writeFloat(matrix.m22);
          stream.writeFloat(matrix.m32);
          stream.writeFloat(matrix.m13);
          stream.writeFloat(matrix.m23);
          stream.writeFloat(matrix.m33);


          stream.writeFloat(entry.scale); // x
          stream.writeFloat(entry.scale); // y
          stream.writeFloat(entry.scale); // z

          stream.writeByte(entry.type.getId());
          stream.writeShort(0); // event id
          stream.writeByte(0);
        }
      }

      for (EntityEntry entry : level.entityEntries) {
        if (entry instanceof HouseEntry) {
          writeHouseEntry((HouseEntry) entry, stream, level);
        } else if (entry instanceof DoorEntry) {
          writeDoorEntry((DoorEntry) entry, stream, level);
        } else {
          writeEntityEntry(entry, stream, level);
        }
      }
    }
  }

  private boolean shouldSkip(String meshFileName, LevelData level) {
    if (!processedCgfs.contains(meshFileName)) { // skip ignored or missing cgf
      if (!ignoredCgfs.contains(meshFileName) && !missingCgfs.contains(meshFileName))
        log.warning(level + ": Cgf was not processed: " + meshFileName);
      return true;
    }
    return emptyCgfs.contains(meshFileName);
  }

  private void writeHouseEntry(HouseEntry entry, DataOutputStream stream, LevelData level) throws IOException {
    for (String mesh : entry.meshes) {
      if (shouldSkip(mesh, level))
        continue;
      byte[] nameBytes = mesh.getBytes(StandardCharsets.US_ASCII);
      stream.writeShort(nameBytes.length);
      stream.write(nameBytes);

      stream.writeFloat(entry.pos.x);
      stream.writeFloat(entry.pos.y);
      stream.writeFloat(entry.pos.z);

      Matrix4f matrix = entry.getMatrix();
      stream.writeFloat(matrix.m11);
      stream.writeFloat(matrix.m21);
      stream.writeFloat(matrix.m31);
      stream.writeFloat(matrix.m12);
      stream.writeFloat(matrix.m22);
      stream.writeFloat(matrix.m32);
      stream.writeFloat(matrix.m13);
      stream.writeFloat(matrix.m23);
      stream.writeFloat(matrix.m33);

      stream.writeFloat(entry.scale.x);
      stream.writeFloat(entry.scale.y);
      stream.writeFloat(entry.scale.z);


      if (mesh.equalsIgnoreCase(entry.mesh)) {
        stream.writeByte(EntryType.HOUSE_DOOR.getId()); // node type 0 = none
      } else {
        stream.writeByte(entry.type.getId());
      }
      stream.writeShort(entry.address); // no static id
      stream.writeByte(0);
    }
  }

  private void writeEntityEntry(EntityEntry entry, DataOutputStream stream, LevelData level) throws IOException {
    if (shouldSkip(entry.mesh, level)) {
      if (entry.entityClass == EntityClass.TOWN_OBJECT) {
        boolean found = false;
        for (int i = entry.level; i >= 1; i--) {
          String meshName = entry.mesh.replace(entry.level + ".cgf", i + ".cgf");
          if (!shouldSkip(meshName, level)) {
            entry.mesh = meshName;
            found = true;
            break;
          }
        }
        if (!found) {
          log.warning(level + ": Could not find Entity cgf for EntityClass TOWN_OBJECT: " + entry.mesh);
          return;
        }
      } else {
        return;
      }
    }
    String name = entry.mesh;
    byte[] nameBytes = name.getBytes(StandardCharsets.US_ASCII);
    stream.writeShort(nameBytes.length);
    stream.write(nameBytes);

    stream.writeFloat(entry.pos.x);
    stream.writeFloat(entry.pos.y);
    stream.writeFloat(entry.pos.z);

    Matrix4f matrix = entry.getMatrix();
    stream.writeFloat(matrix.m11);
    stream.writeFloat(matrix.m21);
    stream.writeFloat(matrix.m31);
    stream.writeFloat(matrix.m12);
    stream.writeFloat(matrix.m22);
    stream.writeFloat(matrix.m32);
    stream.writeFloat(matrix.m13);
    stream.writeFloat(matrix.m23);
    stream.writeFloat(matrix.m33);

    stream.writeFloat(entry.scale.x);
    stream.writeFloat(entry.scale.y);
    stream.writeFloat(entry.scale.z);
    stream.writeByte(entry.type.getId());
    if (entry.entityClass == EntityClass.TOWN_OBJECT) {
      stream.writeShort(entry.townId);
      stream.writeByte(entry.level);
    } else {
      stream.writeShort(entry.entityId);
      stream.writeByte(0);
    }
  }

  private void writeDoorEntry(DoorEntry entry, DataOutputStream stream, LevelData level) throws IOException {
    if (shouldSkip(entry.mesh, level))
      return;
    String name = entry.mesh + entry.suffix;
    byte[] nameBytes = name.getBytes(StandardCharsets.US_ASCII);
    stream.writeShort(nameBytes.length);
    stream.write(nameBytes);
    // pos
    stream.writeFloat(entry.pos.x);
    stream.writeFloat(entry.pos.y);
    stream.writeFloat(entry.pos.z);

    // transform
    Matrix4f matrix = entry.getMatrix();
    stream.writeFloat(matrix.m11);
    stream.writeFloat(matrix.m21);
    stream.writeFloat(matrix.m31);
    stream.writeFloat(matrix.m12);
    stream.writeFloat(matrix.m22);
    stream.writeFloat(matrix.m32);
    stream.writeFloat(matrix.m13);
    stream.writeFloat(matrix.m23);
    stream.writeFloat(matrix.m33);

    // scale, always 1 for doors?
    stream.writeFloat(entry.scale.x);
    stream.writeFloat(entry.scale.y);
    stream.writeFloat(entry.scale.z);
    stream.writeByte(entry.type.getId());
    stream.writeShort(entry.entityId);
    stream.writeByte(0);
  }

  private void processCgfFiles(PakFile pakFile, Map<String, List<MeshData>> availableMeshes, AtomicInteger totalMeshes) {
    pakFile.getFileNames().parallelStream().forEach(name -> {
      if (!requiredCgfs.remove(name)) // not a required cgf or might also be a duplicate which has already been processed
        return;
      try {
        CgfLoader loader = new CgfLoader();
        ByteBuffer file = pakFile.unpak(name);
        loader.load(file);
        List<MeshData> meshes = new ArrayList<>();
        loader.traverseNodes(meshes);
        if (meshes.size() > 0) {
          if (availableMeshes.putIfAbsent(name, meshes) != null) // should never happen
            throw new IOException("Duplicate mesh name: " + name);
          if (requiredDoorCgas.contains(name)) {
            CgfLoader loaderState2 = loader.cloneAtTime(999999, file);
            List<MeshData> meshes2 = new ArrayList<>();
            loaderState2.traverseNodes(meshes2);
            String doorNameSecondaryState = name + "_state2";
            if (meshes2.size() > 0 && availableMeshes.putIfAbsent(doorNameSecondaryState, meshes2) == null) {
              if (BrushLstLoader.EVENT_MESHES.contains(name))
                meshes2.forEach(mesh -> mesh.collisionIntention |= CollisionIntention.EVENT.getId());
              processedCgfs.add(doorNameSecondaryState);
              totalMeshes.incrementAndGet();
            }
          }
          if (BrushLstLoader.EVENT_MESHES.contains(name))
            meshes.forEach(mesh -> mesh.collisionIntention |= CollisionIntention.EVENT.getId());
        } else {
          emptyCgfs.add(name);
        }
        processedCgfs.add(name);
        log.info("[" + processedCgfs.size() + "/" + totalMeshes + "] meshes processed\r");
      } catch (Exception e) {
        throw new RuntimeException("Error processing " + name, e);
      }
    });
  }

  private void writeMeshes(String path, List<MeshData> data, DataOutputStream stream) {
    try {
      byte[] nameBytes = path.getBytes(StandardCharsets.US_ASCII);
      if (nameBytes.length > 0xFFFF)
        throw new IOException("Data doesn't fit in short (nameBytes.length = " + nameBytes.length + ")");
      stream.writeShort(nameBytes.length);
      stream.write(nameBytes);
      if (data.size() > 0xFF)
        throw new IOException("Data doesn't fit in byte (data.size() = " + data.size() + ")");
      stream.writeByte(data.size());
      for (MeshData mesh : data) {
        if (mesh.vertices.size() > 0xFFFF)
          throw new IOException("Data doesn't fit in short (mesh.vertices.size() = " + mesh.vertices.size() + ")");
        stream.writeShort(mesh.vertices.size());
        for (Vector3 vec : mesh.vertices) {
          stream.writeFloat(vec.x);
          stream.writeFloat(vec.y);
          stream.writeFloat(vec.z);
        }
        if (mesh.faces.size() > 0xFFFF)
          throw new IOException("Data doesn't fit in short (mesh.faces.size() = " + mesh.faces.size() + ")");
        stream.writeShort(mesh.faces.size());
        for (MeshFace face : mesh.faces) {
          stream.writeShort(face.v0);
          stream.writeShort(face.v1);
          stream.writeShort(face.v2);
        }
        stream.writeByte(mesh.materialId);
        stream.writeByte(mesh.collisionIntention);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
