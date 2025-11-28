package com.lesson2;

import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import org.apache.activemq.ActiveMQConnectionFactory;

public class ChatClient {

    private static final Set<String> blockedUsers = new HashSet<>();

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
        // Enable persistent delivery for offline messaging
        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
        
        MessageConsumer consumer = session.createConsumer(myQueue);

        System.out.println("Chat started. Commands:");
        System.out.println("  /block <user>   - Block a user");
        System.out.println("  /unblock <user> - Unblock a user");
        System.out.println("  /list           - List blocked users");

        // Receiving thread
        Thread receivingThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Message msg = consumer.receive();
                    if (msg instanceof TextMessage) {
                        TextMessage tm = (TextMessage) msg;
                        String sender = tm.getStringProperty("sender");
                        String body = tm.getText();
                        
                        // Check if sender is blocked
                        synchronized (blockedUsers) {
                            if (blockedUsers.contains(sender)) {
                                // Silently ignore messages from blocked users
                                continue;
                            }
                        }
                        
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
                
                // Handle commands
                if (line.startsWith("/block ")) {
                    String userToBlock = line.substring(7).trim();
                    synchronized (blockedUsers) {
                        blockedUsers.add(userToBlock);
                    }
                    System.out.println("Blocked user: " + userToBlock);
                    continue;
                } else if (line.startsWith("/unblock ")) {
                    String userToUnblock = line.substring(9).trim();
                    synchronized (blockedUsers) {
                        blockedUsers.remove(userToUnblock);
                    }
                    System.out.println("Unblocked user: " + userToUnblock);
                    continue;
                } else if (line.equals("/list")) {
                    synchronized (blockedUsers) {
                        if (blockedUsers.isEmpty()) {
                            System.out.println("No blocked users.");
                        } else {
                            System.out.println("Blocked users: " + blockedUsers);
                        }
                    }
                    continue;
                }
                
                // Send regular message with persistent delivery
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
