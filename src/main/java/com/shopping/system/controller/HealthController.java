package com.shopping.system.controller;

import javax.sql.DataSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Liveness + database connectivity probe. Returns a JSON response with an
 * HTTP 200 when the database can be reached and 503 otherwise.
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("service", "online-shopping-system");

        boolean dbUp = false;
        String dbError = null;
        try (Connection connection = dataSource.getConnection()) {
            try (java.sql.Statement stmt = connection.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery("SELECT 1")) {
                dbUp = rs.next() && rs.getInt(1) == 1;
            }
            if (dbUp) {
                Map<String, Object> db = new LinkedHashMap<>();
                db.put("status", "UP");
                db.put("vendor", "H2");
                db.put("databaseProduct", connection.getMetaData().getDatabaseProductName()
                        + " " + connection.getMetaData().getDatabaseProductVersion());
                db.put("url", "" + dataSource);
                body.put("database", db);
            }
        } catch (Exception e) {
            dbUp = false;
            dbError = e.getClass().getSimpleName() + ": " + e.getMessage();
        }

        Map<String, Object> db = new LinkedHashMap<>();
        db.put("status", dbUp ? "UP" : "DOWN");
        if (!dbUp) {
            db.put("error", dbError);
            body.put("status", "DOWN");
        }
        body.put("database", db);

        HttpStatus status = dbUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(body);
    }
}