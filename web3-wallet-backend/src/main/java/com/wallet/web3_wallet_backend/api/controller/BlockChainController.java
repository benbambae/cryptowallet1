package com.wallet.web3_wallet_backend.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;

@RestController
public class BlockChainController {

    private final Web3j web3j;

    public BlockChainController(Web3j web3j) {
        this.web3j = web3j;
    }

    @GetMapping("/api/v1/blockNumber")
    public String getBlockNumber() throws Exception {
        return web3j.ethBlockNumber().send()
                   .getBlockNumber()
                   .toString();
    }
}
