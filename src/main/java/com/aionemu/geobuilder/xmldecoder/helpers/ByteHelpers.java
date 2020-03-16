package com.aionemu.geobuilder.xmldecoder.helpers;

import java.nio.charset.StandardCharsets;


public final class ByteHelpers {

	private ByteHelpers() {
	}

	public static String ReadUTF16Z(byte[] data, int startIndex) {
		int endIndex = startIndex;
		while (endIndex < data.length && (data[endIndex] != 0 || data[endIndex + 1] != 0))
			endIndex += 2;
		if (endIndex == startIndex)
			return "";
		return new String(data, startIndex, endIndex - startIndex, StandardCharsets.UTF_16LE);
	}
}
