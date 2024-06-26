package com.aionemu.geobuilder;

import com.aionemu.geobuilder.entries.EntityEntry;
import com.aionemu.geobuilder.meshData.BrushLstMeshData;
import com.aionemu.geobuilder.meshData.ObjectMeshData;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class LevelData {

  public final String levelId;
  public final String levelName;
  public final Path clientLevelPakFile;
  public String missionPath;
  public boolean useTerrain;
  public volatile Terrain terrain;
  public volatile BrushLstMeshData brushMeshData;
  public volatile ObjectMeshData objectMeshData;
  public volatile List<EntityEntry> entityEntries = Collections.emptyList();

  public LevelData(String levelId, String levelName, Path clientLevelPakFile) {
    this.levelId = levelId;
    this.levelName = levelName;
    this.clientLevelPakFile = clientLevelPakFile;
  }

  public boolean isTestLevel() {
    return levelId.charAt(0) == '9' && levelName.toLowerCase().contains("test") || levelName.equalsIgnoreCase("IDTest_Dungeon");
  }

  public Stream<String> streamAllMeshFileNames() {
    Stream<Stream<String>> streams = Stream.of(
      brushMeshData == null ? Stream.empty() : brushMeshData.brushEntries.stream().mapToInt(o -> o.meshIndex).distinct().mapToObj(brushMeshData.meshFileNames::get), // map only used meshes in case some are not placed in brush.lst
      objectMeshData == null ? Stream.empty() : objectMeshData.objectEntries.stream().mapToInt(o -> o.meshIndex).distinct().mapToObj(objectMeshData.meshFiles::get), // map only used meshes in case some are not placed in objects.lst
      entityEntries.stream().flatMap(EntityEntry::getAllMeshNames)
    );
    return streams.flatMap(s->s);
  }

  @Override
  public String toString() {
    return '[' + levelId + "] " + levelName;
  }
}
