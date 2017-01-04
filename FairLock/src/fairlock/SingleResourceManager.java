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
public interface SingleResourceManager {
    enum ResourceState {
        FREE,
        BUSY,
        // @NOTICE: the following states are completely optional
        //WAITING_FOR_A,
        //WAITING_FOR_B,
    }
    
    enum PriorityClass {
        TYPE_A,
        TYPE_B,
    }
    
    ResourceState getState();
    
    boolean isFree();
    
    void request(PriorityClass prio);
    
    void release();
    
}
