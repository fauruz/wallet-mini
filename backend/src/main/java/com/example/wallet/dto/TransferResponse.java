package com.example.wallet.dto;

import java.math.BigDecimal;

public record TransferResponse(Long id, BigDecimal amount, BigDecimal balanceAfter, boolean replayed) {}