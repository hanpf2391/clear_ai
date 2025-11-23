package com.hanpf.clearai.react.state;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.List;
import java.util.ArrayList;

/**
 * 状态管理器 - 管理对话状态的生命周期
 * 支持多会话并发，提供线程安全的状态访问
 */
public class StateManager {

    // 当前活跃的对话状态
    private ConversationState currentState;

    // 历史对话状态存储（支持多会话）
    private final ConcurrentHashMap<String, ConversationState> stateHistory;

    // 读写锁，保证线程安全
    private final ReadWriteLock lock;

    public StateManager() {
        this.stateHistory = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
        this.currentState = new ConversationState();
    }

    /**
     * 获取当前对话状态
     */
    public ConversationState getCurrentState() {
        lock.readLock().lock();
        try {
            return currentState;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 更新当前对话状态
     */
    public void updateState(ConversationState newState) {
        lock.writeLock().lock();
        try {
            // 保存旧状态到历史
            if (currentState != null) {
                stateHistory.put(currentState.getConversationId(), currentState);
            }

            // 更新当前状态
            this.currentState = newState;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 重置当前对话状态
     */
    public void reset() {
        lock.writeLock().lock();
        try {
            if (currentState != null) {
                stateHistory.put(currentState.getConversationId(), currentState);
            }
            this.currentState = new ConversationState();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 根据ID获取历史对话状态
     */
    public ConversationState getState(String conversationId) {
        lock.readLock().lock();
        try {
            return stateHistory.get(conversationId);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 切换到指定的历史对话状态
     */
    public boolean switchToState(String conversationId) {
        lock.writeLock().lock();
        try {
            ConversationState targetState = stateHistory.get(conversationId);
            if (targetState != null) {
                if (currentState != null) {
                    stateHistory.put(currentState.getConversationId(), currentState);
                }
                this.currentState = targetState;
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 清理过期的对话状态
     */
    public void cleanupExpiredStates(int maxHistorySize) {
        lock.writeLock().lock();
        try {
            if (stateHistory.size() > maxHistorySize) {
                // 简单的FIFO清理策略
                int toRemove = stateHistory.size() - maxHistorySize;
                List<String> keysToRemove = new ArrayList<>();

                // 收集需要删除的键
                for (String key : stateHistory.keySet()) {
                    if (toRemove <= 0) break;
                    keysToRemove.add(key);
                    toRemove--;
                }

                // 删除旧状态
                for (String key : keysToRemove) {
                    stateHistory.remove(key);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取活跃对话数量
     */
    public int getActiveConversationCount() {
        lock.readLock().lock();
        try {
            return stateHistory.size() + 1; // +1 for current state
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取所有对话ID
     */
    public String[] getAllConversationIds() {
        lock.readLock().lock();
        try {
            String[] ids = new String[stateHistory.size() + 1];
            int i = 0;
            for (String id : stateHistory.keySet()) {
                ids[i++] = id;
            }
            if (currentState != null) {
                ids[i] = currentState.getConversationId();
            }
            return ids;
        } finally {
            lock.readLock().unlock();
        }
    }
}