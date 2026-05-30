package com.betclick.repository;

import com.betclick.model.Transaction;
import com.betclick.model.enums.TransactionType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    List<Transaction> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, TransactionType type);
    List<Transaction> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, TransactionType type, Pageable pageable);
    List<Transaction> findAllByOrderByCreatedAtDesc();
    List<Transaction> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
