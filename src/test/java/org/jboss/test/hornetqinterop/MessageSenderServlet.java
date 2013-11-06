package org.jboss.test.hornetqinterop;

import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

/**
 * This sends a message to an EAP5 server.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
@WebServlet(name = "MessageSenderServlet", urlPatterns = "/" + MessageSenderServlet.URL_PATTERN)
public class MessageSenderServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    static final String URL_PATTERN = "messagesender";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String msg = request.getParameter("msg");
        String destinationName = request.getParameter("destinationName");
        String eap5Host = request.getParameter("eap5Host");
        System.out.println("msg: " + msg);
        System.out.println("destinationName: " + destinationName);
        System.out.println("eap5Host: " + eap5Host);

        try {
            final InitialContext ctx = new InitialContext();
            final QueueConnectionFactory connectionFactory = (QueueConnectionFactory) ctx.lookup("java:/HornetQEAP5RA");
            final QueueConnection connection = connectionFactory.createQueueConnection();
            connection.start();
            final QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            final Queue replyDestination = session.createTemporaryQueue();
            final QueueReceiver receiver = session.createReceiver(replyDestination);

            final Message message = session.createTextMessage(msg);
            message.setJMSReplyTo(replyDestination);
            final Context eap5Context = createEAP5InitialContext(eap5Host);
            final Destination destination = (Destination) eap5Context.lookup(destinationName);
            final MessageProducer messageProducer = session.createProducer(destination);
            messageProducer.send(message);
            messageProducer.close();

            final Message reply = receiver.receive(5 * 1000);
            final String result = (reply != null) ? ((TextMessage) reply).getText() : "no reply";
            System.out.println("result: " + result);

            response.setContentType("text/plain");
            PrintWriter out = response.getWriter();
            out.print(result);
            out.close();

            session.close();
            connection.close();
            ctx.close();
            eap5Context.close();

        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw new ServletException(e);
        }
    }

    private Context createEAP5InitialContext(final String host) throws NamingException, IOException {
        final String port = "1099";
        final String user = "guest";
        final String pass = "guest";

        Properties properties = new Properties();
        properties.put(Context.INITIAL_CONTEXT_FACTORY, ModuleInitialContextFactory.class.getName());
        properties.put(ModuleInitialContextFactory.MODULE_NAME, "eap5-client");
        properties.put(ModuleInitialContextFactory.MODULE_INITIAL_CONTEXT_FACTORY, "org.jboss.security.jndi.JndiLoginInitialContextFactory");
        properties.put(Context.URL_PKG_PREFIXES, "org.jboss.naming:org.jnp.interfaces");
        properties.put(Context.PROVIDER_URL, "jnp://"+ host +":"+port);
        properties.put(Context.SECURITY_PRINCIPAL, user);
        properties.put(Context.SECURITY_CREDENTIALS, pass);

        return new InitialContext(properties);
    }

}
