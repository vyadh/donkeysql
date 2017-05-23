[![Build Status](https://travis-ci.org/vyadh/donkeysql.svg?branch=master)](https://travis-ci.org/vyadh/donkeysql)
[![codecov](https://codecov.io/gh/vyadh/donkeysql/branch/master/graph/badge.svg)](https://codecov.io/gh/vyadh/donkeysql)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/24302f1aa4194276ba13d00b81137d1e)](https://www.codacy.com/app/vyadh/donkeysql?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=vyadh/donkeysql&amp;utm_campaign=Badge_Grade)

Donkey SQL
==========

A tiny library that allows SQL queries to be performed through the Java 8 `Stream`
interface with lambda expressions.

Donkey SQL:
* is not an ORM.  
* embraces the power of SQL rather than abstracting away from it.  
* greatly simplifies use of JBDC by providing a fluent interface.  

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

A query with named parameter-style parameters (recommended):

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

A query with auto-expanding parameters when an `Iterable` supplied for an IN operator:

```java
  Stream<Animal> results = DB.with(dataSource)
        .query("SELECT name, legs FROM animals WHERE legs IN (:specificLegs)")
        .param("specificLegs", Arrays.asList(0, 8))
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

This mechanism should also be used if not consuming the whole `ResultSet` in order to
close the `Connection` appropriately.

Exceptions
----------

Checked exceptions don't tend to play well with lambda expressions and other frameworks.
This library takes the position that checked exceptions hinder more than they help with
this style of programming, and therefore wrap `SQLException` into an
`UncheckedSQLException`, which can be explicitly caught as required.


[1]: https://github.com/brettwooldridge/HikariCP
