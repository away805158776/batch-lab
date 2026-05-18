package com.example.batchlab.batch.reader;

import com.example.batchlab.domain.entity.TransactionEntity;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.MyBatisCursorItemReader;
import org.mybatis.spring.batch.builder.MyBatisCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DbTransactionReaderConfig {

    @Bean
    public MyBatisCursorItemReader<TransactionEntity> transactionDbReader(SqlSessionFactory sqlSessionFactory) {
        return new MyBatisCursorItemReaderBuilder<TransactionEntity>()
                .sqlSessionFactory(sqlSessionFactory)
                .queryId("com.example.batchlab.mapper.TransactionMapper.selectAll")
                .build();
    }
}
