package test;

import manager.SingleResourceManagerFSM;
import manager.SingleResourceManagerLock;
import manager.SingleResourceManagerFairLock;
import manager.SingleResourceManager;
import manager.SingleResourceManager.PriorityClass;
import java.awt.AWTException;
import java.awt.Robot;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Class used to test the {@link SingleResourceManager} implemnentations
 * provided in this package (and the {@link fairlock.FairLock} class, in the
 * case of the corresponding Manager).
 * 
 * @author Gabriele Ara
 */
public class TestClass {
    
    /**
     * If the main is executed in Netbeans (c) console, this method clears the
     * screen.
     */
    protected static void clearNetbeansConsole() {
        try {
            Robot pressbot = new Robot();
            pressbot.keyPress(17); // Holds CTRL key.
            pressbot.keyPress(76); // Holds L key.
            pressbot.keyRelease(17); // Releases CTRL key.
            pressbot.keyRelease(76); // Releases L key.
        } catch (AWTException ex) {
            System.out.println();
            System.out.println();
            System.out.println();
            System.out.println();
            System.out.println();
        }
        
        try{
            Thread.sleep(100);
        } catch(InterruptedException ex ) {
            
        }
    }
    
    /**
     * Client that "uses" the resource protected by the Manager in its
     * constructor; there is no actual resource but that's not a problem when
     * testing the manager.
     * 
     * <p>Each ClientThread executes a given number of request/release in a
     * loop. Between sequential operations, random delays are added in order to
     * simulate operations on a real resource.</p>
     * 
     * <p>Every operation performed by the client is printed on the standard
     * output. Notice that since there is not atomicity between printing and the
     * execution of the following operation, there is no guarantee that the
     * actual operations are executed in the same exact order of the prints.</p>
     * 
     * @see #test(manager.SingleResourceManager)
     */
    protected static class ClientThread extends Thread {
        private static long seed = System.nanoTime();
        private static int nextId = 1;
        
        private final PriorityClass priority;
        private final SingleResourceManager manager;
        private final Random generator;
        private final int id;
        private final int N;

        public ClientThread(PriorityClass priority,
                SingleResourceManager manager,
                int n) {
            this.priority = priority;
            this.manager = manager;
            this.N = n;
            id = nextId++;
            
            generator = new Random(seed);
            
            seed += 46856; // A "random" increment for the seed
        }
        
        @Override
        public void run() {
            for(int i = 0; i < N; ++i) {
                try {
                    Thread.sleep(generator.nextInt(100));
                } catch (InterruptedException ex) {
                    
                }
                
                System.out.println("Thread " + priority + id + ": Starting a request...");
                
                manager.request(priority);
                
                System.out.println("Thread " + priority + id + ": Using resource...");
                
                try {
                    Thread.sleep(generator.nextInt(200));
                } catch (InterruptedException ex) {
                    
                }
                
                System.out.println("Thread " + priority + id + ": Releasing resource...");
                
                manager.release();
            }
        }
        
    }
    
    private static final Scanner SCANNER = new Scanner(System.in);
    
    private static final String PATTERN_A = "^\\s*[Aa]\\s*$";
    private static final String PATTERN_B = "^\\s*[Bb]\\s*$";
    private static final String PATTERN_C = "^\\s*[Cc]\\s*$";
    private static final String PATTERN_Y = "^\\s*[Yy]\\s*$";
    
    /**
     * Performs a test on the given manager. To do so, it creates three threads
     * (two with a priority equal to
     * {@link SingleResourceManager.PriorityClass#PRIO_A PriorityClass.PRIO_A}
     * and one with a priority equal to
     * {@link SingleResourceManager.PriorityClass#PRIO_B PriorityClass.PRIO_B}
     * that execute a certain amount of operations on the resource.
     * 
     * <p>If the test thread set goes in deadlock, the testing program never
     * exits the loop waiting for the test to finish, experiencing a deadlock
     * too. If the three threads terminate each their execution, the test ends.
     * </p>
     * 
     * @param manager the manager that needs to be tested
     * 
     * @see SingleResourceManager#request(manager.SingleResourceManager.PriorityClass) 
     * @see SingleResourceManager#release() 
     */
    protected static void test(SingleResourceManager manager) {
        System.out.print("Insert the number of acquires that each thread should try: ");
        int n = SCANNER.nextInt();
        
        Thread a1 = new ClientThread(PriorityClass.PRIO_A, manager, n);
        Thread a2 = new ClientThread(PriorityClass.PRIO_A, manager, n);
        Thread b1 = new ClientThread(PriorityClass.PRIO_B, manager, n);
        
        a1.start();
        a2.start();
        b1.start();
        
        while(a1.isAlive() || a2.isAlive() || b1.isAlive()) {
            try {
                a1.join();
                a2.join();
                b1.join();
                
            } catch (InterruptedException ex) {

            }
        }
        
        System.out.println();
        System.out.println("Test finished!");
    }
    
    /**
     * Performs different tests of the {@link SingleResourceManager}
     * implementations provided in the same package of the interface.
     * 
     * @param args unused
     */
    public static void main(String[] args) {
        String input;
        boolean continue_ = true;
        
        for(; continue_; clearNetbeansConsole()) {
            System.out.println("Select which kind of Manager you want to test:");
            System.out.println("A) The one which uses FairLock to implement the monitor in signal and urgent pattern;");
            System.out.println("B) The one which uses the standard class Lock to implement the monitor in signal and continue pattern.");
            System.out.println("C) The one derived from the FSM specification, which uses the standard class Lock to implement the monitor in signal and continue pattern.");

            System.out.print("Submit your choice (A/B/C): ");

            input = SCANNER.next();
            
            if(Pattern.matches(PATTERN_A, input)) {
                clearNetbeansConsole();
                
                System.out.println("Starting testing of the manager A...");
                System.out.println();
                
                test(new SingleResourceManagerFairLock());
                
                System.out.println();
                
            } else if (Pattern.matches(PATTERN_B, input)) {
                clearNetbeansConsole();
                
                System.out.println("Starting testing of the manager B...");
                System.out.println();
                
                test(new SingleResourceManagerLock());
                
                System.out.println();
            } else if (Pattern.matches(PATTERN_C, input)) {
                clearNetbeansConsole();
                
                System.out.println("Starting testing of the manager C...");
                System.out.println();
                
                test(new SingleResourceManagerFSM());
                
                System.out.println();
            } else {
                System.out.println("Wrong input, please insert again.");
                System.out.println("Press any letter key then Enter to continue...");
                SCANNER.next();
                
                continue;
            }
            
            System.out.print("Do you wish to try another test? (Y/n) ");
            
            input = SCANNER.next();
            
            continue_ = Pattern.matches(PATTERN_Y, input);
        }
    }
    
}
