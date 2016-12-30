/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fairlock;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author Gabriele
 */
public class SingleResourceManagerLock {
    private enum ResourceState {
        FREE,
        NON_FREE,
        WAITING_FOR_A,
        WAITING_FOR_B
    }
    
    
    private final Lock lock;
    private final Condition conditionA;
    private final Condition conditionB;
    
    private ResourceState state;
    
    private int countA;
    private int countB;
    
    public SingleResourceManagerLock() {
        lock = new ReentrantLock(true);
        conditionA = lock.newCondition();
        conditionB = lock.newCondition();
        state = ResourceState.FREE;
        countA = 0;
        countB = 0;
    }
    
    private boolean isFree() {
        return state == ResourceState.FREE;
    }
    
    // true => A
    public void request(boolean type) throws InterruptedException {
        try {
            lock.lock();
            
            if(isFree()) {
                state = ResourceState.NON_FREE;
                return;
            }
            
            // States WAITING_FOR_A and WAITING_FOR_B
            // prevent both that a Thread another than
            // the one in the condition variable can acquire
            // the resource, forcing it to enter the condition
            // variable.

            if(type) {
                
                // Loop prevents spurious wakeups
                do {
                    ++countA;
                    conditionA.await();
                    --countA;
                } while(state != ResourceState.WAITING_FOR_A);
            } else {
                do {
                    ++countB;
                    conditionB.await();
                    --countB;
                } while(state != ResourceState.WAITING_FOR_B);
            }
            
            state = ResourceState.NON_FREE;
        } finally {
            // TODO: check if THIS thread holds the lock, how can I do this?
            lock.unlock();
        }
    }
    
    public void release() throws InterruptedException {
        try {
            lock.lock();

            if(countB > 0) {
                state = ResourceState.WAITING_FOR_B;
                conditionB.signal();
            } else if(countA > 0) {
                state = ResourceState.WAITING_FOR_A;
                conditionA.signal();
            } else
                state = ResourceState.FREE;
            
        } finally {
            // TODO: check if THIS thread holds the lock, how can I do this?
            lock.unlock();
        }
    }
}
