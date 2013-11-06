hornetq-interop
===============

HornetQ interoperability EAP5 vs. EAP6

MessageSenderServlet sends a message from WildFly to EAP5 server using EAP5's HornetQ resource adapter on the WildFly side.

Steps to run:
1. set up a /queue/outQueue in EAP5, e.g. <EAP5_HOME>/server/all/deploy/hornetq/hornetq-jms.xml
      <queue name="outQueue">
         <entry name="/queue/outQueue"/>
      </queue>
2. start up EAP5 server on a different IP than the WildFly server
3. export JBOSS_HOME=<path to WildFly>
4. mvn test -Deap5.host=<IP which EAP5 server is running on>


It fails on a test #cleanup() method right now due to a Arquillian conf issue, but who cares :)
