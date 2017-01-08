package fairlock;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class implements a {@link SingleResourceManager} derived from a FSM.
 * Definition of the original FSM can be found in the file DesignModelAra.lts.
 * 
 * <p>This implementation, for simplicity, does not guarantee FIFO ordering nor
 * fairness. In fact, the only policy implemented by this class is the "clients
 * with priority B first" one, as described in the SingleResourceManager
 * interface.</p>
 * 
 * <p>This implementation has been proved right via the LSTA tool, see file
 * ImplementationModelAra.lts.</p>
 * 
 * @author Gabriele Ara
 */
public class SingleResourceManagerFSM implements SingleResourceManager {
    private final Lock lock;
    private final Condition conditionA;
    private final Condition conditionB;
    private final Condition access;
    
    /**
     * Current state of the FSM.
     * Legend:
     *
     * 0   FREE
     * 1   BUSY[0][0]
     * 2   BUSY[1][0]
     * 3   BUSY[2][0]
     * 4   BUSY[0][1]
     * 5   BUSY[1][1]
     * 6   RELEASE_TO_B[0][1]
     * 7   RELEASE_TO_B[1][1]
     * 8   RELEASE_TO_A[1][0]
     * 9   RELEASE_TO_A[2][0]
     * 
     */
    private int state;

    public SingleResourceManagerFSM() {
        this.lock = new ReentrantLock();
        this.conditionA = lock.newCondition();
        this.conditionB = lock.newCondition();
        this.access = lock.newCondition();
        
        this.state = 0;
    }
    
    @Override
    public ResourceState getState() {
        lock.lock();
        try {
            if(state == 0) return ResourceState.FREE;
            return ResourceState.BUSY;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isFree() {
        return getState() == ResourceState.FREE;
    }
    
    /**
     * Private method called by {@link #request(PriorityClass) request} when the
     * argument is equal to {@link PriorityClass#PRIO_A}.
     * 
     * <p>The mutual exclusion is handled by the request method, so no
     * lock/unlock is required.</p>
     * 
     * <p>If a release operation has been executed and there were already
     * threads waiting for the resource, subsequent calls of this method are
     * delayed until any pending request has been completed.</p>
     * 
     * @throws IllegalMonitorStateException if a request operation canot be
     * executed by a client with priority equal to {@link PriorityClass#PRIO_A}
     * right now
     */
    private void requestA() {
        if(state == 3 || state == 5 || state == 9)
            throw new IllegalMonitorStateException("A request operation from a client with priority class A is not supported right now!");
        
        // Another thread has been awakened in order to acquire the resource,
        // but it hasn't executed yet.
        // In order to avoid interference, any new call is delayed until any
        // thread completes a precedent request.
        while(state >= 6)
            try { access.await(); } catch(InterruptedException ex) { }
        
        // acquire[PrioA]
        
        // Resource was free, take it.
        if(state == 0) {
            state = 1;
            // endacquire[PrioA]
            return;
        }
        
        state += 1;
        
        // Waiting for the resource to be assigned to a thread with priority A.
        // Any thread is fine since in this class there's no need for FIFO
        // ordering.
        while(state != 8 && state != 9)
            try { conditionA.await(); } catch (InterruptedException ex) { }
        
        state -= 7;
        
        // endacquire[PrioA]
    }
    
    /**
     * Private method called by {@link #request(PriorityClass) request} when the
     * argument is equal to {@link PriorityClass#PRIO_B}.
     * 
     * <p>The mutual exclusion is handled by the request method, so no
     * lock/unlock is required.</p>
     * 
     * <p>If a release operation has been executed and there were already
     * threads waiting for the resource, subsequent calls of this method are
     * delayed until any pending request has been completed.</p>
     * 
     * @throws IllegalMonitorStateException if a request operation canot be
     * executed by a client with priority equal to {@link PriorityClass#PRIO_B}
     * right now
     */
    private void requestB() {
        if(state >= 3 && state <= 7)
            throw new IllegalMonitorStateException("A request operation from a client with priority class B is not supported right now!");
        
        // Another thread has been awakened in order to acquire the resource,
        // but it hasn't executed yet.
        // In order to avoid interference, any new call is delayed until any
        // thread completes a precedent request.
        while(state >= 6)
            try { access.await(); } catch(InterruptedException ex) { }
        
        // acquire[PrioB]
        
        // Resource was free, take it.
        if(state == 0) {
            state = 1;
            // endacquire[PrioB]
            return;
        }
        
        state += 3;
        
        // Waiting for the resource to be assigned to a thread with priority B.
        // Any thread is fine since in this class there's no need for FIFO
        // ordering.
        while(state != 6 && state != 7)
            try { conditionB.await(); } catch (InterruptedException ex) { }
        
        state -= 5;
        
        // endacquire[PrioB]
    }
    
    /**
     * This method simply dispatches requrests to {@link #requestA() requestA}
     * or {@link #requestB() requestB} methods, depending the value of the
     * argument prio.
     * 
     * @param prio the priority of the client that is requesting the resource
     */
    @Override
    public void request(PriorityClass prio) {
        lock.lock();
        try {
            switch(prio) {
                case PRIO_A:
                    requestA();
                    break;
                case PRIO_B:
                    requestB();
                    break;
            }
        } finally {
            
            // When a request operation is completed, any waiting thread in the
            // access queue can now enter the montior, because the pending
            // request they were waiting for has been completed
            access.signalAll();
            lock.unlock();
        }
    }
    
    @Override
    public void release() {
        lock.lock();
        try {
            switch(state) {
                case 4:
                case 5:
                    // Resource is given to any thread with prority B, no matter
                    // which one.
                    state += 2;
                    conditionB.signal();
                    break;
                case 2:
                case 3:
                    // Resource is given to any thread with prority A, no matter
                    // which one.
                    state += 6;
                    conditionA.signal();
                    break;
                case 1:
                    // Otherwise resource is set again as free.
                    state = 0;
                    break;
                default:
                    throw new IllegalMonitorStateException("A release operation is not supported right now!");
            }
            
        } finally {
            lock.unlock();
        }
    }
    
}
