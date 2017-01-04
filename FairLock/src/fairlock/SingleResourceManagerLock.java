/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fairlock;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author Gabriele Ara
 */
public class SingleResourceManagerLock implements SingleResourceManager {
    private final Lock lock;
    private final Condition conditionA;
    private final Condition conditionB;
    
    private ResourceState state;
    
    //private int countA;
    //private int countB;
    
    private class MutableBoolean {
        boolean b;
        MutableBoolean(boolean b) {
            this.b = b;
        }
        
        boolean test() {
            return b;
        }
        
        void set() {
            b = true;
        }
        
        void reset() {
            b = false;
        }
    }
    
    private final Queue<MutableBoolean> conditionAQueue;
    private final Queue<MutableBoolean> conditionBQueue;
    
    public SingleResourceManagerLock() {
        lock = new ReentrantLock(true);
        conditionA = lock.newCondition();
        conditionB = lock.newCondition();
        state = ResourceState.FREE;
        
        conditionAQueue = new LinkedList<>();
        conditionBQueue = new LinkedList<>();
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
    
    private void enqueue(Condition c, Queue<MutableBoolean> q) {
        MutableBoolean canIGo = new MutableBoolean(false);
        
        do {
            q.add(canIGo);

            try { c.await(); } catch (InterruptedException ex) { }

        } while(q.peek() != canIGo && !canIGo.test());

        q.remove(canIGo);
    }
    
    @Override
    public void request(PriorityClass prio) {
        try {
            lock.lock();
            
            if(state == ResourceState.FREE)
                return;
            
            switch(prio) {
                case TYPE_A:
                    enqueue(conditionA, conditionAQueue);
                case TYPE_B:
                    enqueue(conditionB, conditionBQueue);
            }
        } finally {
            // @TODO: REMOVE state = ResourceState.BUSY;
            lock.unlock();
        }
    }
    
    @Override
    public void release() {
        try {
            lock.lock();
            
            if(conditionBQueue.size() > 0) {
                // @TODO: REMOVE state = ResourceState.WAITING_FOR_B;
                conditionBQueue.peek().set();
                conditionB.signal();
                
            } else if(conditionAQueue.size() > 0) {
                // @TODO: REMOVE state = ResourceState.WAITING_FOR_A;
                conditionAQueue.peek().set();
                conditionA.signal();
                
            } else
                state = ResourceState.FREE;
            
        } finally {
            lock.unlock();
        }
    }
}
