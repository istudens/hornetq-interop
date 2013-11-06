package org.jboss.test.hornetqinterop;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

/**
 * TODO
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/inQueue")
})
public class ConsumerMDB implements MessageListener {

    @Resource(mappedName = "java:/JmsXA")
    private ConnectionFactory factory;

    private Connection connection;
    private Session session;

    @Override
    public void onMessage(Message message) {
        try {
            System.out.println("TADY Message: " + message);
            final Destination destination = message.getJMSReplyTo();
            System.out.println("message.getJMSReplyTo(): " + message.getJMSReplyTo());
            // ignore messages that need no reply
            if (destination == null)
                return;

            final MessageProducer replyProducer = session.createProducer(destination);
            final Message replyMsg = session.createTextMessage("replying " + ((TextMessage) message).getText());
            replyMsg.setJMSCorrelationID(message.getJMSMessageID());
            System.out.println("replyMsg: " + ((TextMessage) replyMsg).getText());
            replyProducer.send(replyMsg);
            replyProducer.close();
            System.out.println("sent");

        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    protected void preDestroy() throws JMSException {
        session.close();
        connection.close();
    }

    @PostConstruct
    protected void postConstruct() throws JMSException {
        connection = factory.createConnection();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

}
