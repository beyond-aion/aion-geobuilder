package com.aionemu.geobuilder.entries;

import java.util.stream.IntStream;
import java.util.stream.Stream;

public class TownEntry extends EntityEntry {

  public int level;
  public int townId;

  public TownEntry() {
    type = EntryType.TOWN;
  }
  @Override
  public Stream<String> getAllMeshNames() {
    return Stream.concat(super.getAllMeshNames(), getHigherLevelMeshNames());
  }

  /**
   * The mission xml only references town_grow_*_01.cgf files. The client automatically loads 02.cgf, …, 05.cgf according to the town's level.
   * Not every town level has a dedicated cgf. In such cases the client uses the next lower level town_grow_* cgf it can find.
   * For example if 02.cgf and 03.cgf are missing, then level 2 and 3 use 01.cgf.
   * This client-side logic must be replicated by the server, otherwise these additional meshes are not used.
   */
  public Stream<String> getHigherLevelMeshNames() {
    return IntStream.range(level + 1, 6).mapToObj(i -> mesh.replace("_01.cgf", "_0" + i + ".cgf"));
  }
}
