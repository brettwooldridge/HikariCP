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

   @SuppressWarnings("unused")
   final SQLException checkException(SQLException e)
   {
      return connection.checkException(e);
   }

   /** {@inheritDoc} */
   @Override
   public final String toString()
   {
      final String delegateToString = delegate.toString();
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
      ResultSet resultSet = delegate.getProcedures(catalog, schemaPattern, procedureNamePattern);
      ProxyStatement statement = (ProxyStatement) ProxyFactory.getProxyStatement(connection, resultSet.getStatement());
      return ProxyFactory.getProxyResultSet(connection, statement, resultSet);
   }

   @Override
   public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern) throws SQLException {
      ResultSet resultSet = delegate.getProcedureColumns(catalog, schemaPattern, procedureNamePattern, columnNamePattern);
      ProxyStatement statement = (ProxyStatement) ProxyFactory.getProxyStatement(connection, resultSet.getStatement());
      return ProxyFactory.getProxyResultSet(connection, statement, resultSet);
   }

   @Override
   public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) throws SQLException {
      ResultSet resultSet = delegate.getTables(catalog, schemaPattern, tableNamePattern, types);
      ProxyStatement statement = (ProxyStatement) ProxyFactory.getProxyStatement(connection, resultSet.getStatement());
      return ProxyFactory.getProxyResultSet(connection, statement, resultSet);
   }

   @Override
   public ResultSet getSchemas() throws SQLException {
      ResultSet resultSet = delegate.getSchemas();
      ProxyStatement statement = (ProxyStatement) ProxyFactory.getProxyStatement(connection, resultSet.getStatement());
      return ProxyFactory.getProxyResultSet(connection, statement, resultSet);
   }

   @Override
   public ResultSet getCatalogs() throws SQLException {
      ResultSet resultSet = delegate.getCatalogs();
      ProxyStatement statement = (ProxyStatement) ProxyFactory.getProxyStatement(connection, resultSet.getStatement());
      return ProxyFactory.getProxyResultSet(connection, statement, resultSet);
   }

   @Override
   public ResultSet getTableTypes() throws SQLException {
      ResultSet resultSet = delegate.getTableTypes();
      ProxyStatement statement = (ProxyStatement) ProxyFactory.getProxyStatement(connection, resultSet.getStatement());
      return ProxyFactory.getProxyResultSet(connection, statement, resultSet);
   }

   @Override
   public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
      ResultSet resultSet = delegate.getColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern);
      ProxyStatement statement = (ProxyStatement) ProxyFactory.getProxyStatement(connection, resultSet.getStatement());
      return ProxyFactory.getProxyResultSet(connection, statement, resultSet);
   }

   @Override
   public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) throws SQLException {
      ResultSet resultSet = delegate.getColumnPrivileges(catalog, schema, table, columnNamePattern);
      ProxyStatement statement = (ProxyStatement) ProxyFactory.getProxyStatement(connection, resultSet.getStatement());
      return ProxyFactory.getProxyResultSet(connection, statement, resultSet);
   }

   @Override
   public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
      ResultSet resultSet = delegate.getTablePrivileges(catalog, schemaPattern, tableNamePattern);
      ProxyStatement statement = (ProxyStatement) ProxyFactory.getProxyStatement(connection, resultSet.getStatement());
      return ProxyFactory.getProxyResultSet(connection, statement, resultSet);
   }

   @Override
   public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable) throws SQLException {
      ResultSet resultSet = delegate.getBestRowIdentifier(catalog, schema, table, scope, nullable);
      ProxyStatement statement = (ProxyStatement) ProxyFactory.getProxyStatement(connection, resultSet.getStatement());
      return ProxyFactory.getProxyResultSet(connection, statement, resultSet);
   }

   @Override
   public ResultSet getVersionColumns(String catalog, String schema, String table) throws SQLException {
      ResultSet resultSet = delegate.getVersionColumns(catalog, schema, table);
      ProxyStatement statement = (ProxyStatement) ProxyFactory.getProxyStatement(connection, resultSet.getStatement());
      return ProxyFactory.getProxyResultSet(connection, statement, resultSet);
   }

   @Override
   public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
      ResultSet resultSet = delegate.getPrimaryKeys(catalog, schema, table);
      ProxyStatement statement = (ProxyStatement) ProxyFactory.getProxyStatement(connection, resultSet.getStatement());
      return ProxyFactory.getProxyResultSet(connection, statement, resultSet);
   }

   @Override
   public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
      ResultSet resultSet = delegate.getImportedKeys(catalog, schema, table);
      ProxyStatement statement = (ProxyStatement) ProxyFactory.getProxyStatement(connection, resultSet.getStatement());
      return ProxyFactory.getProxyResultSet(connection, statement, resultSet);
   }

   @Override
   public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
      ResultSet resultSet = delegate.getExportedKeys(catalog, schema, table);
      ProxyStatement statement = (ProxyStatement) ProxyFactory.getProxyStatement(connection, resultSet.getStatement());
      return ProxyFactory.getProxyResultSet(connection, statement, resultSet);
   }

   @Override
   public ResultSet getCrossReference(String parentCatalog, String parentSchema, String parentTable, String foreignCatalog, String foreignSchema, String foreignTable) throws SQLException {
      ResultSet resultSet = delegate.getCrossReference(parentCatalog, parentSchema, parentTable, foreignCatalog, foreignSchema, foreignTable);
      ProxyStatement statement = (ProxyStatement) ProxyFactory.getProxyStatement(connection, resultSet.getStatement());
      return ProxyFactory.getProxyResultSet(connection, statement, resultSet);
   }

   @Override
   public ResultSet getTypeInfo() throws SQLException {
      ResultSet resultSet = delegate.getTypeInfo();
      ProxyStatement statement = (ProxyStatement) ProxyFactory.getProxyStatement(connection, resultSet.getStatement());
      return ProxyFactory.getProxyResultSet(connection, statement, resultSet);
   }

   @Override
   public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate) throws SQLException {
      ResultSet resultSet = delegate.getIndexInfo(catalog, schema, table, unique, approximate);
      ProxyStatement statement = (ProxyStatement) ProxyFactory.getProxyStatement(connection, resultSet.getStatement());
      return ProxyFactory.getProxyResultSet(connection, statement, resultSet);
   }

   @Override
   public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types) throws SQLException {
      ResultSet resultSet = delegate.getUDTs(catalog, schemaPattern, typeNamePattern, types);
      ProxyStatement statement = (ProxyStatement) ProxyFactory.getProxyStatement(connection, resultSet.getStatement());
      return ProxyFactory.getProxyResultSet(connection, statement, resultSet);
   }

   @Override
   public ResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern) throws SQLException {
      ResultSet resultSet = delegate.getSuperTypes(catalog, schemaPattern, typeNamePattern);
      ProxyStatement statement = (ProxyStatement) ProxyFactory.getProxyStatement(connection, resultSet.getStatement());
      return ProxyFactory.getProxyResultSet(connection, statement, resultSet);
   }

   @Override
   public ResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
      ResultSet resultSet = delegate.getSuperTables(catalog, schemaPattern, tableNamePattern);
      ProxyStatement statement = (ProxyStatement) ProxyFactory.getProxyStatement(connection, resultSet.getStatement());
      return ProxyFactory.getProxyResultSet(connection, statement, resultSet);
   }

   @Override
   public ResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern, String attributeNamePattern) throws SQLException {
      ResultSet resultSet = delegate.getAttributes(catalog, schemaPattern, typeNamePattern, attributeNamePattern);
      ProxyStatement statement = (ProxyStatement) ProxyFactory.getProxyStatement(connection, resultSet.getStatement());
      return ProxyFactory.getProxyResultSet(connection, statement, resultSet);
   }

   @Override
   public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
      ResultSet resultSet = delegate.getSchemas(catalog, schemaPattern);
      ProxyStatement statement = (ProxyStatement) ProxyFactory.getProxyStatement(connection, resultSet.getStatement());
      return ProxyFactory.getProxyResultSet(connection, statement, resultSet);
   }

   @Override
   public ResultSet getClientInfoProperties() throws SQLException {
      ResultSet resultSet = delegate.getClientInfoProperties();
      ProxyStatement statement = (ProxyStatement) ProxyFactory.getProxyStatement(connection, resultSet.getStatement());
      return ProxyFactory.getProxyResultSet(connection, statement, resultSet);
   }

   @Override
   public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern) throws SQLException {
      ResultSet resultSet = delegate.getFunctions(catalog, schemaPattern, functionNamePattern);
      ProxyStatement statement = (ProxyStatement) ProxyFactory.getProxyStatement(connection, resultSet.getStatement());
      return ProxyFactory.getProxyResultSet(connection, statement, resultSet);
   }

   @Override
   public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern, String columnNamePattern) throws SQLException {
      ResultSet resultSet = delegate.getFunctionColumns(catalog, schemaPattern, functionNamePattern, columnNamePattern);
      ProxyStatement statement = (ProxyStatement) ProxyFactory.getProxyStatement(connection, resultSet.getStatement());
      return ProxyFactory.getProxyResultSet(connection, statement, resultSet);
   }

   @Override
   public ResultSet getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
      ResultSet resultSet = delegate.getPseudoColumns(catalog, schemaPattern, tableNamePattern, columnNamePattern);
      ProxyStatement statement = (ProxyStatement) ProxyFactory.getProxyStatement(connection, resultSet.getStatement());
      return ProxyFactory.getProxyResultSet(connection, statement, resultSet);
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
