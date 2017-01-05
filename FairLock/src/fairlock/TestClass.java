/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
 *
 * @author Gabriele
 */
public class TestClass {
    
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
            
            seed += 46856; // Result of 5d10 rolls.
        }
        
        @Override
        public void run() {
            for(int i = 0; i < 100; ++i) {
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
                    Thread.sleep(generator.nextInt(300));
                } catch (InterruptedException ex) {
                    
                }
                
                //System.out.println("Thread " + id + ": Releasing resource...");
                OPERATIONS.add(priority.toString() + "-" + id + "-Rel");
                
                manager.release();
            }
        }
        
    }
    
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
    
    private static void test(SingleResourceManager manager) {
        OPERATIONS.clear();
        
        Thread a1 = new ClientThread(PriorityClass.TYPE_A, manager);
        Thread a2 = new ClientThread(PriorityClass.TYPE_A, manager);
        Thread b1 = new ClientThread(PriorityClass.TYPE_B, manager);
        
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
    
    private static void testNoFair(SingleResourceManager manager) {
        OPERATIONS.clear();
        
        Thread a1 = new ClientThread(PriorityClass.TYPE_A, manager);
        Thread a2 = new ClientThread(PriorityClass.TYPE_A, manager);
        Thread b1 = new ClientThread(PriorityClass.TYPE_B, manager);
        
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
                System.out.println("NOTICE: this manager may suffer from spurious wakeups!");
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
