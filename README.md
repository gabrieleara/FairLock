# FairLock.java #

A Java Lock implementation that processes requests in a strictly FIFO order, following the _signal-and-urgent_ semantic.

## The assignement

This project has beed developed for the Concurrent and Distributed Systems class of Embedded Computing Systems Master degree at Scuola Sant'Anna of Pisa.

The original assignement (part of it) is the following:

> Implement a synchronization mechanism similar to the mechanism provided by java within the java.util.concurrent packages (Explicit Locks and Condition variables) but whose behaviour is in accordance with the semantic "signal-and-urgent".

The _signal-and-urgent_ semantic referred here is the one defined by [C.A.R. Hoare][1].

## Usage

The usage of this class is illustrated in the [manager][2] subfolder, in which we implement a Single Resource Manager class following different patterns.

Anyway its usage is basically the same of the standard Java [Lock][3] class, except for the different implementation. An associated [Condition][4] class is also provided, exactly like the original Lock class.

```
FairLock l = new FairLock();
l.lock();
try {
   // access the resource protected by this lock
} finally {
   l.unlock();
}
```

## Running the tests

This project contains also a class defined to test the correct behavior of the FairLock class and the Managers defined with it.
You can find it under the [test][5] subfolder. See Javadoc for further information.

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details

## Acknowledgments

A great acknowledgement to professor Paolo Ancilotti, who held for us this class and explained us with such patience all the threats to a distributed or concurrent system and how to handle them.

[1]:http://doi.acm.org/10.1145/355620.361161
[2]:https://github.com/gabrieleara/FairLock/tree/master/FairLock/src/manager
[3]:https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/locks/Lock.html
[4]:https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/locks/Condition.html
[5]:https://github.com/gabrieleara/FairLock/blob/master/FairLock/src/test/
