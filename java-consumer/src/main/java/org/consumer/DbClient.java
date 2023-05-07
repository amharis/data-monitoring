package org.consumer;

import org.consumer.model.TelemetryDataPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
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

    private void init(String dbConnectionString, String username, String password) {
        try {
            //Class.forName("org.postgresql.Driver");
            logger.info("Connection String {}, username {} password {}", dbConnectionString, username, password);
            dbConnection = DriverManager
                    .getConnection(dbConnectionString, username, password);

            logger.info("Opened database successfully");
            Statement s = dbConnection.createStatement();
            s.close();
            logger.info("DB inited successfully");

        } catch ( Exception e ) {
            logger.error( e.getClass().getName()+": "+ e.getMessage() );
            throw new RuntimeException(e);
        }
    }

    protected void submitEventsForWrite(List<TelemetryDataPoint> telemetryDataPoints) {
        try {
            if (dbConnection.isClosed()) {
                // TODO: try to reconnect, for now just throw exception
                throw new RuntimeException("Db connection closed");
            }

            for(TelemetryDataPoint e: telemetryDataPoints) {
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

    public static String getQueryString(TelemetryDataPoint e) {
        //return String.format("INSERT INTO monitoring_results (DEVICE_ID,TEMPERATURE,TIMESTAMP,DOOR_STATUS) " +
        //        "VALUES (%s, %d,to_timestamp(%d),%s );", e.deviceId, e.temperature, e.ts, e.door);
        int status =  e.door.equalsIgnoreCase("close") ? 0:1;
        return "INSERT INTO MONITORING_RESULTS (DEVICE_ID,TEMPERATURE,TIMESTAMP,DOOR_STATUS) " +
                "VALUES ('" + e.deviceId + "', " + e.temperature + ",to_timestamp(" + e.ts + ")," + status + ");";
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
