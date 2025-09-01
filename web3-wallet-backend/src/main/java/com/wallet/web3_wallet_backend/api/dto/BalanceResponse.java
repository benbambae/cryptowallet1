
package com.wallet.web3_wallet_backend.api.dto;

import java.math.BigDecimal;

public record BalanceResponse(String address, BigDecimal balanceEther) {}