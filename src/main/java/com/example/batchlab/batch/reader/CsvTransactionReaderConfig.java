package com.example.batchlab.batch.reader;

import com.example.batchlab.domain.dto.TransactionDto;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import java.io.File;

@Configuration
public class CsvTransactionReaderConfig {

    @Value("${app.batch.input-dir}")
    private String inputDir;

    @Bean
    public FlatFileItemReader<TransactionDto> transactionCsvReader() {
        return new FlatFileItemReaderBuilder<TransactionDto>()
                .name("transactionItemReader")
                .resource(new FileSystemResource(inputDir + File.separator + "sample.csv"))
                .delimited()
                .names("id", "name", "amount", "status")
                .linesToSkip(1) // skip header
                .targetType(TransactionDto.class)
                .build();
    }
}
