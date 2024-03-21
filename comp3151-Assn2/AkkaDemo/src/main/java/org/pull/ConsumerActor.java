WeChat: cstutorcs
QQ: 749389476
Email: tutorcs@163.com
package org.pull;

import org.lib.Processing;

import akka.actor.typed.PostStop;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

public class ConsumerActor extends AbstractBehavior<ConsumerActor.Msg> {
    public static interface Msg {}
    
    /* a wrapper of the actual data */
    public static class DataMsg implements Msg {
        public final String data;
        public DataMsg(String data) {
            this.data = data;
        }
    }

    private final ActorRef<BufferActor.BufferCommand> buffer;
    private final long nConsume;
    private long nConsumed = 0;

    private ConsumerActor(ActorContext<Msg> context, ActorRef<BufferActor.BufferCommand> buffer, long nConsume) {
        super(context);
        this.buffer = buffer;
        this.nConsume = Math.max(0, nConsume);
        requestConsume(); 
        // the consumer is ready at start up 
        // as it is not doing anything at the moment
    }

    private ConsumerActor(ActorContext<Msg> context, ActorRef<BufferActor.BufferCommand> buffer) {
        this(context, buffer, 0);
    }

    @Override
    public Receive<Msg> createReceive() {
        return newReceiveBuilder()
          .onMessage(DataMsg.class, this::receiveMsg)
          .onSignal(PostStop.class, this::onStop)
          .build();
    }

    private Behavior<Msg> receiveMsg(DataMsg msg) {
        nConsumed++;
        if (nConsume == 0 || nConsumed < nConsume) {
            proceess(msg.data);
            requestConsume(); // consumer is ready for the next message
        } else {
            return Behaviors.stopped();
        }
        return this;
    }

    private void requestConsume() {
        buffer.tell(new BufferActor.Consume(getContext().getSelf()));
    }

    private void proceess(String data) {
        // some complex calculations
        long processingTime = 2;
        String result = Processing.consuming(getContext().getSelf().path().name(), data, processingTime);
        getContext().getLog().info("Consumer {} consumes the data from {}.", getContext().getSelf().path(), result);
    }

    public static Behavior<ConsumerActor.Msg> create(ActorRef<BufferActor.BufferCommand> buffer) {
        return Behaviors.setup(context -> new ConsumerActor(context, buffer));
    }

    public static Behavior<ConsumerActor.Msg> create(ActorRef<BufferActor.BufferCommand> buffer, long nConsume) {
        return Behaviors.setup(context -> new ConsumerActor(context, buffer, nConsume));
    }

    private Behavior<ConsumerActor.Msg> onStop(PostStop signal) {
        getContext().getLog().info("{} is terminated!", getContext().getSelf().path());
        return Behaviors.same();
    }
}
