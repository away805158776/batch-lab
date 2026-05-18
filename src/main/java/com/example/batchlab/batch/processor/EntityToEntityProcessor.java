package com.example.batchlab.batch.processor;

import com.example.batchlab.domain.entity.TransactionEntity;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class EntityToEntityProcessor implements ItemProcessor<TransactionEntity, TransactionEntity> {

    @Override
    public TransactionEntity process(TransactionEntity item) throws Exception {
        // 抽出时可以做些逻辑，比如只处理 IMPORTED 状态的，并把导出的状态修改为 EXPORTED
        item.setStatus("EXPORTED");
        return item;
    }
}
