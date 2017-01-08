package fairlock;

/**
 * Implementation of the {@link SingleResourceManager} interface which uses the
 * {@link FairLock} class as synchronization mechanism.
 * 
 * <p>As additional policy, this class ensures a total FIFO ordering between
 * requests (will anyway be given higher priority to requests of threads with
 * priority equal to {@link PriorityClass#PRIO_B}).</p>
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
        lock.lock();
        try {
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
        lock.lock();
        try {
            if(state == ResourceState.FREE) {
                state = ResourceState.BUSY;
                return;
            }
            
            switch(prio) {
                case PRIO_A:
                    conditionA.await();
                    break;
                case PRIO_B:
                    conditionB.await();
                    break;
            }
        } finally{
            lock.unlock();
        }
    }
    
    /**
     * @throws IllegalMonitorStateException if the resource was already free
     */
    @Override
    public void release() {
        lock.lock();
        try {
            if(state != ResourceState.BUSY)
                throw new IllegalMonitorStateException("Resource was already free, cannot execute release operation!");
            
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
