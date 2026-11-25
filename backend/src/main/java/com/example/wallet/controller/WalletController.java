package com.example.wallet.controller;

import com.example.wallet.dto.TopUpRequest;
import com.example.wallet.entity.Wallet;
import com.example.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public Map<String, Object> getWallet(Authentication authentication) {
        Wallet wallet = walletService.getByUserId(Long.valueOf(authentication.getName()));
        return Map.of("id", wallet.getId(), "balance", wallet.getBalance());
    }

    @PostMapping("/top-up")
    public Map<String, Object> topUp(Authentication authentication,
                                     @Valid @RequestBody TopUpRequest request) {
        Wallet wallet = walletService.topUp(
                Long.valueOf(authentication.getName()), request.amount());
        return Map.of("id", wallet.getId(), "balance", wallet.getBalance());
    }
}
