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
public class SingleResourceManagerFSM implements SingleResourceManager {
    private final Lock lock;
    private final Condition conditionA;
    private final Condition conditionB;
    
    private ResourceState state;
    
    private int counterA;
    private int counterB;

    public SingleResourceManagerFSM() {
        this.lock = new ReentrantLock();
        this.conditionA = lock.newCondition();
        this.conditionB = lock.newCondition();
        this.state = ResourceState.FREE;
        this.counterA = 0;
        this.counterB = 0;
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
    
    // @NOTICE: this implementation suffers spurious wakups!
    @Override
    public void request(PriorityClass prio) {
        try {
            lock.lock();
            if(state == ResourceState.FREE) {
                state = ResourceState.BUSY;
                return;
            }
            
            switch(prio) {
                case TYPE_A:
                    ++counterA;
                    try { conditionA.await(); } catch(InterruptedException ex) { }
                    --counterA;
                    
                    break;
                case TYPE_B:
                    ++counterB;
                    try { conditionB.await(); } catch(InterruptedException ex) { }
                    --counterB;
                    
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
            
            if(counterB > 0) {
                conditionB.signal();
            } else if (counterA > 0) {
                conditionA.signal();
            } else {
                state = ResourceState.FREE;
            }
            
        } finally {
            lock.unlock();
        }
    }
    
}
