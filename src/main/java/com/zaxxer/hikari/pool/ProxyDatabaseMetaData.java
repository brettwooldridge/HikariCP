package com.zaxxer.hikari.pool;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class ProxyDatabaseMetaData implements DatabaseMetaData
{
   protected final ProxyConnection connection;

   @SuppressWarnings("WeakerAccess")
   protected final DatabaseMetaData delegate;

   ProxyDatabaseMetaData(ProxyConnection connection, DatabaseMetaData metaData)
   {
      this.connection = connection;
      this.delegate = metaData;
   }

   final SQLException checkException(SQLException e)
   {
      return connection.checkException(e);
   }

   /** {@inheritDoc} */
   @Override
   public final String toString()
   {
      final var delegateToString = delegate.toString();
      return this.getClass().getSimpleName() + '@' + System.identityHashCode(this) + " wrapping " + delegateToString;
   }

   // **********************************************************************
   //                 Overridden java.sql.DatabaseMetaData Methods
   // **********************************************************************

   /** {@inheritDoc} */
   @Override
   public final Connection getConnection()
   {
      return connection;
   }

   @Override
   public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern) throws SQLException {
      var resultSet = delegate.getProcedures(catalog, schemaPattern, procedureNamePattern);
      var statement = resultSet.getStatement();
      if (statement != null) {
         statement = ProxyFactory.getProxyStatement(connection, statement);
      }
      return ProxyFactory.getProxyResultSet(connection, (ProxyStatement) statement, resultSet);
   }

   @Override
   public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern) throws SQLException {
      var resultSet = delegate.getProcedureColumns(catalog, schemaPattern, procedureNamePattern, columnNamePattern);
      var statement = resultSet.getStatement();
      if (statement != null) {
         statement = ProxyFactory.getProxyStatement(connection, statement);
      }
      return ProxyFactory.getProxyResultSet(connection, (ProxyStatement) statement, resultSet);
   }

   @Override
   public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) throws SQLException {
      var resultSet = delegate.getTables(catalog, schemaPattern, tableNamePattern, types);
      var statement = resultSet.getStatement();
      if (statement != null) {
         statement = ProxyFactory.getProxyStatement(connection, statement);
      }
      return ProxyFactory.getProxyResultSet(connection, (ProxyStatement) statement, resultSet);
   }

   @Override
   public ResultSet getSchemas() throws SQLException {
      var resultSet = delegate.getSchemas();
      var statement = resultSet.getStatement();
      if (statement != null) {
         statement = ProxyFactory.getProxyStatement(connection, statement);
      }
      return ProxyFactory.getProxyResultSet(connection, (ProxyStatement) statement, resultSet);
   }

   @Override
   public ResultSet getCatalogs() throws SQLException {
      var resultSet = delegate.getCatalogs();
      var statement = resultSet.getStatement();
      if (statement != null) {
         statement = ProxyFactory.getProxyStatement(connection, statement);
      }
      return ProxyFactory.getProxyResultSet(connection, (ProxyStatement) statement, resultSet);
   }

   @Override
   public ResultSet getTableTypes() throws SQLException {
      var resultSet = delegate.getTableTypes();
      var statement = resultSet.getStatement();
      if (statement != null) {
         statement = ProxyFactory.getProxyStatement(connection, statement);
      }
      return ProxyFactory.getProxyResultSet(connection, (ProxyStatement) statement, resultSet);
   }

   @Override
   public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
      var resultSet = delegate.getColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern);
      var statement = resultSet.getStatement();
      if (statement != null) {
         statement = ProxyFactory.getProxyStatement(connection, statement);
      }
      return ProxyFactory.getProxyResultSet(connection, (ProxyStatement) statement, resultSet);
   }

   @Override
   public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) throws SQLException {
      var resultSet = delegate.getColumnPrivileges(catalog, schema, table, columnNamePattern);
      var statement = resultSet.getStatement();
      if (statement != null) {
         statement = ProxyFactory.getProxyStatement(connection, statement);
      }
      return ProxyFactory.getProxyResultSet(connection, (ProxyStatement) statement, resultSet);
   }

   @Override
   public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
      var resultSet = delegate.getTablePrivileges(catalog, schemaPattern, tableNamePattern);
      var statement = resultSet.getStatement();
      if (statement != null) {
         statement = ProxyFactory.getProxyStatement(connection, statement);
      }
      return ProxyFactory.getProxyResultSet(connection, (ProxyStatement) statement, resultSet);
   }

   @Override
   public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable) throws SQLException {
      var resultSet = delegate.getBestRowIdentifier(catalog, schema, table, scope, nullable);
      var statement = resultSet.getStatement();
      if (statement != null) {
         statement = ProxyFactory.getProxyStatement(connection, statement);
      }
      return ProxyFactory.getProxyResultSet(connection, (ProxyStatement) statement, resultSet);
   }

   @Override
   public ResultSet getVersionColumns(String catalog, String schema, String table) throws SQLException {
      var resultSet = delegate.getVersionColumns(catalog, schema, table);
      var statement = resultSet.getStatement();
      if (statement != null) {
         statement = ProxyFactory.getProxyStatement(connection, statement);
      }
      return ProxyFactory.getProxyResultSet(connection, (ProxyStatement) statement, resultSet);
   }

   @Override
   public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
      var resultSet = delegate.getPrimaryKeys(catalog, schema, table);
      var statement = resultSet.getStatement();
      if (statement != null) {
         statement = ProxyFactory.getProxyStatement(connection, statement);
      }
      return ProxyFactory.getProxyResultSet(connection, (ProxyStatement) statement, resultSet);
   }

   @Override
   public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
      var resultSet = delegate.getImportedKeys(catalog, schema, table);
      var statement = resultSet.getStatement();
      if (statement != null) {
         statement = ProxyFactory.getProxyStatement(connection, statement);
      }
      return ProxyFactory.getProxyResultSet(connection, (ProxyStatement) statement, resultSet);
   }

   @Override
   public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
      var resultSet = delegate.getExportedKeys(catalog, schema, table);
      var statement = resultSet.getStatement();
      if (statement != null) {
         statement = ProxyFactory.getProxyStatement(connection, statement);
      }
      return ProxyFactory.getProxyResultSet(connection, (ProxyStatement) statement, resultSet);
   }

   @Override
   public ResultSet getCrossReference(String parentCatalog, String parentSchema, String parentTable, String foreignCatalog, String foreignSchema, String foreignTable) throws SQLException {
      var resultSet = delegate.getCrossReference(parentCatalog, parentSchema, parentTable, foreignCatalog, foreignSchema, foreignTable);
      var statement = resultSet.getStatement();
      if (statement != null) {
         statement = ProxyFactory.getProxyStatement(connection, statement);
      }
      return ProxyFactory.getProxyResultSet(connection, (ProxyStatement) statement, resultSet);
   }

   @Override
   public ResultSet getTypeInfo() throws SQLException {
      var resultSet = delegate.getTypeInfo();
      var statement = resultSet.getStatement();
      if (statement != null) {
         statement = ProxyFactory.getProxyStatement(connection, statement);
      }
      return ProxyFactory.getProxyResultSet(connection, (ProxyStatement) statement, resultSet);
   }

   @Override
   public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate) throws SQLException {
      var resultSet = delegate.getIndexInfo(catalog, schema, table, unique, approximate);
      var statement = resultSet.getStatement();
      if (statement != null) {
         statement = ProxyFactory.getProxyStatement(connection, statement);
      }
      return ProxyFactory.getProxyResultSet(connection, (ProxyStatement) statement, resultSet);
   }

   @Override
   public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types) throws SQLException {
      var resultSet = delegate.getUDTs(catalog, schemaPattern, typeNamePattern, types);
      var statement = resultSet.getStatement();
      if (statement != null) {
         statement = ProxyFactory.getProxyStatement(connection, statement);
      }
      return ProxyFactory.getProxyResultSet(connection, (ProxyStatement) statement, resultSet);
   }

   @Override
   public ResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern) throws SQLException {
      var resultSet = delegate.getSuperTypes(catalog, schemaPattern, typeNamePattern);
      var statement = resultSet.getStatement();
      if (statement != null) {
         statement = ProxyFactory.getProxyStatement(connection, statement);
      }
      return ProxyFactory.getProxyResultSet(connection, (ProxyStatement) statement, resultSet);
   }

   @Override
   public ResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
      var resultSet = delegate.getSuperTables(catalog, schemaPattern, tableNamePattern);
      var statement = resultSet.getStatement();
      if (statement != null) {
         statement = ProxyFactory.getProxyStatement(connection, statement);
      }
      return ProxyFactory.getProxyResultSet(connection, (ProxyStatement) statement, resultSet);
   }

   @Override
   public ResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern, String attributeNamePattern) throws SQLException {
      var resultSet = delegate.getAttributes(catalog, schemaPattern, typeNamePattern, attributeNamePattern);
      var statement = resultSet.getStatement();
      if (statement != null) {
         statement = ProxyFactory.getProxyStatement(connection, statement);
      }
      return ProxyFactory.getProxyResultSet(connection, (ProxyStatement) statement, resultSet);
   }

   @Override
   public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
      var resultSet = delegate.getSchemas(catalog, schemaPattern);
      var statement = resultSet.getStatement();
      if (statement != null) {
         statement = ProxyFactory.getProxyStatement(connection, statement);
      }
      return ProxyFactory.getProxyResultSet(connection, (ProxyStatement) statement, resultSet);
   }

   @Override
   public ResultSet getClientInfoProperties() throws SQLException {
      var resultSet = delegate.getClientInfoProperties();
      var statement = resultSet.getStatement();
      if (statement != null) {
         statement = ProxyFactory.getProxyStatement(connection, statement);
      }
      return ProxyFactory.getProxyResultSet(connection, (ProxyStatement) statement, resultSet);
   }

   @Override
   public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern) throws SQLException {
      var resultSet = delegate.getFunctions(catalog, schemaPattern, functionNamePattern);
      var statement = resultSet.getStatement();
      if (statement != null) {
         statement = ProxyFactory.getProxyStatement(connection, statement);
      }
      return ProxyFactory.getProxyResultSet(connection, (ProxyStatement) statement, resultSet);
   }

   @Override
   public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern, String columnNamePattern) throws SQLException {
      var resultSet = delegate.getFunctionColumns(catalog, schemaPattern, functionNamePattern, columnNamePattern);
      var statement = resultSet.getStatement();
      if (statement != null) {
         statement = ProxyFactory.getProxyStatement(connection, statement);
      }
      return ProxyFactory.getProxyResultSet(connection, (ProxyStatement) statement, resultSet);
   }

   @Override
   public ResultSet getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
      var resultSet = delegate.getPseudoColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern);
      var statement = resultSet.getStatement();
      if (statement != null) {
         statement = ProxyFactory.getProxyStatement(connection, statement);
      }
      return ProxyFactory.getProxyResultSet(connection, (ProxyStatement) statement, resultSet);
   }

   /** {@inheritDoc} */
   @Override
   @SuppressWarnings("unchecked")
   public final <T> T unwrap(Class<T> iface) throws SQLException
   {
      if (iface.isInstance(delegate)) {
         return (T) delegate;
      }
      else if (delegate != null) {
         return delegate.unwrap(iface);
      }

      throw new SQLException("Wrapped DatabaseMetaData is not an instance of " + iface);
   }
}
