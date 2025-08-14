import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.charset.StandardCharsets;

import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonObject;

/*
 * This script demonstrates how to store and retrieve JSON documents in an Oracle 19c or later database.
 * For Oracle 21c and later, you can use the native JSON data type. For Oracle 19c, JSON is stored in a BLOB column with a check constraint
 * to avoid character set conversion issues.
 *
 * Pre-requisites:
 * 1. Oracle Database instance (19c or later).
 * 2. Oracle JDBC driver (ojdbc11.jar or later) in the classpath.
 *
 * How to compile and run:
 * 1. Make sure you have the Oracle JDBC driver in your classpath.
 *    For example: export CLASSPATH=.:/path/to/ojdbc11.jar
 * 2. Compile the Java file:
 *    javac OracleJsonExample.java
 * 3. Run the compiled class:
 *    java OracleJsonExample
 */
public class OracleJsonExample {

    //
    // !!! IMPORTANT !!!
    //
    // UPDATE THE FOLLOWING DATABASE CONNECTION DETAILS BEFORE RUNNING THE SCRIPT
    //
    private static final String DB_URL = "jdbc:oracle:thin:@//localhost:1521/FREE";
    private static final String DB_USER = "your_username";
    private static final String DB_PASSWORD = "your_password";

    public static void main(String[] args) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            if (connection != null) {
                System.out.println("Connected to the Oracle database!");
                createTableAndInsertJson(connection);
                retrieveAndPrintJson(connection);
            } else {
                System.out.println("Failed to make connection!");
            }
        } catch (SQLException e) {
            System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createTableAndInsertJson(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // Drop the table if it already exists to make the script idempotent
            try {
                statement.execute("DROP TABLE json_docs");
                System.out.println("Dropped existing table json_docs.");
            } catch (SQLException e) {
                // Ignore error if table doesn't exist
                if (e.getErrorCode() != 942) {
                    throw e;
                }
            }

            // Create a table with a BLOB column to store JSON, with a check constraint to ensure it's valid JSON
            statement.execute("CREATE TABLE json_docs (id NUMBER PRIMARY KEY, data BLOB CHECK (data IS JSON))");
            System.out.println("Created table json_docs.");

            // Create a JSON object
            OracleJsonFactory factory = new OracleJsonFactory();
            OracleJsonObject jsonObject = factory.createObject();
            jsonObject.put("name", "John Doe");
            jsonObject.put("email", "john.doe@example.com");
            jsonObject.put("age", 30);

            // Insert the JSON document into the table
            String sql = "INSERT INTO json_docs (id, data) VALUES (?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setInt(1, 1);
                preparedStatement.setBytes(2, jsonObject.toString().getBytes(StandardCharsets.UTF_8));
                preparedStatement.executeUpdate();
                System.out.println("Inserted JSON document into json_docs table.");
            }
        }
    }

    private static void retrieveAndPrintJson(Connection connection) throws SQLException {
        String sql = "SELECT data FROM json_docs WHERE id = 1";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    byte[] retrievedBytes = resultSet.getBytes(1);
                    String retrievedJson = new String(retrievedBytes, StandardCharsets.UTF_8);
                    System.out.println("\nRetrieved JSON document from database:");
                    System.out.println(retrievedJson);
                }
            }
        }
    }
}
