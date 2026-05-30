package com.betclick.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class AdminDataSourceConfig {

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${DB_ADMIN_PASSWORD}")
    private String adminPassword;

    @Value("${DB_ADMIN_USERNAME:betclick_admin}")
    private String adminUsername;

    @Bean(name = "adminDataSource")
    public DataSource adminDataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(adminUsername);
        ds.setPassword(adminPassword);
        ds.setDriverClassName(driverClassName);
        ds.setPoolName("BetClickAdminPool");
        ds.setMaximumPoolSize(3);
        ds.setMinimumIdle(1);
        return ds;
    }

    @Bean(name = "adminJdbcTemplate")
    public JdbcTemplate adminJdbcTemplate() {
        return new JdbcTemplate(adminDataSource());
    }
}
