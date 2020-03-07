package com.aionemu.geobuilder.pakaccessor;

import com.aionemu.geobuilder.utils.DataInputStream;
import com.aionemu.geobuilder.utils.DataOutputStream;

import java.io.IOException;

abstract class PakBlock {

  long bodyStartPosition;

  abstract long getBodySize();

  abstract void read(DataInputStream stream) throws IOException;

  abstract void write(DataOutputStream stream) throws IOException;
}
