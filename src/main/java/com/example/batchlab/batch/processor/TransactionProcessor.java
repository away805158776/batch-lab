package com.example.batchlab.batch.processor;

import com.example.batchlab.domain.dto.TransactionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class TransactionProcessor implements ItemProcessor<TransactionDto, TransactionDto> {

    private static final Logger log = LoggerFactory.getLogger(TransactionProcessor.class);

    @Override
    public TransactionDto process(TransactionDto item) throws Exception {
        log.info("Processing transaction id: {}, name: {}", item.getId(), item.getName());
        // 模拟一些复杂的业务逻辑：检查数据、转换格式等
        item.setStatus("PROCESSED");
        return item;
    }
}
