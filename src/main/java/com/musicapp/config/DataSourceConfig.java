package com.musicapp.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Slf4j
@Configuration
public class DataSourceConfig {

    @Value("${DATABASE_URL:#{null}}")
    private String databaseUrl;

    @Value("${SPRING_DATASOURCE_URL:#{null}}")
    private String springDatasourceUrl;

    @Value("${spring.datasource.url:#{null}}")
    private String defaultDatasourceUrl;

    @Value("${spring.datasource.username:${DB_USERNAME:postgres}}")
    private String defaultUsername;

    @Value("${spring.datasource.password:${DB_PASSWORD:postgres}}")
    private String defaultPassword;

    @Value("${DB_HOST:localhost}")
    private String dbHost;

    @Value("${DB_PORT:5432}")
    private String dbPort;

    @Value("${DB_NAME:musicapp}")
    private String dbName;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.postgresql.Driver");

        // Priority order: DATABASE_URL > SPRING_DATASOURCE_URL > defaultDatasourceUrl
        String rawUrl = databaseUrl;
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            rawUrl = springDatasourceUrl;
        }
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            rawUrl = defaultDatasourceUrl;
        }

        if (rawUrl != null && !rawUrl.trim().isEmpty()) {
            rawUrl = rawUrl.trim();
            // Handle postgres:// or postgresql:// URI format (standard in Neon / Render)
            if (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://")) {
                try {
                    // Normalize protocol to http:// for java.net.URI parsing
                    String uriString = rawUrl.startsWith("postgres://")
                        ? "http://" + rawUrl.substring("postgres://".length())
                        : "http://" + rawUrl.substring("postgresql://".length());

                    URI uri = URI.create(uriString);
                    String host = uri.getHost();
                    int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                    String path = uri.getPath() != null && !uri.getPath().isEmpty() ? uri.getPath() : "/neondb";
                    String query = uri.getQuery();

                    String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
                    if (query != null && !query.isEmpty()) {
                        if (!query.contains("sslmode=")) {
                            jdbcUrl += "?" + query + "&sslmode=require";
                        } else {
                            jdbcUrl += "?" + query;
                        }
                    } else {
                        jdbcUrl += "?sslmode=require";
                    }

                    config.setJdbcUrl(jdbcUrl);

                    if (uri.getUserInfo() != null && !uri.getUserInfo().isEmpty()) {
                        String[] userInfo = uri.getUserInfo().split(":", 2);
                        config.setUsername(userInfo[0]);
                        if (userInfo.length > 1) {
                            config.setPassword(userInfo[1]);
                        }
                    } else {
                        config.setUsername(defaultUsername);
                        config.setPassword(defaultPassword);
                    }

                    log.info("Configured PostgreSQL DataSource from Neon/Render URI to host: {}", host);
                } catch (Exception e) {
                    log.error("Failed to parse DATABASE_URL URI, falling back to raw JDBC configuration", e);
                    config.setJdbcUrl(rawUrl);
                    config.setUsername(defaultUsername);
                    config.setPassword(defaultPassword);
                }
            } else {
                // Already jdbc:... formatted
                String jdbcUrl = rawUrl;
                if ((jdbcUrl.contains("neon.tech") || jdbcUrl.contains("render.com")) && !jdbcUrl.contains("sslmode=")) {
                    jdbcUrl += (jdbcUrl.contains("?") ? "&" : "?") + "sslmode=require";
                }
                config.setJdbcUrl(jdbcUrl);
                config.setUsername(defaultUsername);
                config.setPassword(defaultPassword);
                log.info("Configured PostgreSQL DataSource with JDBC URL: {}", jdbcUrl);
            }
        } else {
            // Local fallback host & port
            String jdbcUrl = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName;
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(defaultUsername);
            config.setPassword(defaultPassword);
            log.info("Configured default local PostgreSQL DataSource: {}:{}", dbHost, dbPort);
        }

        // HikariCP connection pool settings optimized for Cloud / Serverless Neon
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setMaxLifetime(60000); // 60s max lifetime prevents stale connection timeouts with Neon pooler
        config.setConnectionTimeout(30000);
        config.setPoolName("VoxaHikariPool");

        return new HikariDataSource(config);
    }
}
