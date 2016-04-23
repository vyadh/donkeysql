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
  List<Animal> results = DB.with(connection)
        .query("SELECT name, legs FROM animals WHERE legs > 4")
        .map(resultSet -> new Animal(
              resultSet.getString("name"),
              resultSet.getInt("legs")
        ))
        .stream()
        .collect(toList());
```

A query with `PreparedStatement`-style parameters:

```java
  List<Animal> results = DB.with(connection)
        .query("SELECT name, legs FROM animals WHERE legs >= ? AND name LIKE ?")
        .params(5, "s%")
        .map(resultSet -> new Animal(
              resultSet.getString("name"),
              resultSet.getInt("legs")
        ))
        .stream()
        .collect(toList());
```

A query with named parameter-style parameters:

```java
  List<Animal> results = DB.with(connection)
        .query("SELECT name, legs FROM animals WHERE legs >= :minLegs AND name LIKE :name")
        .param("minLegs", 4)
        .param("name", "s%")
        .map(resultSet -> new Animal(
              resultSet.getString("name"),
              resultSet.getInt("legs")
        ))
        .stream()
        .collect(toList());
```

Resource Management
-------------------

The `ResultSet` is closed automatically when the `Stream` has been consumed.
If a `Connection` is "closed" after each query, such as might be the case
when using a good connection pooling library such as [HikariCP][1], it can be
done like this:

```java
  try (Connection connection = dataSource.getConnection()) {
    List<String> names = DB.with(connection)
          .query("SELECT name FROM animals")
          .map(resultSet -> resultSet.getString("name"))
          .stream()
          .collect(toList());
          
    //...
  }
```

Exceptions
----------

Checked exceptions do not play well with lambda expressions and other frameworks.
This library takes the position that checked exceptions hinder more than they help
with this style of programming, and therefore wrap `SQLException` into an
`UncheckedSQLException`, which can be explicitly caught as required.

Since obtaining a connection from a `DataSource` is currently done via normal JDBC,
so an `SQLException` here still needs to be handled.


[1]: https://github.com/brettwooldridge/HikariCP
