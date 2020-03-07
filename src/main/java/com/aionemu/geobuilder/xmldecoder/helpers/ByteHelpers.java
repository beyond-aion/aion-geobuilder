package com.aionemu.geobuilder.xmldecoder.helpers;

import com.aionemu.geobuilder.utils.BitConverter;

import java.nio.charset.StandardCharsets;


public final class ByteHelpers {

	private ByteHelpers() {
	}

	public static void Reset(byte[] data, byte value) {
		for (int index = 0; index < data.length; ++index)
			data[index] = value;
	}

	public static String ReadUTF16Z(byte[] data, int offset) throws Exception {
		int startIndex = offset;
		while (startIndex < data.length && BitConverter.toInt16(data, startIndex) != 0)
			startIndex += 2;
		if (startIndex == offset)
			return "";
		return new String(data, offset, startIndex - offset, StandardCharsets.UTF_16LE);
	}
}
