package com.example.wallet.service;

import java.util.Optional;

import com.example.wallet.dto.PageResponse;
import com.example.wallet.dto.TransferHistoryItem;
import com.example.wallet.dto.TransferRequest;
import com.example.wallet.dto.TransferResponse;
import com.example.wallet.entity.Transfer;
import com.example.wallet.entity.User;
import com.example.wallet.entity.Wallet;
import com.example.wallet.repository.TransferRepository;
import com.example.wallet.repository.UserRepository;
import com.example.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final TransferRepository transferRepository;

    @Transactional
    public TransferResponse transfer(Long fromUserId, String idempotencyKey, TransferRequest request) {
        Long fromId = walletRepository.findIdByUserId(fromUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        User toUser = userRepository.findByEmail(request.toEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient not found"));
        Long toId = walletRepository.findIdByUserId(toUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient wallet not found"));

        if (fromId.equals(toId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot transfer to yourself");
        }

        // Khóa hai ví theo thứ tự id nhỏ trước để tránh deadlock
        Long firstId = Math.min(fromId, toId);
        Long secondId = Math.max(fromId, toId);
        Wallet first = walletRepository.findByIdForUpdate(firstId).orElseThrow();
        Wallet second = walletRepository.findByIdForUpdate(secondId).orElseThrow();
        Wallet from = first.getId().equals(fromId) ? first : second;
        Wallet to = (from == first) ? second : first;

        // Kiểm tra idempotency SAU khi đã khóa: request cùng key phải xếp hàng sau request đầu
        Optional<Transfer> existing =
                transferRepository.findByFromWalletIdAndIdempotencyKey(fromId, idempotencyKey);
        if (existing.isPresent()) {
            Transfer old = existing.get();
            boolean sameRequest = old.getToWallet().getId().equals(toId)
                    && old.getAmount().compareTo(request.amount()) == 0;
            if (!sameRequest) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Idempotency-Key was already used with a different request");
            }
            return new TransferResponse(old.getId(), old.getAmount(), old.getFromBalanceAfter(), true);
        }

        if (from.getBalance().compareTo(request.amount()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
        }

        from.setBalance(from.getBalance().subtract(request.amount()));
        to.setBalance(to.getBalance().add(request.amount()));

        Transfer transfer = new Transfer();
        transfer.setFromWallet(from);
        transfer.setToWallet(to);
        transfer.setAmount(request.amount());
        transfer.setIdempotencyKey(idempotencyKey);
        transfer.setFromBalanceAfter(from.getBalance());
        transferRepository.save(transfer);

        return new TransferResponse(transfer.getId(), request.amount(), from.getBalance(), false);
    }

    @Transactional(readOnly = true)
    public PageResponse<TransferHistoryItem> history(Long userId, int page, int size) {
        Long walletId = walletRepository.findIdByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));

        Page<Transfer> result = transferRepository.findHistory(walletId, pageable);
        var items = result.getContent().stream().map(t -> toItem(t, walletId)).toList();
        return new PageResponse<>(items, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    private TransferHistoryItem toItem(Transfer t, Long myWalletId) {
        boolean outgoing = t.getFromWallet().getId().equals(myWalletId);
        String counterparty = outgoing
                ? t.getToWallet().getUser().getEmail()
                : t.getFromWallet().getUser().getEmail();
        return new TransferHistoryItem(t.getId(), outgoing ? "OUT" : "IN",
                t.getAmount(), counterparty, t.getCreatedAt());
    }
}