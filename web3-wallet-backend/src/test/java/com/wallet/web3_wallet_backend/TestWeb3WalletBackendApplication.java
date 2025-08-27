package com.wallet.web3_wallet_backend;

import org.springframework.boot.SpringApplication;

public class TestWeb3WalletBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(Web3WalletBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
