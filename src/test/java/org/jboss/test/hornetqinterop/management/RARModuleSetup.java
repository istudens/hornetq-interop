package org.jboss.test.hornetqinterop.management;

import org.jboss.as.arquillian.container.ManagementClient;

import java.io.File;
import java.io.IOException;

/**
 * TODO
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public class RARModuleSetup extends AbstractMgmtServerSetupTask {

    @Override
    protected void doSetup(ManagementClient managementClient) throws Exception {
        try {
            createRarModule();
        } catch (Exception e) {
            System.err.println("doSetup failed! " + e.getMessage());
            e.printStackTrace(System.err);
            throw e;
        }
    }

    @Override
    public void tearDown(ManagementClient managementClient, String s) throws Exception {
        try {
            removeRarModule();
        } catch (Exception e) {
            System.err.println("tearDown failed! " + e.getMessage());
            e.printStackTrace(System.err);
            throw e;
        }
    }

    private void createRarModule() throws IOException {
        final File slot = new File(getRarModuleRoot(), "main");
        if (slot.exists()) {
            throw new IllegalArgumentException(slot + " already exists");
        }
        if (!slot.mkdirs()) {
            throw new IllegalArgumentException("Could not create " + slot);
        }
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        copyFile(new File(slot, "module.xml"), tccl.getResource("rar/module.xml").openStream());
        copyFile(new File(slot, "hornetq-ra.jar"), tccl.getResource("rar/hornetq-ra.jar").openStream());
        copyFile(new File(slot, "hornetq-jms.jar"), tccl.getResource("rar/hornetq-jms.jar").openStream());
        copyFile(new File(slot, "hornetq-core-client.jar"), tccl.getResource("rar/hornetq-core-client.jar").openStream());
        copyFile(new File(slot, "netty.jar"), tccl.getResource("rar/netty.jar").openStream());
    }

    private void removeRarModule() throws Exception {
        File file = getRarModuleRoot();
        while (!ModelUtil.getModulePath().equals(file.getParentFile()))
            file = file.getParentFile();
        ModelUtil.deleteRecursively(file);
    }

    private File getRarModuleRoot() {
        return new File(ModelUtil.getModulePath(), "hornetq-eap5");
    }

}

