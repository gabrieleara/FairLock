/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fairlock;

import java.util.LinkedList;
import java.util.Queue;

/**
 *
 * @author Gabriele
 */
public class FairLock {
    
    private enum LockState {
        UNLOCKED,
        LOCKED,
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    private static class BinarySemaphore {
        boolean hasSignal;
        
        BinarySemaphore() {
            hasSignal = false;
        }
        
        public synchronized void await(Queue q) throws InterruptedException {
            while(!hasSignal) {
                try {
                    wait();
                } catch(InterruptedException e) {
                    if(q != null)
                        q.remove(this);
                    hasSignal = false;
                    throw e;
                }
            }
            
            assert hasSignal;
            
            hasSignal = false;
        }
        
        public synchronized void signal() {
            assert !hasSignal;
            
            hasSignal = true;
            notify();
        }
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    public class Condition {
        private final Queue<BinarySemaphore> conditionQueue;
        
        Condition() {
            conditionQueue = new LinkedList<>();
        }
        
        public void await() throws InterruptedException {
            BinarySemaphore semaphore = new BinarySemaphore();
            
            synchronized(this) {
                conditionQueue.add(semaphore);
            }
            
            FairLock.this.unlock();
            
            semaphore.await(conditionQueue);
            
            synchronized(this) {
                assert conditionQueue.peek() == semaphore;
                
                conditionQueue.remove(semaphore);
            }
        }
        
        /*
        NOTICE: This method assumes that current thread owns lock on the 
        linked FairLock;
        */
        public void signal() throws InterruptedException {
            BinarySemaphore semaphore = new BinarySemaphore();
            
            BinarySemaphore awakeningSemaphore;
            
            synchronized(this) {
                if(conditionQueue.isEmpty())
                    return;
                
                awakeningSemaphore = conditionQueue.peek();
            }
            
            synchronized(FairLock.this) {
                assert isLocked();
                
                urgentQueue.add(semaphore);
            }
            
            awakeningSemaphore.signal();
            
            semaphore.await(urgentQueue);
            
            synchronized(FairLock.this) {
                assert isLocked();
                assert urgentQueue.peek() == semaphore;
                
                urgentQueue.remove(semaphore);
            }
        }
        
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    private final Queue<BinarySemaphore> entryQueue;
    private final Queue<BinarySemaphore> urgentQueue;
    
    LockState state;
    Condition currentCondition;
        
    public FairLock() {
        entryQueue = new LinkedList<>();
        urgentQueue = new LinkedList<>();
        state = LockState.UNLOCKED;
        currentCondition = null;
    }
    
    // NOTICE: urgentQueue cannot contain something AND state be UNLOCKED at
    // the same time, never.
    private synchronized boolean canILock(BinarySemaphore sem) {
        assert isLocked() || urgentQueue.isEmpty();
        
        assert !entryQueue.isEmpty();
        
        return isUnlocked() && sem == entryQueue.peek();
    }
    
    
    public synchronized boolean isLocked() {
        return state == LockState.LOCKED;
    }
    
    public synchronized boolean isUnlocked() {
        return state == LockState.UNLOCKED;
    }
    
    
    
    
    // NOTICE: this method assumes that current Thread DOES NOT own this lock,
    // otherwise it will end up this being a deadlock.
    public void lock() throws InterruptedException {
        BinarySemaphore semaphore = new BinarySemaphore();
        
        synchronized(this) {
            entryQueue.add(semaphore);
            
            if(canILock(semaphore)) {
                entryQueue.remove(semaphore);
                state = LockState.LOCKED;
                return;
            }
        }
        
        semaphore.await(entryQueue);
        
        synchronized(this) {
            assert isUnlocked();
            assert entryQueue.peek() == semaphore;
            
            entryQueue.remove(semaphore);
            state = LockState.LOCKED;
        }
    }
    
    // NOTICE: this method assumes that current Thread owns this lock, otherwise
    // it has an unpredictable behaviour.
    public synchronized void unlock() {
        /* NOTICE: this check isn't enought of course. */
        assert isLocked();
        
        if(!urgentQueue.isEmpty()) {
            urgentQueue.peek().signal();
            return;
        }
        
        state = LockState.UNLOCKED;
        
        if(!entryQueue.isEmpty()) {
            entryQueue.peek().signal();
        }
    }
    
    public Condition newCondition() {
        return this.new Condition();
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
    }
    
}
