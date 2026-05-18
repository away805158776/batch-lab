package com.example.batchlab.batch.writer;

import com.example.batchlab.domain.entity.TransactionEntity;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.FormatterLineAggregator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import java.io.File;

@Configuration
public class FixedTransactionWriterConfig {

    @Value("${app.batch.output-dir}")
    private String outputDir;

    @Bean
    public FlatFileItemWriter<TransactionEntity> transactionFixedWriter() {
        
        BeanWrapperFieldExtractor<TransactionEntity> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(new String[]{"id", "name", "amount", "status"});

        // 固定长格式：id(10位数字), name(20位字符左对齐), amount(10位浮点数), status(10位字符)
        FormatterLineAggregator<TransactionEntity> lineAggregator = new FormatterLineAggregator<>();
        lineAggregator.setFormat("%010d%-20s%10.2f%-10s");
        lineAggregator.setFieldExtractor(fieldExtractor);

        return new FlatFileItemWriterBuilder<TransactionEntity>()
                .name("transactionFixedWriter")
                .resource(new FileSystemResource(outputDir + File.separator + "export.txt"))
                .lineAggregator(lineAggregator)
                .build();
    }
}
