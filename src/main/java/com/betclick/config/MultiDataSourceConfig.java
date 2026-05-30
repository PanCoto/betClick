package com.betclick.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class MultiDataSourceConfig {

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${spring.datasource.password}")
    private String runtimePassword;

    @Value("${DB_EMPLOYEE_PASSWORD}")
    private String employeePassword;

    @Bean
    @Primary
    public DataSource dataSource() {

        HikariDataSource runtimeDS = new HikariDataSource();
        runtimeDS.setJdbcUrl(url);
        runtimeDS.setUsername("betclick_runtime");
        runtimeDS.setPassword(runtimePassword);
        runtimeDS.setDriverClassName(driverClassName);
        runtimeDS.setPoolName("BetClickRuntimePool");
        runtimeDS.setMaximumPoolSize(10);
        runtimeDS.setMinimumIdle(2);

        HikariDataSource employeeDS = new HikariDataSource();
        employeeDS.setJdbcUrl(url);
        employeeDS.setUsername("betclick_employee");
        employeeDS.setPassword(employeePassword);
        employeeDS.setDriverClassName(driverClassName);
        employeeDS.setPoolName("BetClickEmployeePool");
        employeeDS.setMaximumPoolSize(10);
        employeeDS.setMinimumIdle(2);

        CatalogRoutingDataSource routingDS = new CatalogRoutingDataSource();
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DataSourceContextHolder.DataSourceType.RUNTIME, runtimeDS);
        targetDataSources.put(DataSourceContextHolder.DataSourceType.EMPLOYEE, employeeDS);

        routingDS.setTargetDataSources(targetDataSources);
        routingDS.setDefaultTargetDataSource(runtimeDS);
        
        routingDS.afterPropertiesSet();

        return routingDS;
    }
}
