/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fairlock;

import java.util.PriorityQueue;

/**
 *
 * @author Gabriele
 */
public class FairLock {
    enum LockState {
        UNLOCKED,
        LOCKED,
        WAITING_URGENT_AWAKENING,
        WAITING_CONDITION_AWAKENING,
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    static class Ticket implements Comparable<Ticket>{
        long ticket;
        
        Ticket() {
            ticket = System.nanoTime();
        }

        @Override
        public int compareTo(Ticket o) {
            return (ticket < o.ticket) ? -1 : ((ticket == ticket) ? 0 : 1);
        }
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    public class Condition {
        private final PriorityQueue<Ticket> conditionQueue;
        
        Condition() {
            conditionQueue = new PriorityQueue<>(0);    
        }
        
        public void await() throws InterruptedException {
            Ticket ticket = new Ticket();
            boolean canIContinue = false;
            
            synchronized(this) {
                conditionQueue.add(ticket);
            }
            
            FairLock.this.unlock();
            
            synchronized(FairLock.this) {
                synchronized(this) {
                    canIContinue = ticket == conditionQueue.element() && state == LockState.WAITING_CONDITION_AWAKENING && currentCondition == this;
                }
            }
            
            while(!canIContinue) {
                synchronized(ticket) {
                    try {
                        ticket.wait();
                    } catch(InterruptedException e) {
                        conditionQueue.remove(ticket);
                        throw e;
                    } 
                }
                
                synchronized(FairLock.this) {
                    synchronized(this) {
                        canIContinue = ticket == conditionQueue.element() && state == LockState.WAITING_CONDITION_AWAKENING && currentCondition == this;
                    }
                }
                
            }
                
            synchronized(FairLock.this) {
                synchronized(this) {
                    conditionQueue.poll();
                    state = LockState.LOCKED;
                    currentCondition = null;
                }
            }
        }
        
        /*
        NOTICE: This method assumes that current thread owns lock on the 
        linked FairLock;
        */
        public void signal() throws InterruptedException {
            Ticket ticket = new Ticket();
            
            Ticket awTicket = null;
            
            synchronized(this) {
                if(conditionQueue.isEmpty())
                    return;
                
                awTicket = conditionQueue.element();
            }
            
            synchronized(FairLock.this) {
                urgentQueue.add(ticket);
                state = LockState.WAITING_CONDITION_AWAKENING;
                currentCondition = this;
            }
            
            synchronized(awTicket) {
                awTicket.notify();
            }
            
            boolean canIContinue;
            
            synchronized(FairLock.this) {
                synchronized(this) {
                    canIContinue = state == LockState.WAITING_URGENT_AWAKENING && urgentQueue.element() == ticket;
                }
            }
            
            while (!canIContinue){
                synchronized(ticket) {
                    try {
                        ticket.wait();
                    } catch(InterruptedException e) {
                        urgentQueue.remove(ticket);
                        throw e;
                    }
                }
                
                synchronized(FairLock.this) {
                    synchronized(this) {
                        canIContinue = state == LockState.WAITING_URGENT_AWAKENING && urgentQueue.element() == ticket;
                    }
                }
                
            }
            
            synchronized(FairLock.this) {
                synchronized(this) {
                    urgentQueue.poll();
                    state = LockState.LOCKED;
                }
            }
        }
        
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    private final PriorityQueue<Ticket> entryQueue;
    private final PriorityQueue<Ticket> urgentQueue;
    
    LockState state;
    Condition currentCondition;
        
    public FairLock() {
        entryQueue = new PriorityQueue<>(0);
        urgentQueue = new PriorityQueue<>(0);
        state = LockState.UNLOCKED;
        currentCondition = null;
    }
    
    public void lock() throws InterruptedException {
        Ticket ticket = new Ticket();
        boolean ownThisLock;
        
        synchronized(this) {
            entryQueue.add(ticket);
            
            ownThisLock = state == LockState.UNLOCKED && ticket == entryQueue.element() && urgentQueue.isEmpty();
        }
        
        while(!ownThisLock) {
            synchronized(ticket) {
                try {
                    ticket.wait();
                } catch (InterruptedException e) {
                    entryQueue.remove(ticket);
                    throw e;
                }
            }
            
            synchronized(this) {
                ownThisLock = state == LockState.UNLOCKED && ticket == entryQueue.element() && urgentQueue.isEmpty();
            }
        }
        
        synchronized(this) {
            entryQueue.poll();
            state = LockState.LOCKED;
        }
    }
    
    public synchronized void unlock() {
        if(!urgentQueue.isEmpty()) {
            state = LockState.WAITING_URGENT_AWAKENING;
            Ticket t = urgentQueue.element();
            synchronized(t) {
                t.notify();
            }
            return;
        }
        
        state = LockState.UNLOCKED;
        
        if(!entryQueue.isEmpty()) {
            Ticket t = entryQueue.element();
            synchronized(t) {
                t.notify();
            }
        }
    }
    
    public Condition newCondition() {
        return new Condition();
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
    }
    
}
