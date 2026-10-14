package ipiranga.fatec.qrvet;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.sql.DatabaseMetaData;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class AuditLogMigrationTests {
    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("qrvet")
            .withUsername("qrvet")
            .withPassword("qrvet-test");

    @Test
    void appliesAuditLogMigrationWithExpectedSchema() throws SQLException {
        Flyway.configure()
                .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .load()
                .migrate();

        try (var connection = mysql.createConnection("")) {
            assertEquals("audit_logs", tableName(connection));

            Map<String, ColumnMetadata> columns = columns(connection);
            assertEquals(Types.BIGINT, columns.get("id").jdbcType());
            assertEquals(Types.TIMESTAMP, columns.get("timestamp").jdbcType());
            assertEquals(6, timestampPrecision(connection));
            assertEquals("JSON", columns.get("details").typeName());
            assertEquals(DatabaseMetaData.columnNoNulls, columns.get("id").nullable());
            assertEquals(DatabaseMetaData.columnNoNulls, columns.get("timestamp").nullable());

            Set<String> indexes = indexes(connection);
            assertTrue(indexes.contains("idx_audit_logs_timestamp"));
            assertTrue(indexes.contains("idx_audit_logs_user_id"));
            assertTrue(indexes.contains("idx_audit_logs_action"));
            assertTrue(indexes.contains("idx_audit_logs_resource"));
        }
    }

    private int timestampPrecision(java.sql.Connection connection) throws SQLException {
        try (var statement = connection.prepareStatement("""
                SELECT DATETIME_PRECISION
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'audit_logs'
                  AND column_name = 'timestamp'
                """);
             ResultSet result = statement.executeQuery()) {
            assertTrue(result.next());
            return result.getInt(1);
        }
    }

    private String tableName(java.sql.Connection connection) throws SQLException {
        try (ResultSet tables = connection.getMetaData().getTables(null, null, "audit_logs", null)) {
            assertTrue(tables.next());
            return tables.getString("TABLE_NAME");
        }
    }

    private Map<String, ColumnMetadata> columns(java.sql.Connection connection) throws SQLException {
        Map<String, ColumnMetadata> result = new HashMap<>();
        try (ResultSet columns = connection.getMetaData().getColumns(null, null, "audit_logs", null)) {
            while (columns.next()) {
                result.put(columns.getString("COLUMN_NAME").toLowerCase(),
                        new ColumnMetadata(
                                columns.getInt("DATA_TYPE"),
                                columns.getInt("DECIMAL_DIGITS"),
                                columns.getInt("NULLABLE"),
                                columns.getString("TYPE_NAME")));
            }
        }
        return result;
    }

    private Set<String> indexes(java.sql.Connection connection) throws SQLException {
        Set<String> result = new java.util.HashSet<>();
        try (ResultSet indexes = connection.getMetaData().getIndexInfo(null, null, "audit_logs", false, false)) {
            while (indexes.next()) {
                String name = indexes.getString("INDEX_NAME");
                if (name != null) {
                    result.add(name);
                }
            }
        }
        return result;
    }

    private record ColumnMetadata(int jdbcType, int decimalDigits, int nullable, String typeName) {
    }
}
