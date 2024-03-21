WeChat: cstutorcs
QQ: 749389476
Email: tutorcs@163.com
package org.push;

import java.util.ArrayDeque;
import java.util.Queue;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;

public class BoundedBuffer extends BufferActor {

    private final Queue<Long> buffer = new ArrayDeque<>();
    private final Queue<ActorRef<ConsumerActor.Msg>> consumersQueue = new ArrayDeque<>();
    private final Queue<ActorRef<ProducerActor.Command>> producersQueue = new ArrayDeque<>();
    private final long maxSize;

    public static Behavior<BufferActor.BufferCommand> create(long bufferSize) {
        return Behaviors.setup(context -> new BoundedBuffer(context, bufferSize));
    }

    private BoundedBuffer(ActorContext<BufferCommand> context, long bufferSize) {
        super(context);
        this.maxSize = bufferSize;
    }

    @Override
    protected Behavior<BufferCommand> onConsume(Consume request) {
        // same as unbounded buffer
        if (buffer.isEmpty()) {
            consumersQueue.add(request.consumer);
        } else {
            request.consumer.tell(new ConsumerActor.DataMsg(buffer.poll()));
        }

        // wake up all rejected producers
        for (ActorRef<ProducerActor.Command> producer: producersQueue) {
            producer.tell(ProducerActor.RequestProduce.INSTANCE);
        }

        // // wake up one of the rejected producer 
        // // (may wake up a producer that never terminates/crashed producer)
        // if (!producersQueue.isEmpty()) {
        //     producersQueue.poll().tell(ProducerActor.RequestProduce.INSTANCE);
        // }

        return this;
    }

    @Override
    protected Behavior<BufferCommand> onProduce(Produce request) {
        if (isBufferFull()) {
            // reject as the buffer is full
            request.producer.tell(ProducerActor.ProduceResponse.REJECT);
            producersQueue.add(request.producer);
        } else {
            buffer.add(request.data);
            request.producer.tell(ProducerActor.ProduceResponse.ACCEPT);
        }
        return this;
    }
    
    private boolean isBufferFull() {
        return buffer.size() >= maxSize;
    }
}
