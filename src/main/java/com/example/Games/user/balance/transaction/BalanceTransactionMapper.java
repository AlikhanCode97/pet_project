package com.example.Games.user.balance.transaction;

import com.example.Games.user.balance.transaction.dto.BalanceTransactionDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BalanceTransactionMapper {

    BalanceTransactionDTO toDto(BalanceTransaction transaction);
    
    List<BalanceTransactionDTO> toDtoList(List<BalanceTransaction> transactions);
}
