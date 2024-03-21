WeChat: cstutorcs
QQ: 749389476
Email: tutorcs@163.com
package org.push;

import java.math.BigInteger;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

public class ConsumerActor extends AbstractBehavior<ConsumerActor.Msg> {
    public static interface Msg {}
    
    public static class DataMsg implements Msg {
        public final long data;
        public DataMsg(long data) {
            this.data = data;
        }
    }

    private final ActorRef<BufferActor.BufferCommand> buffer;

    private ConsumerActor(ActorContext<Msg> context, ActorRef<BufferActor.BufferCommand> buffer) {
        super(context);
        this.buffer = buffer;
        requestConsume();
    }

    @Override
    public Receive<Msg> createReceive() {
        return newReceiveBuilder()
          .onMessage(DataMsg.class, this::receiveMsg)
          .build();
    }

    private Behavior<Msg> receiveMsg(DataMsg msg) {
        proceess(msg.data);
        requestConsume();
        return this;
    }

    private void requestConsume() {
        buffer.tell(new BufferActor.Consume(getContext().getSelf()));
    }

    private void proceess(long data) {
        getContext().getLog().info("Consumer {} is processing data: {}", getContext().getSelf().path(), data);
        // some complex calculations
        BigInteger result = BigInteger.ZERO;
        for (int i = 0; i <= data; i++) {
            result = result.add(BigInteger.valueOf(i).pow(3));
        }
        getContext().getLog().info("Consumer {} completes processing! The result is {}.", getContext().getSelf().path(), result);
    }

    public static Behavior<ConsumerActor.Msg> create(ActorRef<BufferActor.BufferCommand> buffer) {
        return Behaviors.setup(context -> new ConsumerActor(context, buffer));
    }

}
