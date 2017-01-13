package manager;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of the {@link SingleResourceManager} interface which uses the
 * {@link Lock} class as synchronization mechanism.
 * 
 * <p>As additional policy, this class ensures a total FIFO ordering between
 * requests (will anyway be given higher priority to requests of threads with
 * priority equal to
 * {@link SingleResourceManager.PriorityClass#PRIO_B PriorityClass.PRIO_B}).</p>
 * 
 * @author Gabriele Ara
 */
public class SingleResourceManagerLock implements SingleResourceManager {
    private final Lock lock;
    private final Condition conditionA;
    private final Condition conditionB;
    
    private ResourceState state;
    
    // 
    // The owner attribute is used to prevent spurious wakeups. This class uses
    // ReentrantLock as implementation of the Lock interface; this could lead to
    // the phenomenom of spurious wakeups, resulting in threads that haven't
    // been signaled awakening from an await operation.
    //
    // In simple words, the owner tells an awakened thread if it was actually
    // signaled or not. If it wasn't, it will enter again in the waiting state.
    //
    // Due to this behavior, using even a fair implementation of a FairLock
    // doesn't solve the problem, because a thread awakened "by error" due to a
    // spurious wakeup will enter again the await and will be put as last in
    // the waiting queue for that condition variable and this could lead to an
    // ordering error.
    //
    // So in this class it will be used the non-fair implementation of the
    // ReentrantLock; the following algorithm is applied:
    //
    // - the thread that owns the resource at any given time (if any) will be
    // referred via the owner attribute of this object;
    //
    // - every thread calling a request that cannot be executed immediately
    // (e.g. another threads is using the resource) will be put either in the
    // conditionAQueue or in the conditionBQueue, depending on its priority and
    // it will then execute an await operation on the corresponding condition
    // variable;
    //
    // - when a resource is released, if there is at least one thread in the
    // conditionBQueue, then the first one will be set as owner of the resource
    // and a signalAll will be executed on the conditionB;
    //
    // - when a resource is released, if there are no threads in the
    // conditionBQueue and there is at least one thread in the conditionAQueue,
    // then the first one will be set as owner of the resource and a signalAll
    // will be executed on the conditionA;
    //
    // - every thread that awakens from a Condition variable will check if it's
    // actually the owner of the resource, if not it will enter again its
    // waiting state in the same Condition variable.
    //
    private Thread owner;
    
    private final Queue<Thread> conditionAQueue;
    private final Queue<Thread> conditionBQueue;
    
    public SingleResourceManagerLock() {
        lock = new ReentrantLock();
        conditionA = lock.newCondition();
        conditionB = lock.newCondition();
        state = ResourceState.FREE;
        
        conditionAQueue = new LinkedList<>();
        conditionBQueue = new LinkedList<>();
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
    
    /**
     * Enqueues the current thread on the {@link Queue} q and performs an await
     * operation on the {@link Condition} variable c until the current thread
     * becomes the owner of the resource protected by this object.
     * 
     * @param c the condition variable that has to be used to await for the
     * resource
     * 
     * @param q the queue in which the current thread must be put
     */
    protected void enqueue(Condition c, Queue<Thread> q) {
        q.add(Thread.currentThread());
        
        do {
            try { c.await(); } catch (InterruptedException ex) { }
        } while(owner != Thread.currentThread());
    }
    
    @Override
    public void request(PriorityClass prio) {
        lock.lock();
        try {
            if(state == ResourceState.FREE) {
                state = ResourceState.BUSY;
                owner = Thread.currentThread();
                return;
            }
            
            switch(prio) {
                case PRIO_A:
                    enqueue(conditionA, conditionAQueue);
                    break;
                case PRIO_B:
                    enqueue(conditionB, conditionBQueue);
                    break;
            }
        } finally {
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
            
            if(conditionBQueue.size() > 0) {
                owner = conditionBQueue.poll();
                conditionB.signalAll();
            } else if(conditionAQueue.size() > 0) {
                owner = conditionAQueue.poll();
                conditionA.signalAll();
            } else {
                owner = null;
                state = ResourceState.FREE;
            }
            
        } finally {
            lock.unlock();
        }
    }
}
