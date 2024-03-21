# comp3151 Assignment 2
This project attempts to solve the producer consumer problem with `Java` using `Akka` library, which is based on the actor model. It is for educational purpose.

## Run Program
Follow https://gradle.org/install/ to install gradle if you haven't install it already. Then, run the following command.
```bash
cd AkkaDemo
gradle run
```

## Project structure
`src/main/java/org/pull` contains code that is demonstrated in the video. It uses a pull based approach to solve producer consumer problem. The idea is inspired by the poll-base backpressure mentioned in Akka Stream documentaion (https://doc.akka.io/docs/akka/current/stream/stream-flows-and-basics.html#back-pressure-explained). 

This code terminates when all consumers and producers terminates. The consumers terminate when they consume the number of tasks provided in the constructor, and the producers terminate when they produce the number of messages provided in the constructor. The default constructor for them set this number to `0`. When the number is `0`, the consumer / producer does not terminate.

You can changed the code in `lib\Processing.java` to change the `producing` function and `consuming` function, which change how the producer and consumer process.


`src/main/java/org/push` implements a push based approach. The buffer signals the producer that it fails to insert with a `Failure` message, and the producer will store the data it produced and stop producing the next data. We think this approach is not as good as the poll based approach, so this code is not as great as the other approach.

## Playing with the Code
You may change the parameters in `Main.java` to change the number of producers and consumers, and the number of tasks they perform. You may also set the buffer size as `0` to use `UnboundedBuffer` instead of `BoundedBuffer`.

## Assumptions
1. We can use limited amount of extra memory to store data for each producer and consumer (`producerBuffer` and `consumersQueue`) in our buffer
2. No actor crashes (e.g. raise excpetions) before it terminates
3. All messages arrives eventually

Note that `Akka` does not provide gurantees for assumptions 2 and 3. In practice, to ensure no data loss, we may 

- use the "persistence" module to ensure that the state can be reverted 

- Use "watch" function to monitor if other actors are down 

- Use timeout to prevent loss of messages and schedule a re-send after timeout 

- Use message id to identify duplicated messages (the process can ignore the new message with the same id)

## Properties of the Pull Based Solution
- A producer or a consumer can eventually produce or consume once the `Produce` or `Consume` is received by the buffer. 
- The producer sleeps when the buffer is full. The consumer sleeps when the buffer is empty.
- Producers can produce in parallel and consumers can consume in parallel.

## Notes
We did not use "persistence", "timeout" or "ask" in our programs since we think these are more complicated, and are not fundamental to actor model.
