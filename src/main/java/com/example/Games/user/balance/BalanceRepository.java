package com.example.Games.user.balance;

import com.example.Games.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;

public interface BalanceRepository extends JpaRepository<Balance, Long> {

    // ✔️ Already present — get balance by User
    Optional<Balance> findByUser(User user);

    // ✔️ Get balance by userId (more flexible for joins and API)
    Optional<Balance> findById(Long userId);

    // ✔️ Get all balances above a certain threshold (analytics use)
    List<Balance> findByAmountGreaterThan(BigDecimal amount);

    // ✔️ Get all balances below a certain threshold (e.g. low funds)
    List<Balance> findByAmountLessThan(BigDecimal amount);

    // ✔️ Check if user has sufficient funds (boolean exists)
    boolean existsByUserAndAmountGreaterThanEqual(User user, BigDecimal amount);

    // ✔️ Optional - Custom query for top balances
    List<Balance> findTop5ByOrderByAmountDesc();
}
