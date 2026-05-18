package com.example.batchlab.batch.step;

import com.example.batchlab.domain.dto.TransactionDto;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class CsvProcessStepConfig {

    @Bean
    public Step processCsvStep(JobRepository jobRepository, 
                               PlatformTransactionManager transactionManager,
                               FlatFileItemReader<TransactionDto> transactionCsvReader,
                               ItemProcessor<TransactionDto, TransactionDto> transactionProcessor,
                               FlatFileItemWriter<TransactionDto> transactionCsvWriter) {
        
        return new StepBuilder("processCsvStep", jobRepository)
                .<TransactionDto, TransactionDto>chunk(10, transactionManager)
                .reader(transactionCsvReader)
                .processor(transactionProcessor)
                .writer(transactionCsvWriter)
                .build();
    }
}
