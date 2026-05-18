package com.example.batchlab.batch.writer;

import com.example.batchlab.domain.entity.TransactionEntity;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.MyBatisBatchItemWriter;
import org.mybatis.spring.batch.builder.MyBatisBatchItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DbTransactionWriterConfig {

    @Bean
    public MyBatisBatchItemWriter<TransactionEntity> transactionDbWriter(SqlSessionFactory sqlSessionFactory) {
        return new MyBatisBatchItemWriterBuilder<TransactionEntity>()
                .sqlSessionFactory(sqlSessionFactory)
                .statementId("com.example.batchlab.mapper.TransactionMapper.insert")
                .build();
    }
}
