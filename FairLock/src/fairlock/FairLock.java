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
    
    // TODO: with a real semaphore, I don't even need states for WAITING_ETC.
    private enum LockState {
        UNLOCKED,
        LOCKED,
        /*WAITING_URGENT_AWAKENING,
        WAITING_CONDITION_AWAKENING,*/
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
        
        // NOTICE: conditionQueue can't be empty
        /*
        private boolean checkConditionContinue(BinarySemaphore sem) {
            synchronized(FairLock.this) {
                synchronized(this) {
                    assert !conditionQueue.isEmpty();
                    
                    return sem == conditionQueue.peek() && state == LockState.WAITING_CONDITION_AWAKENING && currentCondition == this;
                }
            }
        }
        */
        
        public void await() throws InterruptedException {
            BinarySemaphore semaphore = new BinarySemaphore();
            
            synchronized(this) {
                conditionQueue.add(semaphore);
            }
            
            FairLock.this.unlock();
            
            /*if(!checkConditionContinue(semaphore)) {*/
            semaphore.await(conditionQueue);
            /*}*/
            
            synchronized(this) {
                assert conditionQueue.peek() == semaphore;
                conditionQueue.remove(semaphore);
            }
                
            /*
            synchronized(FairLock.this) {
                synchronized(this) {
                    conditionQueue.remove(semaphore);
                    //state = LockState.LOCKED;
                    //currentCondition = null;
                }
            }
            */
        }
        
        /*
        NOTICE: This method assumes that current thread owns lock on the 
        linked FairLock;
        */
        public void signal() throws InterruptedException {
            BinarySemaphore semaphore = new BinarySemaphore();
            
            /* Ticket del thread da risvegliare */
            BinarySemaphore awSemaphore = null;
            
            synchronized(this) {
                /* Se non ci sono thread bloccati sulla condition, la signal
                    non ha effetto.
                */
                if(conditionQueue.isEmpty())
                    return;
                
                awSemaphore = conditionQueue.peek();
            }
            
            /* Aggiunge il thread corrente alla urgent queue.
                Segnala inoltre che il (primo) processo bloccato sulla
                condition deve essere risvegliato.
            */
            synchronized(FairLock.this) {
                urgentQueue.add(semaphore);
                // state = LockState.WAITING_CONDITION_AWAKENING;
                // currentCondition = this;
            }
            
            /* Segnala al thread bloccato di proseguire.
            */
            awSemaphore.signal();
            
            // boolean canIContinue;
            
            semaphore.await(urgentQueue);
            
            /*canIContinue = checkUrgentContinue(semaphore);
            
            while (!canIContinue){
                semaphore.await(urgentQueue);
                
                canIContinue = checkUrgentContinue(semaphore);
            }*/
            
            synchronized(FairLock.this) {
                urgentQueue.remove(semaphore);
                //state = LockState.LOCKED;
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
    // the same time, never
    private synchronized boolean checkEntryContinue(BinarySemaphore sem) {
        assert (state == LockState.UNLOCKED && urgentQueue.isEmpty())
                || state != LockState.UNLOCKED;
        
        assert !entryQueue.isEmpty();
        
        return state == LockState.UNLOCKED && sem == entryQueue.peek(); /* && urgentQueue.isEmpty() */
    }
    
    /*private synchronized boolean checkUrgentContinue(BinarySemaphore sem) {
        assert !urgentQueue.isEmpty();
        
        return state == LockState.WAITING_URGENT_AWAKENING && urgentQueue.peek() == sem;
    }*/
    
    
    
    
    // NOTICE: this method assumes that current Thread DOES NOT own this lock,
    // otherwise it will end up this being a deadlock.
    public void lock() throws InterruptedException {
        BinarySemaphore semaphore = new BinarySemaphore();
        
        synchronized(this) {
            entryQueue.add(semaphore);
            
            if(checkEntryContinue(semaphore)) {
                entryQueue.remove(semaphore);
                state = LockState.LOCKED;
                return;
            }
        }
        
        semaphore.await(entryQueue);
        
        synchronized(this) {
            assert entryQueue.peek() == semaphore;
            assert state == LockState.UNLOCKED;
            
            entryQueue.remove(semaphore);
            state = LockState.LOCKED;
        }
    }
    
    // NOTICE: this method assumes that current Thread owns this lock, otherwise
    // it has an unpredictable behaviour.
    public synchronized void unlock() {
        /* NOTICE: this check isn't enought of course. */
        assert state == LockState.LOCKED;
        
        if(!urgentQueue.isEmpty()) {
            //state = LockState.WAITING_URGENT_AWAKENING;
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
        // TODO code application logic here
    }
    
}
