package fairlock;

import java.util.LinkedList;
import java.util.Queue;

/**
 * This class implements a synchronization mechanism similar to the one provided
 * by Java within the {@link java.util.concurrent} package
 * (Explicit {@link java.util.concurrent.locks.Lock Locks} and
 * {@link java.util.concurrent.locks.Condition} variables) but whose
 * behaviour is in accordance with the semantic "signal-and-urgent", as
 * described by C.A.R. Hoare. 
 * 
 * <p>The implementation of this class guarantees that threads waiting to
 * acquire a FairLock (either via {@link FairLock#lock()}, after
 * being awakened by a {@link FairLock.Condition#signal()} or waiting in
 * the <i>urgent queue</i>) are awakened in a FIFO order.</p>
 * 
 * @author Gabriele Ara
 * 
 */
public class FairLock {
    /**
     * This enum specifies the current state of the lock.
     */
    protected enum LockState {
        UNLOCKED,
        LOCKED,
    }
    
    /**
     * This class implements a private binary samaphore, mainly used in order to
     * wait for an event to occur.
     * 
     * <p>NOTICE: This class suppresses every
     * {@link java.lang.InterruptedException} that can be thrown while waiting
     * for the given event to occur.</p>
     */
    protected static class PrivateEventSemaphore {
        private boolean hasSignal;
        private final Thread owner;
        
        /**
         * Initializes the event occurrance to false and sets the
         * owner of this semaphore with the value returned by
         * <tt>{@link java.lang.Thread#currentThread() Thread.currentThread()}
         * </tt>.
         */
        public PrivateEventSemaphore() {
            hasSignal = false;
            owner = Thread.currentThread();
        }
        
        /**
         * @return The owner of this semaphore
         */
        protected Thread getOwner() {
            return owner;
        }
        
        /**
         * Checks if the event monitored by this semaphore is already occurred.
         * If so, it resets the event occurrance and returns.
         * If the event isn't occurred yet, the current thread is suspended and
         * it will be awakened after another thread executes a
         * {@link #signal() signal}.
         * 
         * <p>Notice that if the current thread will be suspended, every
         * {@link java.lang.InterruptedException} that may be thrown after a
         * {@link java.lang.Object#wait() wait} call will be suppressed.</p>
         * 
         * <p>This method cannot terminate without an actual occurrance of the
         * given event (i.e. no spurious wakeup can happen).</p>
         */
        public synchronized void await() {
            while(!hasSignal) {
                try { wait(); } catch(InterruptedException e) { }
            }
            
            hasSignal = false;
        }
        
        /**
         * Registers an occurrance of the given event and awakens the thread
         * that was waiting inside the {@link #await() await}, if any.
         * 
         * <p>After each {@link #await() await} await is terminated, the event
         * occurrance is resetted to false; subsequent calls of this method
         * before any termination of a {@link #await() await} call will be
         * equivalent to a no operation.</p>
         */
        public synchronized void signal() {
            hasSignal = true;
            notify();
        }
    }
    
    /**
     * This class is used in combination with {@link FairLock}. It implements a
     * condition variable whose behavior is in accordance with the
     * "signal-and-urgent" pattern:
     * 
     * <ul>
     * <li>a thread waiting for a condition to occur releases the lock and is
     * suspended until the execution of a {@link #signal() signal};</li>
     * 
     * <li>a thread which executes a {@link #signal() signal} is suspended in
     * a queue different from <i>the entry queue</i> (the <i>urgent queue</i>)
     * and has a precedence on the acquiring of the lock after an
     * {@link #unlock() unlock} is executed.</li>
     * </ul>
     * 
     * <p>Every queue is guaranteed to be purely FIFO.
     */
    public class Condition {
        private final Queue<PrivateEventSemaphore> conditionQueue;
        
        /**
         * Creates a new Condition instance bound to an instance of a
         * {@link FairLock}.
         */
        Condition() {
            conditionQueue = new LinkedList<>();
        }
        
        /** 
         * @todo: refactor name
         * @return The number of threads waiting in the <i>condition queue</i>
         */
        public synchronized int size() {
            return conditionQueue.size();
        }
        
        /**
         * Checks wether there are threads waiting in the <i>condition queue</i>
         * or not.
         * 
         * @return true if there are threads waiting, false otherwise
         */
        public boolean isEmpty() {
            return size() == 0;
        }
        
        /**
         * Adds the current thread in the <i>condition queue</i> of this
         * Condition object and then releases the lock on the bounded
         * {@link FairLock}.
         * 
         * <p>If the current thread doesn't hold the lock on the bounded
         * {@link FairLock} then {@link IllegalMonitorStateException} is thrown.
         * </p>
         * 
         * <p>When this method terminates its execution, it is ensured that the
         * given condition variable has been signaled by another thread and that
         * the current thread is now the owner of the bounded {@link FairLock}.
         * </p>
         * 
         * <p>Differently from the
         * {@link java.util.concurrent.locks.Condition Condition} implementation
         * provided by the Java API, spurious wakeups cannot happen.</p>
         * 
         * @throws IllegalMonitorStateException if the current thread does not
         * hold the bounded lock
         */
        public void await() {
            synchronized(FairLock.this) {
                if(!isLocked() || !isOwner())
                    throw new IllegalMonitorStateException("You can't execute an await on a condition if you don't hold the bounded lock!");
            }
            
            PrivateEventSemaphore semaphore = new PrivateEventSemaphore();
            
            synchronized(this) {
                conditionQueue.add(semaphore);
            }
            
            FairLock.this.unlock();
            
            semaphore.await();
        }
        
        
        /**
         * If there is at least one thread waiting in the <i>condition queue</i>
         * of this Condition object, the first one is awakened and this thread
         * suspends itself waiting for the lock to be released in the <i>urgent
         * queue</i> of the bounded {@link FairLock}.
         * 
         * <p>If the <i>condition queue</i> is empty, the call of this method is
         * equivalent to a no operation.</p>
         * 
         * <p>If the current thread doesn't hold the lock on the bounded
         * {@link FairLock} then {@link IllegalMonitorStateException} is thrown.
         * </p>
         * 
         * <p>If the <i>condition queue</i> is not empty, the lock will be given
         * to the awakened thread; if there is no thread to awake the lock will
         * be maintained by the current thread (i.e. this call will result in a
         * no operation).</p>
         * 
         * @throws IllegalMonitorStateException if the current thread does not
         * hold the bounded lock
         */
        public void signal() throws IllegalMonitorStateException {
            synchronized(FairLock.this) {
                if(!isLocked() || !isOwner())
                    throw new IllegalMonitorStateException("You can't execute a signal on a condition if you don't hold the bounded lock!");
            }
                    
            PrivateEventSemaphore semaphore = new PrivateEventSemaphore();
            
            PrivateEventSemaphore awakeningSemaphore;
            
            synchronized(this) {
                if(conditionQueue.isEmpty())
                    return;
                
                awakeningSemaphore = conditionQueue.poll();
            }
            
            synchronized(FairLock.this) {
                setOwner(awakeningSemaphore.getOwner());
                
                urgentQueue.add(semaphore);
            }
            
            awakeningSemaphore.signal();
            
            semaphore.await();
        }
        
    }
    
    protected final Queue<PrivateEventSemaphore> entryQueue;
    protected final Queue<PrivateEventSemaphore> urgentQueue;
    
    LockState state;
    Thread owner;
    
    /**
     * Creates a new instance of a FairLock.
     */
    public FairLock() {
        entryQueue = new LinkedList<>();
        urgentQueue = new LinkedList<>();
        state = LockState.UNLOCKED;
        
        owner = null;
    }
    
    /**
     * 
     * @return true if the lock has been locked
     */
    public synchronized boolean isLocked() {
        return state == LockState.LOCKED;
    }
    
    /**
     * 
     * @return true if the lock is unlocked
     */
    public synchronized boolean isUnlocked() {
        return state == LockState.UNLOCKED;
    }
    
    /**
     * Returns the owner of the lock, if any.
     * This method may be useful when extending this class.
     * 
     * @return The thread that holds this lock, or null if the lock is unlocked
     */
    protected synchronized Thread getOwner() {
        return owner;
    }
    
    /**
     * Sets the owner of this lock.
     * This method may be useful when extending this class.
     * Be careful when calling this method.
     * 
     * @param owner the new owner for this lock.
     */
    protected synchronized void setOwner(Thread owner) {
        this.owner = owner;
    }
    
    /**
     * Checks wether the current thread is the owner of the lock.
     * This method may be useful when extending this class.
     * 
     * @return true if the current thread owns the lock
     */
    protected synchronized boolean isOwner() {
        return owner == Thread.currentThread();
    }
    
    /**
     * Acquires the lock, if free. Otherwise the current thread is suspended in
     * the <i>entry queue</i> until the lock becomes available for him.
     * 
     * <p>The <i>entry queue</i> is guaranteed to be FIFO.</p>
     * 
     * <p>When this method terminates its execution, the current thread is
     * guaranteed to be the owner of this lock.</p>
     * 
     * <p>If the current thread already holds the lock then
     * {@link IllegalMonitorStateException} is thrown.
     * </p>
     * 
     * @throws IllegalMonitorStateException if the current thread already holds
     * this lock
     */
    public void lock() {
        PrivateEventSemaphore semaphore;
        
        synchronized(this) {
            if(isOwner())
                throw new IllegalMonitorStateException("You can't acquire more than once a FairLock! Consider moving to a ReentrantLock.");
            
            if(isUnlocked()) {
                state = LockState.LOCKED;
                setOwner(Thread.currentThread());
                return;
            }
            
            semaphore = new PrivateEventSemaphore();
                
            entryQueue.add(semaphore);
        }
        
        semaphore.await();
    }
    
    /**
     * Releases the lock:
     * 
     * <ul>
     * <li> if there is at least one thread waiting in the <i>urgent queue</i>,
     * the first one will be awakened and it will receive the lock ownership;
     * </li>
     * 
     * <li> if there are no thread in the <i>urgent queue</i> and there is at
     * least one thread waiting in the <i>entry queue</i>, the first one of this
     * queue will be awakened and it will receive the lock ownership;</li>
     * 
     * <li> if there are no threads waiting either in the <i>urgent queue</i> or
     * in the <i>entry queue</i>, the lock will be set as free.</li>
     * </ul>
     * 
     * <p>If the current thread doesn't hold the lock then
     * {@link IllegalMonitorStateException} is thrown.
     * </p>
     * 
     * @throws IllegalMonitorStateException if the current thread does not hold
     * this lock
     */
    public synchronized void unlock() {
        if(!isLocked() || !isOwner())
            throw new IllegalMonitorStateException("You can't release a lock that you don't hold!");
        
        PrivateEventSemaphore awakeningSemaphore;
        
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
    
    /**
     * Returns a new {@link FairLock.Condition} instance that is bound to this
     * FairLock instance.
     * 
     * @return A new {@link FairLock.Condition} instance for this FairLock
     *         instance
     */
    public Condition newCondition() {
        return this.new Condition();
    }
    
}
