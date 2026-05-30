package com.betclick.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class CatalogRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return DataSourceContextHolder.get();
    }
}
