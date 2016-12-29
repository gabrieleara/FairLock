/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fairlock;

import java.util.PriorityQueue;

/**
 *
 * @author Gabriele
 */
public class FairLock {
    private enum LockState {
        UNLOCKED,
        LOCKED,
        WAITING_URGENT_AWAKENING,
        WAITING_CONDITION_AWAKENING,
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    /* @TODO: probably, this class should be enough to check if the condition
        is true, i.e. I don't even need the while in the waits!
    */
    private static class BinaryPrivateSemaphore implements Comparable<BinaryPrivateSemaphore>{
        long ticket;
        boolean hasSignal;
        
        BinaryPrivateSemaphore() {
            ticket = System.nanoTime();
            hasSignal = false;
        }

        @Override
        public int compareTo(BinaryPrivateSemaphore o) {
            return (ticket < o.ticket) ? -1 : ((ticket == ticket) ? 0 : 1);
        }
        
        public synchronized void await(PriorityQueue q) throws InterruptedException {
            if(hasSignal) {
                hasSignal = false;
                return;
            }
            
            try {
                wait();
            } catch(InterruptedException e) {
                q.remove(this);
                throw e;
            } finally {
                hasSignal = false;
            }
        }
        
        public synchronized void signal() {
            hasSignal = true;
            notify();
        }
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    public class Condition {
        private final PriorityQueue<BinaryPrivateSemaphore> conditionQueue;
        
        Condition() {
            conditionQueue = new PriorityQueue<>(0);    
        }
        
        // NOTICE: conditionQueue can't be empty
        private boolean checkConditionContinue(BinaryPrivateSemaphore sem) {
            synchronized(FairLock.this) {
                synchronized(this) {
                    assert !conditionQueue.isEmpty();
                    
                    return sem == conditionQueue.element() && state == LockState.WAITING_CONDITION_AWAKENING && currentCondition == this;
                }
            }
        }
        
        public void await() throws InterruptedException {
            BinaryPrivateSemaphore semaphore = new BinaryPrivateSemaphore();
            boolean canIContinue;
            
            synchronized(this) {
                conditionQueue.add(semaphore);
            }
            
            FairLock.this.unlock();
            
            canIContinue = checkConditionContinue(semaphore);
            
            while(!canIContinue) {
                semaphore.await(conditionQueue);
                
                canIContinue = checkConditionContinue(semaphore);
            }
                
            synchronized(FairLock.this) {
                synchronized(this) {
                    conditionQueue.remove(semaphore);
                    state = LockState.LOCKED;
                    currentCondition = null;
                }
            }
        }
        
        /*
        NOTICE: This method assumes that current thread owns lock on the 
        linked FairLock;
        */
        public void signal() throws InterruptedException {
            BinaryPrivateSemaphore semaphore = new BinaryPrivateSemaphore();
            
            /* Ticket del thread da risvegliare */
            BinaryPrivateSemaphore awTicket = null;
            
            synchronized(this) {
                /* Se non ci sono thread bloccati sulla condition, la signal
                    non ha effetto.
                */
                if(conditionQueue.isEmpty())
                    return;
                
                awTicket = conditionQueue.element();
            }
            
            /* Aggiunge il thread corrente alla urgent queue.
                Segnala inoltre che il (primo) processo bloccato sulla
                condition deve essere risvegliato.
            */
            synchronized(FairLock.this) {
                urgentQueue.add(semaphore);
                state = LockState.WAITING_CONDITION_AWAKENING;
                currentCondition = this;
            }
            
            /* Segnala al thread bloccato di proseguire.
            */
            awTicket.signal();
            
            boolean canIContinue;
            
            canIContinue = checkUrgentContinue(semaphore);
            
            while (!canIContinue){
                semaphore.await(urgentQueue);
                
                canIContinue = checkUrgentContinue(semaphore);
            }
            
            synchronized(FairLock.this) {
                synchronized(this) {
                    urgentQueue.remove(semaphore);
                    state = LockState.LOCKED;
                }
            }
        }
        
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    private final PriorityQueue<BinaryPrivateSemaphore> entryQueue;
    private final PriorityQueue<BinaryPrivateSemaphore> urgentQueue;
    
    LockState state;
    Condition currentCondition;
        
    public FairLock() {
        entryQueue = new PriorityQueue<>(0);
        urgentQueue = new PriorityQueue<>(0);
        state = LockState.UNLOCKED;
        currentCondition = null;
    }
    
    // NOTICE: urgentQueue cannot contain something AND state be UNLOCKED at
    // the same time, never
    private synchronized boolean checkEntryContinue(BinaryPrivateSemaphore sem) {
        assert (state == LockState.UNLOCKED && urgentQueue.isEmpty())
                || state != LockState.LOCKED;
        
        assert !entryQueue.isEmpty();
        
        return state == LockState.UNLOCKED && sem == entryQueue.element(); /* && urgentQueue.isEmpty() */
    }
    
    private synchronized boolean checkUrgentContinue(BinaryPrivateSemaphore sem) {
        assert !urgentQueue.isEmpty();
        
        return state == LockState.WAITING_URGENT_AWAKENING && urgentQueue.element() == sem;
    }
    
    // NOTICE: this method assumes that current Thread DOES NOT own this lock,
    // otherwise it will end up this being a deadlock.
    public void lock() throws InterruptedException {
        BinaryPrivateSemaphore semaphore = new BinaryPrivateSemaphore();
        boolean canIContinue;
        
        synchronized(this) {
            entryQueue.add(semaphore);
            
            canIContinue = checkEntryContinue(semaphore);
        }
        
        while(!canIContinue) {
            semaphore.await(entryQueue);
            
            canIContinue = checkEntryContinue(semaphore);
        }
        
        synchronized(this) {
            entryQueue.remove(semaphore);
            state = LockState.LOCKED;
        }
    }
    
    // NOTICE: this method assumes that current Thread owns this lock, otherwise
    // it has an unpredictable behaviour.
    public synchronized void unlock() {
        if(!urgentQueue.isEmpty()) {
            state = LockState.WAITING_URGENT_AWAKENING;
            urgentQueue.element().signal();
            return;
        }
        
        state = LockState.UNLOCKED;
        
        if(!entryQueue.isEmpty()) {
            entryQueue.element().signal();
        }
    }
    
    public Condition newCondition() {
        return new Condition();
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
    }
    
}
