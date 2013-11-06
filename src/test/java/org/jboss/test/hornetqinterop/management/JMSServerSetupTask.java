package org.jboss.test.hornetqinterop.management;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;

/**
 * TODO
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public class JMSServerSetupTask implements ServerSetupTask {
    private HornetQJMSOperations jmsAdminOperations;

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        jmsAdminOperations = new HornetQJMSOperations(managementClient);
        jmsAdminOperations.createJmsQueue("inQueue", "queue/inQueue");
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        if (jmsAdminOperations != null) {
            jmsAdminOperations.removeJmsQueue("inQueue");
            jmsAdminOperations.close();
        }
    }

}
