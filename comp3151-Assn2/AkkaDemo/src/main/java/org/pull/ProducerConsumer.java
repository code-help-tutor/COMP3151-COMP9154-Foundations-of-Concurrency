WeChat: cstutorcs
QQ: 749389476
Email: tutorcs@163.com
package org.pull;

import org.pull.BufferActor.BufferCommand;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.*;

/*
 * Poll-based algorithm: Buffer request from the producer. We need to register 
 * producers to the buffer so the buffer can request the producer.
 */
public class ProducerConsumer extends AbstractBehavior<ProducerConsumer.Command> {
    /* communication protocols */
    public interface Command {}
    public static class RegisterProducer implements Command {
        public long nProduce;
        public RegisterProducer(long nProduce) {
            this.nProduce = nProduce;
        }
    }

    public static class RegisterConsumer implements Command {
        public long nConsume;
        public RegisterConsumer(long nConsume) {
            this.nConsume = nConsume;
        }
    }

    /* local state */
    private ActorRef<BufferCommand> buffer;
    private long nextProducerId = 0;
    private long nextConsumerId = 0;
    private long deficit = 0;

    /* constructor */
    public static Behavior<Command> create(long nProducers, long nConsumers, long bufferSize) {
        return Behaviors.setup(context -> new ProducerConsumer(context, nProducers, nConsumers, bufferSize));
    }

    private ProducerConsumer(ActorContext<Command> context, long nProducers, long nConsumers, long bufferSize) {
        super(context);
        nProducers = Math.max(0, nProducers);
        nConsumers = Math.max(0, nConsumers);
        bufferSize = Math.max(0, bufferSize);
        
        // spawn producer, consumer and buffer actors

        if (bufferSize == 0) {
            // spawn unbounded buffer when buffersize is 0
            buffer = getContext().spawn(UnboundedBuffer.create(), "unbounded-buffer");
        } else {
            buffer = getContext().spawn(BoundedBuffer.create(bufferSize), "bounded-buffer");
        }

        for (; nextProducerId < nProducers; nextProducerId++) {
            registerProducer();
        }

        for (; nextConsumerId < nConsumers; nextConsumerId++) {
            registerConsumer();
        }
    }

    private ProducerConsumer(ActorContext<Command> context, long bufferSize) {
        this(context, 0, 0, bufferSize);
    }

    /* pattern matching on request and call the corresponding handler function */
    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
        .onMessage(RegisterProducer.class, this::registerProducer)
        .onMessage(RegisterConsumer.class, this::registerConsumer)
        .onSignal(Terminated.class, this::onTerminated)
        .onSignal(PostStop.class, this::onStop)
        .build();
    }

    /* create producer and register it to the buffer */
    private Behavior<Command> registerProducer(RegisterProducer request) {
        return registerProducer(request.nProduce);
    }

    private Behavior<Command> registerProducer() {
        return registerProducer(0); 
        // 0 means the producers will produce forever
    }

    private Behavior<Command> registerProducer(long nProduce) {
        ActorRef<ProducerActor.Command> newProducer;
        if (nProduce <= 0) {
            newProducer = getContext().spawn(ProducerActor.create(buffer), "producer-" + nextProducerId);
        } else {
            newProducer = getContext().spawn(ProducerActor.create(buffer, nProduce), "producer-" + nextProducerId);
        }
        return registerProducer(newProducer);
    }

    private Behavior<Command> registerProducer(ActorRef<ProducerActor.Command> newProducer) {
        buffer.tell(new BufferActor.RegisterProducer(newProducer));
        getContext().watch(newProducer);
        nextProducerId++;
        deficit++;
        return this;
    }

    /* create a consumer, which consumes from the buffer */
    private Behavior<Command> registerConsumer(RegisterConsumer request) {
        return registerConsumer(request.nConsume);
    }

    private Behavior<Command> registerConsumer(ActorRef<ConsumerActor.Msg> newConsumer) {
        getContext().watch(newConsumer);
        nextConsumerId++;
        deficit++;
        return this;
    }

    private Behavior<Command> registerConsumer() {
        return registerConsumer(0);
    }

    private Behavior<Command> registerConsumer(long nConsume) {
        ActorRef<ConsumerActor.Msg> newConsumer;
        if (nConsume <= 0) {
            newConsumer = getContext().spawn(ConsumerActor.create(buffer), "consumer-" + nextConsumerId);
        } else {
            newConsumer = getContext().spawn(ConsumerActor.create(buffer, nConsume), "consumer-" + nextConsumerId);
        }
        return registerConsumer(newConsumer);
    }

    /* when producer/consumer terminated */
    private Behavior<Command> onTerminated(Terminated signal) {
        deficit--;
        if (deficit <= 0) {
            return Behaviors.stopped();
        }
        return this;
    }

    /* when this actor stops */
    private Behavior<Command> onStop(PostStop signal) {
        getContext().getLog().info("Program completes!");
        return Behaviors.same();
    }
}
