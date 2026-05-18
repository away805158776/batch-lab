package com.example.batchlab.mapper;

import com.example.batchlab.domain.entity.TransactionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TransactionMapper {

    void insert(TransactionEntity transaction);

    void insertBatch(@Param("list") List<TransactionEntity> list);

    List<TransactionEntity> selectAll();
}
