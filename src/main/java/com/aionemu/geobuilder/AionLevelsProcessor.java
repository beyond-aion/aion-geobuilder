package com.aionemu.geobuilder;

import com.aionemu.geobuilder.entries.*;
import com.aionemu.geobuilder.loaders.BrushLstLoader;
import com.aionemu.geobuilder.loaders.CgfLoader;
import com.aionemu.geobuilder.loaders.EntityLoader;
import com.aionemu.geobuilder.loaders.ObjectsLstLoader;
import com.aionemu.geobuilder.meshData.CollisionIntention;
import com.aionemu.geobuilder.meshData.MeshData;
import com.aionemu.geobuilder.meshData.MeshFace;
import com.aionemu.geobuilder.meshData.ObjectMeshData;
import com.aionemu.geobuilder.pakaccessor.PakFile;
import com.aionemu.geobuilder.utils.Matrix4f;
import com.aionemu.geobuilder.utils.PathSanitizer;
import com.aionemu.geobuilder.utils.Vector3;
import com.aionemu.geobuilder.utils.XmlParser;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.PathConverter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AionLevelsProcessor {

  @Parameter(description = "<client path>", converter = PathConverter.class, required = true)
  protected Path clientPath;

  @Parameter(names = "-lvl", description = "Limit file generation to given level ID(s) (default: all levels) Example: -lvl 110010000 -lvl 110020000", order = 1)
  protected Set<String> levelIds;

  @Parameter(names = "-w", description = "Path to server world_maps.xml or client WorldId.xml (default: client WorldId.xml)", order = 2)
  protected Path worldIdPath;

  @Parameter(names = "-o", description = "Path to the output folder", order = 3)
  protected Path outPath = Path.of("./out");

  @Parameter(names = "-am", description = "Generate all terrain materials (default: only skill-using materials)", order = 4)
  protected boolean allTerrainMaterials;

  @Parameter(names = "-do", description = "Disable oxipng optimizer (used for improved terrain PNG compression)", order = 5)
  protected boolean disableOxipng;

  @Parameter(names = "-dc", description = "Disable mesh compacting (removal of unused or duplicate vertices and degenerate or duplicate faces)", order = 6)
  protected boolean disableMeshCompacting;

  @Parameter(names = "-ds", description = "Disable mesh sorting to keep original order of vertices, faces and the face winding order", order = 7)
  protected boolean disableMeshSorting;

  @Parameter(names = "-v", description = "Activate verbose logging", order = 8)
  protected boolean verbose;

  private static final Logger log = Logger.getLogger("GeoBuilder");
  private static final Logger oxipngLogger = Logger.getLogger("oxipng");

  static {
    System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tT [%4$s] %5$s%6$s");
    log.setUseParentHandlers(false);
    oxipngLogger.setUseParentHandlers(false);
    StreamHandler consoleHandler = new StreamHandler(System.out, new SimpleFormatter() {
      @Override
      public String format(LogRecord record) {
        String msg = super.format(record);
        boolean isTemporaryLine = msg.endsWith("\r");
        if (isTemporaryLine) // remove carriage return char temporarily to insert the "Erase in Line" ANSI escape code before it
          msg = msg.substring(0, msg.length() - 1);
        if (record.getLevel() == Level.SEVERE)
          msg = "\u001B[31m" + msg + "\u001b[0m";
        else if (record.getLevel() == Level.WARNING)
          msg = "\u001B[33m" + msg + "\u001b[0m";
        else if (record.getLevel().intValue() < Level.INFO.intValue())
          msg = "\u001B[37m" + msg + "\u001b[0m";
        return isTemporaryLine ? msg + "\033[K\r" : msg + "\033[K" + System.lineSeparator();
      }
    }) {
      @Override
      public void publish(LogRecord record) {
        super.publish(record);
        flush();
      }
    };
    consoleHandler.setLevel(Level.ALL);
    log.addHandler(consoleHandler);
    oxipngLogger.addHandler(consoleHandler);
  }

  private final Set<String> requiredCgfs = ConcurrentHashMap.newKeySet();
  private final Set<String> requiredDoorCgas = ConcurrentHashMap.newKeySet();

  private final Set<String> processedCgfs = ConcurrentHashMap.newKeySet();
  private final Set<String> emptyCgfs = ConcurrentHashMap.newKeySet();

  protected void process() {
    log.setLevel(verbose ? Level.ALL : Level.INFO);
    oxipngLogger.setLevel(verbose ? Level.ALL : Level.WARNING);
    long time = System.currentTimeMillis();
    try {
      log.info("Loading available levels\r");
      List<LevelData> levels = findLevelsToProcess();
      if (!levels.isEmpty()) {
        if (Files.isDirectory(outPath))
          Files.list(outPath).filter(p -> p.toString().endsWith(".geo") || p.toString().endsWith(".mesh") || p.toString().endsWith(".png")).forEach(p -> p.toFile().delete());
        else
          Files.createDirectories(outPath);

        int materialsCount = CgfLoader.loadMaterials(clientPath);
        log.info("Found " + materialsCount + " materials");
        Map<String, Short> houseAddresses = loadHouseAddresses();
        processLevels(levels, houseAddresses);
        Thread terrainTask = Thread.startVirtualThread(() -> createTerrains(outPath, levels));
        List<Path> meshPaks = collectMeshFilePaths();
        createMeshes(outPath, meshPaks, levels);
        createGeoFiles(outPath, levels);
        if (terrainTask.isAlive()) {
          log.info("Waiting for terrain generation to finish\r");
          oxipngLogger.setLevel(verbose ? Level.ALL : Level.INFO);
          terrainTask.join();
        }
        log.info("Output folder: " + outPath.toRealPath());
        log.info("Processing time: " + (System.currentTimeMillis() - time) / 1000 + " s");
      }
    } catch (Throwable t) {
      log.log(Level.SEVERE, "", t);
    }
  }

  private List<LevelData> findLevelsToProcess() throws JDOMException, IOException {
    // Read world_maps.xml or WorldId.xml and find Levels to process
    String worldIdXml = worldIdPath == null ? "client WorldId.xml" : worldIdPath.getFileName().toString();

    Path levelsRootFolder = clientPath.resolve("Levels");
    Map<String, File> levelsByName = Files.list(levelsRootFolder)
        .map(Path::toFile)
        .filter(file -> file.isDirectory() && !file.getName().equalsIgnoreCase("common"))
        .collect(Collectors.toMap(file -> file.getName().toLowerCase(), file -> file));
    log.fine("Found " + levelsByName.size() + " levels in " + levelsRootFolder);

    // read client maps
    Document document = worldIdPath == null ? XmlParser.parse(getClientWorldIdFile()) : new SAXBuilder().build(worldIdPath.toFile());
    Element rootNode = document.getRootElement();
    boolean clientXml = rootNode.getName().equalsIgnoreCase("world_id");
    List<Element> worldIdXmlLevels = clientXml ? rootNode.getChildren("data") : rootNode.getChildren("map");
    log.fine("Validating " + worldIdXmlLevels.size() + " levels referenced in " + worldIdXml);

    List<LevelData> levels = new ArrayList<>();
    // buggy client files are normal, only log warning when we generate from server file or -lvl parameter
    Level logLevel = levelIds == null && clientXml ? Level.FINE : Level.WARNING;
    for (Element element : worldIdXmlLevels) {
      String levelId = element.getAttributeValue("id");
      String levelName = clientXml ? element.getText() : element.getAttributeValue("cName");
      File levelClientFolder = levelsByName.remove(levelName.toLowerCase());
      if (levelIds == null || levelIds.contains(levelId)) {
        if (levelClientFolder == null) {
          log.log(logLevel, "Ignoring " + levelName + ": Folder missing in " + levelsRootFolder);
        } else {
          Path clientLevelPakFile = Files.find(levelClientFolder.toPath(), 1, (p, attrs) -> attrs.isRegularFile() && p.getFileName().toString().equalsIgnoreCase("level.pak")).findAny().orElse(null);
          if (clientLevelPakFile != null)
            levels.add(new LevelData(levelId, levelName, clientLevelPakFile));
          else
            log.log(logLevel, "Ignoring " + levelName + ": Level.pak missing");
        }
      }
    }
    if (!levelsByName.isEmpty()) {
      log.fine(levelsByName.size() + " levels from " + levelsRootFolder + " were not referenced in " + worldIdXml + ": " + levelsByName.values().stream().map(File::getName).sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.joining(", ")));
    }
    if (levels.isEmpty() && levelIds != null && levelIds.size() == 1) {
      log.info("Level " + levelIds.iterator().next() + " wasn't found or is not processable");
    } else {
      String num = levelIds == null && clientXml ? levels.size() + "" : levels.size() + "/" + (levelIds == null ? worldIdXmlLevels.size() : levelIds.size());
      log.info("Found " + num + " processable level" + (levels.size() == 1 ? "" : "s"));
    }
    return levels;
  }

  private Map<String, Short> loadHouseAddresses() throws IOException {
    log.info("Loading available house addresses\r");
    Document document = XmlParser.parse(getClientHouseAddressFile());
    Element rootNode = document.getRootElement();
    Map<String, Short> addressIdsByName = new HashMap<>();
    for (Element address : rootNode.getChildren("client_housing_address")) {
      String name = address.getChildText("name");
      short id = Short.parseShort(address.getChildText("id"));
      if (addressIdsByName.putIfAbsent(name, id) != null)
        log.warning("Duplicate house name in client_housing_address.xml: " + name);
    }
    log.info("Found " + addressIdsByName.size() + " house addresses");
    return addressIdsByName;
  }

  private ByteBuffer getClientWorldIdFile() throws IOException {
    try (PakFile pakFile = PakFile.open(clientPath.resolve("Data/World/World.pak"))) {
      return pakFile.unpak("worldid.xml");
    }
  }

  private ByteBuffer getClientHouseAddressFile() throws IOException {
    try (PakFile pakFile = PakFile.open(clientPath.resolve("Data/Housing/Housing.pak"))) {
      return pakFile.unpak("client_housing_address.xml");
    }
  }

  private void processLevels(List<LevelData> levels, Map<String, Short> houseAddresses) {
    levels.parallelStream().forEach(level -> {
      parseLevelPak(level, houseAddresses);
      level.streamAllMeshFileNames().filter(m -> !isIgnored(m, level)).forEach(requiredCgfs::add);
    });
    log.info("Found " + requiredCgfs.size() + " mesh references in " + levels.size() + " level" + (levels.size() == 1 ? "" : "s"));
  }

  private void parseLevelPak(LevelData level, Map<String, Short> houseAddresses) {
    log.info(level + ": Processing " + level.clientLevelPakFile.getFileName() + '\r');
    String levelName = level.levelName.toLowerCase();
    boolean needsBrushLst = !(level.isTestLevel() || levelName.contains("system_basic") || levelName.endsWith("prison"));
    try (PakFile pakFile = PakFile.open(level.clientLevelPakFile)) {
      processPakFile(pakFile, "leveldata.xml", level, buffer -> parseLevelData(buffer, level)); // loads missionPath and useTerrain
      if (level.useTerrain)
        processPakFile(pakFile, "terrain/land_map.h32", level, buffer -> level.terrain.load(buffer, allTerrainMaterials));
      else
        log.fine(level + ": skipped loading terrain (disabled in leveldata.xml)");
      if (level.missionPath != null) {
        processPakFile(pakFile, level.missionPath, level, buffer -> level.entityEntries = EntityLoader.loadPlaceables(buffer, houseAddresses));
        // secondary door state will be generated from doors in primary state
        level.entityEntries.stream().filter(e -> e.type == EntryType.DOOR).map(e -> e.mesh).forEach(requiredDoorCgas::add);
      }
      if (pakFile.contains("brush.lst"))
        processPakFile(pakFile, "brush.lst", level, buffer -> level.brushMeshData = BrushLstLoader.load(buffer));
      else
        log.log(needsBrushLst ? Level.WARNING : Level.FINE, level + ": " + level.clientLevelPakFile + " does not contain brush.lst");
      if (pakFile.contains("objects.lst"))
        processPakFile(pakFile, "objects.lst", level, buffer -> ObjectsLstLoader.load(buffer, level));
      else
        log.fine(level + ": " + level.clientLevelPakFile + " does not contain objects.lst");
      log.info(level + ": Processing " + level.clientLevelPakFile.getFileName() + " finished\r");
    } catch (NoSuchFileException e) {
      log.warning("Cannot process " + level.clientLevelPakFile + ": " + e.getFile() + " is missing");
    } catch (Exception e) {
      log.log(Level.SEVERE, "Error processing " + level.clientLevelPakFile, e);
    }
  }

  private void parseLevelData(ByteBuffer leveldataXml, LevelData level) throws Exception {
    Element rootNode = XmlParser.parse(leveldataXml).getRootElement();
    Element levelInfo = rootNode.getChild("LevelInfo");
    int heightmapXSize = levelInfo.getAttribute("HeightmapXSize").getIntValue();
    int heightmapYSize = levelInfo.getAttribute("HeightmapYSize").getIntValue();
    int heightmapUnitSize = levelInfo.getAttribute("HeightmapUnitSize").getIntValue();
    List<Element> objects = rootNode.getChild("SurfaceTypes").getChildren();
    level.terrain = new Terrain(heightmapXSize, heightmapYSize, heightmapUnitSize);
    level.terrain.materialIds = new byte[objects.size()];
    for (int i = 0; i < objects.size(); i++) {
      String material = objects.get(i).getAttributeValue("Material").trim();
      if (!material.isEmpty()) {
        int matId = CgfLoader.getMaterialId(material);
        if (matId == -1) {
          if (level.isTestLevel())
            log.fine(level + ": Missing material: " + material);
          else
            throw new IllegalArgumentException("Missing material: " + material);
        }
        if (matId > 0xFF)
          throw new IllegalArgumentException("Encountered out of range material ID " + matId + " for " + material);
        level.terrain.materialIds[i] = (byte) matId;
      }
    }

    List<Element> vegetation = rootNode.getChild("Vegetation").getChildren();
    if (!vegetation.isEmpty()) {
      level.objectMeshData = new ObjectMeshData();
      level.objectMeshData.meshFiles = new ArrayList<>(vegetation.size());
      for (Element element : vegetation) {
        level.objectMeshData.meshFiles.add(PathSanitizer.sanitize(element.getAttributeValue("FileName")));
      }
    }

    Element mission = rootNode.getChild("Missions").getChild("Mission");
    level.missionPath = Objects.requireNonNull(mission.getAttributeValue("File"));
    level.useTerrain = mission.getChild("LevelOption").getChild("Initialize").getAttribute("Render_Terrain").getIntValue() != 0;
  }

  private void processPakFile(PakFile pakFile, String fileName, LevelData level, ThrowingConsumer<ByteBuffer> consumer) {
    log.fine(level + ": Processing " + fileName + '\r');
    try {
      consumer.consume(pakFile.unpak(fileName));
      log.fine(level + ": Processing " + fileName + " finished\r");
    } catch (Exception e) {
      log.log(Level.SEVERE, level + ": Error processing " + fileName, e);
    }
  }

  private List<Path> collectMeshFilePaths() throws IOException {
    log.info("Collecting mesh file paths\r");
    List<Path> clientMeshFiles = findMeshPaks("Levels/common", "Levels/idabpro", "Objects");

    if (clientMeshFiles.isEmpty())
      log.warning("Found no mesh archives in the Aion client");
    else
      log.info("Found " + clientMeshFiles.size() + " mesh archives");

    return clientMeshFiles;
  }

  private List<Path> findMeshPaks(String... rootFolders) throws IOException {
    List<Path> meshPaks = new ArrayList<>();
    for (String relativeFolderPath : rootFolders) {
      Path folder = clientPath.resolve(relativeFolderPath);
      if (!Files.isDirectory(folder))
        throw new NotDirectoryException(folder.toString());
      Files.find(folder, Integer.MAX_VALUE, (path, attributes) -> attributes.isRegularFile() && (path.toString().matches(".+_Mesh.*\\.pak") || path.getFileName().toString().equalsIgnoreCase("idabpro.pak")))
          .forEach(meshPaks::add);
    }
    return meshPaks;
  }

  private void createMeshes(Path outputFolder, List<Path> meshPaks, List<LevelData> levels) throws IOException {
    if (requiredCgfs.isEmpty()) {
      log.info("No referenced meshes, skipping generating mesh file");
      return;
    }
    log.info("Generating mesh file\r");
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
    List<String> missingMeshes = new ArrayList<>(requiredCgfs);
    int processedCount = processedCgfs.size() + missingMeshes.size();
    if (processedCount != totalMeshes.get()) // should only happen on parsing/processing error
      log.warning("Only " + processedCount + " of " + totalMeshes + " meshes have been successfully processed!");
    log.info("Found " + availableMeshes.size() + " valid meshes of " + totalMeshes + " total (skipped " + emptyCgfs.size() + " empty and " + missingMeshes.size() + " missing ones)");
    if (!missingMeshes.isEmpty()) {
      Set<String> missingTownGrowthMeshes = levels.stream().flatMap(l -> l.entityEntries.stream().flatMap(e -> e instanceof TownEntry townEntry ? townEntry.getHigherLevelMeshNames() : Stream.empty())).filter(missingMeshes::contains).collect(Collectors.toSet());
      Set<String> missingTestLevelMeshes = levels.stream().filter(LevelData::isTestLevel).flatMap(LevelData::streamAllMeshFileNames).filter(missingMeshes::contains).collect(Collectors.toSet());
      if (!missingTownGrowthMeshes.isEmpty()) {
        log.fine(missingTownGrowthMeshes.size() + " missing meshes are optional town growth meshes");
      }
      if (!missingTestLevelMeshes.isEmpty()) {
        log.fine(missingTestLevelMeshes.size() + " missing meshes are on test maps: " + missingTestLevelMeshes.stream().sorted().collect(Collectors.joining(", ")));
      }
      missingMeshes.removeAll(missingTownGrowthMeshes);
      missingMeshes.removeAll(missingTestLevelMeshes);
      if (!missingMeshes.isEmpty()) {
        log.warning(missingMeshes.size() + " missing meshes may exist in an unscanned archive: " + missingMeshes.stream().sorted().collect(Collectors.joining(", ")));
      }
    }
    if (!disableMeshCompacting)
      compact(availableMeshes.values());

    log.info("Merging duplicate meshes\r");
    if (!disableMeshSorting)
      availableMeshes.values().parallelStream().forEach(m -> m.forEach(MeshData::sort)); // this helps find duplicates
    Map<List<MeshData>, String> uniqueMeshes = availableMeshes.entrySet().stream()
        .sorted(Map.Entry.comparingByKey()) // sort to generate .mesh files with deterministic, comparable hashes
        .collect(Collectors.groupingBy(Map.Entry::getValue, LinkedHashMap::new, Collectors.mapping(Map.Entry::getKey, Collectors.joining("|"))));
    int duplicateCount = availableMeshes.size() - uniqueMeshes.size();
    String meshes = uniqueMeshes.size() + " unique meshes (" + duplicateCount + " duplicates have been merged)";
    log.info("Writing " + meshes + '\r');

    Path meshFile = outputFolder.resolve("models.mesh");
    try (DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(meshFile)))) {
      uniqueMeshes.forEach((data, paths) -> writeMeshes(paths, data, stream));
    }
    log.info("Created " + meshFile.getFileName() + " with " + meshes);
  }

  private void compact(Collection<List<MeshData>> meshes) {
    log.info("Compacting meshes\r");
    AtomicInteger oldSize = new AtomicInteger(), newSize = new AtomicInteger();
    meshes.parallelStream().forEach(m -> m.parallelStream().forEach(meshData -> {
      oldSize.addAndGet(meshData.getSize());
      meshData.compact();
      newSize.addAndGet(meshData.getSize());
    }));
    log.info("Compacted meshes to " + Math.round(10000f * newSize.get() / oldSize.get()) / 100f + " % of their original size (before: " + (oldSize.get() / 1024 / 1024) + " MiB, after: " + (newSize.get() / 1024 / 1024) + " MiB)");
  }

  private void createTerrains(Path outputFolder, List<LevelData> levels) {
    log.info("Generating terrain files" + (disableOxipng ? '\r' : " using oxipng optimization"));
    Set<String> skipped = ConcurrentHashMap.newKeySet();
    Map<ShortBuffer, Terrain.Heightmap> heightmaps = new ConcurrentHashMap<>();
    Map<ByteBuffer, Terrain.Heightmap> materials = new ConcurrentHashMap<>();
    levels.parallelStream().forEach(level -> {
      if (!level.useTerrain) {
        skipped.add(level.levelId);
        return;
      }
      heightmaps.compute(level.terrain.heightmap, (k, heightmap) -> {
        if (heightmap == null)
          heightmap = level.terrain.createHeightmap();
        heightmap.fileNames.add(level.levelId);
        return heightmap;
      });
      if (level.terrain.materials != null) {
        materials.compute(level.terrain.materials, (k, matmap) -> {
          if (matmap == null)
            matmap = level.terrain.createMaterialmap();
          matmap.fileNames.add(level.levelId);
          return matmap;
        });
      }
    });
    Stream.concat(heightmaps.values().stream(), materials.values().stream())
        .parallel()
        .forEach(heightmap -> heightmap.write(outputFolder, disableOxipng, oxipngLogger));
    log.info("Created " + heightmaps.size() + " unique heightmaps and " + materials.size() + " unique material maps" + (skipped.isEmpty() ? "" : " (skipped " + skipped.size() + " levels with disabled terrain: " + skipped.stream().sorted().collect(Collectors.joining(", ")) + ")"));
  }

  private void createGeoFiles(Path outputFolder, List<LevelData> levels) {
    log.info("Generating geo files\r");
    Set<String> skipped = ConcurrentHashMap.newKeySet();
    AtomicInteger i = new AtomicInteger();
    levels.parallelStream().forEach(level -> {
      try {
        if (createGeoFile(outputFolder, level)) {
          log.info("[" + i.incrementAndGet() + "/" + (levels.size() - skipped.size()) + "] " + level.levelName + ": " + level.levelId + ".geo Done\r");
        } else {
          skipped.add(level.levelId);
        }
      } catch (Exception e) {
        log.log(Level.SEVERE, "Error generating " + level.levelId + ".geo", e);
      }
    });
    log.info("Created " + i.get() + " geo file" + (i.get() == 1 ? "" : "s") + (skipped.isEmpty() ? "" : " (skipped " + skipped.size() + " levels without entities: " + skipped.stream().sorted().collect(Collectors.joining(", ")) + ")"));
    int notGenerated = levels.size() - i.get() - skipped.size();
    if (notGenerated != 0)
      log.warning(notGenerated + " file" + (notGenerated == 1 ? "" : "s") + " could not be generated.");
  }

  private boolean createGeoFile(Path outputFolder, LevelData level) throws IOException {
    if (level.streamAllMeshFileNames().findAny().isEmpty()) {
      return false;
    }
    Path geoFile = outputFolder.resolve(level.levelId + ".geo");
    try (DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(geoFile)))) {
      // brushes
      if (level.brushMeshData != null) {
        for (BrushEntry entry : level.brushMeshData.brushEntries) {
          String meshFileName = level.brushMeshData.meshFileNames.get(entry.meshIndex);
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
          String meshFileName = level.objectMeshData.meshFiles.get(entry.meshIndex);
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
        if (entry instanceof HouseEntry houseEntry) {
          writeHouseEntry(houseEntry, stream, level);
        } else {
          writeEntityEntry(entry, stream, level);
        }
      }
    }
    return true;
  }

  private boolean shouldSkip(String meshFileName, LevelData level) {
    if (!processedCgfs.contains(meshFileName)) { // skip ignored or missing cgf
      if (!isIgnored(meshFileName, level) && !requiredCgfs.contains(meshFileName))
        log.warning(level + ": " + meshFileName + " was not processed");
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
      return;
    }
    String name = entry instanceof DoorEntry doorEntry ? entry.mesh + doorEntry.suffix : entry.mesh;
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
    if (entry instanceof TownEntry townEntry) {
      stream.writeShort(townEntry.townId);
      stream.writeByte(townEntry.level);
    } else {
      stream.writeShort(entry.entityId);
      stream.writeByte(0);
    }
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
        int maxIndex = mesh.getMaxFaceVertexIndex();
        if (maxIndex > 0xFFFF)
          throw new IOException("MeshFace index " + maxIndex + " doesn't fit in short");
        if (maxIndex > 0xFF) {
          stream.writeByte(2);
          for (MeshFace face : mesh.faces) {
            stream.writeShort(face.v0);
            stream.writeShort(face.v1);
            stream.writeShort(face.v2);
          }
        } else {
          stream.writeByte(1);
          for (MeshFace face : mesh.faces) {
            stream.writeByte(face.v0);
            stream.writeByte(face.v1);
            stream.writeByte(face.v2);
          }
        }
        stream.writeByte(mesh.materialId);
        stream.writeByte(mesh.collisionIntention);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean isIgnored(String meshFileName, LevelData level) {
    // TODO remove this method after figuring out the correct collision logic
    return switch (meshFileName) {
      // Door ID 145 is invisible and clickable but can be walked through (actual obstacle is elemental_alchemyroom_collisionwall.cgf)
      case "objects/npc/level_object/idyun_bridge/idyun_bridge_01a.cga" -> Set.of("300280000", "300620000").contains(level.levelId);
      // mesh collides in 300040000 x:676.4767 y:1196.3914 z:143.58203 but doesn't in instances 300280000 or 300620000 (near Vasharti)
      case "levels/common/light/structures/props/exterior/pr_l_caldron.cgf" -> Set.of("300280000", "300620000").contains(level.levelId);
      default -> false;
    };
  }

  @FunctionalInterface
  interface ThrowingConsumer<T> {
    void consume(T buffer) throws Exception;
  }
}
