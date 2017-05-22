/*
 * Copyright (c) 2016, Kieron Wilkinson
 */
package org.softpres.donkeysql;

import java.sql.SQLException;

/**
 * Interface to represent resources that should be closed so that we can make
 * use of the try-with-resources to concisely close them.
 */
interface QueryResource extends AutoCloseable {

  void close() throws SQLException;

}
