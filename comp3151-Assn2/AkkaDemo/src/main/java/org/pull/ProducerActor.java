WeChat: cstutorcs
QQ: 749389476
Email: tutorcs@163.com
package org.pull;

import org.lib.Processing;
import org.pull.BufferActor.BufferCommand;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;


public class ProducerActor extends AbstractBehavior<ProducerActor.Command> {
    /* protocols */
    public static interface Command {}

    public static enum RequestProduce implements Command {
        INSTANCE
    }

    /* local state */
    private final ActorRef<BufferCommand> buffer;
    private long msgId = 0;
    private final long nData; // number of data the producer will produce

    /* constructors */
    public static Behavior<Command> create(ActorRef<BufferCommand> buffer) {
        return Behaviors.setup(context -> new ProducerActor(context, buffer));
    }

    public static Behavior<Command> create(ActorRef<BufferCommand> buffer, long nData) {
        return Behaviors.setup(context -> new ProducerActor(context, buffer, nData));
    }

    private ProducerActor(ActorContext<Command> context, ActorRef<BufferCommand> buffer) {
        super(context);
        this.buffer = buffer;
        this.nData = 0; // 0 means infinite stream
    }

    private ProducerActor(ActorContext<Command> context, ActorRef<BufferCommand> buffer, long nData) {
        super(context);
        this.buffer = buffer;
        this.nData = Math.abs(nData);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
        .onMessage(RequestProduce.class, context -> this.requestProduce())
        .onSignal(PostStop.class, signal -> {
            getContext().getLog().info(
                "{} is terminated!", 
                getContext().getSelf().path().name()
            );
            return Behaviors.same();
        })
        .build();
    }

    /* Buffer reqeust new data from the producer */
    private Behavior<Command> requestProduce() {
        if (nData == 0 || msgId < nData) {
            // generate data
            String data = generateData();
            // insert to the buffer
            buffer.tell(new BufferActor.Produce(getContext().getSelf(), msgId, data));
            // update request id
            msgId++;
        } else {
            // signal the buffer that the producer has finished producing
            buffer.tell(new BufferActor.Finish(getContext().getSelf()));
            // we can terminate the process now as the consumers will not request
            // this producer anymore
            return Behaviors.stopped();
        }
        return this;
    }

    private String generateData() {
        String myName = getContext().getSelf().path().name();
        long producingTime = 1;
        String data = Processing.producing(myName, msgId, producingTime);
        getContext().getLog().info("Producer {} produced {}", getContext().getSelf().path(), data);
        return data;
    }
}