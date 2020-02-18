package com.aionemu.geobuilder;

public enum CollisionIntention {
  NONE(0),
  PHYSICAL(1 << 0), // Physical collision
  MATERIAL(1 << 1), // Mesh materials with skills
  SKILL(1 << 2), // Skill obstacles
  WALK(1 << 3), // Walk/NoWalk obstacles
  DOOR(1 << 4), // Doors which have a state opened/closed
  EVENT(1 << 5), // Appear on event only
  MOVEABLE(1 << 6); // Ships, shugo boxes

  private final short id;

  CollisionIntention(int id) {
    this.id = (short) id;
  }

  public short getId() {
    return id;
  }
}