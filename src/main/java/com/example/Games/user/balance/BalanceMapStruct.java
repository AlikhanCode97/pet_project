package com.example.Games.user.balance;

import com.example.Games.user.balance.dto.BalanceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface BalanceMapStruct {

    @Mapping(target = "balance", source = "balance")
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "amount", ignore = true)
    @Mapping(target = "operation", ignore = true)
    BalanceResponse toBalanceResponse(BigDecimal balance);
    

    @Mapping(target = "balance", source = "balance")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "amount", ignore = true)
    @Mapping(target = "operation", ignore = true)
    BalanceResponse toUserBalanceResponse(Long userId, BigDecimal balance);
    

    @Mapping(target = "balance", source = "newBalance")
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "amount", source = "amount")
    @Mapping(target = "operation", source = "operation")
    BalanceResponse toBalanceOperationResponse(
            BigDecimal amount, 
            BigDecimal newBalance, 
            String operation
    );
}
