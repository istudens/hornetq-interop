package org.jboss.test.hornetqinterop;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdapterSubsystemParser;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.test.hornetqinterop.management.AbstractMgmtTestBase;
import org.jboss.test.hornetqinterop.management.EAP5ClientModuleSetup;
import org.jboss.test.hornetqinterop.management.JMSServerSetupTask;
import org.jboss.test.hornetqinterop.management.ManualServerSetup;
import org.jboss.test.hornetqinterop.management.RARModuleSetup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * TODO
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ManualServerSetup({RARModuleSetup.class, EAP5ClientModuleSetup.class, JMSServerSetupTask.class})
public class InteropTestCase extends AbstractMgmtTestBase {

    private static final String CONTAINER = "wildfly";
    private static final String RAR_DEPLOYMENT = "hornetq-eap5-ra.rar";
    private static final String WAR_DEPLOYMENT = "messagesender.war";

    private static final String tmpdir = System.getProperty("java.io.tmpdir");

    private ModelNode address = null;

    private ManagementClient managementClient;

    @ArquillianResource
    protected ContainerController controller;

    @ArquillianResource
    protected Deployer deployer;

    @Deployment(name = RAR_DEPLOYMENT, managed = false, testable = false)
    @TargetsContainer(CONTAINER)
    public static Archive<?> rarDeployment() {
        final Archive<?> deployment = ShrinkWrap.create(ResourceAdapterArchive.class, RAR_DEPLOYMENT)
//                .addAsLibrary("rar/hornetq-ra.jar", "hornetq-ra.jar")
//                .addAsLibrary("rar/hornetq-jms.jar", "hornetq-jms.jar")
//                .addAsLibrary("rar/hornetq-core-client.jar", "hornetq-core-client.jar")
                .setResourceAdapterXML("rar/ra.xml")
                .addAsManifestResource(new StringAsset("Dependencies: hornetq-eap5, org.jboss.jboss-transaction-spi\n"), "MANIFEST.MF");
        deployment.as(ZipExporter.class).exportTo(new File(tmpdir, RAR_DEPLOYMENT), true);
        System.out.println(deployment.toString(true));
        return deployment;
    }

    @Deployment(name = WAR_DEPLOYMENT, managed = false, testable = false)
    @TargetsContainer(CONTAINER)
    public static Archive<?> webDeployment() {
        final Archive<?> deployment = ShrinkWrap.create(WebArchive.class, WAR_DEPLOYMENT)
                .addClasses(ConsumerMDB.class, MessageSenderServlet.class, ModuleInitialContextFactory.class)
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.modules, hornetq-eap5\n"), "MANIFEST.MF");
//                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.modules, hornetq-eap5, eap5-client\n"), "MANIFEST.MF");
        deployment.as(ZipExporter.class).exportTo(new File(tmpdir, WAR_DEPLOYMENT), true);
        System.out.println(deployment.toString(true));
        return deployment;
    }

    @Before
    public void before() throws Exception {
        System.out.println("starting the container");
        controller.start(CONTAINER);
    }

    @After
    public void after() throws Exception {
        System.out.println("stopping the container");
        controller.stop(CONTAINER);
    }

    @Test
    @InSequence(100)
    public void clean() throws Exception {
        System.out.println("clean()...");
        System.out.println("controller.isStarted(): " + controller.isStarted(CONTAINER));
        removeResourceAdapter();
        System.out.println("undeploying both");
        deployer.undeploy(WAR_DEPLOYMENT);
        deployer.undeploy(RAR_DEPLOYMENT);
    }

    @Test
    @OperateOnDeployment(WAR_DEPLOYMENT)
    @InSequence(-1)
    public void createResourceAdapter(@ArquillianResource ManagementClient client) throws Exception {
        System.out.println("deploying both");
        deployer.deploy(RAR_DEPLOYMENT);
        deployer.deploy(WAR_DEPLOYMENT);

        this.managementClient = client;
        createResourceAdapter();
    }

    @Test
    @OperateOnDeployment(WAR_DEPLOYMENT)
    public void sendMessageToEAP5(@ArquillianResource(MessageSenderServlet.class) URL baseURL) throws Exception {

        final String msg = "Hello";
        final String destinationName = System.getProperty("message.sender.queue", "/queue/outQueue");
        final String eap5Host = System.getProperty("eap5.host", "127.0.0.1");

        URL url = new URL(baseURL + MessageSenderServlet.URL_PATTERN + "?msg=" + msg + "&destinationName=" + destinationName + "&eap5Host=" + eap5Host);
        HttpGet httpget = new HttpGet(url.toURI());
        DefaultHttpClient httpclient = new DefaultHttpClient();

        System.out.println("executing request: " + httpget.getRequestLine());
        HttpResponse response = httpclient.execute(httpget);
        System.out.println("response: " + response);

        int statusCode = response.getStatusLine().getStatusCode();
        assertEquals("Wrong response code: " + statusCode, HttpURLConnection.HTTP_OK, statusCode);
    }

    private void createResourceAdapter() throws Exception {
        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        final String xml = readFile(tccl.getResource("rar/basic.xml"));
        List<ModelNode> operations = xmlToModelOperations(xml,
                "urn:jboss:domain:resource-adapters:2.0",
                new ResourceAdapterSubsystemParser());
        address = operations.get(1).get("address");
        operations.remove(0);
        for (ModelNode op : operations) {
            final ModelNode result = executeOperation(op);
            System.out.println("result: " + result);
        }
    }

    private void removeResourceAdapter() throws Exception {
        if (address != null)
            remove(address);
    }

    @Override
    protected ModelControllerClient getModelControllerClient() {
        return managementClient.getControllerClient();
    }

}
