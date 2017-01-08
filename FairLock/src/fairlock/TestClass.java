package fairlock;

import fairlock.SingleResourceManager.PriorityClass;
import java.awt.AWTException;
import java.awt.Robot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Class used to test the {@link SingleResourceManager} implemnentations
 * provided in this package (and the {@link FairLock} class, in the case of the
 * corresponding Manager.
 * 
 * @author Gabriele Ara
 */
public class TestClass {
    
    /**
     * If the main is executed in Netbeans (c) console, this method clears the
     * screen.
     */
    private static void clearNetbeansConsole() {
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
     * <p>Each ClientThread executes a certain number of request/release in a
     * loop. Between sequential operations, random delays are added in order to
     * simulate operations on a real resource.</p>
     * 
     * <p>Every operation performed by the client is added to a trace that can
     * later be analyzed automatically.</p>
     * 
     * <p>At the end of the test, the user is asked if he/she wants to print the
     * trace analyzed or not.</p>
     * 
     * @see TestClass#test(fairlock.SingleResourceManager) 
     * @see TestClass#testNoFair(fairlock.SingleResourceManager) 
     */
    public static class ClientThread extends Thread {
        private static long seed = System.nanoTime();
        private static int nextId = 1;
        
        private final PriorityClass priority;
        private final SingleResourceManager manager;
        private final Random generator;
        private final int id;

        public ClientThread(PriorityClass priority, SingleResourceManager manager) {
            this.priority = priority;
            this.manager = manager;
            id = nextId++;
            
            generator = new Random(seed);
            
            seed += 46856; // A "random" increment for the seed
        }
        
        @Override
        public void run() {
            for(int i = 0; i < 1000; ++i) {
                try {
                    Thread.sleep(generator.nextInt(100));
                } catch (InterruptedException ex) {
                    
                }
                
                //System.out.println("Thread " + id + ": Starting a request...");
                OPERATIONS.add(priority.toString() + "-" + id + "-Req");
                
                manager.request(priority);
                
                //System.out.println("Thread " + id + ": Using resource...");
                OPERATIONS.add(priority.toString() + "-" + id + "-Use");
                
                try {
                    Thread.sleep(generator.nextInt(200));
                } catch (InterruptedException ex) {
                    
                }
                
                //System.out.println("Thread " + id + ": Releasing resource...");
                OPERATIONS.add(priority.toString() + "-" + id + "-Rel");
                
                manager.release();
            }
        }
        
    }
    
    // Trace used to analyze the execution of a set of threads
    private static final List<String> OPERATIONS = Collections.synchronizedList(new ArrayList<String>());
    
    private static final Scanner SCANNER = new Scanner(System.in);
    
    private static final String PATTERN_A = "^\\s*[Aa]\\s*$";
    private static final String PATTERN_B = "^\\s*[Bb]\\s*$";
    private static final String PATTERN_C = "^\\s*[Cc]\\s*$";
    private static final String PATTERN_Y = "^\\s*[Yy]\\s*$";
    
    private static void printTrace() {
        printTrace(OPERATIONS.size());
    }
    
    private static void printTrace(int max) {
        for(int i = 0; i < max; ++i) {
            System.out.println(OPERATIONS.get(i));
        }
    }
    
    /**
     * Performs a test on the given manager. To do so, it creates three threads
     * (two with a priority equal to {@link PriorityClass#PRIO_A} and one with a
     * priority equal to {@link PriorityClass#PRIO_B}) that execute a certain
     * amount of operations on the resource.
     * 
     * <p>If the set goes in deadlock, the testing program experiences a
     * deadlock too and trace of operations executed is not printed. To print
     * operations as soon as they are executed, uncomment the corresponding
     * lines in {@link ClientThread#run()}.</p>
     * 
     * <p>If the three threads terminate each their execution, this method
     * checks if the produced trace was valid, in terms of monitor consistency
     * and FIFO ordering. For a test that doesn't take into account FIFO
     * ordering of requests see
     * {@link #testNoFair(fairlock.SingleResourceManager) testNoFair}.</p>
     * 
     * <p>At the end of the test, the user is asked if he/she wants to print the
     * trace analyzed or not.</p>
     * 
     * @param manager the manager that needs to be tested
     */
    private static void test(SingleResourceManager manager) {
        OPERATIONS.clear();
        
        Thread a1 = new ClientThread(PriorityClass.PRIO_A, manager);
        Thread a2 = new ClientThread(PriorityClass.PRIO_A, manager);
        Thread b1 = new ClientThread(PriorityClass.PRIO_B, manager);
        
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
        
        System.out.println("Test finished! Checking if everything is fine...");
        System.out.println();
        
        boolean free = true;
        boolean used = false;
        
        int owner = -1;
        
        ArrayList<Integer> waitA = new ArrayList<>();
        ArrayList<Integer> waitB = new ArrayList<>();
        
        for(int i = 0; i < OPERATIONS.size(); ++i) {
            String[] op = OPERATIONS.get(i).split("-");
            
            switch(op[2]) {
                case "Req":
                    if(free) {
                        owner = Integer.parseInt(op[1]);
                        free = false;
                        used = false;
                        continue;
                    }
                    
                    if(Integer.parseInt(op[1]) == owner) {
                        System.out.println("Property violation! Printing the trace...");
                        System.out.println();
                        printTrace(i+1);
                        return;
                    }
                    
                    switch(op[0].charAt(0)) {
                        case 'A':
                            waitA.add(Integer.parseInt(op[1]));
                            break;
                        case 'B':
                            waitB.add(Integer.parseInt(op[1]));
                            break;
                    }
                    break;
                case "Rel":
                    if(free || !used || Integer.parseInt(op[1]) != owner) {
                        System.out.println("Property violation! Printing the trace...");
                        System.out.println();
                        printTrace(i+1);
                        return;
                    }
                    
                    if(waitB.size() > 0) {
                        owner = waitB.remove(0);
                        used = false;
                    } else if (waitA.size() > 0) {
                        owner = waitA.remove(0);
                        used = false;
                    } else {
                        used = false;
                        free = true;
                        owner = -1;
                    }
                    break;
                case "Use":
                    if(free || used || Integer.parseInt(op[1]) != owner) {
                        System.out.println("Property violation! Printing the trace...");
                        System.out.println();
                        printTrace(i+1);
                        return;
                    }
                    
                    used = true;
                    
                    break;
                default:
                    System.out.println("Unrecognized operation! Printing the trace...");
                    System.out.println();
                    printTrace(i+1);
                    return;
            }
        }
        
        System.out.println("Seems everything was fine!");
        System.out.print("Do you want to print out trace? (Y/n) ");
        
        String input = SCANNER.next();
        
        if(Pattern.matches(PATTERN_Y, input)) {
            System.out.println();
            printTrace();
        }
            
    }
    
    /**
     * Performs a test on the given manager. To do so, it creates three threads
     * (two with a priority equal to {@link PriorityClass#PRIO_A} and one with a
     * priority equal to {@link PriorityClass#PRIO_B}) that execute a certain
     * amount of operations on the resource.
     * 
     * <p>If the set goes in deadlock, the testing program experiences a
     * deadlock too and trace of operations executed is not printed. To print
     * operations as soon as they are executed, uncomment the corresponding
     * lines in {@link ClientThread#run()}.</p>
     * 
     * <p>If the three threads terminate each their execution, this method
     * checks if the produced trace was valid, in terms of monitor consistency
     * only. For a test that takes into account FIFO
     * ordering of requests too, see
     * {@link #test(fairlock.SingleResourceManager) test}.</p>
     * 
     * @param manager the manager that needs to be tested
     */
    private static void testNoFair(SingleResourceManager manager) {
        OPERATIONS.clear();
        
        Thread a1 = new ClientThread(PriorityClass.PRIO_A, manager);
        Thread a2 = new ClientThread(PriorityClass.PRIO_A, manager);
        Thread b1 = new ClientThread(PriorityClass.PRIO_B, manager);
        
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
        
        System.out.println("Test finished! Checking if everything is fine...");
        System.out.println();
        
        boolean free = true;
        boolean used = false;
        
        String ownerType = null;
        int owner = -1;
        int counterA = 0;
        int counterB = 0;
        
        for(int i = 0; i < OPERATIONS.size(); ++i) {
            String[] op = OPERATIONS.get(i).split("-");
            
            switch(op[2]) {
                case "Req":
                    if(free) {
                        ownerType = op[0];
                        owner = Integer.parseInt(op[1]);
                        free = false;
                        used = false;
                        continue;
                    }
                    
                    switch(op[0].charAt(0)) {
                        case 'A':
                            ++counterA;
                            break;
                        case 'B':
                            ++counterB;
                            break;
                    }
                    break;
                case "Rel":
                    if(free || !used || !op[0].equals(ownerType)) {
                        System.out.println("Property violation! Printing the trace...");
                        System.out.println();
                        printTrace(i+1);
                        return;
                    }
                    
                    if(counterB > 0) {
                        --counterB;
                        ownerType = "B";
                        owner = -1;
                        used = false;
                    } else if (counterA > 0) {
                        --counterA;
                        ownerType = "A";
                        owner = -1;
                        used = false;
                    } else {
                        used = false;
                        free = true;
                        ownerType = null;
                    }
                    break;
                case "Use":
                    if(free || used || !ownerType.equals(op[0]) || ( owner > 0 && owner != Integer.parseInt(op[1]))) {
                        System.out.println("Property violation! Printing the trace...");
                        System.out.println();
                        printTrace(i+1);
                        return;
                    }
                    owner = Integer.parseInt(op[1]);
                    used = true;
                    
                    break;
                default:
                    System.out.println("Unrecognized operation! Printing the trace...");
                    System.out.println();
                    printTrace(i+1);
                    return;
            }
        }
        
        System.out.println("Seems everything was fine!");
        System.out.print("Do you want to print out trace? (Y/n) ");
        
        String input = SCANNER.next();
        
        if(Pattern.matches(PATTERN_Y, input)) {
            System.out.println();
            printTrace();
        }
            
    }
    
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
                System.out.println("NOTICE: this might take a while...");
                System.out.println();
                
                test(new SingleResourceManagerFairLock());
                
                System.out.println();
                
            } else if (Pattern.matches(PATTERN_B, input)) {
                clearNetbeansConsole();
                
                System.out.println("Starting testing of the manager B...");
                System.out.println("NOTICE: this might take a while...");
                System.out.println();
                
                test(new SingleResourceManagerLock());
                
                System.out.println();
            } else if (Pattern.matches(PATTERN_C, input)) {
                clearNetbeansConsole();
                
                System.out.println("Starting testing of the manager C...");
                System.out.println("NOTICE: this might take a while...");
                System.out.println();
                
                testNoFair(new SingleResourceManagerFSM());
                
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
