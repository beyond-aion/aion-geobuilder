package com.aionemu.geobuilder.entries;

public enum EntryType {

	NONE(0),
	EVENT(1),
	PLACEABLE(2),
	HOUSE(3),
	HOUSE_DOOR(4),
	TOWN(5),
	DOOR(6),
	DOOR2(7);

	byte id;

	private EntryType(int id) {
		this.id = (byte)id;
	}
	public byte getId() {
		return id;
	}
}
