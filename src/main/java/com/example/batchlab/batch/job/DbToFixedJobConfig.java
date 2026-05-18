package com.example.batchlab.batch.job;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DbToFixedJobConfig {

    @Bean
    public Job dbToFixedJob(JobRepository jobRepository, Step dbToFixedStep) {
        return new JobBuilder("dbToFixedJob", jobRepository)
                .start(dbToFixedStep)
                .build();
    }
}
