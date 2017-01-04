/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fairlock;

/**
 *
 * @author Gabriele Ara
 */
public class SingleResourceManagerFairLock implements SingleResourceManager {

    
    
    private final FairLock lock;
    private final FairLock.Condition conditionA;
    private final FairLock.Condition conditionB;
    
    private ResourceState state;
    
    public SingleResourceManagerFairLock() {
        lock = new FairLock();
        conditionA = lock.newCondition();
        conditionB = lock.newCondition();
        state = ResourceState.FREE;
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
                    conditionA.await();
                    break;
                case TYPE_B:
                    conditionB.await();
                    break;
            }
        } finally{
            lock.unlock();
        }
    }
    
    @Override
    public void release() {
        try {
            lock.lock();
            
            if(!conditionB.isEmpty())
                conditionB.signal();
            else if(!conditionA.isEmpty())
                conditionA.signal();
            else
                state = ResourceState.FREE;
        } finally {
            lock.unlock();
        }
    }
}
