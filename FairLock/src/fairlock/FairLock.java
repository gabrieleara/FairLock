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
 * @author Gabriele Ara
 */
public class FairLock {
    protected enum LockState {
        UNLOCKED,
        LOCKED,
    }
    
    protected static class SimpleEventSemaphore {
        private boolean hasSignal;
        private final Thread owner;
        
        public SimpleEventSemaphore() {
            hasSignal = false;
            owner = Thread.currentThread();
        }
        
        protected Thread getOwner() {
            return owner;
        }
        
        public synchronized void await() {
            while(!hasSignal) {
                try { wait(); } catch(InterruptedException e) { }
            }
            
            assert hasSignal;
            hasSignal = false;
        }
        
        public synchronized void signal() {
            hasSignal = true;
            notify();
        }
    }
    
    public class Condition {
        private final Queue<SimpleEventSemaphore> conditionQueue;
        
        Condition() {
            conditionQueue = new LinkedList<>();
        }
        
        // @TODO: refactor name
        public synchronized int size() {
            return conditionQueue.size();
        }
        
        public boolean isEmpty() {
            return size() == 0;
        }
        
        // NOTICE: This method assumes that current thread owns lock on the 
        // linked FairLock;
        public void await() {
            SimpleEventSemaphore semaphore = new SimpleEventSemaphore();
            
            synchronized(this) {
                conditionQueue.add(semaphore);
            }
            
            FairLock.this.unlock();
            
            semaphore.await();
            
            assert isOwner();
        }
        
        
        // NOTICE: This method assumes that current thread owns lock on the 
        // linked FairLock;
        public void signal() {
            assert isLocked();
            assert isOwner();
                    
            SimpleEventSemaphore semaphore = new SimpleEventSemaphore();
            
            SimpleEventSemaphore awakeningSemaphore;
            
            synchronized(this) {
                if(conditionQueue.isEmpty())
                    return;
                
                awakeningSemaphore = conditionQueue.poll();
            }
            
            synchronized(FairLock.this) {
                assert isLocked();
                
                setOwner(awakeningSemaphore.getOwner());
                
                urgentQueue.add(semaphore);
            }
            
            awakeningSemaphore.signal();
            
            semaphore.await();
            
            synchronized(FairLock.this) {
                assert isLocked();
                assert isOwner();
            }
        }
        
    }
    
    
    
    
    protected final Queue<SimpleEventSemaphore> entryQueue;
    protected final Queue<SimpleEventSemaphore> urgentQueue;
    
    LockState state;
    Thread owner;
        
    public FairLock() {
        entryQueue = new LinkedList<>();
        urgentQueue = new LinkedList<>();
        state = LockState.UNLOCKED;
        
        owner = null;
    }
    
    public synchronized boolean isLocked() {
        return state == LockState.LOCKED;
    }
    
    public synchronized boolean isUnlocked() {
        return state == LockState.UNLOCKED;
    }
    
    protected synchronized Thread getOwner() {
        return owner;
    }
    
    protected synchronized void setOwner(Thread o) {
        owner = o;
    }
    
    protected synchronized boolean isOwner() {
        return owner == Thread.currentThread();
    }
    
    // NOTICE: this method assumes that current Thread DOES NOT own this lock,
    // otherwise it will end up this being a deadlock.
    public void lock() {
        SimpleEventSemaphore semaphore;
        
        synchronized(this) {
            assert !isOwner();
            
            if(isUnlocked()) {
                assert entryQueue.isEmpty();
                assert urgentQueue.isEmpty();
                
                state = LockState.LOCKED;
                setOwner(Thread.currentThread());
                return;
            }
            
            semaphore = new SimpleEventSemaphore();
                
            entryQueue.add(semaphore);
        }
        
        semaphore.await();
        
        synchronized(this) {
            assert isLocked();
            assert urgentQueue.isEmpty();
            assert isOwner();
            
            entryQueue.remove(semaphore);
        }
    }
    
    protected synchronized void unlockIfOwner() {
        if(isOwner())
            unlock();
    }
    
    // NOTICE: this method assumes that current Thread owns this lock, otherwise
    // it has an unpredictable behaviour.
    public synchronized void unlock() {
        assert isLocked();
        assert isOwner();
        
        SimpleEventSemaphore awakeningSemaphore;
        
        if(!urgentQueue.isEmpty()) {
            awakeningSemaphore = urgentQueue.poll();
            
            setOwner(awakeningSemaphore.getOwner());
            awakeningSemaphore.signal();
            
            return;
        }
        
        if(!entryQueue.isEmpty()) {
            awakeningSemaphore = entryQueue.poll();
            
            setOwner(awakeningSemaphore.getOwner());
            awakeningSemaphore.signal();
            
            return;
        }
        
        setOwner(null);
        state = LockState.UNLOCKED;
    }
    
    public Condition newCondition() {
        return this.new Condition();
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    /*private static class TestRun implements Runnable {
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
            lock.lock();
            NAMEORDER[i++] = Thread.currentThread().getName();
            for(int j = 0; j < 1000; ++j) {
                c1.await();
                
                count();
            }
            lock.unlock();
        }
        
    }*/
    
    
    /**
     * @param args the command line arguments
     */
    /*public static void main(String[] args) {
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
        
        
                
        
    }*/
    
}
