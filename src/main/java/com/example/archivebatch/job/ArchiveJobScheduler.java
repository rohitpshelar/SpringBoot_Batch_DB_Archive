package com.example.archivebatch.job;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class ArchiveJobScheduler {

    private final JobLauncher jobLauncher;
    private final Job archiveJob;

    @Scheduled(fixedRate = 120000) // every 2 minutes
    public void runArchiveJob() throws Exception {
        jobLauncher.run(archiveJob, new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters());
    }
}