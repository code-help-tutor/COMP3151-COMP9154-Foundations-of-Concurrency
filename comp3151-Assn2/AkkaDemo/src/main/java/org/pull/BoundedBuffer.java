WeChat: cstutorcs
QQ: 749389476
Email: tutorcs@163.com
package org.pull;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;

// this implementation is inspired by the Merge class in Akka Stream, which also
// uses a buffer for each inputPort
// we are basically putting a Merge between a buffer and the producers
// reference: https://github.com/akka/akka/blob/afe4a08133f907066b74d00167888c94936481c8/akka-stream/src/main/scala/akka/stream/scaladsl/Graph.scala?fbclid=IwAR2rgASpa4W7yzb_q49RKL-z9Qq0BLbr-ZfVxhqLzD0HDjDKfyI6X8-E-ds#L89

public class BoundedBuffer extends BufferActor {

    // we are going to use more memory than maxSize in order to keep track of 
    // the producers and consumers that are ready

    // stores the actual data
    private final Queue<String> buffer = new ArrayDeque<>();

    // keep track of the producers that have already produced and are 
    // ready for the next production
    // we use LinkedHashMap as we also want to keep track of the order
    private final LinkedHashMap<ActorRef<ProducerActor.Command>, String> 
        producerBuffer = new LinkedHashMap<>();

    // keep track of the consumers that are ready for receiving data
    private final Queue<ActorRef<ConsumerActor.Msg>> consumersQueue = new 
        ArrayDeque<>();

    private final long maxSize;

    /* constructor */
    public static Behavior<BufferActor.BufferCommand> create(long bufferSize) {
        return Behaviors.setup(context -> new BoundedBuffer(context, bufferSize));
    }

    private BoundedBuffer(ActorContext<BufferCommand> context, long bufferSize) {
        super(context);
        this.maxSize = bufferSize;
    }

    // Note:
    // we will send request to the producer if the producer is ready and
    // the buffer is not full

    @Override
    protected Behavior<BufferCommand> onConsume(Consume request) {
        // similar to unbounded buffer
        if (buffer.isEmpty()) {
            consumersQueue.add(request.consumer);
        } else {
            String data = buffer.poll();
            request.consumer.tell(new ConsumerActor.DataMsg(data));
            if (!producerBuffer.isEmpty()) {
                // get data from a producerBuffer to fill up the empty slot in the
                // buffer and request that producer to produce the next data
                Map.Entry<ActorRef<ProducerActor.Command>, String> firstEntry = poll(producerBuffer);
                // nextProducer is ready for the next request
                ActorRef<ProducerActor.Command> nextProducer = firstEntry.getKey();
                nextProducer.tell(ProducerActor.RequestProduce.INSTANCE);
                buffer.add(firstEntry.getValue());
            }
            // if producerBuffer is empty, then no producer is ready for the next
            // request, so we don't send a request
        }

        return this;
    }

    @Override
    protected Behavior<BufferCommand> onProduce(Produce request) {
        if (consumersQueue.isEmpty()) {
            if (isBufferFull()) {
                // the producer is ready, but the buffer is full ->
                // put it in producerBuffer
                producerBuffer.put(request.producer, request.data);
            } else {
                buffer.add(request.data);
                // the producer is ready and buffer is not full
                // send request to the producer
                request.producer.tell(ProducerActor.RequestProduce.INSTANCE);
            }
        } else {
            // send data to a consumer
            consumersQueue.poll().tell(new ConsumerActor.DataMsg(request.data));
            // buffer is empty (if it is not, then the consumerQueue will be empty)
            // and the producer is readys
            request.producer.tell(ProducerActor.RequestProduce.INSTANCE);
        }
        return this;
    }
    
    private boolean isBufferFull() {
        return buffer.size() >= maxSize;
    }

    // get the first entry and remove it from the map
    // Note: LinkedHashMap maintains the insertion order
    private <K, V> Map.Entry<K,V> poll(LinkedHashMap<K,V> map) {
        Map.Entry<K,V> firstEntry = map.entrySet().iterator().next();
        map.remove(firstEntry.getKey());
        return firstEntry;
    }

    @Override
    protected Behavior<BufferCommand> onRegisterProducer(RegisterProducer request) {
        // the producer must be ready, so the buffer requests the producer
        request.producer.tell(ProducerActor.RequestProduce.INSTANCE);
        return this;
    }

    // We don't actually need the Finish protocol
    // Yet, Finish may be useful to distinguish whether the case that the producer
    // has completed its tasks, or the case that the producer has crashed suddenly
    @Override
    protected Behavior<BufferCommand> onFinish(Finish request) {
        // As long as we don't get a Produce from a producer, we are not going to
        // request the producer again.
        return this;
    }

}
