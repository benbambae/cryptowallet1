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

@Component
public class GasManager {
    
    private final Web3j web3j;
    private static final BigInteger DEFAULT_GAS_LIMIT = BigInteger.valueOf(21000L);
    private static final BigDecimal GWEI_TO_WEI = new BigDecimal("1000000000");
    
    public GasManager(Web3j web3j) {
        this.web3j = web3j;
    }
    
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
            return estimated.multiply(BigInteger.valueOf(110)).divide(BigInteger.valueOf(100));
            
        } catch (Exception e) {
            return DEFAULT_GAS_LIMIT;
        }
    }
    
    public GasPrices getLegacyGasPrices() throws IOException {
        EthGasPrice ethGasPrice = web3j.ethGasPrice().send();
        BigInteger basePrice = ethGasPrice.getGasPrice();
        
        BigInteger slow = basePrice.multiply(BigInteger.valueOf(90)).divide(BigInteger.valueOf(100));
        BigInteger medium = basePrice;
        BigInteger fast = basePrice.multiply(BigInteger.valueOf(120)).divide(BigInteger.valueOf(100));
        
        return new GasPrices(slow, medium, fast);
    }
    
    public EIP1559GasPrices getEIP1559GasPrices() throws IOException {
        EthBlock.Block block = web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false)
            .send()
            .getBlock();
        
        BigInteger baseFee = extractBaseFee(block);
        if (baseFee == null) {
            throw new UnsupportedOperationException("Network does not support EIP-1559");
        }
        
        List<BigInteger> priorityFees = getRecentPriorityFees(10);
        BigInteger medianPriorityFee = calculateMedian(priorityFees);
        
        BigInteger slowPriority = medianPriorityFee.multiply(BigInteger.valueOf(80)).divide(BigInteger.valueOf(100));
        BigInteger mediumPriority = medianPriorityFee;
        BigInteger fastPriority = medianPriorityFee.multiply(BigInteger.valueOf(150)).divide(BigInteger.valueOf(100));
        
        BigInteger slowMaxFee = baseFee.multiply(BigInteger.valueOf(2)).add(slowPriority);
        BigInteger mediumMaxFee = baseFee.multiply(BigInteger.valueOf(2)).add(mediumPriority);
        BigInteger fastMaxFee = baseFee.multiply(BigInteger.valueOf(3)).add(fastPriority);
        
        return new EIP1559GasPrices(
            new GasPrices(slowMaxFee, mediumMaxFee, fastMaxFee),
            new GasPrices(slowPriority, mediumPriority, fastPriority)
        );
    }
    
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
    
    public BigInteger getCurrentNonce(String address) throws IOException {
        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
            address, 
            DefaultBlockParameterName.PENDING
        ).send();
        return ethGetTransactionCount.getTransactionCount();
    }
    
    private BigInteger extractBaseFee(EthBlock.Block block) {
        try {
            return (BigInteger) block.getClass().getMethod("getBaseFeePerGas").invoke(block);
        } catch (Exception e) {
            return null;
        }
    }
    
    private List<BigInteger> getRecentPriorityFees(int blockCount) {
        List<BigInteger> fees = new ArrayList<>();
        try {
            BigInteger latestBlockNumber = web3j.ethBlockNumber().send().getBlockNumber();
            
            for (int i = 0; i < blockCount; i++) {
                BigInteger blockNumber = latestBlockNumber.subtract(BigInteger.valueOf(i));
                EthBlock.Block block = web3j.ethGetBlockByNumber(
                    org.web3j.protocol.core.DefaultBlockParameter.valueOf(blockNumber),
                    true
                ).send().getBlock();
                
                if (block != null && block.getTransactions() != null) {
                    BigInteger baseFee = extractBaseFee(block);
                    if (baseFee != null) {
                        block.getTransactions().forEach(txResult -> {
                            if (txResult instanceof EthBlock.TransactionObject) {
                                EthBlock.TransactionObject tx = (EthBlock.TransactionObject) txResult;
                                BigInteger gasPrice = tx.getGasPrice();
                                if (gasPrice != null && gasPrice.compareTo(baseFee) > 0) {
                                    fees.add(gasPrice.subtract(baseFee));
                                }
                            }
                        });
                    }
                }
            }
        } catch (Exception e) {
        }
        
        if (fees.isEmpty()) {
            fees.add(BigInteger.valueOf(1_000_000_000L));
        }
        
        return fees;
    }
    
    private BigInteger calculateMedian(List<BigInteger> values) {
        if (values.isEmpty()) {
            return BigInteger.ZERO;
        }
        
        List<BigInteger> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        
        int size = sorted.size();
        if (size % 2 == 0) {
            return sorted.get(size / 2 - 1).add(sorted.get(size / 2)).divide(BigInteger.valueOf(2));
        } else {
            return sorted.get(size / 2);
        }
    }
    
    public static BigDecimal weiToGwei(BigInteger wei) {
        return new BigDecimal(wei).divide(GWEI_TO_WEI, 2, RoundingMode.HALF_UP);
    }
    
    public static BigInteger gweiToWei(BigDecimal gwei) {
        return gwei.multiply(GWEI_TO_WEI).toBigInteger();
    }
    
    public static class GasPrices {
        public final BigInteger slow;
        public final BigInteger medium;
        public final BigInteger fast;
        
        public GasPrices(BigInteger slow, BigInteger medium, BigInteger fast) {
            this.slow = slow;
            this.medium = medium;
            this.fast = fast;
        }
    }
    
    public static class EIP1559GasPrices {
        public final GasPrices maxFeePerGas;
        public final GasPrices maxPriorityFeePerGas;
        
        public EIP1559GasPrices(GasPrices maxFeePerGas, GasPrices maxPriorityFeePerGas) {
            this.maxFeePerGas = maxFeePerGas;
            this.maxPriorityFeePerGas = maxPriorityFeePerGas;
        }
    }
}