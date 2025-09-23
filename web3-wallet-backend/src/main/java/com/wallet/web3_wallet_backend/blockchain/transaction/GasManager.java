package com.wallet.web3_wallet_backend.blockchain.transaction;

import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages gas-related operations for Ethereum transactions including gas estimation,
 * gas price calculation for both legacy and EIP-1559 transactions, and nonce management.
 */
@Component
public class GasManager {
    
    private final Web3j web3j;
    
    /** Default gas limit for simple ETH transfers (21,000 gas units) */
    private static final BigInteger DEFAULT_GAS_LIMIT = BigInteger.valueOf(21000L);
    
    /** Conversion factor from Gwei to Wei (1 Gwei = 10^9 Wei) */
    private static final BigDecimal GWEI_TO_WEI = new BigDecimal("1000000000");
    
    /**
     * Constructs a GasManager with the provided Web3j instance.
     * 
     * @param web3j The Web3j instance for blockchain communication
     */
    public GasManager(Web3j web3j) {
        this.web3j = web3j;
    }
    
    /**
     * Estimates the gas limit required for a transaction.
     * Adds a 10% safety margin to the estimated gas to prevent transaction failures.
     * 
     * @param from The sender's address
     * @param to The recipient's address
     * @param value The amount of ETH to transfer in Wei
     * @param data The transaction data (contract call data or empty for simple transfers)
     * @return The estimated gas limit with safety margin, or default limit if estimation fails
     */
    public BigInteger estimateGasLimit(String from, String to, BigInteger value, String data) {
        try {
            Transaction transaction = Transaction.createFunctionCallTransaction(
                from, 
                null, 
                null, 
                null,
                to,
                value,
                data
            );
            
            var response = web3j.ethEstimateGas(transaction).send();
            if (response.hasError()) {
                return DEFAULT_GAS_LIMIT;
            }
            
            BigInteger estimated = response.getAmountUsed();
            // Add 10% safety margin to prevent transaction failures
            return estimated.multiply(BigInteger.valueOf(110)).divide(BigInteger.valueOf(100));
            
        } catch (Exception e) {
            return DEFAULT_GAS_LIMIT;
        }
    }
    
    /**
     * Retrieves gas prices for legacy transactions (pre-EIP-1559).
     * Provides slow, medium, and fast gas price options based on network conditions.
     * 
     * @return GasPrices object containing slow, medium, and fast gas prices
     * @throws IOException if the network request fails
     */
    public GasPrices getLegacyGasPrices() throws IOException {
        EthGasPrice ethGasPrice = web3j.ethGasPrice().send();
        BigInteger basePrice = ethGasPrice.getGasPrice();
        
        // Calculate gas prices with different multipliers for various speed preferences
        BigInteger slow = basePrice.multiply(BigInteger.valueOf(90)).divide(BigInteger.valueOf(100));  // 90% of base
        BigInteger medium = basePrice;  // 100% of base
        BigInteger fast = basePrice.multiply(BigInteger.valueOf(120)).divide(BigInteger.valueOf(100)); // 120% of base
        
        return new GasPrices(slow, medium, fast);
    }
    
    /**
     * Retrieves gas prices for EIP-1559 transactions.
     * Calculates both maxFeePerGas and maxPriorityFeePerGas based on recent network activity.
     * 
     * @return EIP1559GasPrices object containing max fee and priority fee options
     * @throws IOException if the network request fails
     * @throws UnsupportedOperationException if the network doesn't support EIP-1559
     */
    public EIP1559GasPrices getEIP1559GasPrices() throws IOException {
        EthBlock.Block block = web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false)
            .send()
            .getBlock();
        
        BigInteger baseFee = extractBaseFee(block);
        if (baseFee == null) {
            throw new UnsupportedOperationException("Network does not support EIP-1559");
        }
        
        // Analyze recent priority fees from the last 10 blocks
        List<BigInteger> priorityFees = getRecentPriorityFees(10);
        BigInteger medianPriorityFee = calculateMedian(priorityFees);
        
        // Calculate priority fees for different speed preferences
        BigInteger slowPriority = medianPriorityFee.multiply(BigInteger.valueOf(80)).divide(BigInteger.valueOf(100));   // 80% of median
        BigInteger mediumPriority = medianPriorityFee;  // 100% of median
        BigInteger fastPriority = medianPriorityFee.multiply(BigInteger.valueOf(150)).divide(BigInteger.valueOf(100)); // 150% of median
        
        // Calculate max fees (base fee + priority fee with different multipliers)
        BigInteger slowMaxFee = baseFee.multiply(BigInteger.valueOf(2)).add(slowPriority);    // 2x base + priority
        BigInteger mediumMaxFee = baseFee.multiply(BigInteger.valueOf(2)).add(mediumPriority); // 2x base + priority
        BigInteger fastMaxFee = baseFee.multiply(BigInteger.valueOf(3)).add(fastPriority);    // 3x base + priority
        
        return new EIP1559GasPrices(
            new GasPrices(slowMaxFee, mediumMaxFee, fastMaxFee),
            new GasPrices(slowPriority, mediumPriority, fastPriority)
        );
    }
    
    /**
     * Checks if the connected network supports EIP-1559 transactions.
     * 
     * @return true if EIP-1559 is supported, false otherwise
     */
    public boolean supportsEIP1559() {
        try {
            EthBlock.Block block = web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false)
                .send()
                .getBlock();
            return extractBaseFee(block) != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Retrieves the current nonce for the specified address.
     * Uses PENDING state to include transactions that are pending but not yet mined.
     * 
     * @param address The Ethereum address to get the nonce for
     * @return The current nonce value
     * @throws IOException if the network request fails
     */
    public BigInteger getCurrentNonce(String address) throws IOException {
        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
            address, 
            DefaultBlockParameterName.PENDING
        ).send();
        return ethGetTransactionCount.getTransactionCount();
    }
    
    /**
     * Extracts the base fee from a block using reflection.
     * This is necessary because the base fee field may not be available in all Web3j versions.
     * 
     * @param block The block to extract the base fee from
     * @return The base fee in Wei, or null if not available
     */
    private BigInteger extractBaseFee(EthBlock.Block block) {
        try {
            return (BigInteger) block.getClass().getMethod("getBaseFeePerGas").invoke(block);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Analyzes recent blocks to collect priority fees from EIP-1559 transactions.
     * This helps determine appropriate priority fee recommendations.
     * 
     * @param blockCount The number of recent blocks to analyze
     * @return List of priority fees found in recent transactions
     */
    private List<BigInteger> getRecentPriorityFees(int blockCount) {
        List<BigInteger> fees = new ArrayList<>();
        try {
            BigInteger latestBlockNumber = web3j.ethBlockNumber().send().getBlockNumber();
            
            // Analyze the specified number of recent blocks
            for (int i = 0; i < blockCount; i++) {
                BigInteger blockNumber = latestBlockNumber.subtract(BigInteger.valueOf(i));
                EthBlock.Block block = web3j.ethGetBlockByNumber(
                    org.web3j.protocol.core.DefaultBlockParameter.valueOf(blockNumber),
                    true
                ).send().getBlock();
                
                if (block != null && block.getTransactions() != null) {
                    BigInteger baseFee = extractBaseFee(block);
                    if (baseFee != null) {
                        // Extract priority fees from EIP-1559 transactions
                        block.getTransactions().forEach(txResult -> {
                            if (txResult instanceof EthBlock.TransactionObject) {
                                EthBlock.TransactionObject tx = (EthBlock.TransactionObject) txResult;
                                BigInteger gasPrice = tx.getGasPrice();
                                // Priority fee = gas price - base fee (for EIP-1559 transactions)
                                // Calculate priority fee for EIP-1559 transactions
                                // Priority fee = gas price - base fee (for EIP-1559 transactions)
                                if (gasPrice != null && gasPrice.compareTo(baseFee) > 0) {
                                    BigInteger priorityFee = gasPrice.subtract(baseFee);
                                    fees.add(priorityFee);
                                }
                            }
                        });
                    }
                }
            }
        } catch (Exception e) {
            // Log error but continue with default fallback
            // Could add proper logging here in production
        }
        
        // Fallback to default priority fee if no recent fees found
        if (fees.isEmpty()) {
            // Default to 1 Gwei (1,000,000,000 Wei) as minimum priority fee
            fees.add(BigInteger.valueOf(1_000_000_000L));
        }
        
        return fees;
    }
    
    /**
     * Calculates the median value from a list of BigInteger values.
     * Used for determining typical gas prices from historical data.
     * 
     * @param values List of values to calculate median from
     * @return Median value, or BigInteger.ZERO if list is empty
     */
    private BigInteger calculateMedian(List<BigInteger> values) {
        if (values.isEmpty()) {
            return BigInteger.ZERO;
        }
        
        // Create sorted copy to avoid modifying original list
        List<BigInteger> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        
        int size = sorted.size();
        // Calculate median: average of two middle values for even count, middle value for odd count
        if (size % 2 == 0) {
            // Even number of elements: average of two middle values
            BigInteger lowerMiddle = sorted.get(size / 2 - 1);
            BigInteger upperMiddle = sorted.get(size / 2);
            return lowerMiddle.add(upperMiddle).divide(BigInteger.valueOf(2));
        } else {
            // Odd number of elements: middle value
            return sorted.get(size / 2);
        }
    }
    
    /**
     * Converts Wei to Gwei for display purposes.
     * 
     * @param wei The value in Wei to convert
     * @return The value in Gwei, rounded to 2 decimal places
     */
    public static BigDecimal weiToGwei(BigInteger wei) {
        return new BigDecimal(wei).divide(GWEI_TO_WEI, 2, RoundingMode.HALF_UP);
    }
    
    /**
     * Converts Gwei to Wei for blockchain calculations.
     * 
     * @param gwei The value in Gwei to convert
     * @return The value in Wei
     */
    public static BigInteger gweiToWei(BigDecimal gwei) {
        return gwei.multiply(GWEI_TO_WEI).toBigInteger();
    }
    
    /**
     * Represents gas prices for different transaction speeds (slow, medium, fast).
     * Used for legacy gas price recommendations.
     */
    public static class GasPrices {
        /** Gas price for slow transactions (lower priority) */
        public final BigInteger slow;
        /** Gas price for medium transactions (balanced priority) */
        public final BigInteger medium;
        /** Gas price for fast transactions (high priority) */
        public final BigInteger fast;
        
        /**
         * Creates a new GasPrices instance with the specified values.
         * 
         * @param slow Gas price for slow transactions
         * @param medium Gas price for medium transactions
         * @param fast Gas price for fast transactions
         */
        public GasPrices(BigInteger slow, BigInteger medium, BigInteger fast) {
            this.slow = slow;
            this.medium = medium;
            this.fast = fast;
        }
    }
    
    /**
     * Represents EIP-1559 gas prices with separate max fee and priority fee recommendations.
     * Used for EIP-1559 transaction fee calculations.
     */
    public static class EIP1559GasPrices {
        /** Maximum fee per gas for different transaction speeds */
        public final GasPrices maxFeePerGas;
        /** Maximum priority fee per gas for different transaction speeds */
        public final GasPrices maxPriorityFeePerGas;
        
        /**
         * Creates a new EIP1559GasPrices instance with the specified fee structures.
         * 
         * @param maxFeePerGas Maximum fee per gas for different speeds
         * @param maxPriorityFeePerGas Maximum priority fee per gas for different speeds
         */
        public EIP1559GasPrices(GasPrices maxFeePerGas, GasPrices maxPriorityFeePerGas) {
            this.maxFeePerGas = maxFeePerGas;
            this.maxPriorityFeePerGas = maxPriorityFeePerGas;
        }
    }
}