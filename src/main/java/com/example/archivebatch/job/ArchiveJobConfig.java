package com.example.archivebatch.job;

import com.example.archivebatch.config.JobCompletionNotificationImpl;
import com.example.archivebatch.model.ArchiveRecord;
import com.example.archivebatch.model.MainRecord;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Configuration
@EnableBatchProcessing
public class ArchiveJobConfig {

    @Bean
    public JdbcCursorItemReader<MainRecord> reader(@Qualifier("mainDataSource") DataSource mainDataSource) {
        JdbcCursorItemReader<MainRecord> reader = new JdbcCursorItemReader<>();
        reader.setDataSource(mainDataSource);
        reader.setSql("SELECT id, data, created_at FROM my_table ORDER BY id LIMIT 2");
        reader.setRowMapper((ResultSet rs, int rowNum) -> {
            MainRecord r = new MainRecord();
            r.setId(rs.getLong("id"));
            r.setData(rs.getString("data"));
            Timestamp ts = rs.getTimestamp("created_at");
            if (ts != null) r.setCreatedAt(ts.toLocalDateTime());
            return r;
        });
        return reader;
    }

    @Bean
    public ItemProcessor<MainRecord, ArchiveRecord> processor() {
        return item -> {
            ArchiveRecord ar = new ArchiveRecord();
            ar.setId(item.getId());
            ar.setData(item.getData());
            ar.setArchivedAt(LocalDateTime.now());
            return ar;
        };
    }

    @Bean
    public ItemWriter<ArchiveRecord> writer(@Qualifier("archiveDataSource") DataSource archiveDataSource, @Qualifier("mainDataSource") DataSource mainDataSource) {
        return new ArchiveThenDeleteWriter(archiveDataSource, mainDataSource);
    }

    @Bean
    public Step archiveStep(JobRepository jobRepository,
                            @Qualifier("mainTransactionManager") DataSourceTransactionManager transactionManager,
                            JdbcCursorItemReader<MainRecord> reader,
                            ItemProcessor<MainRecord, ArchiveRecord> processor,
                            ItemWriter<ArchiveRecord> writer) {
        return new StepBuilder("archiveStep", jobRepository)
                .<MainRecord, ArchiveRecord>chunk(5, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public Job archiveJob(JobRepository jobRepository,
                          JobCompletionNotificationImpl listener, Step archiveStep) {
        return  new JobBuilder("job", jobRepository)
                .listener(listener)
                .start(archiveStep)
                .build();
    }
}