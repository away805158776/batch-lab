package com.example.batchlab.batch.step;

import com.example.batchlab.batch.tasklet.FileArchiveTasklet;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class FileArchiveStepConfig {

    @Bean
    public Step fileArchiveStep(JobRepository jobRepository, 
                                PlatformTransactionManager transactionManager,
                                FileArchiveTasklet fileArchiveTasklet) {
        
        return new StepBuilder("fileArchiveStep", jobRepository)
                .tasklet(fileArchiveTasklet, transactionManager)
                .build();
    }
}
