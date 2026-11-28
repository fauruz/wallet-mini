package com.example.wallet.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferHistoryItem(Long id, String direction, BigDecimal amount,
                                  String counterpartyEmail, Instant createdAt) {}
