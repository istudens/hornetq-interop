package org.jboss.test.hornetqinterop.management;

import org.jboss.as.arquillian.container.ManagementClient;

import java.io.File;
import java.io.IOException;

/**
 * TODO
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public class EAP5ClientModuleSetup extends AbstractMgmtServerSetupTask {

    @Override
    protected void doSetup(ManagementClient managementClient) throws Exception {
        try {
            createEAP5ClientModule();
        } catch (Exception e) {
            System.err.println("doSetup failed! " + e.getMessage());
            e.printStackTrace(System.err);
            throw e;
        }
    }

    @Override
    public void tearDown(ManagementClient managementClient, String s) throws Exception {
        try {
            removeEAP5ClientModule();
        } catch (Exception e) {
            System.err.println("tearDown failed! " + e.getMessage());
            e.printStackTrace(System.err);
            throw e;
        }
    }

    private void createEAP5ClientModule() throws IOException {
        final File slot = new File(getEAP5ClientModuleRoot(), "main");
        if (slot.exists()) {
            throw new IllegalArgumentException(slot + " already exists");
        }
        if (!slot.mkdirs()) {
            throw new IllegalArgumentException("Could not create " + slot);
        }
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        copyFile(new File(slot, "module.xml"), tccl.getResource("eap5-client/module.xml").openStream());
        copyFile(new File(slot, "jnp-client.jar"), tccl.getResource("eap5-client/jnp-client.jar").openStream());
        copyFile(new File(slot, "jboss-logging-spi.jar"), tccl.getResource("eap5-client/jboss-logging-spi.jar").openStream());
    }

    private void removeEAP5ClientModule() throws Exception {
        File file = getEAP5ClientModuleRoot();
        while (!ModelUtil.getModulePath().equals(file.getParentFile()))
            file = file.getParentFile();
        ModelUtil.deleteRecursively(file);
    }

    private File getEAP5ClientModuleRoot() {
        return new File(ModelUtil.getModulePath(), "eap5-client");
    }



}

