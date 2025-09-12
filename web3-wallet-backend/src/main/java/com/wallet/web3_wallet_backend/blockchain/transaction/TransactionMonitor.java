package com.wallet.web3_wallet_backend.blockchain.transaction;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

@Component
public class TransactionMonitor {
    
    private final Web3j web3j;
    private final Map<String, MonitoringTask> activeTasks = new ConcurrentHashMap<>();
    
    private static final int DEFAULT_CONFIRMATION_BLOCKS = 12;
    private static final long POLL_INTERVAL_MS = 3000;
    private static final long MAX_WAIT_TIME_MS = 600000;
    
    public TransactionMonitor(Web3j web3j) {
        this.web3j = web3j;
    }
    
    @Async
    public CompletableFuture<TransactionReceipt> waitForTransaction(String transactionHash) {
        return waitForTransaction(transactionHash, DEFAULT_CONFIRMATION_BLOCKS);
    }
    
    @Async
    public CompletableFuture<TransactionReceipt> waitForTransaction(String transactionHash, int confirmations) {
        CompletableFuture<TransactionReceipt> future = new CompletableFuture<>();
        
        MonitoringTask task = new MonitoringTask(transactionHash, confirmations, (receipt, confirmed) -> {
            if (confirmed) {
                future.complete(receipt);
            }
        });
        
        activeTasks.put(transactionHash, task);
        startMonitoring(task);
        
        future.orTimeout(MAX_WAIT_TIME_MS, TimeUnit.MILLISECONDS)
            .whenComplete((receipt, error) -> {
                activeTasks.remove(transactionHash);
            });
        
        return future;
    }
    
    public void monitorTransaction(String transactionHash, BiConsumer<TransactionReceipt, Integer> callback) {
        monitorTransaction(transactionHash, DEFAULT_CONFIRMATION_BLOCKS, callback);
    }
    
    public void monitorTransaction(String transactionHash, int requiredConfirmations, 
                                  BiConsumer<TransactionReceipt, Integer> callback) {
        MonitoringTask task = new MonitoringTask(transactionHash, requiredConfirmations, (receipt, confirmed) -> {
            BigInteger currentBlock = getCurrentBlockNumber();
            if (currentBlock != null && receipt.getBlockNumber() != null) {
                int confirmations = currentBlock.subtract(receipt.getBlockNumber()).intValue() + 1;
                callback.accept(receipt, confirmations);
                
                if (confirmed) {
                    activeTasks.remove(transactionHash);
                }
            }
        });
        
        activeTasks.put(transactionHash, task);
        startMonitoring(task);
    }
    
    private void startMonitoring(MonitoringTask task) {
        CompletableFuture.runAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            while (!task.isComplete() && (System.currentTimeMillis() - startTime) < MAX_WAIT_TIME_MS) {
                try {
                    Optional<TransactionReceipt> receiptOpt = getTransactionReceipt(task.transactionHash);
                    
                    if (receiptOpt.isPresent()) {
                        TransactionReceipt receipt = receiptOpt.get();
                        BigInteger currentBlock = getCurrentBlockNumber();
                        
                        if (currentBlock != null && receipt.getBlockNumber() != null) {
                            int confirmations = currentBlock.subtract(receipt.getBlockNumber()).intValue() + 1;
                            
                            if (confirmations >= task.requiredConfirmations) {
                                task.complete(receipt);
                                break;
                            } else {
                                task.updateConfirmations(confirmations);
                            }
                        }
                    }
                    
                    Thread.sleep(POLL_INTERVAL_MS);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                }
            }
            
            if (!task.isComplete()) {
                task.timeout();
            }
        });
    }
    
    private Optional<TransactionReceipt> getTransactionReceipt(String transactionHash) throws IOException {
        EthGetTransactionReceipt response = web3j.ethGetTransactionReceipt(transactionHash).send();
        return response.getTransactionReceipt();
    }
    
    private BigInteger getCurrentBlockNumber() {
        try {
            return web3j.ethBlockNumber().send().getBlockNumber();
        } catch (IOException e) {
            return null;
        }
    }
    
    public void stopMonitoring(String transactionHash) {
        MonitoringTask task = activeTasks.remove(transactionHash);
        if (task != null) {
            task.cancel();
        }
    }
    
    public void stopAllMonitoring() {
        activeTasks.values().forEach(MonitoringTask::cancel);
        activeTasks.clear();
    }
    
    public boolean isMonitoring(String transactionHash) {
        return activeTasks.containsKey(transactionHash);
    }
    
    public int getActiveMonitorCount() {
        return activeTasks.size();
    }
    
    private static class MonitoringTask {
        private final String transactionHash;
        private final int requiredConfirmations;
        private final BiConsumer<TransactionReceipt, Boolean> callback;
        private volatile boolean complete = false;
        private volatile boolean cancelled = false;
        private volatile int currentConfirmations = 0;
        
        public MonitoringTask(String transactionHash, int requiredConfirmations,
                            BiConsumer<TransactionReceipt, Boolean> callback) {
            this.transactionHash = transactionHash;
            this.requiredConfirmations = requiredConfirmations;
            this.callback = callback;
        }
        
        public void complete(TransactionReceipt receipt) {
            if (!cancelled && !complete) {
                complete = true;
                callback.accept(receipt, true);
            }
        }
        
        public void updateConfirmations(int confirmations) {
            this.currentConfirmations = confirmations;
        }
        
        public void timeout() {
            if (!cancelled && !complete) {
                complete = true;
                callback.accept(null, false);
            }
        }
        
        public void cancel() {
            cancelled = true;
            complete = true;
        }
        
        public boolean isComplete() {
            return complete || cancelled;
        }
    }
}