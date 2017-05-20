[![Build Status](https://travis-ci.org/vyadh/donkeysql.svg?branch=master)](https://travis-ci.org/vyadh/donkeysql)
[![codecov](https://codecov.io/gh/vyadh/donkeysql/branch/master/graph/badge.svg)](https://codecov.io/gh/vyadh/donkeysql)

Donkey SQL
==========

A tiny library that allows SQL queries to be performed through the Java 8 `Stream`
interface with lambda expressions.

Donkey SQL is not an ORM.  
It does not attempt to abstract away from SQL.  
It greatly simplifies use of plain JBDC.  
It is less than 500 lines of code (not including tests).  

The core idea is to interface with a `ResultSet` through a `Stream` to allow
efficient data extraction and transformation from the underlying `ResultSet`
without the JDBC boilerplate or creation of intermediate collections.

Dependencies
------------

Donkey SQL has no runtime dependencies other than Java 8.

Examples
--------

A simple query:

```java
  Stream<Animal> results = DB.with(dataSource)
        .query("SELECT name, legs FROM animals WHERE legs > 4")
        .map(resultSet -> new Animal(
              resultSet.getString("name"),
              resultSet.getInt("legs")
        ))
        .execute();
```

A query with `PreparedStatement`-style parameters:

```java
  Stream<Animal> results = DB.with(dataSource)
        .query("SELECT name, legs FROM animals WHERE legs >= ? AND name LIKE ?")
        .params(5, "s%")
        .map(resultSet -> new Animal(
              resultSet.getString("name"),
              resultSet.getInt("legs")
        ))
        .execute();
```

A query with named parameter-style parameters:

```java
  Stream<Animal> results = DB.with(dataSource)
        .query("SELECT name, legs FROM animals WHERE legs >= :minLegs AND name LIKE :name")
        .param("minLegs", 4)
        .param("name", "s%")
        .map(resultSet -> new Animal(
              resultSet.getString("name"),
              resultSet.getInt("legs")
        ))
        .execute();
```

All these examples execute as tests in the `ExamplesTest` class. 

Resource Management
-------------------

The above examples assume a good connection pooling library such as [HikariCP][1] is being
used, as this allows us to create and close a connection on the callers behalf without being
concerned about the connection management overhead.

The connection can also be specified explicitly such as the following.

```java
  try (Connection connection = dataSource.getConnection()) {
    Stream<String> results = DB.with(connection)
          .query("SELECT name FROM animals WHERE legs >= 5")
          .map(resultSet -> resultSet.getString("name"))
          .execute();

    // ...
  }
```

Exceptions
----------

Checked exceptions do not play well with lambda expressions and other frameworks.
This library takes the position that checked exceptions hinder more than they help
with this style of programming, and therefore wrap `SQLException` into an
`UncheckedSQLException`, which can be explicitly caught as required.


[1]: https://github.com/brettwooldridge/HikariCP
