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
 * <p>
 * This class provides a lightweight interface to interact with ERC-20 token contracts
 * on the Ethereum blockchain using the web3j library, without the need for generated
 * contract wrappers. It exposes common ERC-20 read-only methods and helpers for encoding
 * transactions.
 * </p>
 */
public class ERC20Contract {

    /**
     * The Web3j instance used to communicate with the Ethereum node.
     */
    private final Web3j web3j;

    /**
     * The address of the ERC-20 token contract.
     */
    private final String contractAddress;

    /**
     * Constructs a new ERC20Contract instance.
     *
     * @param web3j           The Web3j instance for blockchain communication.
     * @param contractAddress The address of the ERC-20 token contract.
     */
    public ERC20Contract(Web3j web3j, String contractAddress) {
        this.web3j = web3j;
        this.contractAddress = contractAddress;
    }

    /**
     * Retrieves the name of the ERC-20 token.
     *
     * @return The token's name as a String, or an empty string if not available.
     * @throws Exception if the call to the blockchain fails.
     */
    public String name() throws Exception {
        // Prepare the 'name' function call (no arguments, returns Utf8String)
        Function function = new Function(
            "name",
            Collections.emptyList(),
            Arrays.asList(new TypeReference<Utf8String>() {})
        );

        // Encode the function for the eth_call
        String encodedFunction = org.web3j.abi.FunctionEncoder.encode(function);

        // Perform a call to the contract (read-only, no sender required)
        EthCall response = web3j.ethCall(
            Transaction.createEthCallTransaction(null, contractAddress, encodedFunction),
            DefaultBlockParameterName.LATEST
        ).send();

        // Decode the returned value
        List<org.web3j.abi.datatypes.Type> result = org.web3j.abi.FunctionReturnDecoder.decode(
            response.getValue(),
            function.getOutputParameters()
        );

        // Return the name or empty string if not present
        return result.isEmpty() ? "" : (String) result.get(0).getValue();
    }

    /**
     * Retrieves the symbol of the ERC-20 token.
     *
     * @return The token's symbol as a String, or an empty string if not available.
     * @throws Exception if the call to the blockchain fails.
     */
    public String symbol() throws Exception {
        // Prepare the 'symbol' function call (no arguments, returns Utf8String)
        Function function = new Function(
            "symbol",
            Collections.emptyList(),
            Arrays.asList(new TypeReference<Utf8String>() {})
        );

        // Encode the function for the eth_call
        String encodedFunction = org.web3j.abi.FunctionEncoder.encode(function);

        // Perform a call to the contract (read-only, no sender required)
        EthCall response = web3j.ethCall(
            Transaction.createEthCallTransaction(null, contractAddress, encodedFunction),
            DefaultBlockParameterName.LATEST
        ).send();

        // Decode the returned value
        List<org.web3j.abi.datatypes.Type> result = org.web3j.abi.FunctionReturnDecoder.decode(
            response.getValue(),
            function.getOutputParameters()
        );

        // Return the symbol or empty string if not present
        return result.isEmpty() ? "" : (String) result.get(0).getValue();
    }

    /**
     * Retrieves the number of decimals used by the ERC-20 token.
     *
     * @return The number of decimals (default 18 if not present).
     * @throws Exception if the call to the blockchain fails.
     */
    public int decimals() throws Exception {
        // Prepare the 'decimals' function call (no arguments, returns Uint8)
        Function function = new Function(
            "decimals",
            Collections.emptyList(),
            Arrays.asList(new TypeReference<Uint8>() {})
        );

        // Encode the function for the eth_call
        String encodedFunction = org.web3j.abi.FunctionEncoder.encode(function);

        // Perform a call to the contract (read-only, no sender required)
        EthCall response = web3j.ethCall(
            Transaction.createEthCallTransaction(null, contractAddress, encodedFunction),
            DefaultBlockParameterName.LATEST
        ).send();

        // Decode the returned value
        List<org.web3j.abi.datatypes.Type> result = org.web3j.abi.FunctionReturnDecoder.decode(
            response.getValue(),
            function.getOutputParameters()
        );

        // Return the decimals or 18 if not present (18 is the ERC-20 default)
        return result.isEmpty() ? 18 : ((BigInteger) result.get(0).getValue()).intValue();
    }

    /**
     * Retrieves the token balance of a specific address.
     *
     * @param owner The address to query the balance for.
     * @return The balance as a BigInteger (in the smallest unit, e.g., wei).
     * @throws Exception if the call to the blockchain fails.
     */
    public BigInteger balanceOf(String owner) throws Exception {
        // Prepare the 'balanceOf' function call (takes address, returns Uint256)
        Function function = new Function(
            "balanceOf",
            Arrays.asList(new Address(owner)),
            Arrays.asList(new TypeReference<Uint256>() {})
        );

        // Encode the function for the eth_call
        String encodedFunction = org.web3j.abi.FunctionEncoder.encode(function);

        // Perform a call to the contract (read-only, no sender required)
        EthCall response = web3j.ethCall(
            Transaction.createEthCallTransaction(null, contractAddress, encodedFunction),
            DefaultBlockParameterName.LATEST
        ).send();

        // Decode the returned value
        List<org.web3j.abi.datatypes.Type> result = org.web3j.abi.FunctionReturnDecoder.decode(
            response.getValue(),
            function.getOutputParameters()
        );

        // Return the balance or zero if not present
        return result.isEmpty() ? BigInteger.ZERO : (BigInteger) result.get(0).getValue();
    }

    /**
     * Retrieves the total supply of the ERC-20 token.
     *
     * @return The total supply as a BigInteger.
     * @throws Exception if the call to the blockchain fails.
     */
    public BigInteger totalSupply() throws Exception {
        // Prepare the 'totalSupply' function call (no arguments, returns Uint256)
        Function function = new Function(
            "totalSupply",
            Collections.emptyList(),
            Arrays.asList(new TypeReference<Uint256>() {})
        );

        // Encode the function for the eth_call
        String encodedFunction = org.web3j.abi.FunctionEncoder.encode(function);

        // Perform a call to the contract (read-only, no sender required)
        EthCall response = web3j.ethCall(
            Transaction.createEthCallTransaction(null, contractAddress, encodedFunction),
            DefaultBlockParameterName.LATEST
        ).send();

        // Decode the returned value
        List<org.web3j.abi.datatypes.Type> result = org.web3j.abi.FunctionReturnDecoder.decode(
            response.getValue(),
            function.getOutputParameters()
        );

        // Return the total supply or zero if not present
        return result.isEmpty() ? BigInteger.ZERO : (BigInteger) result.get(0).getValue();
    }

    /**
     * Encodes the data for a 'transfer' function call.
     * <p>
     * This method does not send a transaction, but returns the encoded data
     * that can be used to build a transaction for transferring tokens.
     * </p>
     *
     * @param to     The recipient's address.
     * @param amount The amount of tokens to transfer (in the smallest unit).
     * @return The ABI-encoded function data as a hex string.
     */
    public String encodeTransfer(String to, BigInteger amount) {
        // Prepare the 'transfer' function call (takes address and amount, no return)
        Function function = new Function(
            "transfer",
            Arrays.asList(new Address(to), new Uint256(amount)),
            Collections.emptyList()
        );

        // Return the ABI-encoded function data
        return org.web3j.abi.FunctionEncoder.encode(function);
    }

    /**
     * Retrieves the allowance for a spender on behalf of an owner.
     *
     * @param owner   The address of the token owner.
     * @param spender The address of the spender.
     * @return The remaining allowance as a BigInteger.
     * @throws Exception if the call to the blockchain fails.
     */
    public BigInteger allowance(String owner, String spender) throws Exception {
        // Prepare the 'allowance' function call (takes owner and spender addresses, returns Uint256)
        Function function = new Function(
            "allowance",
            Arrays.asList(new Address(owner), new Address(spender)),
            Arrays.asList(new TypeReference<Uint256>() {})
        );

        // Encode the function for the eth_call
        String encodedFunction = org.web3j.abi.FunctionEncoder.encode(function);

        // Perform a call to the contract (read-only, no sender required)
        EthCall response = web3j.ethCall(
            Transaction.createEthCallTransaction(null, contractAddress, encodedFunction),
            DefaultBlockParameterName.LATEST
        ).send();

        // Decode the returned value
        List<org.web3j.abi.datatypes.Type> result = org.web3j.abi.FunctionReturnDecoder.decode(
            response.getValue(),
            function.getOutputParameters()
        );

        // Return the allowance or zero if not present
        return result.isEmpty() ? BigInteger.ZERO : (BigInteger) result.get(0).getValue();
    }
}
