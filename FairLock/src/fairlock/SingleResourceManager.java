package fairlock;

/**
 * Interface that can be implemented to provide a manager of a single resource
 * that must be accessed in mutual exclusion.
 *
 * @author Gabriele Ara
 */
public interface SingleResourceManager {
    /**
     * This enum specifies the current state of the resource protected by this
     * manager.
     */
    enum ResourceState {
        FREE,
        BUSY,
    }
    
    /**
     * This enum specifies the priority of a client that want to access a
     * resource protected by a {@link SingleResourceManager} instance.
     * 
     * <p>The resource can be acquired by clients of two types, each of them
     * with a different priotity; in general, clients with a priority equal to
     * {@link PriorityClass#PRIO_B} have a higher priority than the others.</p>
     * 
     * @see SingleResourceManager#request(fairlock.SingleResourceManager.PriorityClass) SingleResourceManager.request
     * @see SingleResourceManager#release() SingleResourceManager.release
     */
    enum PriorityClass {
        PRIO_A,
        PRIO_B;
        
        @Override
        public String toString() {
            switch(this) {
                case PRIO_A:
                    return "A";
                case PRIO_B:
                    return "B";
                    
            }
            
            return "";
        }
    }
    
    /**
     * 
     * @return the state of the resource protected by this instance of the
     *         manager:
     *         {@link ResourceState#FREE} if the resource is free,
     *         {@link ResourceState#BUSY} otherwise
     */
    ResourceState getState();
    
    /**
     * 
     * @return true id the resource protected by this instance of the manager is
     *         free
     */
    boolean isFree();
    
    /**
     * Requests the permission to operate on the shared resource in mutual
     * exclusion:
     * 
     * <ul>
     * <li>if the resource is free, this method returns immediately, allowing the
     * calling client to operate over the resource;</li>
     * 
     * <li>if the resource is currently in use by another client, the current
     * thread is suspended; it will later be awakened by the client holding the
     * resource, accoding with the behavior of the {@link release() release}.</li>
     * </ul>
     * 
     * <p>When this method terminates, the current thread is ensured to have the
     * permission to use the shared resource.</p>
     * 
     * <p>Notice: some implementations of this interface may throw a (unchecked)
     * exception in case a thread executes this method in a wrong time.</p>
     * 
     * @param prio the priority of the client that is requesting the resource
     * 
     * @see release()
     */
    void request(PriorityClass prio);
    
    /**
     * Releases the permission to operate on the shared resource protected by
     * this manager:
     * 
     * <ul>
     * <li> if there is at least one client with priority equal to
     * {@link PriorityClass#PRIO_B} waiting for the resource, one of them will
     * be awakened and it will receive the resource ownership;</li>
     * 
     * <li> if there are no clients with priority equal to
     * {@link PriorityClass#PRIO_B} waiting for the resource and there is at
     * least one client with priority equal to {@link PriorityClass#PRIO_A}
     * waiting for the resource, one of them will be awakened and it will
     * receive the resource ownership;</li>
     
     * <li>if there are no clients waiting for the resource, the resource state
     * is set to free and the method terminates.</li>
     * </ul>
     * 
     * <p>It's up to the implementations of this interface to choose between
     * the waiting clients of the same priority class which one has to be
     * awakened.</p>
     *
     * <p>Notice: some implementations of this interface may throw a (unchecked)
     * exception in case a thread executes this method in a wrong time.</p>
     * 
     * @see request(PriorityClass)
     */
    void release();
    
}
