WeChat: cstutorcs
QQ: 749389476
Email: tutorcs@163.com
package org.push;

import java.util.Random;

import org.push.BufferActor.BufferCommand;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class ProducerActor extends AbstractBehavior<ProducerActor.Command> {
    public static interface Command {}

    public static enum ProduceResponse implements Command {
        ACCEPT,
        REJECT
    }

    public static enum RequestProduce implements Command {
        INSTANCE
    }

    private final ActorRef<BufferCommand> buffer;
    private long reqeustId = 0;
    private long data;

    public static Behavior<Command> create(ActorRef<BufferCommand> buffer) {
        return Behaviors.setup(context -> new ProducerActor(context, buffer));
    }

    private ProducerActor(ActorContext<Command> context, ActorRef<BufferCommand> buffer) {
        super(context);
        this.buffer = buffer;
        produceNext();
        insertBuffer(); // try to insert to the buffer when started
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
        .onMessageEquals(ProduceResponse.ACCEPT, this::accept)
        .onMessageEquals(ProduceResponse.REJECT, this::reject)
        .onMessage(RequestProduce.class, context -> this.requestProduce())
        .build();
    }

    private void produceNext() {
        this.reqeustId += 1;
        getContext().getLog().info("Producer {} is producing...", getContext().getSelf().path());
        this.data = new Random().nextLong();
        getContext().getLog().info("Producer {} produced {}", getContext().getSelf().path(), this.data);
    }

    private Behavior<Command> accept() {
        // last request is accepted, produce the next thing
        produceNext();
        insertBuffer();
        return this;
    }

    private Behavior<Command> reject() {
        // do nothing, wake up until the buffer requestProduce
        return this;
    }

    private Behavior<Command> requestProduce() {
        insertBuffer(); // re-insert the last element since we failed last time
        return this;
    }

    private void insertBuffer() {
        buffer.tell(new BufferActor.Produce(getContext().getSelf(), this.reqeustId , this.data));
    }

}