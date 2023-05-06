package org.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

public class DbClient {

    private final String dbConnectionString;
    private Connection dbConnection;
    private static final Logger logger = LoggerFactory.getLogger(DbClient.class);
    public DbClient(String dbConnectionString) {
        this.dbConnectionString = dbConnectionString;
        init(dbConnectionString,
                System.getenv().getOrDefault("DB_USER", "postgres"),
                System.getenv().getOrDefault("DB_PASSWORD", "postgres"));
    }

    // "jdbc:postgresql://localhost:5432/testdb"
    private void init(String dbConnectionString, String username, String password) {
        try {
            //Class.forName("org.postgresql.Driver");
            logger.info("Connection String {}, username {} password {}", dbConnectionString, username, password);
            dbConnection = DriverManager
                    .getConnection(dbConnectionString, username, password);

            logger.info("Opened database successfully");


            Statement s = dbConnection.createStatement();
            //String sql = "SELECT * FROM pg_catalog.pg_tables WHERE schemaname != 'pg_catalog' AND schemaname != 'information_schema'; ";
            String createTable = "CREATE TABLE MONITORING_RESULTS("
                    + "id SERIAL PRIMARY KEY,"
                    + "device_id VARCHAR(10) NOT NULL,"
                    + "temperature FLOAT NOT NULL,"
                    + "timestamp timestamp without time zone NOT NULL,"
                    + "door_status VARCHAR(6) NOT NULL);";

            logger.info("create query: " + createTable);
            //int result = s.executeUpdate(createTable);

            s.close();
            logger.info("DB inited successfully");

        } catch ( Exception e ) {
            System.err.println( e.getClass().getName()+": "+ e.getMessage() );
            System.exit(0);
        }
    }

    protected void submitEventsForWrite(List<KafkaConsumerWrapper.TelemetryDataPoint> telemetryDataPoints) {
        try {
            if (dbConnection.isClosed()) {
                // reconnect, for now just throw exception
                throw new RuntimeException("Db connection closed");
            }

            for(KafkaConsumerWrapper.TelemetryDataPoint e: telemetryDataPoints) {
                final String queryString = getQueryString(e);
                logger.info("prepared query: {}", queryString);
                Statement s = dbConnection.createStatement();
                int result = s.executeUpdate(queryString);
                logger.info("result: {}", result);
                s.close();
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    public static String getQueryString(KafkaConsumerWrapper.TelemetryDataPoint e) {
        //return String.format("INSERT INTO monitoring_results (DEVICE_ID,TEMPERATURE,TIMESTAMP,DOOR_STATUS) " +
        //        "VALUES (%s, %d,to_timestamp(%d),%s );", e.deviceId, e.temperature, e.ts, e.door);
        int status =  e.door.equalsIgnoreCase("close") ? 0:1;
        return "INSERT INTO MONITORING_RESULTS (DEVICE_ID,TEMPERATURE,TIMESTAMP,DOOR_STATUS) " +
                "VALUES ('" + e.deviceId + "', " + e.temperature + ",to_timestamp(" + e.ts + ")," + status + ");";


    }
    protected void writeEvents() {

    }

    protected void close() {
        try {
            if (!dbConnection.isClosed()) {
                // reconnect, for now just throw exception
               dbConnection.close();
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

}
