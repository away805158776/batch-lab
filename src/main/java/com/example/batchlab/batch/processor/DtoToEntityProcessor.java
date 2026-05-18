package com.example.batchlab.batch.processor;

import com.example.batchlab.domain.dto.TransactionDto;
import com.example.batchlab.domain.entity.TransactionEntity;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class DtoToEntityProcessor implements ItemProcessor<TransactionDto, TransactionEntity> {

    @Override
    public TransactionEntity process(TransactionDto item) throws Exception {
        TransactionEntity entity = new TransactionEntity();
        entity.setId(item.getId());
        entity.setName(item.getName());
        entity.setAmount(item.getAmount());
        // 入库时将状态更新为 IMPORTED
        entity.setStatus("IMPORTED");
        return entity;
    }
}
