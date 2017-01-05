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
        TYPE_B;
        
        @Override
        public String toString() {
            switch(this) {
                case TYPE_A:
                    return "A";
                case TYPE_B:
                    return "B";
                    
            }
            
            return "";
        }
    }
    
    ResourceState getState();
    
    boolean isFree();
    
    void request(PriorityClass prio);
    
    void release();
    
}
