package com.zaxxer.hikari.mocks;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;

public class StubDatabaseMetaData implements DatabaseMetaData {

   /** {@inheritDoc} */
   @Override
   public boolean allProceduresAreCallable() throws SQLException {
      return false;
   }

   /** {@inheritDoc} */
   @Override
   public boolean allTablesAreSelectable() throws SQLException {
      return false;
   }

   /** {@inheritDoc} */
   @Override
   public String getURL() throws SQLException {
      return "jdbc://testUrl";
   }

   /** {@inheritDoc} */
   @Override
   public String getUserName() throws SQLException {
      return null;
   }

   /** {@inheritDoc} */
   @Override
   public boolean isReadOnly() throws SQLException {
      return false;
   }

   /** {@inheritDoc} */
   @Override
   public boolean nullsAreSortedHigh() throws SQLException {
      return false;
   }

   /** {@inheritDoc} */
   @Override
   public boolean nullsAreSortedLow() throws SQLException {
      return false;
   }

   /** {@inheritDoc} */
   @Override
   public boolean nullsAreSortedAtStart() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean nullsAreSortedAtEnd() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public String getDatabaseProductName() throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public String getDatabaseProductVersion() throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public String getDriverName() throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public String getDriverVersion() throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public int getDriverMajorVersion() {
      return 0;
   }


   /** {@inheritDoc} */
   @Override
   public int getDriverMinorVersion() {
      return 0;
   }


   /** {@inheritDoc} */
   @Override
   public boolean usesLocalFiles() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean usesLocalFilePerTable() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsMixedCaseIdentifiers() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean storesUpperCaseIdentifiers() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean storesLowerCaseIdentifiers() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean storesMixedCaseIdentifiers() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public String getIdentifierQuoteString() throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public String getSQLKeywords() throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public String getNumericFunctions() throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public String getStringFunctions() throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public String getSystemFunctions() throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public String getTimeDateFunctions() throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public String getSearchStringEscape() throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public String getExtraNameCharacters() throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsAlterTableWithAddColumn() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsAlterTableWithDropColumn() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsColumnAliasing() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean nullPlusNonNullIsNull() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsConvert() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsConvert(int fromType, int toType) throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsTableCorrelationNames() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsDifferentTableCorrelationNames() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsExpressionsInOrderBy() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsOrderByUnrelated() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsGroupBy() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsGroupByUnrelated() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsGroupByBeyondSelect() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsLikeEscapeClause() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsMultipleResultSets() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsMultipleTransactions() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsNonNullableColumns() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsMinimumSQLGrammar() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsCoreSQLGrammar() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsExtendedSQLGrammar() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsANSI92EntryLevelSQL() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsANSI92IntermediateSQL() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsANSI92FullSQL() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsIntegrityEnhancementFacility() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsOuterJoins() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsFullOuterJoins() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsLimitedOuterJoins() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public String getSchemaTerm() throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public String getProcedureTerm() throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public String getCatalogTerm() throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public boolean isCatalogAtStart() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public String getCatalogSeparator() throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsSchemasInDataManipulation() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsSchemasInProcedureCalls() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsSchemasInTableDefinitions() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsSchemasInIndexDefinitions() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsCatalogsInDataManipulation() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsCatalogsInProcedureCalls() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsCatalogsInTableDefinitions() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsPositionedDelete() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsPositionedUpdate() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsSelectForUpdate() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsStoredProcedures() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsSubqueriesInComparisons() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsSubqueriesInExists() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsSubqueriesInIns() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsSubqueriesInQuantifieds() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsCorrelatedSubqueries() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsUnion() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsUnionAll() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public int getMaxBinaryLiteralLength() throws SQLException {
      return 0;
   }


   /** {@inheritDoc} */
   @Override
   public int getMaxCharLiteralLength() throws SQLException {
      return 0;
   }


   /** {@inheritDoc} */
   @Override
   public int getMaxColumnNameLength() throws SQLException {
      return 0;
   }


   /** {@inheritDoc} */
   @Override
   public int getMaxColumnsInGroupBy() throws SQLException {
      return 0;
   }


   /** {@inheritDoc} */
   @Override
   public int getMaxColumnsInIndex() throws SQLException {
      return 0;
   }


   /** {@inheritDoc} */
   @Override
   public int getMaxColumnsInOrderBy() throws SQLException {
      return 0;
   }


   /** {@inheritDoc} */
   @Override
   public int getMaxColumnsInSelect() throws SQLException {
      return 0;
   }


   /** {@inheritDoc} */
   @Override
   public int getMaxColumnsInTable() throws SQLException {
      return 0;
   }


   /** {@inheritDoc} */
   @Override
   public int getMaxConnections() throws SQLException {
      return 0;
   }


   /** {@inheritDoc} */
   @Override
   public int getMaxCursorNameLength() throws SQLException {
      return 0;
   }


   /** {@inheritDoc} */
   @Override
   public int getMaxIndexLength() throws SQLException {
      return 0;
   }


   /** {@inheritDoc} */
   @Override
   public int getMaxSchemaNameLength() throws SQLException {
      return 0;
   }


   /** {@inheritDoc} */
   @Override
   public int getMaxProcedureNameLength() throws SQLException {
      return 0;
   }


   /** {@inheritDoc} */
   @Override
   public int getMaxCatalogNameLength() throws SQLException {
      return 0;
   }


   /** {@inheritDoc} */
   @Override
   public int getMaxRowSize() throws SQLException {
      return 0;
   }


   /** {@inheritDoc} */
   @Override
   public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public int getMaxStatementLength() throws SQLException {
      return 0;
   }


   /** {@inheritDoc} */
   @Override
   public int getMaxStatements() throws SQLException {
      return 0;
   }


   /** {@inheritDoc} */
   @Override
   public int getMaxTableNameLength() throws SQLException {
      return 0;
   }


   /** {@inheritDoc} */
   @Override
   public int getMaxTablesInSelect() throws SQLException {
      return 0;
   }


   /** {@inheritDoc} */
   @Override
   public int getMaxUserNameLength() throws SQLException {
      return 0;
   }


   /** {@inheritDoc} */
   @Override
   public int getDefaultTransactionIsolation() throws SQLException {
      return 0;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsTransactions() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsTransactionIsolationLevel(int level) throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsDataManipulationTransactionsOnly() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public ResultSet getProcedures(String catalog, String schemaPattern,
      String procedureNamePattern) throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public ResultSet getProcedureColumns(String catalog, String schemaPattern,
      String procedureNamePattern, String columnNamePattern) throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern,
      String[] types) throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public ResultSet getSchemas() throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public ResultSet getCatalogs() throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public ResultSet getTableTypes() throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern,
      String columnNamePattern) throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public ResultSet getColumnPrivileges(String catalog, String schema, String table,
      String columnNamePattern) throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public ResultSet getTablePrivileges(String catalog, String schemaPattern,
      String tableNamePattern) throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope,
      boolean nullable) throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public ResultSet getVersionColumns(String catalog, String schema, String table)
      throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public ResultSet getPrimaryKeys(String catalog, String schema, String table)
      throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public ResultSet getImportedKeys(String catalog, String schema, String table)
      throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public ResultSet getExportedKeys(String catalog, String schema, String table)
      throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public ResultSet getCrossReference(String parentCatalog, String parentSchema,
      String parentTable, String foreignCatalog, String foreignSchema, String foreignTable)
      throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public ResultSet getTypeInfo() throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique,
      boolean approximate) throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsResultSetType(int type) throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsResultSetConcurrency(int type, int concurrency) throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean ownUpdatesAreVisible(int type) throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean ownDeletesAreVisible(int type) throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean ownInsertsAreVisible(int type) throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean othersUpdatesAreVisible(int type) throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean othersDeletesAreVisible(int type) throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean othersInsertsAreVisible(int type) throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean updatesAreDetected(int type) throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean deletesAreDetected(int type) throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean insertsAreDetected(int type) throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsBatchUpdates() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern,
      int[] types) throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public Connection getConnection() throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsSavepoints() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsNamedParameters() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsMultipleOpenResults() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsGetGeneratedKeys() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public ResultSet getSuperTypes(String catalog, String schemaPattern,
      String typeNamePattern) throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public ResultSet getSuperTables(String catalog, String schemaPattern,
      String tableNamePattern) throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public ResultSet getAttributes(String catalog, String schemaPattern,
      String typeNamePattern, String attributeNamePattern) throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsResultSetHoldability(int holdability) throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public int getResultSetHoldability() throws SQLException {
      return 0;
   }


   /** {@inheritDoc} */
   @Override
   public int getDatabaseMajorVersion() throws SQLException {
      return 0;
   }


   /** {@inheritDoc} */
   @Override
   public int getDatabaseMinorVersion() throws SQLException {
      return 0;
   }


   /** {@inheritDoc} */
   @Override
   public int getJDBCMajorVersion() throws SQLException {
      return 0;
   }


   /** {@inheritDoc} */
   @Override
   public int getJDBCMinorVersion() throws SQLException {
      return 0;
   }


   /** {@inheritDoc} */
   @Override
   public int getSQLStateType() throws SQLException {
      return 0;
   }


   /** {@inheritDoc} */
   @Override
   public boolean locatorsUpdateCopy() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsStatementPooling() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public RowIdLifetime getRowIdLifetime() throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public ResultSet getClientInfoProperties() throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public ResultSet getFunctions(String catalog, String schemaPattern,
      String functionNamePattern) throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public ResultSet getFunctionColumns(String catalog, String schemaPattern,
      String functionNamePattern, String columnNamePattern) throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public ResultSet getPseudoColumns(String catalog, String schemaPattern,
      String tableNamePattern, String columnNamePattern) throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public boolean generatedKeyAlwaysReturned() throws SQLException {
      return false;
   }


   /** {@inheritDoc} */
   @Override
   public <T> T unwrap(Class<T> iface) throws SQLException {
      return null;
   }


   /** {@inheritDoc} */
   @Override
   public boolean isWrapperFor(Class<?> iface) throws SQLException {
      return false;
   }
}

