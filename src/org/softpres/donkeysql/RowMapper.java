/*
 * Copyright (c) 2015, Kieron Wilkinson
 */
package org.softpres.donkeysql;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Used to convert each row from a result set into an object.
 */
@FunctionalInterface
public interface RowMapper<T> {

  T apply(ResultSet resultSet) throws SQLException;

}
