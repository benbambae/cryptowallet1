package com.wallet.web3_wallet_backend.service;

import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class NonceManager {
    
    private final Web3j web3j;
    private final Map<String, NonceTracker> nonceTrackers = new ConcurrentHashMap<>();
    
    public NonceManager(Web3j web3j) {
        this.web3j = web3j;
    }
    
    public synchronized BigInteger getNextNonce(String address) throws IOException {
        String normalizedAddress = address.toLowerCase();
        NonceTracker tracker = nonceTrackers.computeIfAbsent(normalizedAddress, k -> new NonceTracker());
        
        BigInteger networkNonce = getNetworkNonce(address);
        BigInteger localNonce = tracker.getNextNonce();
        
        if (localNonce == null || networkNonce.compareTo(localNonce) > 0) {
            tracker.reset(networkNonce);
            return networkNonce;
        }
        
        return localNonce;
    }
    
    public void confirmTransaction(String address, BigInteger nonce) {
        String normalizedAddress = address.toLowerCase();
        NonceTracker tracker = nonceTrackers.get(normalizedAddress);
        if (tracker != null) {
            tracker.confirm(nonce);
        }
    }
    
    public void releaseNonce(String address, BigInteger nonce) {
        String normalizedAddress = address.toLowerCase();
        NonceTracker tracker = nonceTrackers.get(normalizedAddress);
        if (tracker != null) {
            tracker.release(nonce);
        }
    }
    
    public void resetNonce(String address) throws IOException {
        String normalizedAddress = address.toLowerCase();
        BigInteger networkNonce = getNetworkNonce(address);
        nonceTrackers.put(normalizedAddress, new NonceTracker());
        nonceTrackers.get(normalizedAddress).reset(networkNonce);
    }
    
    public BigInteger getPendingNonce(String address) throws IOException {
        return getNetworkNonce(address);
    }
    
    private BigInteger getNetworkNonce(String address) throws IOException {
        return web3j.ethGetTransactionCount(address, DefaultBlockParameterName.PENDING)
            .send()
            .getTransactionCount();
    }
    
    public void clearCache(String address) {
        nonceTrackers.remove(address.toLowerCase());
    }
    
    public void clearAllCaches() {
        nonceTrackers.clear();
    }
    
    private static class NonceTracker {
        private final AtomicReference<BigInteger> currentNonce = new AtomicReference<>();
        private final Map<BigInteger, NonceStatus> pendingNonces = new ConcurrentHashMap<>();
        
        public synchronized BigInteger getNextNonce() {
            BigInteger nonce = currentNonce.get();
            if (nonce == null) {
                return null;
            }
            
            while (pendingNonces.containsKey(nonce)) {
                NonceStatus status = pendingNonces.get(nonce);
                if (status == NonceStatus.RELEASED) {
                    pendingNonces.remove(nonce);
                    break;
                }
                nonce = nonce.add(BigInteger.ONE);
            }
            
            pendingNonces.put(nonce, NonceStatus.PENDING);
            currentNonce.set(nonce.add(BigInteger.ONE));
            return nonce;
        }
        
        public synchronized void reset(BigInteger networkNonce) {
            currentNonce.set(networkNonce);
            pendingNonces.clear();
        }
        
        public synchronized void confirm(BigInteger nonce) {
            pendingNonces.put(nonce, NonceStatus.CONFIRMED);
            cleanupConfirmedNonces();
        }
        
        public synchronized void release(BigInteger nonce) {
            pendingNonces.put(nonce, NonceStatus.RELEASED);
        }
        
        private void cleanupConfirmedNonces() {
            BigInteger baseNonce = currentNonce.get();
            if (baseNonce == null) return;
            
            BigInteger lowestPending = baseNonce;
            for (Map.Entry<BigInteger, NonceStatus> entry : pendingNonces.entrySet()) {
                if (entry.getValue() != NonceStatus.CONFIRMED) {
                    if (entry.getKey().compareTo(lowestPending) < 0) {
                        lowestPending = entry.getKey();
                    }
                }
            }
            
            final BigInteger threshold = lowestPending;
            pendingNonces.entrySet().removeIf(entry -> 
                entry.getValue() == NonceStatus.CONFIRMED && 
                entry.getKey().compareTo(threshold) < 0
            );
        }
        
        private enum NonceStatus {
            PENDING,
            CONFIRMED,
            RELEASED
        }
    }
}