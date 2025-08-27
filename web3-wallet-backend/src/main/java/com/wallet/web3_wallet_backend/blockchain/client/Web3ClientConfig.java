package com.wallet.web3_wallet_backend.blockchain.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Configuration
public class Web3ClientConfig {

    @Bean
    public Web3j web3j(@Value("${web3.ethereum.rpcUrl:http://localhost:8545}") String rpcUrl) {
        return Web3j.build(new HttpService(rpcUrl));
    }
}
