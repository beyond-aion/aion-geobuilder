package com.aionemu.geobuilder.xmldecoder.fileformats;

import java.io.IOException;
import java.io.InputStream;


public final class BinaryXmlFileHelpers {

	public static int ReadPackedS32(InputStream stream) throws IOException {
		int num1 = stream.read();
		int num2 = 0;
		int num3 = 0;
		for (; num1 >= 128; num1 = stream.read()) {
			num2 |= (num1 & Byte.MAX_VALUE) << num3;
			num3 += 7;
		}
		return num2 | num1 << num3;
	}
}
