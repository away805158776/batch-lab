package com.example.batchlab.batch.step;

import com.example.batchlab.domain.dto.TransactionDto;
import com.example.batchlab.domain.entity.TransactionEntity;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.mybatis.spring.batch.MyBatisBatchItemWriter;

@Configuration
public class CsvToDbStepConfig {

    @Bean
    public Step csvToDbStep(JobRepository jobRepository, 
                            PlatformTransactionManager transactionManager,
                            FlatFileItemReader<TransactionDto> transactionCsvReader,
                            ItemProcessor<TransactionDto, TransactionEntity> dtoToEntityProcessor,
                            MyBatisBatchItemWriter<TransactionEntity> transactionDbWriter) {
        
        return new StepBuilder("csvToDbStep", jobRepository)
                .<TransactionDto, TransactionEntity>chunk(10, transactionManager)
                .reader(transactionCsvReader)
                .processor(dtoToEntityProcessor)
                .writer(transactionDbWriter)
                .build();
    }
}
