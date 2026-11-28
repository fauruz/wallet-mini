package com.example.wallet.controller;

import com.example.wallet.dto.PageResponse;
import com.example.wallet.dto.TransferHistoryItem;
import com.example.wallet.dto.TransferRequest;
import com.example.wallet.dto.TransferResponse;
import com.example.wallet.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public TransferResponse transfer(Authentication authentication,
                                     @RequestHeader("Idempotency-Key") String idempotencyKey,
                                     @Valid @RequestBody TransferRequest request) {
        if (idempotencyKey.isBlank() || idempotencyKey.length() > 64) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Idempotency-Key");
        }
        return transferService.transfer(Long.valueOf(authentication.getName()), idempotencyKey, request);
    }

    @GetMapping
    public PageResponse<TransferHistoryItem> history(Authentication authentication,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        return transferService.history(Long.valueOf(authentication.getName()), page, size);
    }
}