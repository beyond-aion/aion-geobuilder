package com.aionemu.geobuilder.xmldecoder.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.aionemu.geobuilder.utils.BitConverter;
import com.aionemu.geobuilder.utils.CommonUtils;

public final class StreamHelpers {

	protected static boolean ShouldSwap(boolean littleEndian) {
		return littleEndian && !CommonUtils.isLittleEndianByteOrder || !littleEndian && CommonUtils.isLittleEndianByteOrder;
	}

	// public static ByteArrayOutputStream ReadToMemoryStream(InputStream stream, long size)
	// {
	// ByteArrayOutputStream memoryStream = new ByteArrayOutputStream();
	// long val1 = size;
	// byte[] buffer = new byte[4096];
	// while (val1 > 0L)
	// {
	// int count = (int) Math.min(val1, 4096L);
	// stream.read(buffer, 0, count);
	// memoryStream.write(buffer, 0, count);
	// val1 -= (long) count;
	// }
	// memoryStream.Seek(0L, SeekOrigin.Begin);
	// return memoryStream;
	// }

	public static void WriteFromStream(OutputStream stream, InputStream input, long size) throws IOException {
		long val1 = size;
		byte[] buffer = new byte[4096];
		while (val1 > 0L) {
			int count = (int) Math.min(val1, 4096L);
			input.read(buffer, 0, count);
			stream.write(buffer, 0, count);
			val1 -= count;
		}
	}

	// public static int ReadAligned(InputStream stream, byte[] buffer, int offset, int size, int align)
	// {
	// if (size == 0)
	// return 0;
	// int num1 = stream.read(buffer, offset, size);
	// int num2 = size % align;
	// if (num2 > 0)
	// stream.Seek((long) (align - num2), SeekOrigin.Current);
	// return num1;
	// }

	public static void WriteAligned(OutputStream stream, byte[] buffer, int offset, int size, int align) throws IOException {
		if (size == 0)
			return;
		stream.write(buffer, offset, size);
		int num = size % align;
		if (num <= 0)
			return;
		byte[] buffer1 = new byte[align - num];
		stream.write(buffer1, 0, align - num);
	}

	public static boolean ReadBoolean(InputStream stream) throws IOException {
		return StreamHelpers.ReadU8(stream) > 0;
	}

	public static void WriteBoolean(OutputStream stream, boolean value) throws IOException {
		StreamHelpers.WriteU8(stream, value ? (byte) 1 : (byte) 0);
	}

	public static float ReadF32(InputStream stream) throws Exception {
		return StreamHelpers.ReadF32(stream, true);
	}

	public static float ReadF32(InputStream stream, boolean littleEndian) throws Exception {
		byte[] buffer = new byte[4];
		// Debug.Assert(stream.read(buffer, 0, 4) == 4);
		if (StreamHelpers.ShouldSwap(littleEndian))
			return BitConverter.toSingle(BitConverter.getBytes(NumberHelpers.Swap(BitConverter.toInt32(buffer, 0))), 0);
		else
			return BitConverter.toSingle(buffer, 0);
	}

	public static void WriteF32(OutputStream stream, float value) throws Exception {
		StreamHelpers.WriteF32(stream, value, true);
	}

	public static void WriteF32(OutputStream stream, float value, boolean littleEndian) throws Exception {
		byte[] buffer = !StreamHelpers.ShouldSwap(littleEndian) ? BitConverter.getBytes(value) : BitConverter.getBytes(NumberHelpers.Swap(BitConverter
			.toInt32(BitConverter.getBytes(value), 0)));
		// Debug.Assert(buffer.Length == 4);
		stream.write(buffer, 0, 4);
	}

	public static double ReadF64(InputStream stream) throws Exception {
		return StreamHelpers.ReadF64(stream, true);
	}

	public static double ReadF64(InputStream stream, boolean littleEndian) throws Exception {
		byte[] buffer = new byte[8];
		// Debug.Assert(stream.read(buffer, 0, 8) == 8);
		if (StreamHelpers.ShouldSwap(littleEndian))
			return BitConverter.int64BitsToDouble(NumberHelpers.Swap(BitConverter.toInt64(buffer, 0)));
		else
			return BitConverter.toDouble(buffer, 0);
	}

	public static void WriteF64(OutputStream stream, double value) throws IOException {
		StreamHelpers.WriteF64(stream, value, true);
	}

	public static void WriteF64(OutputStream stream, double value, boolean littleEndian) throws IOException {
		byte[] buffer = !StreamHelpers.ShouldSwap(littleEndian) ? BitConverter.getBytes(value) : BitConverter.getBytes(NumberHelpers.Swap(BitConverter
			.doubleToInt64Bits(value)));
		// Debug.Assert(buffer.Length == 8);
		stream.write(buffer, 0, 8);
	}

	public static short ReadS16(InputStream stream) throws Exception {
		return StreamHelpers.ReadS16(stream, true);
	}

	public static short ReadS16(InputStream stream, boolean littleEndian) throws Exception {
		byte[] buffer = new byte[2];
		// Debug.Assert(stream.read(buffer, 0, 2) == 2);
		short num = BitConverter.toInt16(buffer, 0);
		if (StreamHelpers.ShouldSwap(littleEndian))
			num = NumberHelpers.Swap(num);
		return num;
	}

	public static void WriteS16(OutputStream stream, short value) throws IOException {
		StreamHelpers.WriteS16(stream, value, true);
	}

	public static void WriteS16(OutputStream stream, short value, boolean littleEndian) throws IOException {
		if (StreamHelpers.ShouldSwap(littleEndian))
			value = NumberHelpers.Swap(value);
		byte[] bytes = BitConverter.getBytes(value);
		// Debug.Assert(bytes.Length == 2);
		stream.write(bytes, 0, 2);
	}

	public static int ReadS32(InputStream stream) throws Exception {
		return StreamHelpers.ReadS32(stream, true);
	}

	public static int ReadS32(InputStream stream, boolean littleEndian) throws Exception {
		byte[] buffer = new byte[4];
		// Debug.Assert(stream.read(buffer, 0, 4) == 4);
		int num = BitConverter.toInt32(buffer, 0);
		if (StreamHelpers.ShouldSwap(littleEndian))
			num = NumberHelpers.Swap(num);
		return num;
	}

	public static void WriteS32(OutputStream stream, int value) throws IOException {
		StreamHelpers.WriteS32(stream, value, true);
	}

	public static void WriteS32(OutputStream stream, int value, boolean littleEndian) throws IOException {
		if (StreamHelpers.ShouldSwap(littleEndian))
			value = NumberHelpers.Swap(value);
		byte[] bytes = BitConverter.getBytes(value);
		// Debug.Assert(bytes.Length == 4);
		stream.write(bytes, 0, 4);
	}

	public static long ReadS64(InputStream stream) throws Exception {
		return StreamHelpers.ReadS64(stream, true);
	}

	public static long ReadS64(InputStream stream, boolean littleEndian) throws Exception {
		byte[] buffer = new byte[8];
		// Debug.Assert(stream.read(buffer, 0, 8) == 8);
		long num = BitConverter.toInt64(buffer, 0);
		if (StreamHelpers.ShouldSwap(littleEndian))
			num = NumberHelpers.Swap(num);
		return num;
	}

	public static void WriteS64(OutputStream stream, long value) throws IOException {
		StreamHelpers.WriteS64(stream, value, true);
	}

	public static void WriteS64(OutputStream stream, long value, boolean littleEndian) throws IOException {
		if (StreamHelpers.ShouldSwap(littleEndian))
			value = NumberHelpers.Swap(value);
		byte[] bytes = BitConverter.getBytes(value);
		// Debug.Assert(bytes.Length == 8);
		stream.write(bytes, 0, 8);
	}

	public static byte ReadS8(InputStream stream) throws IOException {
		return (byte) stream.read();
	}

	public static void WriteS8(OutputStream stream, byte value) throws IOException {
		stream.write(value);
	}

	public static short ReadU16(InputStream stream) throws Exception {
		return StreamHelpers.ReadU16(stream, true);
	}

	public static short ReadU16(InputStream stream, boolean littleEndian) throws Exception {
		byte[] buffer = new byte[2];
		// Debug.Assert(stream.read(buffer, 0, 2) == 2);
		short num = BitConverter.toInt16(buffer, 0);
		if (StreamHelpers.ShouldSwap(littleEndian))
			num = NumberHelpers.Swap(num);
		return num;
	}

	public static void WriteU16(OutputStream stream, short value) throws IOException {
		StreamHelpers.WriteU16(stream, value, true);
	}

	public static void WriteU16(OutputStream stream, short value, boolean littleEndian) throws IOException {
		if (StreamHelpers.ShouldSwap(littleEndian))
			value = NumberHelpers.Swap(value);
		byte[] bytes = BitConverter.getBytes(value);
		// Debug.Assert(bytes.Length == 2);
		stream.write(bytes, 0, 2);
	}

	public static int ReadU24(InputStream stream) throws Exception {
		return StreamHelpers.ReadU24(stream, true);
	}

	public static int ReadU24(InputStream stream, boolean littleEndian) throws Exception {
		byte[] buffer = new byte[4];
		// Debug.Assert(stream.read(buffer, 0, 3) == 3);
		int num = BitConverter.toInt32(buffer, 0);
		if (StreamHelpers.ShouldSwap(littleEndian))
			num = NumberHelpers.Swap24(num);
		return num & 16777215;
	}

	public static void WriteU24(OutputStream stream, int value) throws IOException {
		StreamHelpers.WriteU24(stream, value, true);
	}

	public static void WriteU24(OutputStream stream, int value, boolean littleEndian) throws IOException {
		if (StreamHelpers.ShouldSwap(littleEndian))
			value = NumberHelpers.Swap24(value);
		value &= 16777215;
		byte[] bytes = BitConverter.getBytes(value);
		// Debug.Assert(bytes.Length == 4);
		stream.write(bytes, 0, 3);
	}

	public static int ReadU32(InputStream stream) throws Exception {
		return StreamHelpers.ReadU32(stream, true);
	}

	public static int ReadU32(InputStream stream, boolean littleEndian) throws Exception {
		byte[] buffer = new byte[4];
		// Debug.Assert(stream.read(buffer, 0, 4) == 4);
		int num = BitConverter.toInt32(buffer, 0);
		if (StreamHelpers.ShouldSwap(littleEndian))
			num = NumberHelpers.Swap(num);
		return num;
	}

	public static void WriteU32(OutputStream stream, int value) throws IOException {
		StreamHelpers.WriteU32(stream, value, true);
	}

	public static void WriteU32(OutputStream stream, int value, boolean littleEndian) throws IOException {
		if (StreamHelpers.ShouldSwap(littleEndian))
			value = NumberHelpers.Swap(value);
		byte[] bytes = BitConverter.getBytes(value);
		// Debug.Assert(bytes.Length == 4);
		stream.write(bytes, 0, 4);
	}

	public static long ReadU64(InputStream stream) throws Exception {
		return StreamHelpers.ReadU64(stream, true);
	}

	public static long ReadU64(InputStream stream, boolean littleEndian) throws Exception {
		byte[] buffer = new byte[8];
		// Debug.Assert(stream.read(buffer, 0, 8) == 8);
		long num = BitConverter.toInt64(buffer, 0);
		if (StreamHelpers.ShouldSwap(littleEndian))
			num = NumberHelpers.Swap(num);
		return num;
	}

	public static void WriteU64(OutputStream stream, long value) throws IOException {
		StreamHelpers.WriteU64(stream, value, true);
	}

	public static void WriteU64(OutputStream stream, long value, boolean littleEndian) throws IOException {
		if (StreamHelpers.ShouldSwap(littleEndian))
			value = NumberHelpers.Swap(value);
		byte[] bytes = BitConverter.getBytes(value);
		// Debug.Assert(bytes.Length == 8);
		stream.write(bytes, 0, 8);
	}

	public static int ReadU8(InputStream stream) throws IOException {
		return stream.read();
	}

	public static void WriteU8(OutputStream stream, byte value) throws IOException {
		stream.write(value);
	}

	// public static String ReadASCII(InputStream stream, int size, boolean trailingNull)
	// {
	// byte[] numArray = new byte[size];
	// stream.read(numArray, 0, numArray.length);
	// int length = numArray.length;
	// if (trailingNull)
	// {
	// while (length > 0 && (int) numArray[length - 1] == 0)
	// --length;
	// }
	// Charset.forName("ACII").decode(new MappedByteBuffer())
	// return Encoding.ASCII.GetString(numArray, 0, length);
	// }
	//
	// public static String ReadASCII(InputStream stream, uint size)
	// {
	// return StreamHelpers.ReadASCII(stream, size, false);
	// }
	//
	// public static String ReadASCIIZ(InputStream stream)
	// {
	// int index = 0;
	// byte[] arr = new byte[64];
	// while (true)
	// {
	// if (index >= arr.length)
	// {
	// if (arr.length < 4096)
	// Array.Resize<byte>(ref array, arr.Length + 64);
	// else
	// break;
	// }
	// stream.read(arr, index, 1);
	// if ((int) arr[index] != 0)
	// ++index;
	// else
	// goto label_7;
	// }
	// throw new InvalidOperationException();
	// label_7:
	// if (index == 0)
	// return "";
	// else
	// return Encoding.ASCII.GetString(array, 0, index);
	// }
	//
	// public static void WriteASCII(OutputStream stream, String value)
	// {
	// byte[] bytes = Encoding.ASCII.GetBytes(value);
	// stream.write(bytes, 0, bytes.length);
	// }
	//
	// public static void WriteASCIIZ(OutputStream stream, String value)
	// {
	// byte[] bytes = Encoding.ASCII.GetBytes(value);
	// stream.write(bytes, 0, bytes.length);
	// stream.write(0);
	// }

	// public static String ReadUTF16(InputStream stream, int size)
	// {
	// return StreamHelpers.ReadUTF16(stream, size, true);
	// }
	//
	// public static String ReadUTF16(InputStream stream, int size, boolean littleEndian)
	// {
	// byte[] numArray = new byte[size];
	// stream.read(numArray, 0, numArray.length);
	// if (littleEndian)
	// return Encoding.Unicode.GetString(numArray);
	// else
	// return Encoding.BigEndianUnicode.GetString(numArray);
	// }
	//
	// public static String ReadUTF16Z(InputStream stream)
	// {
	// return StreamHelpers.ReadUTF16Z(stream, true);
	// }
	//
	// public static String ReadUTF16Z(InputStream stream, boolean littleEndian)
	// {
	// int num = 0;
	// byte[] array = new byte[128];
	// while (true)
	// {
	// stream.read(array, num, 2);
	// if ((int) BitConverter.toInt16(array, num) != 0)
	// {
	// if (num >= array.Length)
	// {
	// if (array.Length < 8192)
	// Array.Resize<byte>(ref array, array.Length + 128);
	// else
	// break;
	// }
	// num += 2;
	// }
	// else
	// goto label_7;
	// }
	// throw new InvalidOperationException();
	// label_7:
	// if (num == 0)
	// return "";
	// if (littleEndian)
	// return Encoding.Unicode.GetString(array, 0, num);
	// else
	// return Encoding.BigEndianUnicode.GetString(array, 0, num);
	// }
	//
	// public static void WriteUTF16(OutputStream stream, String value)
	// {
	// StreamHelpers.WriteUTF16(stream, value, true);
	// }
	//
	// public static void WriteUTF16(OutputStream stream, String value, boolean littleEndian)
	// {
	// byte[] buffer = !littleEndian ? Encoding.BigEndianUnicode.GetBytes(value) : Encoding.Unicode.GetBytes(value);
	// stream.write(buffer, 0, buffer.length);
	// }
	//
	// public static void WriteUTF16Z(OutputStream stream, String value)
	// {
	// StreamHelpers.WriteUTF16Z(stream, value, true);
	// }
	//
	// public static void WriteUTF16Z(OutputStream stream, String value, boolean littleEndian)
	// {
	// byte[] buffer = !littleEndian ? Encoding.BigEndianUnicode.GetBytes(value) : Encoding.Unicode.GetBytes(value);
	// stream.write(buffer, 0, buffer.length);
	// StreamHelpers.WriteU16(stream, (short) 0);
	// }

	// public static String ReadUTF8(InputStream stream, int size)
	// {
	// byte[] numArray = new byte[size];
	// stream.read(numArray, 0, numArray.length);
	// return Encoding.UTF8.GetString(numArray);
	// }
	//
	// public static String ReadUTF8Z(InputStream stream)
	// {
	// int index = 0;
	// byte[] array = new byte[64];
	// while (true)
	// {
	// stream.read(array, index, 1);
	// if ((int) array[index] != 0)
	// {
	// if (index >= array.Length)
	// {
	// if (array.Length < 4096)
	// Array.Resize<byte>(ref array, array.Length + 64);
	// else
	// break;
	// }
	// ++index;
	// }
	// else
	// goto label_7;
	// }
	// throw new InvalidOperationException();
	// label_7:
	// if (index == 0)
	// return "";
	// else
	// return Encoding.UTF8.GetString(array, 0, index);
	// }
	//
	// public static void WriteUTF8(OutputStream stream, String value)
	// {
	// byte[] bytes = Encoding.UTF8.GetBytes(value);
	// stream.write(bytes, 0, bytes.length);
	// }
	//
	// public static void WriteUTF8Z(OutputStream stream, String value)
	// {
	// byte[] bytes = Encoding.UTF8.GetBytes(value);
	// stream.write(bytes, 0, bytes.length);
	// stream.write(0);
	// }

	// public static T ReadStructure<T>(InputStream stream)
	// {
	// int count = Marshal.SizeOf(typeof (T));
	// byte[] buffer = new byte[count];
	// if (stream.read(buffer, 0, count) != count)
	// throw new Exception();
	// GCHandle gcHandle = GCHandle.Alloc((object) buffer, GCHandleType.Pinned);
	// T obj = (T) Marshal.PtrToStructure(gcHandle.AddrOfPinnedObject(), typeof (T));
	// gcHandle.Free();
	// return obj;
	// }
	//
	// public static T ReadStructure<T>(InputStream stream, int size)
	// {
	// int length = Marshal.SizeOf(typeof (T));
	// if (size > length)
	// throw new Exception();
	// byte[] buffer = new byte[length];
	// if (stream.read(buffer, 0, size) != size)
	// throw new Exception();
	// GCHandle gcHandle = GCHandle.Alloc((object) buffer, GCHandleType.Pinned);
	// T obj = (T) Marshal.PtrToStructure(gcHandle.AddrOfPinnedObject(), typeof (T));
	// gcHandle.Free();
	// return obj;
	// }
	//
	// public static void WriteStructure<T>(OutputStream stream, T structure)
	// {
	// byte[] buffer = new byte[Marshal.SizeOf(typeof (T))];
	// GCHandle gcHandle = GCHandle.Alloc((object) buffer, GCHandleType.Pinned);
	// Marshal.StructureToPtr((object) structure, gcHandle.AddrOfPinnedObject(), false);
	// gcHandle.Free();
	// stream.write(buffer, 0, buffer.Length);
	// }
}
