package com.aionemu.geobuilder.utils;

/*
 * Code taken from here: http://www.nextgenupdate.com/forums/computer-programming/394645-java-bitconverter-c.html 
 */

public final class BitConverter {

	public static byte[] getBytes(boolean x) {
		return new byte[] { (byte) (x ? 1 : 0) };
	}

	public static byte[] getBytes(char c) {
		return new byte[] { (byte) (c & 0xff), (byte) (c >> 8 & 0xff) };
	}

	public static byte[] getBytes(double x) {
		return getBytes(Double.doubleToRawLongBits(x));
	}

	public static byte[] getBytes(short x) {
		return new byte[] { (byte) (x >>> 8), (byte) x };
	}

	public static byte[] getBytes(int x) {
		return new byte[] { (byte) (x >>> 24), (byte) (x >>> 16), (byte) (x >>> 8), (byte) x };
	}

	public static byte[] getBytes(long x) {
		return new byte[] { (byte) (x >>> 56), (byte) (x >>> 48), (byte) (x >>> 40), (byte) (x >>> 32), (byte) (x >>> 24), (byte) (x >>> 16),
			(byte) (x >>> 8), (byte) x };
	}

	public static byte[] getBytes(float x) {
		return getBytes(Float.floatToRawIntBits(x));
	}

	public static byte[] getBytes(String x) {
		return x.getBytes();
	}

	public static long doubleToInt64Bits(double x) {
		return Double.doubleToRawLongBits(x);
	}

	public static double int64BitsToDouble(long x) {
		return x;
	}

	public static boolean toBoolean(byte[] bytes, int index) throws Exception {
		if (bytes.length < index + 1)
			throw new Exception("The byte array must contain at least 1 avaiable byte to read. Got only " + (bytes.length - index) + " bytes");
		return bytes[index] != 0;
	}

	public static char toChar(byte[] bytes, int index) throws Exception {
		if (bytes.length < index + 2)
			throw new Exception("The byte array must contain at least 2 avaiable bytes to read. Got only " + (bytes.length - index) + " bytes");
		return (char) ((0xff & bytes[index]) << 8 | (0xff & bytes[index + 1]) << 0);
	}

	public static double toDouble(byte[] bytes, int index) throws Exception {
		if (bytes.length < index + 8)
			throw new Exception("The byte array must contain at least 8 avaiable bytes to read. Got only " + (bytes.length - index) + " bytes");
		return Double.longBitsToDouble(toInt64(bytes, index));
	}

	public static short toInt16(byte[] bytes, int index) throws Exception {
		if (bytes.length < index + 2)
			throw new Exception("The byte array must contain at least 2 avaiable bytes to read. Got only " + (bytes.length - index) + " bytes");
		return (short) ((0xff & bytes[index]) << 8 | (0xff & bytes[index + 1]) << 0);
	}

	public static int toInt32(byte[] bytes, int index) throws Exception {
		if (bytes.length < index + 4)
			throw new Exception("The byte array must contain at least 4 avaiable bytes to read. Got only " + (bytes.length - index) + " bytes");
		return (0xff & bytes[index]) << 56 | (0xff & bytes[index + 1]) << 48 | (0xff & bytes[index + 2]) << 40 | (0xff & bytes[index + 3]) << 32;
	}

	public static long toInt64(byte[] bytes, int index) throws Exception {
		if (bytes.length < index + 8)
			throw new Exception("The byte array must contain at least 8 avaiable bytes to read. Got only " + (bytes.length - index) + " bytes");
		return (long) (0xff & bytes[index]) << 56 | (long) (0xff & bytes[index + 1]) << 48 | (long) (0xff & bytes[index + 2]) << 40
			| (long) (0xff & bytes[index + 3]) << 32 | (long) (0xff & bytes[index + 4]) << 24 | (long) (0xff & bytes[index + 5]) << 16
			| (long) (0xff & bytes[index + 6]) << 8 | (long) (0xff & bytes[index + 7]) << 0;
	}

	public static float toSingle(byte[] bytes, int index) throws Exception {
		if (bytes.length < index + 4)
			throw new Exception("The byte array must contain at least 4 avaiable bytes to read. Got only " + (bytes.length - index) + " bytes");
		return Float.intBitsToFloat(toInt32(bytes, index));
	}

	public static String toString(byte[] bytes) throws Exception {
		if (bytes == null)
			throw new Exception("The byte array must have at least 1 byte.");
		return new String(bytes);
	}
}
