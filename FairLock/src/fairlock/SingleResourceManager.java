/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fairlock;

/**
 *
 * @author Gabriele
 */
public class SingleResourceManager {
    private enum ResourceState {
        FREE,
        NON_FREE,
    }
    
    private final FairLock lock;
    private final FairLock.Condition conditionA;
    private final FairLock.Condition conditionB;
    
    private ResourceState state;
    
    private int countA;
    private int countB;
    
    public SingleResourceManager() {
        lock = new FairLock();
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
        lock.lock();
        
        if(isFree()) {
            state = ResourceState.NON_FREE;
            lock.unlock();
            return;
        }
        
        if(type) {
            ++countA;
            try {
                conditionA.await();
            } finally {
                --countA;
            }
        } else {
            ++countB;
            try {
                conditionB.await();
            } finally {
                --countB;
            }
        }
        
        lock.unlock();
    }
    
    public void release() throws InterruptedException {
        lock.lock();
        
        if(countB > 0)
            conditionB.signal();
        else if(countA > 0)
            conditionA.signal();
        else
            state = ResourceState.FREE;
        
        lock.unlock();
    }
}
