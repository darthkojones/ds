package com.lesson2;

import java.util.Scanner;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import org.apache.activemq.ActiveMQConnectionFactory;

public class ChatClient {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: java ChatClient <myId> <otherId>");
            return;
        }

        String myId = args[0];
        String otherId = args[1];

        String myQueueName = "queue_" + myId;
        String otherQueueName = "queue_" + otherId;

        // JMS provider = ActiveMQ broker
        ConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        Connection connection = factory.createConnection();
        connection.start();

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        Queue myQueue = session.createQueue(myQueueName);
        Queue otherQueue = session.createQueue(otherQueueName);

        MessageProducer producer = session.createProducer(otherQueue);
        MessageConsumer consumer = session.createConsumer(myQueue);

        // Receiving thread
        Thread receivingThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Message msg = consumer.receive();
                    if (msg instanceof TextMessage) {
                        TextMessage tm = (TextMessage) msg;
                        String sender = tm.getStringProperty("sender");
                        String body = tm.getText();
                        System.out.println("[" + sender + "]: " + body);
                    }
                }
            } catch (javax.jms.JMSException e) {
                System.err.println("Error receiving message: " + e.getMessage());
            }
        });
        receivingThread.start();

        // Sending loop
        try (Scanner sc = new Scanner(System.in)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                TextMessage m = session.createTextMessage(line);
                m.setStringProperty("sender", myId);
                producer.send(m);
            }
        } finally {
            receivingThread.interrupt();
            consumer.close();
            producer.close();
            session.close();
            connection.close();
        }
    }
}
