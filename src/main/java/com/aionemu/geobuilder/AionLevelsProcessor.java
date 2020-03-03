package com.aionemu.geobuilder;

import com.aionemu.geobuilder.entries.*;
import com.aionemu.geobuilder.exceptions.AionException;
import com.aionemu.geobuilder.loaders.BrushLstLoader;
import com.aionemu.geobuilder.loaders.CgfLoader;
import com.aionemu.geobuilder.loaders.EntityLoader;
import com.aionemu.geobuilder.loaders.ObjectsLstLoader;
import com.aionemu.geobuilder.math.Matrix4f;
import com.aionemu.geobuilder.meshData.*;
import com.aionemu.geobuilder.pakaccessor.DefaultPakAccessor;
import com.aionemu.geobuilder.utils.*;
import com.google.common.io.LittleEndianDataOutputStream;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.kohsuke.args4j.Option;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AionLevelsProcessor {

  @Option(name = "-c", usage = "Client path.", metaVar = "PATH", required = true)
  protected String clientPath;

  @Option(name = "-lvl", usage = "Level-Id to generate geodata for e.g. 110010000. default: all levels. ", metaVar = "LVLID")
  protected String levelId = null;

  @Option(name = "-w", usage = "Path to server world_maps.xml or client WorldId.xml. default: client WorldId.xml", metaVar = "PATH")
  protected String worldIdPath;

  @Option(name = "-t", usage = "Path to the temporary folder.", metaVar = "PATH")
  protected String tmpPath = "./temp";

  @Option(name = "-o", usage = "Path to the output folder.", metaVar = "PATH")
  protected String outPath = "./out";

  private static final int NUM_THREADS = 4;

  private class StringComparer implements IStringComparer {

    @Override
    public boolean compare(final String s1, final String s2) {
      return s1.equalsIgnoreCase(s2);
    }
  }

  private class EndsWithStringComparer implements IStringComparer {

    @Override
    public boolean compare(final String s1, final String s2) {
      return s1.endsWith(s2);
    }
  }

  private class EndsWithStringComparer2 implements IStringComparer {

    @Override
    public boolean compare(final String s1, final String s2) {
      return s2.endsWith(s1);
    }
  }

  private class EndsWithStringComparer3 implements IStringComparer {

    @Override
    public boolean compare(final String s1, final String s2) {
      return s2.toLowerCase().endsWith(s1.toLowerCase());
    }
  }

  private class StartsWithStringComparer implements IStringComparer {
    @Override
    public boolean compare(final String s1, final String s2) {
      return s1.startsWith(s2);
    }
  }

  private final static int H32_POINT_SIZE = 3;
  private final static int MAP_POINT_SIZE = 2;
  private final static int H32_BUFFER_MAX_SIZE = (CommonUtils.BUFFER_MAX_SIZE / MAP_POINT_SIZE) * H32_POINT_SIZE;
  private final static int MAP_BUFFER_MAX_SIZE = (CommonUtils.BUFFER_MAX_SIZE / MAP_POINT_SIZE) * MAP_POINT_SIZE;

  private BrushLstLoader brushLoader = new BrushLstLoader();
  private ObjectsLstLoader objectsLoader = new ObjectsLstLoader();
  private EntityLoader entityLoader = new EntityLoader();
  private File levelsTmpFolder = null;
  private File meshesTmpFolder = null;
  private File levelsCommonDir = null;
  private File levelsIdabproDir = null;
  private File objectsDir = null;
  private File worldIdFile = null;
  private File houseAddressFile = null;

  private DefaultPakAccessor pakAccessor = new DefaultPakAccessor();
  private File[] clientMeshFiles = null;
  private Map<String, String> mapIdClientFolderMap = null;
  private Map<String, Integer> houseAdresses = null;
  private String[] clientLevelFolders = null;

  private Map<String, BrushLstMeshData> levelBrushMeshData = new HashMap<>();
  private Map<String, ObjectMeshData> levelObjectsMeshData = new HashMap<>();
  private Map<String, DoorMeshData> levelDoorsMeshData = new HashMap<>();
  private Map<String, EntityMeshData> levelEntitiesMeshData = new HashMap<>();
  private Map<String, LevelMeshData> levelMeshDataMap = new HashMap<>();
  private Map<String, List<Byte>> levelTerrainMaterials = new HashMap<>();
  private Map<String, List<BrushEntry>> levelBrushEntries = new HashMap<>();
  private Map<String, List<ObjectEntry>> levelObjectEntries = new HashMap<>();
  private Map<String, List<DoorEntry>> levelDoorEntries = new HashMap<>();
  private Map<String, List<EntityEntry>> levelEntityEntries = new HashMap<>();
  private LinkedHashSet<String> requiredCgfs = new LinkedHashSet<>();
  private LinkedHashSet<String> requiredDoorCgas = new LinkedHashSet<>();
  private LinkedHashSet<String> requiredPlaceableCgfs = new LinkedHashSet<>();
  private LinkedHashSet<String> requiredBasicCgfs = new LinkedHashSet<>();
  private LinkedHashSet<String> requiredHouseCgfs = new LinkedHashSet<>();
  private LinkedHashSet<String> requiredHouseDoorCgfs = new LinkedHashSet<>();
  private LinkedHashSet<String> requiredTownCgfs = new LinkedHashSet<>();
  private Map<String, String> validLevels = new HashMap<>();

  private List<String> missingCgfs = new Vector<>();
  private List<String> processedCgfs = new Vector<>();
  private List<String> emptyCgfs = new Vector<>();
  static List<String> ignoreCgfs = new ArrayList<>();
  private ConcurrentMap<String, List<MeshData>> availableMeshes = new ConcurrentHashMap<>();

  private void addIgnoreCgfs() {
    ignoreCgfs.add("objects/npc/level_object/idyun_bridge/idyun_bridge_01a.cga");
    ignoreCgfs.add("levels/common/light/structures/props/exterior/pr_l_caldron.cgf");
  }

  protected void process() throws JDOMException, IOException {
    addIgnoreCgfs();
    long timer = -System.currentTimeMillis();
    final File tempDir = new File(tmpPath);
    if (!tempDir.exists() || !tempDir.isDirectory()) {
      throw new FileNotFoundException("Temporary folder path [" + tmpPath + "] doesn't exist or is not a folder path");
    }

    final File clientDir = new File(clientPath);
    if (!clientDir.exists() || !clientDir.isDirectory()) {
      throw new FileNotFoundException("Aion client installation path [" + clientDir + "] doesn't exist or is not a folder path");
    }

    System.out.println("Collecting resources..");
    initResources();
    collectMeshFilePaths();
    collectLevelFoldersName();

    System.out.println("Processing levels...");
    boolean containsValidLevel = false;
    for (Map.Entry<String, String> pairs : mapIdClientFolderMap.entrySet()) {
      final String clientLevelId = pairs.getKey();
      final String levelFolder = pairs.getValue();
      final String clientLevelFolderName = correctLevelFolderName(levelFolder);

      if (levelId != null && !levelId.equalsIgnoreCase(clientLevelId)) {
        continue;
      }

      System.out.println();
      if (clientLevelFolderName == null) {
        System.out.println(". [WARN] Level folder doesn't exist (" + levelFolder + ")");
        continue;
      } else if (clientLevelFolderName.endsWith("_prison")) {
        System.out.println(". [INFO] skipping empty " + clientLevelFolderName);
        continue;
      }

      System.out.println(". [" + clientLevelId + "] " + clientLevelFolderName + "...");

      final String clientLevelFileName = getLevelFileName(clientLevelFolderName);

      if (clientLevelFileName == null) {
        System.out.println(". . [WARN] Level file doesn't exist (" + levelFolder + "/Level.pak)");
        continue;
      }
      final File clientLevelPakFile = new File(clientPath, "/Levels/" + clientLevelFolderName + "/" + clientLevelFileName);
      final File brushLstFile = new File(levelsTmpFolder, clientLevelFolderName + "_brush.lst");
      final File objectsFile = new File(levelsTmpFolder, clientLevelFolderName + "_objects.lst");
      final File landMapH32File = new File(levelsTmpFolder, clientLevelFolderName + "_land_map.h32");
      final File levelDataFile = new File(levelsTmpFolder, clientLevelFolderName + "_leveldata.xml");
      final File missionFile = new File(levelsTmpFolder, clientLevelFolderName + "_missions.xml");
      if (!parseLevelPak(clientLevelFolderName, clientLevelPakFile, brushLstFile, landMapH32File, objectsFile, levelDataFile, missionFile)) {
        continue;
      }


      if (landMapH32File.exists() && landMapH32File.length() > 0) {
        List<Byte> terrainMaterials = parseTerrainMaterials(levelDataFile);
        if (terrainMaterials != null) {
          levelTerrainMaterials.put(clientLevelFolderName, terrainMaterials);
        }
      }
      LevelMeshData levelMeshData = new LevelMeshData();

      if (brushLstFile.exists() && brushLstFile.length() > 0) {
        List<BrushEntry> brushEntries = parseBrushLst(clientLevelFolderName, brushLstFile);
        levelMeshData.meshFiles = levelBrushMeshData.get(clientLevelFolderName).meshFiles;
        levelMeshData.meshUsage = levelBrushMeshData.get(clientLevelFolderName).meshUsage;
        levelBrushEntries.put(clientLevelFolderName, brushEntries);
      }

      if (objectsFile.exists() && objectsFile.length() > 0) {
        List<ObjectEntry> objectEntries = parseObjects(clientLevelFolderName, objectsFile, levelDataFile);
        levelObjectEntries.put(clientLevelFolderName, objectEntries);
        if (levelObjectsMeshData.containsKey(clientLevelFolderName)) {
          Set<String> meshFiles = new LinkedHashSet<>();
          if (levelMeshData.meshFiles != null) {
            meshFiles.addAll(levelMeshData.meshFiles);
          }
          meshFiles.addAll(levelObjectsMeshData.get(clientLevelFolderName).meshFiles);
          levelMeshData.meshFiles = new ArrayList<>();
          levelMeshData.meshFiles.addAll(meshFiles);
          levelMeshData.meshUsage = new int[levelMeshData.meshFiles.size()];
        }
      }

      if (missionFile.exists() && missionFile.length() > 0) {
        /*List<DoorEntry> doorEntries = parseDoors(clientLevelFolderName, missionFile);
        levelDoorEntries.put(clientLevelFolderName, doorEntries);
        if (levelDoorsMeshData.containsKey(clientLevelFolderName)) {
          Set<String> meshFiles = new LinkedHashSet<>();
          if (levelMeshData.meshFiles != null) {
            meshFiles.addAll(levelMeshData.meshFiles);
          }
          meshFiles.addAll(levelDoorsMeshData.get(clientLevelFolderName).meshFiles);
          levelMeshData.meshFiles = new ArrayList<>();
          levelMeshData.meshFiles.addAll(meshFiles);
          levelMeshData.meshUsage = new int[levelMeshData.meshFiles.size()];
          requiredDoorCgas.addAll(levelDoorsMeshData.get(clientLevelFolderName).meshFiles);
        }
        */

        List<EntityEntry> entityEntries = parseEntities(clientLevelFolderName, missionFile);
        levelEntityEntries.put(clientLevelFolderName, entityEntries);
        if (levelEntitiesMeshData.containsKey(clientLevelFolderName)) {
          Set<String> meshFiles = new LinkedHashSet<>();
          if (levelMeshData.meshFiles != null) {
            meshFiles.addAll(levelMeshData.meshFiles);
          }
          meshFiles.addAll(levelEntitiesMeshData.get(clientLevelFolderName).placeableMeshFiles);
          meshFiles.addAll(levelEntitiesMeshData.get(clientLevelFolderName).basicMeshFiles);
          meshFiles.addAll(levelEntitiesMeshData.get(clientLevelFolderName).townMeshFiles);
          meshFiles.addAll(levelEntitiesMeshData.get(clientLevelFolderName).houseMeshFiles);
          meshFiles.addAll(levelEntitiesMeshData.get(clientLevelFolderName).houseDoorMeshFiles);
          meshFiles.addAll(levelEntitiesMeshData.get(clientLevelFolderName).doorMeshFiles);
          levelMeshData.meshFiles = new ArrayList<>();
          levelMeshData.meshFiles.addAll(meshFiles);
          levelMeshData.meshUsage = new int[levelMeshData.meshFiles.size()];
          requiredPlaceableCgfs.addAll(levelEntitiesMeshData.get(clientLevelFolderName).placeableMeshFiles);
          requiredBasicCgfs.addAll(levelEntitiesMeshData.get(clientLevelFolderName).basicMeshFiles);
          requiredTownCgfs.addAll(levelEntitiesMeshData.get(clientLevelFolderName).townMeshFiles);
          requiredHouseCgfs.addAll(levelEntitiesMeshData.get(clientLevelFolderName).houseMeshFiles);
          requiredHouseDoorCgfs.addAll(levelEntitiesMeshData.get(clientLevelFolderName).houseDoorMeshFiles);
          requiredDoorCgas.addAll(levelEntitiesMeshData.get(clientLevelFolderName).doorMeshFiles);
        }

      }
      levelMeshDataMap.put(clientLevelFolderName, levelMeshData);
      requiredCgfs.addAll(levelMeshData.meshFiles);
      validLevels.put(clientLevelId, clientLevelFolderName);
      containsValidLevel = true;
      System.out.println(". [" + clientLevelId + "] " + clientLevelFolderName + " Done");
    }
    System.out.println("\nProcessing levels... Done");

    if (containsValidLevel) {
      System.out.println("\nGenerating geo.mesh...");
      final File meshFile = new File(outPath, "geo.mesh");
      final FileOutputStream meshFileStream = new FileOutputStream(meshFile);
      final LittleEndianDataOutputStream stream = new LittleEndianDataOutputStream(meshFileStream);

      final Map<String, File> cgfFiles = new HashMap<>();
      for (final File meshPakFile : clientMeshFiles) {
        try {
          if (meshPakFile.getPath().toLowerCase().contains("idabpro")) {
            System.out.println("SETTING PAK-ACESSOR");
          }
          pakAccessor.setPakFile(meshPakFile.getPath(), 2);
          final Map<String, OutputStream> filterStreamMap = new HashMap<>();
          findCgfFiles(cgfFiles, filterStreamMap);
          if (filterStreamMap.size() > 0) {
            try {
              Charset charset = null; // default is utf8
              // seems like these 2 files are the only files with this encoding
              if (meshPakFile.getPath().contains("\\Levels\\common\\Mesh_Meshes_093.pak")) {
                charset = Charset.forName("Cp437");
              } else if (meshPakFile.getPath().contains("\\Objects\\npc\\human\\Mesh_Meshes_008.pak")) {
                charset = Charset.forName("Cp437");
              } else if (meshPakFile.getPath().contains("\\Objects\\monster\\Mesh_Meshes_000.pak")) {
                charset = Charset.forName("Cp437");
              }

              retreiveFromPak(meshPakFile.getName() + ".zip", filterStreamMap, new EndsWithStringComparer2(), true, charset);
            } catch (Exception e) {
              System.err.println(e.toString());
              e.printStackTrace();
            }
          }

        } catch (Exception e) {
          System.err.println(e.toString());
          e.printStackTrace();
        } finally {
          pakAccessor.close();
        }
        if (requiredCgfs.isEmpty()) {
          break;
        }
      }
      ExecutorService executors = Executors.newFixedThreadPool(NUM_THREADS);
      CountDownLatch progressLatch = new CountDownLatch(cgfFiles.size());
      AtomicInteger counts = new AtomicInteger(0);
      for (Map.Entry<String, File> cgfFile : cgfFiles.entrySet()) {
        executors.submit(() -> {
          try {
            processCgf(cgfFile);
            int i =  counts.incrementAndGet();
            if (i % 100 == 0 || i == cgfFiles.size()) {
              System.out.println("[" + i + "/" + cgfFiles.size() + "] Meshes processed");
            }
          } catch (Exception e) {
            e.printStackTrace();
          } finally {
            progressLatch.countDown();
          }
        });
      }
      try {
        progressLatch.await();
        executors.shutdown();
      } catch (Exception e) {
        e.printStackTrace();
      }

      System.out.println("Writing Meshes..");
      int writtenMeshes = 0;
      for (Map.Entry<String, List<MeshData>> mesh : availableMeshes.entrySet()) {
        writeMeshes(mesh.getKey(), mesh.getValue(), stream, false, EntityClass.NONE);
        writtenMeshes++;
        if (writtenMeshes % 500 == 0 || writtenMeshes == cgfFiles.size()) {
          System.out.println("Writing [" + writtenMeshes + "/" + cgfFiles.size() + "] Meshes");
        }
      }
      if (!requiredCgfs.isEmpty()) {
        System.out.println(". " + requiredCgfs.size() + " required cgfs could not be found.");
        missingCgfs.addAll(requiredCgfs);
      }
      if (emptyCgfs.size() > 0) {
        System.out.println(". There are " + emptyCgfs.size() + " empty cgfs.");
      }
      System.out.println("Generating geo.mesh... Done");
      stream.close();
      meshFileStream.close();
    }

    ExecutorService executors = Executors.newFixedThreadPool(NUM_THREADS);
    CountDownLatch progressLatch = new CountDownLatch(validLevels.size());
    AtomicInteger i = new AtomicInteger(0);
    System.out.println();
    for (Map.Entry<String, String> entry : validLevels.entrySet()) {
      executors.submit(() -> {
        try {
          System.out.println("[" + i.incrementAndGet() + "/" + validLevels.size() + "] Creating " + entry.getValue() + ": " + entry.getKey() + ".geo file.");
          createGeoFile(entry.getKey(), new File(levelsTmpFolder, entry.getValue() + "_land_map.h32"),
              levelBrushEntries.get(entry.getValue()), levelBrushMeshData.get(entry.getValue()),
              levelObjectEntries.get(entry.getValue()), levelObjectsMeshData.get(entry.getValue()),
              levelEntityEntries.get(entry.getValue()), levelTerrainMaterials.get(entry.getValue()));
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          progressLatch.countDown();
        }
      } );

    }

    try {
      progressLatch.await();
      executors.shutdown();
    } catch (Exception e) {
      e.printStackTrace();
    }
    timer += System.currentTimeMillis();
    int seconds = (int) (timer / 1000) % 60;
    int minutes = (int) ((timer / (1000 * 60)) % 60);
    int hours = (int) ((timer / (1000 * 60 * 60)) % 24);
    System.out.println("\nProcessing time: " + hours + " h " + minutes + " m " + seconds + "s");
    System.out.println("---------------- EMPTY CGFS ----------------");
    for (String s : emptyCgfs) {
      System.out.println("   " + s);
    }

    System.out.println("---------------- MISSING CGFS ----------------");
    for (String s : missingCgfs) {
      System.out.println("   " + s);
    }
  }

  private void initResources() throws IOException {
    levelsTmpFolder = new File(tmpPath, "Levels");
    levelsTmpFolder.mkdirs();

    meshesTmpFolder = new File(tmpPath, "Meshes");
    meshesTmpFolder.mkdirs();

    new File(outPath).mkdirs();

    worldIdFile = null;
    if (worldIdPath != null) {
      worldIdFile = new File(worldIdPath);
      if (!worldIdFile.exists() || !worldIdFile.isFile()) {
        System.out.println("Path to world_maps.xml file [" + worldIdPath + "] doesn't exist or not a file path. Trying to use client WorldId.xml");
        worldIdFile = null;
      }
    }
    if (worldIdFile == null) {
      worldIdFile = getClientWorldIdFile();
      if (!worldIdFile.exists() || !worldIdFile.isFile())
        throw new FileNotFoundException("Cannot find client WorldId.xml.");
    }

    // Path to the levels' meshes
    levelsCommonDir = new File(clientPath, "/Levels/common");
    if (!levelsCommonDir.isDirectory()) {
      throw new FileNotFoundException("Path to clients Levels/common folder [" + levelsCommonDir.getPath() + "] doesn't exist or is not a folder path");
    }
    // some cgfs for this map are in its levels folder
    levelsIdabproDir = new File(clientPath, "/Levels/idabpro");
    if (!levelsIdabproDir.isDirectory()) {
      throw new FileNotFoundException("Path to clients Levels/idabpro folder [" + levelsIdabproDir.getPath() + "] doesn't exist or is not a folder path.");
    }

    // Path to the objects' meshes
    objectsDir = new File(clientPath, "/Objects");
    if (!objectsDir.isDirectory()) {
      throw new FileNotFoundException("Path to clients Objects folder [" + objectsDir.getPath() + "] doesn't exist or is not a folder path");
    }
    houseAddressFile = getClientHouseAddressFile(clientPath);
  }

  private File getClientHouseAddressFile(final String aionClientPath) throws IOException {
    final File housePakFile = new File(aionClientPath, "Data/Housing/Housing.pak");
    final File houseAddressXmlFile = new File(tmpPath, "client_housing_address_orig.xml");
    final FileOutputStream origHouseAddressXmlStream = new FileOutputStream(houseAddressXmlFile);
    final Map<String, OutputStream> filterStreamMap = new HashMap<>();
    filterStreamMap.put("client_housing_address.xml", origHouseAddressXmlStream);
    try {
      pakAccessor.setPakFile(housePakFile.getPath(), 2);
      retreiveFromPak(housePakFile.getName() + ".zip", filterStreamMap, new AionLevelsProcessor.EndsWithStringComparer());
      pakAccessor.close();
    } catch (final Exception e) {
      System.err.println("ERROR: Cannot process Pak file: " + housePakFile.getPath());
      System.err.println(e.toString());
      if (houseAddressXmlFile.exists()) {
        houseAddressXmlFile.delete();
      }
    } finally {
      try {
        origHouseAddressXmlStream.close();
      } catch (final IOException e) {

      }
    }

    // decode XML
    final FileInputStream inputStream = new FileInputStream(houseAddressXmlFile);
    final File convertedHouseAddressXmlFile = new File(tmpPath, "client_housing_address.xml");
    try {
      final FileOutputStream outputStream = new FileOutputStream(convertedHouseAddressXmlFile);

      BinaryXmlDecoder.Decode(inputStream, outputStream);

      inputStream.close();
      outputStream.close();
    } finally {
      if (!houseAddressXmlFile.delete()) {
        System.err.println("Cannot delete file: " + houseAddressXmlFile.getPath());
      }
    }

    return convertedHouseAddressXmlFile;
  }

  private File getClientWorldIdFile() throws IOException {

    final File worldIdPakFile = new File(clientPath, "Data/World/World.pak");
    final File worldIdXmlFile = new File(tmpPath, "WorldId_orig.xml");
    final FileOutputStream origWorldIdXmlStream = new FileOutputStream(worldIdXmlFile);

    final Map<String, OutputStream> filterStreamMap = new HashMap<>();
    filterStreamMap.put("WorldId.xml", origWorldIdXmlStream);

    try {
      pakAccessor.setPakFile(worldIdPakFile.getPath(), 2);
      retreiveFromPak(worldIdPakFile.getName() + ".zip", filterStreamMap, new AionLevelsProcessor.EndsWithStringComparer());
      pakAccessor.close();
    } catch (final Exception e) {
      System.err.println("ERROR: Cannot process Pak file: " + worldIdPakFile.getPath());
      System.err.println(e.toString());

      if (worldIdXmlFile.exists()) {
        worldIdXmlFile.delete();
      }
    } finally {
      try {
        origWorldIdXmlStream.close();
      } catch (final IOException e) {
      }
    }

    // decode XML
    final FileInputStream inputStream = new FileInputStream(worldIdXmlFile);
    final File convertedWorldIdXmlFile = new File(tmpPath, "WorldId.xml");
    try {
      final FileOutputStream outputStream = new FileOutputStream(convertedWorldIdXmlFile);

      BinaryXmlDecoder.Decode(inputStream, outputStream);

      inputStream.close();
      outputStream.close();
    } finally {
      if (!worldIdXmlFile.delete()) {
        System.err.println("Cannot delete file: " + worldIdXmlFile.getPath());
      }
    }

    return convertedWorldIdXmlFile;
  }

  private void retreiveFromPak(final String zipFileName, final Map<String, OutputStream> filterStreamMap, final IStringComparer comparer)
      throws AionException, IOException {
    retreiveFromPak(zipFileName, filterStreamMap, comparer, false, null);
  }

  private void retreiveFromPak(final String zipFileName, final Map<String, OutputStream> filterStreamMap, final IStringComparer comparer, boolean toLowerCase, Charset charset)
      throws AionException, IOException {
    // convert to zip
    final File zipFile = new File(tmpPath, zipFileName);
    final FileOutputStream zipStream = new FileOutputStream(zipFile);
    pakAccessor.convertToZip(zipStream);
    zipStream.close();

    // extract files by filter
    try {
      ZipUtils.unzipEntry(zipFile, filterStreamMap, comparer, toLowerCase, charset);
    } finally {
      if (!zipFile.delete()) {
        System.err.println("Cannot delete file: " + zipFile.getPath() + " location: " + zipFileName);
      }
    }
  }

  private void collectMeshFilePaths() throws JDOMException, IOException {
    System.out.println("Collecting mesh file paths ...");
    clientMeshFiles = collectMeshesPath(new File[] { levelsCommonDir, levelsIdabproDir, objectsDir });

    //manager = new DirManager(clientPath, new File[] {levelsCommonDir, objectsDir} );

    if (clientMeshFiles.length == 0) {
      System.out.println("There are no mesh files in the Aion client. No geomaps generated.");
      return;
    } else {
      System.out.println("" + clientMeshFiles.length + " files collected");
    }

    // Read world_maps.xml and WorldId.xml and find Levels to process
    System.out.println("Generating avaiable levels list...");
    mapIdClientFolderMap = generateWorldMapsList(worldIdFile);

    System.out.println("Generating available house addresses...");
    try {
      houseAdresses = generateHouseAddressList();
    } finally {
      if (!houseAddressFile.delete()) {
        System.out.println("Cannot delete " + houseAddressFile.getPath());
      }
    }
    System.out.println("Done.");
  }

  private File[] collectMeshesPath(final File[] rootFolders) {
    File[] res = new File[0];
    for (final File file : rootFolders) {
      // collect meshes
      final File[] meshFiles = file.listFiles((dir, name) -> name.matches("Mesh_Meshes_\\d\\d\\d\\.pak") || name.endsWith("_Meshes.pak") || name.toLowerCase().endsWith("idabpro.pak"));
      if (meshFiles.length > 0) {
        res = CommonUtils.concat(res, meshFiles);
      }

      // process subfolders recursively
      final File[] subFolders = file.listFiles(File::isDirectory);
      if (subFolders.length > 0) {
        res = CommonUtils.concat(res, collectMeshesPath(subFolders));
      }
    }
    return res;
  }

  private Map<String, String> generateWorldMapsList(final File clientWorldMapsFile) throws JDOMException, IOException {
    final SAXBuilder builder = new SAXBuilder();

    // read client maps
    final Document document = builder.build(clientWorldMapsFile);
    final Element rootNode = document.getRootElement();
    boolean clientFile = rootNode.getName().equalsIgnoreCase("world_id");
    final List<?> list = clientFile ? rootNode.getChildren("data") : rootNode.getChildren("map");

    final Map<String, String> res = new HashMap<>();
    for (Object aList : list) {
      final Element node = (Element) aList;
      res.put(node.getAttributeValue("id"), clientFile ? node.getText() : node.getAttributeValue("cName"));
    }
    return res;
  }

  private void collectLevelFoldersName() {
    final File clientLevelRootFolder = new File(clientPath, "/Levels/");
    final FilenameFilter filter = (dir, name) -> {
      final File file = new File(dir, name);
      return file.isDirectory() && !name.toLowerCase().equals("common");
    };
    clientLevelFolders = clientLevelRootFolder.list(filter);
  }

  private Map<String, Integer> generateHouseAddressList() throws JDOMException, IOException{
    final SAXBuilder builder = new SAXBuilder();
    final Document document = builder.build(houseAddressFile);
    final Element rootNode = document.getRootElement();
    final List<?> addresses = rootNode.getChildren("client_housing_address");
    final Map<String, Integer> res = new HashMap<>();
    for (Object obj : addresses) {
      final Element node = (Element) obj;
      if (node.getChild("name") != null && node.getChild("id") != null) {
        String name = node.getChildText("name");
        if (!name.trim().isEmpty()) {
          String id = node.getChildText("id");
          if (!id.trim().isEmpty()) {
            if (res.containsKey(name)) {
              System.err.println("Duplicate house name in client_housing_adress.xml: " + name);
              continue;
            }
            res.put(name, Integer.parseInt(id));
          }
        }
      }
    }
    System.out.println("generated house adresses with size: " + res.size());
    return res;
  }

  private String correctLevelFolderName(final String folder) {
    final String n = folder.toLowerCase();
    for (final String dir : clientLevelFolders) {
      if (dir.toLowerCase().equals(n))
        return dir;
    }
    return null;
  }

  private String getLevelFileName(final String folder) {
    final File clientLevelFolder = new File(clientPath, "/Levels/" + folder);
    final FilenameFilter filter = (dir, name) -> {
      final File file = new File(clientPath, dir + "/" + name);
      return /* file.isFile() && */name.toLowerCase().equals("level.pak");
    };
    final String[] list = clientLevelFolder.list(filter);
    return list.length != 0 ? list[0] : null;
  }

  private boolean parseLevelPak(final String levelFolderName, final File clientLevelPakFile, final File brushLstFile, final File landMapH32File, final File objectsLstFile, final File levelDataFile, final File missionFile)
      throws FileNotFoundException {
    System.out.println(". . [Level.pak] Extracting data... ");
    final Map<String, OutputStream> filterStreamMap = new HashMap<>();

    FileOutputStream landMapH32OutputStream = new FileOutputStream(landMapH32File);
    filterStreamMap.put("land_map.h32", landMapH32OutputStream);

    final FileOutputStream brushLstStream = new FileOutputStream(brushLstFile);
    filterStreamMap.put("brush.lst", brushLstStream);

    final FileOutputStream objectsStream = new FileOutputStream(objectsLstFile);
    filterStreamMap.put("objects.lst", objectsStream);

    final FileOutputStream levelDataStream = new FileOutputStream(levelDataFile);
    filterStreamMap.put("leveldata.xml", levelDataStream);

    final FileOutputStream missionStream = new FileOutputStream(missionFile);
    filterStreamMap.put("mission_mission0.xml", missionStream);

    try {
      pakAccessor.setPakFile(clientLevelPakFile.getPath(), 2);
      retreiveFromPak(clientLevelPakFile.getName() + ".zip", filterStreamMap, new EndsWithStringComparer());
      pakAccessor.close();
    } catch (final Exception e) {
      System.out.println(". . . [ERROR] Cannot process Pak file: " + clientLevelPakFile.getPath());
      System.out.println(e.toString());

      if (landMapH32File.exists() && !landMapH32File.delete()) {
        System.out.println(". . . [WARN] Cannot delete file: " + landMapH32File.getPath());
      }
      if (brushLstFile.exists() && !brushLstFile.delete()) {
        System.out.println(". . . [WARN] Cannot delete file: " + brushLstFile.getPath());
      }
      if (objectsLstFile.exists() && !objectsLstFile.delete()){
        System.out.println(". . . [WARN] Cannot delete file: " + objectsLstFile.getPath());
      }
      if (levelDataFile.exists() && !levelDataFile.delete()) {
        System.out.println(". . . [WARN] Cannot delete file: " + levelDataFile.getPath());
      }
      if (missionFile.exists() && !missionFile.delete()) {
        System.out.println(". . . [WARN] Cannot delete file: " + missionFile.getPath());
      }
      return false;
    } finally {
      try {
        landMapH32OutputStream.close();
        brushLstStream.close();
        objectsStream.close();
        levelDataStream.close();
        missionStream.close();
      } catch (final IOException e) {
      }
    }
    System.out.println(". . [Level.pak] Done");
    return true;
  }

  private List<Byte> parseTerrainMaterials(final File levelDataXml) {
    System.out.println(". . [leveldata.xml] processing");
    List<String> materials = new ArrayList<>();
    List<Byte> materialIds = new ArrayList<>();
    final SAXBuilder builder = new SAXBuilder();
    try {
      final Document document = builder.build(levelDataXml);
      final Element rootNode = document.getRootElement();
      final List<?> objects = rootNode.getChildren("SurfaceTypes").get(0).getChildren();
      for (Object obj : objects) {
        final Element node = (Element) obj;
        String s = node.getAttributeValue("Material").trim();
        materials.add(node.getAttributeValue("Material").trim());
      }
      
    } catch (Exception e) {
      System.err.println(e.toString());
      e.printStackTrace();
    }

    for (int i = 0; i < materials.size(); i++) {
      int matId = CgfLoader.getMaterialIdFor(materials.get(i));
      if (CgfLoader.isMaterialIntention(matId)) {
        materialIds.add((byte)matId);
      } else {
        materialIds.add((byte)0);
      }
    }
    System.out.println(". . [leveldata.xml] Done");
    if (materialIds.isEmpty()) {
      return null;
    }
    return materialIds;
  }

  private List<BrushEntry> parseBrushLst(final String levelFolderName, final File brushLstFile) {
    System.out.println(". . [brush.lst] processing");
    List<BrushEntry> brushEntries = new ArrayList<>();
    try {
      BrushLstMeshData meshData = new BrushLstMeshData();
      brushLoader.load(brushLstFile.getPath());
      meshData.meshFiles = brushLoader.getMeshFileNames();
      meshData.meshUsage = new int[meshData.meshFiles.size()];
      levelBrushMeshData.put(levelFolderName, meshData);
      brushEntries = brushLoader.getMeshEntries();
      brushLoader.close();
    } catch (Exception e) {
      System.err.println(". . . [ERROR] Cannot access mesh file: " + brushLstFile.getPath());
      System.err.println(e.toString());
      e.printStackTrace();
    } finally {
      if (brushLstFile.exists() && !brushLstFile.delete()) {
        System.out.println(". . . [WARN] Cannot delete file: " + brushLstFile.getPath());
      }
    }
    System.out.println(". . [brush.lst] Done");
    return brushEntries;
  }

  private List<ObjectEntry> parseObjects(final String levelFolderName, final File objectsFile, final File levelDataFile) {
    System.out.println(". . [objects.lst] processing");
    List<ObjectEntry> objectEntries = new ArrayList<>();
    try {
      objectsLoader.loadLevelData(levelDataFile, objectsFile.getPath());
      ObjectMeshData meshData = new ObjectMeshData();
      meshData.meshFiles = objectsLoader.getVegetationFileNames();
      meshData.meshUsage = new int[meshData.meshFiles.size()];
      levelObjectsMeshData.put(levelFolderName, meshData);
      objectEntries = objectsLoader.getObjectEntries();
      objectsLoader.clear();
    } catch (Exception e) {
      System.err.println(e.toString());
      e.printStackTrace();
    }
    System.out.println(". . [objects.lst] done");
    return objectEntries;
  }

  private List<EntityEntry> parseEntities(final String levelFolderName, final File missionFile) {
    System.out.println(". . [mission_mission0.xml] processing entities");
    List<EntityEntry> entityEntries = new ArrayList<>();
    try {
      entityLoader.loadPlaceables(missionFile, houseAdresses);
      EntityMeshData meshData = new EntityMeshData();
      meshData.placeableMeshFiles = entityLoader.getEntityFileNames(EntityClass.PLACEABLE);
      meshData.basicMeshFiles = entityLoader.getEntityFileNames(EntityClass.BASIC);
      meshData.townMeshFiles = entityLoader.getEntityFileNames(EntityClass.TOWN_OBJECT);
      meshData.houseMeshFiles = entityLoader.getEntityFileNames(EntityClass.HOUSE);
      meshData.houseDoorMeshFiles = entityLoader.getEntityFileNames(EntityClass.HOUSE_DOOR);
      meshData.doorMeshFiles = entityLoader.getEntityFileNames(EntityClass.DOOR);
      levelEntitiesMeshData.put(levelFolderName, meshData);
      entityEntries = entityLoader.getEntityEntries();
      entityLoader.clear();
    } catch (Exception e) {
      System.err.println(e.toString());
      e.printStackTrace();
    }
    System.out.println(". . [mission_mission0.xml] entities done");
    return entityEntries;

  }

  private void createGeoFile(final String clientLevelId, final File landMapH32File,
                               final List<BrushEntry> brushEntries, final BrushLstMeshData brushMeshData,
                final List<ObjectEntry> objectEntries, final ObjectMeshData objectMeshData,
                               final List<EntityEntry> entityEntries, final List<Byte> terrainMaterials) throws IOException {
    final File geoFile = new File(outPath, clientLevelId +".geo");
    final FileOutputStream geoFileStream = new FileOutputStream(geoFile);
    final LittleEndianDataOutputStream stream = new LittleEndianDataOutputStream(geoFileStream);

    boolean isEmpty = true;
    // terrain
    if (landMapH32File.exists() && landMapH32File.length() > 0) {
      FileInputStream landInputStream = new FileInputStream(landMapH32File);
      int mapPointSize = terrainMaterials != null ? 3 : MAP_POINT_SIZE;
      final byte[] h32PointBuffer = new byte[(int)landMapH32File.length()];
      final byte[] mapPointBuffer = new byte[h32PointBuffer.length / H32_POINT_SIZE * mapPointSize];

      final int bytesRead = landInputStream.read(h32PointBuffer);
      if (bytesRead > 0) {
        if (bytesRead % H32_POINT_SIZE != 0) {
          throw new IOException(". Wrong count of bytes per point in input stream");
        }
        final int pointsCount = bytesRead / H32_POINT_SIZE;
        for (int i = 0; i < pointsCount; i++) {
          if (h32PointBuffer[i * 3 + 2] == 0x3F) { // cutout, Short.MIN_VALUE
            mapPointBuffer[i*mapPointSize] = 0;
            mapPointBuffer[i*mapPointSize + 1] = -128;
            if (mapPointSize == 3) {
              mapPointBuffer[i*mapPointSize + 2] = 0;
            }
          } else {
            mapPointBuffer[i * mapPointSize] = h32PointBuffer[i * 3]; // p1
            mapPointBuffer[i * mapPointSize + 1] = h32PointBuffer[i * 3 + 1]; // p2
            if (mapPointSize == 3) {
              mapPointBuffer[i*mapPointSize + 2] = terrainMaterials.get((h32PointBuffer[i * 3 + 2] & 0xFF));
            }
            if (isEmpty) {
              if (mapPointBuffer[i * 2] != 0 || mapPointBuffer[i * 2 + 1] != 0) {
                isEmpty = false;
              }
            }
          }
          // h32PointBuffer[i * 3 + 2] == 0x3F ? cutout(remove quad) : material index in level data; // p3
        }
        if (!isEmpty) {
          stream.writeByte(1); // mesh exists
          stream.writeInt((int) landMapH32File.length() / 3); // terrain data count
          stream.write(mapPointBuffer, 0, pointsCount * mapPointSize);
        }
      }
    }
    if (isEmpty) {
      stream.writeByte(0); // mesh does not exist
      stream.writeShort(0); // stub
      // TODO: 0 for no cutout data
    }

    // brushes
    if (brushEntries != null && brushEntries.size() > 0) {
      for (final BrushEntry entry : brushEntries) {
        final String meshFileName = brushMeshData.meshFiles.get(entry.meshIdx);
        if (!processedCgfs.contains(meshFileName)) { // skip not loaded or empty cgf
          //System.out.println(". [WARN] [" + clientLevelId + "] Cgf was not processed: " + meshFileName);
          continue;
        }
        if (emptyCgfs.contains(meshFileName)) {
          continue;
        }
        final byte[] meshFileNameBytes = meshFileName.getBytes(Charset.forName("ASCII"));
        stream.writeShort(meshFileNameBytes.length);
        stream.write(meshFileNameBytes);
        final float[] matrix = entry.matrix;
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
    if (objectEntries != null && objectMeshData != null && objectEntries.size() > 0) {
      for (ObjectEntry entry : objectEntries) {
        String meshFileName = objectMeshData.meshFiles.get(entry.objectId);
        if (!processedCgfs.contains(meshFileName)) { // skip not loaded or empty cgf
          //System.out.println(". [WARN] [" + clientLevelId + "] Cgf was not processed: " + meshFileName);
          continue;
        }
        if (emptyCgfs.contains(meshFileName)) {
          continue;
        }
        byte[] meshFileNameBytes = meshFileName.getBytes(Charset.forName("ASCII"));
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

    if (entityEntries != null && entityEntries.size() > 0) {
      for (EntityEntry entry : entityEntries) {
        if (entry instanceof HouseEntry) {
          writeHouseEntry((HouseEntry) entry, stream);
        } else if (entry instanceof DoorEntry) {
                  writeDoorEntry((DoorEntry) entry, stream);
        } else {
          writeEntityEntry(entry, stream);
        }
      }
    }

    stream.close();
    geoFileStream.close();
  }

  private void writeHouseEntry(HouseEntry entry, LittleEndianDataOutputStream stream) throws IOException {
    if (entry.meshes.size() > 0) {
      for (String mesh : entry.meshes) {
        if (processedCgfs.contains(mesh) && !emptyCgfs.contains(mesh)) {
          byte[] nameBytes = mesh.getBytes(Charset.forName("ASCII"));
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
    }
  }

  private void writeEntityEntry(EntityEntry entry, LittleEndianDataOutputStream stream) throws IOException {
    boolean exists = true;
    if (!processedCgfs.contains(entry.mesh) || emptyCgfs.contains(entry.mesh)) {
      if (entry.entityClass == EntityClass.TOWN_OBJECT) {
        exists = false;
      } else {
        return;
      }
    }
    if (entry.entityClass == EntityClass.TOWN_OBJECT && !exists) {
      boolean found = false;
      for (int i = entry.level; i >= 1; i--) {
        String meshName = entry.mesh.replace(entry.level + ".cgf", i + ".cgf");
        if (processedCgfs.contains(meshName) && !emptyCgfs.contains(meshName)) {
          entry.mesh = meshName;
          found = true;
          break;
        }
      }
      if (!found) {
        System.err.println(". Could not find Entity cgf for EntityClass Town_Object: " + entry.mesh);
        return;
      }
    }
    String name = entry.mesh;
    byte[] nameBytes = name.getBytes(Charset.forName("ASCII"));
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

  private void writeDoorEntry(DoorEntry entry, LittleEndianDataOutputStream stream) throws IOException {
    if (!processedCgfs.contains(entry.mesh)) { // skip not loaded or empty cgf
      return;
    }
    if (emptyCgfs.contains(entry.mesh)) {
      return;
    }
    String name = entry.mesh + entry.suffix;
    byte [] nameBytes = name.getBytes(Charset.forName("ASCII"));
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



  private void findCgfFiles(Map<String, File> cgfFiles, Map<String, OutputStream> filterStreamMap) throws IOException {
    final Set<String> cgfFileNames = pakAccessor.getFilesName();
    for (String name : cgfFileNames) {
      if (requiredCgfs.isEmpty()) {
        break;
      } else if (requiredCgfs.contains(name)) {
        if (processedCgfs.contains(name)) {
          continue;
        } else if (emptyCgfs.contains(name)) {
          continue;
        } else {
          final File cgfOutputFile = new File(meshesTmpFolder, name.replaceAll("/", "_"));
          cgfFiles.put(name, cgfOutputFile);
          if (!cgfOutputFile.exists() || cgfOutputFile.length() <= 0) {
            final FileOutputStream cgfOutputStream = new FileOutputStream(cgfOutputFile);
            filterStreamMap.put(name, cgfOutputStream);
          }
          requiredCgfs.remove(name);
        }
      }
    }
  }

  private void processCgf(Map.Entry<String, File> cgfFileInfo) {
    try {
      if (!processedCgfs.contains(cgfFileInfo.getKey())) {
        File f = new File(cgfFileInfo.getValue().getPath());
        if (!f.exists()) {
          System.out.println(". [WARN] CGF does not exist: " + cgfFileInfo.getValue().getPath());
        } else {
          if (!ignoreCgfs.contains(cgfFileInfo.getKey())) {
            processedCgfs.add(cgfFileInfo.getKey());
            CgfLoader loader = new CgfLoader();
            loader.load(cgfFileInfo.getValue().getPath());
            List<MeshData> meshes = new ArrayList<>();
            if (requiredDoorCgas.contains(cgfFileInfo.getKey())) {
              loader.traverseNodes(meshes);
              if (meshes.size() > 0) {
                availableMeshes.putIfAbsent(cgfFileInfo.getKey(), meshes);
                CgfLoader loaderState2 = loader.cloneAtTime(999999);
                List<MeshData> meshes2 = new ArrayList<>();
                loaderState2.traverseNodes(meshes2);
                if (meshes2.size() > 0) {
                  availableMeshes.putIfAbsent(cgfFileInfo.getKey() + "_state2", meshes2);
                }
              } else {
                emptyCgfs.add(cgfFileInfo.getKey());
              }
            } else {
              loader.traverseNodes(meshes);
              if (meshes.size() > 0) {
                availableMeshes.putIfAbsent(cgfFileInfo.getKey(), meshes);
              } else {
                emptyCgfs.add(cgfFileInfo.getKey());
              }
            }
          }
        }
      }
    } catch (IOException e) {

    }
  }

  private void writeMeshes(String path, List<MeshData> data, LittleEndianDataOutputStream stream, boolean isDoor, EntityClass entityClass) throws IOException {
    byte[] nameBytes = path.getBytes(Charset.forName("ASCII"));
    stream.writeShort(nameBytes.length);
    stream.write(nameBytes);
    stream.writeShort(data.size());
    for (MeshData mesh : data) {
      stream.writeShort(mesh.vertices.size());
      for (Vector3 vec : mesh.vertices) {
        stream.writeFloat(vec.x);
        stream.writeFloat(vec.y);
        stream.writeFloat(vec.z);
      }
      stream.writeInt(mesh.indices.size() * 3);
      for (MeshFace face : mesh.indices) {
        stream.writeShort(face.v0);
        stream.writeShort(face.v1);
        stream.writeShort(face.v2);
      }
      stream.writeByte(mesh.materialId);
      if (BrushLstLoader.eventMeshes.contains(path)) {
        mesh.collisionIntention |= CollisionIntention.EVENT.getId();
      }
      stream.writeByte(mesh.collisionIntention);
    }
  }
}
