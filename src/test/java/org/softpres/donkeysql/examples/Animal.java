/*
 * Copyright (c) 2016, Kieron Wilkinson
 */

package org.softpres.donkeysql.examples;

import java.util.Objects;

/**
 * Class for examples.
 */
class Animal {

  private final String name;
  private final int legs;

  Animal(String name, int legs) {
    this.name = name;
    this.legs = legs;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Animal animal = (Animal) o;
    return Objects.equals(name, animal.name) &&
          legs == animal.legs;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, legs);
  }

  @Override
  public String toString() {
    return "Animal(" + name + ", " + legs + ')';
  }

}
