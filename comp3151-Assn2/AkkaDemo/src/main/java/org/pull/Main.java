WeChat: cstutorcs
QQ: 749389476
Email: tutorcs@163.com
package org.pull;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;

public class Main {
    public static void main(String[] args) {
        ActorRef<ProducerConsumer.Command> producerConsumerActor = ActorSystem.create(ProducerConsumer.create(0,0, 10), "producer-consumer");
        producerConsumerActor.tell(new ProducerConsumer.RegisterProducer(5));
        producerConsumerActor.tell(new ProducerConsumer.RegisterProducer(10));
        producerConsumerActor.tell(new ProducerConsumer.RegisterConsumer(15));
    }
}
