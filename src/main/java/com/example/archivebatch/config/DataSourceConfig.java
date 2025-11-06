package com.example.archivebatch.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.main")
    public DataSourceProperties mainDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = {"mainDataSource", "dataSource"})
    @Primary
    public DataSource mainDataSource() {
        return mainDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.archive")
    public DataSourceProperties archiveDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource archiveDataSource() {
        return archiveDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean(name = {"mainTransactionManager", "transactionManager"})
    public DataSourceTransactionManager mainTransactionManager(@Qualifier("mainDataSource") DataSource ds) {
        return new DataSourceTransactionManager(ds);
    }

    @Bean(name = "archiveTransactionManager")
    public DataSourceTransactionManager archiveTransactionManager(@Qualifier("archiveDataSource") DataSource ds) {
        return new DataSourceTransactionManager(ds);
    }
}