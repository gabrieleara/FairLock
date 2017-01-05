/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fairlock;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author Gabriele Ara
 */
public class SingleResourceManagerLock implements SingleResourceManager {
    private final Lock lock;
    private final Condition conditionA;
    private final Condition conditionB;
    
    private ResourceState state;
    
    private Thread owner;
    
    private final Queue<Thread> conditionAQueue;
    private final Queue<Thread> conditionBQueue;
    
    public SingleResourceManagerLock() {
        lock = new ReentrantLock(true);
        conditionA = lock.newCondition();
        conditionB = lock.newCondition();
        state = ResourceState.FREE;
        
        conditionAQueue = new LinkedList<>();
        conditionBQueue = new LinkedList<>();
    }
    
    @Override
    public ResourceState getState() {
        try {
            lock.lock();
            return state;
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public boolean isFree() {
        return getState() == ResourceState.FREE;
    }
    
    private void enqueue(Condition c, Queue<Thread> q) {
        q.add(Thread.currentThread());
        
        do {
            try { c.await(); } catch (InterruptedException ex) { }
        } while(owner != Thread.currentThread());
    }
    
    @Override
    public void request(PriorityClass prio) {
        try {
            lock.lock();
            
            if(state == ResourceState.FREE) {
                state = ResourceState.BUSY;
                owner = Thread.currentThread();
                return;
            }
            
            switch(prio) {
                case TYPE_A:
                    enqueue(conditionA, conditionAQueue);
                    break;
                case TYPE_B:
                    enqueue(conditionB, conditionBQueue);
                    break;
            }
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public void release() {
        try {
            lock.lock();
            
            if(conditionBQueue.size() > 0) {
                owner = conditionBQueue.poll();
                conditionB.signal();
            } else if(conditionAQueue.size() > 0) {
                owner = conditionAQueue.poll();
                conditionA.signal();
            } else {
                owner = null;
                state = ResourceState.FREE;
            }
            
        } finally {
            lock.unlock();
        }
    }
}
