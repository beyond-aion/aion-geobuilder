package com.aionemu.geobuilder.entries;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class HouseEntry extends EntityEntry {

  public int address;
  public List<String> meshes = new ArrayList<>();

  public HouseEntry() {
    type = EntryType.HOUSE;
  }

  @Override
  public Stream<String> getAllMeshNames() {
    return Stream.concat(super.getAllMeshNames(), meshes.stream());
  }
}
