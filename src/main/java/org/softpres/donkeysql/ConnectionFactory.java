/*
 * Copyright (c) 2017, Kieron Wilkinson
 */
package org.softpres.donkeysql;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Allows abstracting creation of a connection to allow a DataSource and
 * Connection be used transparently.
 */
interface ConnectionFactory {

  Connection create() throws SQLException;

}
