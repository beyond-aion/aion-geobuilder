package com.aionemu.geobuilder.xmldecoder.helpers;

import com.aionemu.geobuilder.utils.BitConverter;

import java.nio.charset.Charset;


public final class ByteHelpers {

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
		else {
			Charset charset = Charset.forName("UTF-16LE");
			// CharsetDecoder decoder = charset.newDecoder();
			// CharBuffer cbuf = decoder.decode(ByteBuffer.wrap(data, offset, startIndex - offset));
			// return cbuf.toString();

			return new String(data, offset, startIndex - offset, charset);
			// return Encoding.Unicode.GetString(data, offset, startIndex - offset);
		}
	}

	// public static String ReadUTF16Z(byte[] data, uint offset)
	// {
	// return ByteHelpers.ReadUTF16Z(data, (int) offset);
	// }
}
