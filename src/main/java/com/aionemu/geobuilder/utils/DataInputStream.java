package com.aionemu.geobuilder.utils;

import java.io.DataInput;
//import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class DataInputStream extends FilterInputStream implements DataInput {

	private byte[] readBuffer = new byte[8];

	public DataInputStream(InputStream paramInputStream) {
		super(paramInputStream);
	}

	@Override
	public final int read(byte[] paramArrayOfByte) throws IOException {
		return this.in.read(paramArrayOfByte, 0, paramArrayOfByte.length);
	}

	@Override
	public final int read(byte[] paramArrayOfByte, int paramInt1, int paramInt2) throws IOException {
		return this.in.read(paramArrayOfByte, paramInt1, paramInt2);
	}

	@Override
	public final void readFully(byte[] paramArrayOfByte) throws IOException {
		readFully(paramArrayOfByte, 0, paramArrayOfByte.length);
	}

	@Override
	public final void readFully(byte[] paramArrayOfByte, int paramInt1, int paramInt2) throws IOException {
		if (paramInt2 < 0)
			throw new IndexOutOfBoundsException();
		int i = 0;
		while (i < paramInt2) {
			int j = this.in.read(paramArrayOfByte, paramInt1 + i, paramInt2 - i);
			if (j < 0)
				throw new EOFException();
			i += j;
		}
	}

	@Override
	public final int skipBytes(int paramInt) throws IOException {
		int i = 0;
		int j = 0;
		while ((i < paramInt) && ((j = (int) this.in.skip(paramInt - i)) > 0))
			i += j;
		return i;
	}

	@Override
	public final boolean readBoolean() throws IOException {
		int i = this.in.read();
		if (i < 0)
			throw new EOFException();
		return (i != 0);
	}

	@Override
	public final byte readByte() throws IOException {
		int i = this.in.read();
		if (i < 0)
			throw new EOFException();
		return (byte) i;
	}

	@Override
	public final int readUnsignedByte() throws IOException {
		int i = this.in.read();
		if (i < 0)
			throw new EOFException();
		return i;
	}

	@Override
	public final short readShort() throws IOException {
		int i = this.in.read();
		int j = this.in.read();
		if ((i | j) < 0)
			throw new EOFException();
		return (short) ((j << 8) + (i << 0)); // little endian
	}

	@Override
	public final int readUnsignedShort() throws IOException {
		int i = this.in.read();
		int j = this.in.read();
		if ((i | j) < 0)
			throw new EOFException();
		return ((j << 8) + (i << 0)); // little endian
	}

	@Override
	public final char readChar() throws IOException {
		int i = this.in.read();
		int j = this.in.read();
		if ((i | j) < 0)
			throw new EOFException();
		return (char) ((i << 8) + (j << 0));
	}

	@Override
	public final int readInt() throws IOException {
		int i = this.in.read();
		int j = this.in.read();
		int k = this.in.read();
		int l = this.in.read();
		if ((i | j | k | l) < 0)
			throw new EOFException();
		return ((l << 24) + (k << 16) + (j << 8) + (i << 0)); // little endian
	}

	@Override
	public final long readLong() throws IOException {
		readFully(this.readBuffer, 0, 8);
		return ((this.readBuffer[7] << 56) + ((this.readBuffer[6] & 0xFF) << 48) + ((this.readBuffer[5] & 0xFF) << 40)
			+ ((this.readBuffer[4] & 0xFF) << 32) + ((this.readBuffer[3] & 0xFF) << 24) + ((this.readBuffer[2] & 0xFF) << 16)
			+ ((this.readBuffer[1] & 0xFF) << 8) + ((this.readBuffer[0] & 0xFF) << 0)); // little endian
	}

	@Override
	public final float readFloat() throws IOException {
		return Float.intBitsToFloat(readInt());
	}

	@Override
	public final double readDouble() throws IOException {
		return Double.longBitsToDouble(readLong());
	}

	@Override
	public String readLine() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String readUTF() throws IOException {
		throw new UnsupportedOperationException();
	}
}
