WeChat: cstutorcs
QQ: 749389476
Email: tutorcs@163.com
package org.push;

import java.util.HashMap;
import java.util.Map;

import org.push.BufferActor.BufferCommand;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;


/*
 * Problems with this protocol: 
 * - The ProducerActor needs to wait for a response from the buffer, which is 
 * basically blocking. It harms performance as Producer can actually produce
 *  in parallel instead of waiting and doing nothing.
 * - A lot of unneessary messages sending. The buffer needs to send a message to * reject the insertion for every insertion request when the buffer is full. The * producers needs to resend the message, which causes overhead.
 * Solutions to the above problems:
 * - We may use a poll-based protocol instead of a push-based protocol. Produce 
 * as much as the number of requests. And stop producing until new requests comes.
 * This solves the second problem as we don't have to resend and the buffer do 
 * not have to respond with a reject or accept.
 * - We may use another buffer for each producer for better parallelism. The 
 * producer blocks when its buffer when is full. The producer may still do nothing
 * while it can still produce. Yet, this situatuion is less likely when the rate
 * of producing and consuming is similar, as the producer can produce to the 
 * local buffer. We basically treat the buffer as the consumer of the extra 
 * buffers and the producer be the producer of its extra buffer.
 */
public class ProducerConsumer extends AbstractBehavior<ProducerConsumer.Command> {
    public interface Command {}
    public static enum RegisterProducer implements Command {
        INSTANCE
    }

    public static enum RegisterConsumer implements Command {
        INSTANCE
    }

    private ActorRef<BufferCommand> buffer;
    private Map<Long, ActorRef<ProducerActor.Command>> producers = new HashMap<>();
    private Map<Long, ActorRef<ConsumerActor.Msg>> consumers = new HashMap<>();

    public static Behavior<Command> create(long nProducers, long nConsumers, long bufferSize) {
        return Behaviors.setup(context -> new ProducerConsumer(context, nProducers, nConsumers, bufferSize));
    }

    private ProducerConsumer(ActorContext<Command> context, long nProducers, long nConsumers, long bufferSize) {
        super(context);
        nProducers = Math.max(1, nProducers);
        nConsumers = Math.max(1, nConsumers);
        bufferSize = Math.max(0, bufferSize);
        
        // spawn producer, consumer and buffer actors

        if (bufferSize == 0) {
            // spawn unbounded buffer when buffersize is 0
            buffer = getContext().spawn(UnboundedBuffer.create(), "unbounded-buffer");
        } else {
            buffer = getContext().spawn(BoundedBuffer.create(bufferSize), "bounded-buffer");
        }

        for (long i = 0; i < nProducers; i++) {
            ActorRef<ProducerActor.Command> newProducer = getContext().spawn(ProducerActor.create(buffer), "producer-" + i);
            producers.put(i, newProducer);
        }

        for (long i = 0; i < nConsumers; i++) {
            ActorRef<ConsumerActor.Msg> newConsumer = getContext().spawn(ConsumerActor.create(buffer), "consumer-" + i);
            consumers.put(i, newConsumer);
        }
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
        .onMessage(RegisterProducer.class, this::registerProducer)
        .onMessage(RegisterConsumer.class, this::registerConsumer)
        .build();
    }

    private Behavior<Command> registerProducer(RegisterProducer request) {
        long i = producers.size()-1;
        ActorRef<ProducerActor.Command> newProducer = getContext().spawn(ProducerActor.create(buffer), "producer-" + i);
        producers.put(i, newProducer);
        return this;
    }

    private Behavior<Command> registerConsumer(RegisterConsumer request) {
        long i = consumers.size()-1;
        ActorRef<ConsumerActor.Msg> newConsumer = getContext().spawn(ConsumerActor.create(buffer), "consumer-" + i);
        consumers.put(i, newConsumer);
        return this;
    }
}
