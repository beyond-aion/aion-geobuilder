package com.aionemu.geobuilder.pakaccessor;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

abstract class PakBlock {

  long bodyStartPosition;

  abstract long getBodySize();

  abstract void read(ByteBuffer stream) throws IOException;

  abstract void write(DataOutputStream stream) throws IOException;
}
