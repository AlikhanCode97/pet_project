package com.example.Games.user.balance.transaction;

import com.example.Games.user.balance.Balance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BalanceTransactionRepository extends JpaRepository<BalanceTransaction, Long> {

    List<BalanceTransaction> findAllByBalanceOrderByTimestampDesc(Balance balance);
}
