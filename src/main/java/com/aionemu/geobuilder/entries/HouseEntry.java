package com.aionemu.geobuilder.entries;

import java.util.ArrayList;
import java.util.List;

public class HouseEntry extends EntityEntry {

  public int address;
  public List<String> meshes = new ArrayList<>();

  public HouseEntry() {
    type = EntryType.HOUSE;
  }

}
