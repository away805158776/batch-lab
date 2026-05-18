package com.example.batchlab.batch.job;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DailyReportCompositeJobConfig {

    @Bean
    public Job dailyReportCompositeJob(JobRepository jobRepository, 
                                       Step csvToDbStep, 
                                       Step dbToFixedStep, 
                                       Step fileArchiveStep) {
        return new JobBuilder("dailyReportCompositeJob", jobRepository)
                .start(csvToDbStep)
                .next(dbToFixedStep)
                .next(fileArchiveStep)
                .build();
    }
}
