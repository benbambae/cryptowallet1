package com.wallet.web3_wallet_backend.blockchain.contract;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * ERC-20 Token Contract Interface.
 * Provides methods to interact with ERC-20 tokens without generating contract wrappers.
 */
public class ERC20Contract {

    private final Web3j web3j;
    private final String contractAddress;

    public ERC20Contract(Web3j web3j, String contractAddress) {
        this.web3j = web3j;
        this.contractAddress = contractAddress;
    }

    /**
     * Get token name.
     */
    public String name() throws Exception {
        Function function = new Function(
            "name",
            Collections.emptyList(),
            Arrays.asList(new TypeReference<Utf8String>() {})
        );

        String encodedFunction = org.web3j.abi.FunctionEncoder.encode(function);
        EthCall response = web3j.ethCall(
            Transaction.createEthCallTransaction(null, contractAddress, encodedFunction),
            DefaultBlockParameterName.LATEST
        ).send();

        List<org.web3j.abi.datatypes.Type> result = org.web3j.abi.FunctionReturnDecoder.decode(
            response.getValue(),
            function.getOutputParameters()
        );

        return result.isEmpty() ? "" : (String) result.get(0).getValue();
    }

    /**
     * Get token symbol.
     */
    public String symbol() throws Exception {
        Function function = new Function(
            "symbol",
            Collections.emptyList(),
            Arrays.asList(new TypeReference<Utf8String>() {})
        );

        String encodedFunction = org.web3j.abi.FunctionEncoder.encode(function);
        EthCall response = web3j.ethCall(
            Transaction.createEthCallTransaction(null, contractAddress, encodedFunction),
            DefaultBlockParameterName.LATEST
        ).send();

        List<org.web3j.abi.datatypes.Type> result = org.web3j.abi.FunctionReturnDecoder.decode(
            response.getValue(),
            function.getOutputParameters()
        );

        return result.isEmpty() ? "" : (String) result.get(0).getValue();
    }

    /**
     * Get token decimals.
     */
    public int decimals() throws Exception {
        Function function = new Function(
            "decimals",
            Collections.emptyList(),
            Arrays.asList(new TypeReference<Uint8>() {})
        );

        String encodedFunction = org.web3j.abi.FunctionEncoder.encode(function);
        EthCall response = web3j.ethCall(
            Transaction.createEthCallTransaction(null, contractAddress, encodedFunction),
            DefaultBlockParameterName.LATEST
        ).send();

        List<org.web3j.abi.datatypes.Type> result = org.web3j.abi.FunctionReturnDecoder.decode(
            response.getValue(),
            function.getOutputParameters()
        );

        return result.isEmpty() ? 18 : ((BigInteger) result.get(0).getValue()).intValue();
    }

    /**
     * Get balance of an address.
     */
    public BigInteger balanceOf(String owner) throws Exception {
        Function function = new Function(
            "balanceOf",
            Arrays.asList(new Address(owner)),
            Arrays.asList(new TypeReference<Uint256>() {})
        );

        String encodedFunction = org.web3j.abi.FunctionEncoder.encode(function);
        EthCall response = web3j.ethCall(
            Transaction.createEthCallTransaction(null, contractAddress, encodedFunction),
            DefaultBlockParameterName.LATEST
        ).send();

        List<org.web3j.abi.datatypes.Type> result = org.web3j.abi.FunctionReturnDecoder.decode(
            response.getValue(),
            function.getOutputParameters()
        );

        return result.isEmpty() ? BigInteger.ZERO : (BigInteger) result.get(0).getValue();
    }

    /**
     * Get total supply.
     */
    public BigInteger totalSupply() throws Exception {
        Function function = new Function(
            "totalSupply",
            Collections.emptyList(),
            Arrays.asList(new TypeReference<Uint256>() {})
        );

        String encodedFunction = org.web3j.abi.FunctionEncoder.encode(function);
        EthCall response = web3j.ethCall(
            Transaction.createEthCallTransaction(null, contractAddress, encodedFunction),
            DefaultBlockParameterName.LATEST
        ).send();

        List<org.web3j.abi.datatypes.Type> result = org.web3j.abi.FunctionReturnDecoder.decode(
            response.getValue(),
            function.getOutputParameters()
        );

        return result.isEmpty() ? BigInteger.ZERO : (BigInteger) result.get(0).getValue();
    }

    /**
     * Encode transfer function data.
     * Use this to build a transaction that transfers tokens.
     */
    public String encodeTransfer(String to, BigInteger amount) {
        Function function = new Function(
            "transfer",
            Arrays.asList(new Address(to), new Uint256(amount)),
            Collections.emptyList()
        );

        return org.web3j.abi.FunctionEncoder.encode(function);
    }

    /**
     * Get allowance for spender.
     */
    public BigInteger allowance(String owner, String spender) throws Exception {
        Function function = new Function(
            "allowance",
            Arrays.asList(new Address(owner), new Address(spender)),
            Arrays.asList(new TypeReference<Uint256>() {})
        );

        String encodedFunction = org.web3j.abi.FunctionEncoder.encode(function);
        EthCall response = web3j.ethCall(
            Transaction.createEthCallTransaction(null, contractAddress, encodedFunction),
            DefaultBlockParameterName.LATEST
        ).send();

        List<org.web3j.abi.datatypes.Type> result = org.web3j.abi.FunctionReturnDecoder.decode(
            response.getValue(),
            function.getOutputParameters()
        );

        return result.isEmpty() ? BigInteger.ZERO : (BigInteger) result.get(0).getValue();
    }
}
