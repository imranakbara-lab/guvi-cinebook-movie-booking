package com.cinebook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.URI;
import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class CineBookApplication {

    public static void main(String[] args) {
        // Show times are Indian times; keep the server clock on IST even when hosted elsewhere.
        TimeZone.setDefault(TimeZone.getTimeZone(System.getenv().getOrDefault("APP_TIMEZONE", "Asia/Kolkata")));
        useDatabaseUrlIfPresent();
        SpringApplication.run(CineBookApplication.class, args);
    }

    /** Converts Render's DATABASE_URL (postgres://user:pass@host:port/db) into Spring JDBC settings. */
    static void useDatabaseUrlIfPresent() {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank() || System.getenv("DB_URL") != null) return;
        URI uri = URI.create(databaseUrl.replaceFirst("^postgres(ql)?://", "postgresql://"));
        String[] userInfo = uri.getUserInfo() == null ? new String[0] : uri.getUserInfo().split(":", 2);
        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        System.setProperty("spring.datasource.url", "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getPath());
        if (userInfo.length > 0) System.setProperty("spring.datasource.username", userInfo[0]);
        if (userInfo.length > 1) System.setProperty("spring.datasource.password", userInfo[1]);
    }
}
