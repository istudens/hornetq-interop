/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat, Inc., and individual contributors
* as indicated by the @author tags. See the copyright.txt file in the
* distribution for a full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jboss.test.hornetqinterop.management;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLMapper;
import org.xnio.IoUtils;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
public abstract class AbstractMgmtTestBase {

    protected abstract ModelControllerClient getModelControllerClient();

    protected ModelNode executeOperation(final ModelNode op, boolean unwrapResult) throws IOException, MgmtOperationException {
        if(unwrapResult) {
            return ManagementOperations.executeOperation(getModelControllerClient(), op);
        } else {
            return ManagementOperations.executeOperationRaw(getModelControllerClient(), op);
        }
    }

    protected void executeOperations(final List<ModelNode> operations) throws IOException, MgmtOperationException {
        for(ModelNode op : operations) {
            executeOperation(op);
        }
    }
    protected ModelNode executeOperation(final ModelNode op) throws IOException, MgmtOperationException {
        return executeOperation(op, true);
    }

    protected ModelNode executeOperation(final String address, final String operation) throws IOException, MgmtOperationException {
        return executeOperation(ModelUtil.createOpNode(address, operation));
    }

    protected void remove(final ModelNode address) throws IOException, MgmtOperationException {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("remove");
        operation.get(OP_ADDR).set(address);
        executeOperation(operation);
    }

    protected Map<String, ModelNode> getChildren(final ModelNode result) {
        assert result.isDefined();
        final Map<String, ModelNode> steps = new HashMap<String, ModelNode>();
        for (final Property property : result.asPropertyList()) {
            steps.put(property.getName(), property.getValue());
        }
        return steps;
    }

    protected ModelNode findNodeWithProperty(List<ModelNode> newList, String propertyName, String setTo) {
        ModelNode toReturn = null;
        for (ModelNode result : newList) {
            final Map<String, ModelNode> parseChildren = getChildren(result);
            if (!parseChildren.isEmpty() && parseChildren.get(propertyName) != null && parseChildren.get(propertyName).asString().equals(setTo)) {
                toReturn = result;
                break;
            }
        }
        return toReturn;
    }

    public static List<ModelNode> xmlToModelOperations(String xml, String nameSpaceUriString, XMLElementReader<List<ModelNode>> parser) throws Exception {
        XMLMapper mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(new QName(nameSpaceUriString, "subsystem"), parser);

        StringReader strReader = new StringReader(xml);

        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StreamSource(strReader));
        List<ModelNode> newList = new ArrayList<ModelNode>();
        mapper.parseDocument(newList, reader);

        return newList;
    }

    public static ModelNode operationListToCompositeOperation(List<ModelNode> operations) {
        return operationListToCompositeOperation(operations, true);
    }

    public static ModelNode operationListToCompositeOperation(List<ModelNode> operations, boolean skipFirst) {
        if (skipFirst) operations.remove(0);
        ModelNode[] steps = new ModelNode[operations.size()];
        operations.toArray(steps);
        return ModelUtil.createCompositeNode(steps);
    }

    public static String readXmlResource(final String name) throws IOException {
        File f = new File(name);
        BufferedReader reader = new BufferedReader(new FileReader(f));
        StringWriter writer = new StringWriter();
        String line;
        while ((line = reader.readLine()) != null) {
            writer.write(line);
        }
        return writer.toString();
    }

    protected void takeSnapShot() throws Exception{
        final ModelNode operation0 = new ModelNode();
        operation0.get(OP).set("take-snapshot");

        executeOperation(operation0);
    }

    public static String readFile(Class<?> testClass, String fileName) {
        final URL res = testClass.getResource(fileName);
        return readFile(res);
    }

    public static String readFile(URL url) {
        BufferedInputStream stream = null;
        try {
            stream = new BufferedInputStream(url.openStream());
            byte[] buff = new byte[1024];
            StringBuilder builder = new StringBuilder();
            int read = -1;
            while ((read = stream.read(buff)) != -1) {
                builder.append(new String(buff, 0, read));
            }
            return builder.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
    }

    protected void copyFile(File target, InputStream src) throws IOException {
   		final BufferedOutputStream out = new BufferedOutputStream(
   				new FileOutputStream(target));
   		try {
   			int i = src.read();
   			while (i != -1) {
   				out.write(i);
   				i = src.read();
   			}
   		} finally {
   			IoUtils.safeClose(out);
   		}
   	}

}
