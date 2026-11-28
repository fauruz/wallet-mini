package com.example.wallet.repository;

import com.example.wallet.entity.Transfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    Optional<Transfer> findByFromWalletIdAndIdempotencyKey(Long fromWalletId, String idempotencyKey);

    @Query(value = """
            select t from Transfer t
              join fetch t.fromWallet fw join fetch fw.user
              join fetch t.toWallet tw join fetch tw.user
            where fw.id = :walletId or tw.id = :walletId
            """,
            countQuery = """
            select count(t) from Transfer t
            where t.fromWallet.id = :walletId or t.toWallet.id = :walletId
            """)
    Page<Transfer> findHistory(@Param("walletId") Long walletId, Pageable pageable);
}
