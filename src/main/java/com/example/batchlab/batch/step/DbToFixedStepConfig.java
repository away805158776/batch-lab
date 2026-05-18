package com.example.batchlab.batch.step;

import com.example.batchlab.domain.entity.TransactionEntity;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.mybatis.spring.batch.MyBatisCursorItemReader;

@Configuration
public class DbToFixedStepConfig {

    @Bean
    public Step dbToFixedStep(JobRepository jobRepository, 
                              PlatformTransactionManager transactionManager,
                              MyBatisCursorItemReader<TransactionEntity> transactionDbReader,
                              ItemProcessor<TransactionEntity, TransactionEntity> entityToEntityProcessor,
                              FlatFileItemWriter<TransactionEntity> transactionFixedWriter) {
        
        return new StepBuilder("dbToFixedStep", jobRepository)
                .<TransactionEntity, TransactionEntity>chunk(10, transactionManager)
                .reader(transactionDbReader)
                .processor(entityToEntityProcessor)
                .writer(transactionFixedWriter)
                .build();
    }
}
