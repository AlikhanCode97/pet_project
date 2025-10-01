package com.example.Games.user.balance;

import com.example.Games.user.balance.dto.BalanceOperationResponse;
import com.example.Games.user.balance.dto.BalanceResponse;
import com.example.Games.user.balance.transaction.OperationType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface BalanceMapStruct {

    @Mapping(target = "balance", source = "balance")
    @Mapping(target = "userId", source = "userId")
    BalanceResponse toBalanceResponse(Long userId, BigDecimal balance);

    @Mapping(target = "balance", source = "balance")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "amount", source = "amount")
    @Mapping(target = "operation", source = "operation")
    BalanceOperationResponse toBalanceOperationResponse(
            BigDecimal balance,
            Long userId,
            BigDecimal amount,
            OperationType operation
    );
}
