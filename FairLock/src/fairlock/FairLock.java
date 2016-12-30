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
        private boolean hasSignal;
        private final Thread owner;
        
        public BinarySemaphore() {
            hasSignal = false;
            owner = Thread.currentThread();
        }
        
        // NOTICE: problems may occur operating on q without synchronized
        // access of the lock class
        public synchronized void await() throws InterruptedException {
            while(!hasSignal) {
                try {
                    wait();
                } catch(InterruptedException e) {
                    /*
                    if(q != null) {
                        q.remove(this);
                    */
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
            
            try {
                semaphore.await();
            } catch(InterruptedException e) {
                
                // TODO: CLEANUP
                // Check if the current thread is the expected-to-be owner
                // of the lock, if so unlock the lock.
                synchronized(FairLock.this) {
                    synchronized(this) {
                        conditionQueue.remove(semaphore);
                        
                        FairLock.this.unlockIfOwner();
                    }   
                }
                
                throw e;
            }
            
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
                
                owner = awakeningSemaphore.owner;
                
                urgentQueue.add(semaphore);
            }
            
            awakeningSemaphore.signal();
            
            try {
                semaphore.await();
            } catch(InterruptedException e) {
                
                // TODO: CLEANUP
                // Check if the current thread is the expected-to-be owner
                // of the lock, if so unlock the lock.
                synchronized(FairLock.this) {
                    synchronized(this) {
                        urgentQueue.remove(semaphore);
                        
                        FairLock.this.unlockIfOwner();
                    }   
                }
                
                throw e;
            }
            
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
    Thread owner;
        
    public FairLock() {
        entryQueue = new LinkedList<>();
        urgentQueue = new LinkedList<>();
        state = LockState.UNLOCKED;
        currentCondition = null;
        
        owner = null;
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
        BinarySemaphore semaphore;
        
        synchronized(this) {
            if(state == LockState.UNLOCKED) {
                assert entryQueue.isEmpty();
                assert urgentQueue.isEmpty();
                
                state = LockState.LOCKED;
                owner = Thread.currentThread();
                return;
            }
            
            semaphore = new BinarySemaphore();
                
            entryQueue.add(semaphore);
        }
        
        try {
            semaphore.await();
        } catch(InterruptedException e) {

            // TODO: CLEANUP
            // Check if the current thread is the expected-to-be owner
            // of the lock, if so unlock the lock.
            synchronized(this) {
                entryQueue.remove(semaphore);

                unlockIfOwner();
            }

            throw e;
        }
        
        synchronized(this) {
            assert isLocked();
            assert urgentQueue.isEmpty();
            assert entryQueue.peek() == semaphore;
            
            entryQueue.remove(semaphore);
        }
    }
    
    protected synchronized void unlockIfOwner() {
        if(owner == Thread.currentThread())
            unlock();
            
    }
    
    // NOTICE: this method assumes that current Thread owns this lock, otherwise
    // it has an unpredictable behaviour.
    public synchronized void unlock() {
        /* NOTICE: this check isn't enought of course. */
        assert isLocked();
        
        if(!urgentQueue.isEmpty()) {
            owner = urgentQueue.peek().owner;
            urgentQueue.peek().signal();
            return;
        }
        
        if(!entryQueue.isEmpty()) {
            owner = entryQueue.peek().owner;
            entryQueue.peek().signal();
            return;
        }
        
        state = LockState.UNLOCKED;
    }
    
    public Condition newCondition() {
        return this.new Condition();
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    private static class TestRun implements Runnable {
        final FairLock lock;
        final Condition c1;
        final Condition c2;

        public TestRun(FairLock lock, Condition c1, Condition c2) {
            this.lock = lock;
            this.c1 = c1;
            this.c2 = c2;
        }
        
        private static int counter = 0;
        private static final String[] NAMEORDER = new String[3];
        private static int i = 0;
        private static String lastName = null;
        
        public static void count() {
            ++counter;
            lastName = Thread.currentThread().getName();
        }

        @Override
        public void run() {
            try {
                lock.lock();
                NAMEORDER[i++] = Thread.currentThread().getName();
                
                for(int j = 0; j < 1000; ++j) {                    
                    c1.await();
                    
                    count();
                }
                lock.unlock();
            } catch(InterruptedException e ) {
                e.printStackTrace();
            }
        }
        
    }
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        FairLock l = new FairLock();
        Condition c1 = l.newCondition();
        Condition c2 = l.newCondition();
        
        Runnable body = new TestRun(l, c1, c2);
        
        ThreadGroup workers = new ThreadGroup("WORKERS");
        
        Thread t1 = new Thread(workers, body);
        Thread t2 = new Thread(workers, body);
        Thread t3 = new Thread(workers, body);
        
        try {
        l.lock();
        
        t1.start();
        t2.start();
        t3.start();
        
        l.unlock();
        synchronized(body) {
            body.wait(1000);
        }
        l.lock();

        
        while(TestRun.counter != 3000) {
            if(TestRun.counter % 3 != 0)
                System.out.println("NUMERIC ERROR!, current value -> " + TestRun.counter);
            
            if(TestRun.counter == 3000)
                break;
            
            c1.signal();
            
            if(!TestRun.lastName.equals(TestRun.NAMEORDER[0]))
                System.out.println("ORDER ERROR!");
            
            c1.signal();
            
            if(!TestRun.lastName.equals(TestRun.NAMEORDER[1]))
                System.out.println("ORDER ERROR!");
            
            c1.signal();
            
            if(!TestRun.lastName.equals(TestRun.NAMEORDER[2]))
                System.out.println("ORDER ERROR!");
        }
        
        System.out.println("Main Thread, final value " + TestRun.counter);
        
        l.unlock();
        
        t1.join();
        t2.join();
        t3.join();
        
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
        
        
                
        
    }
    
}
