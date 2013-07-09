/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.systest.ws.wssec11;


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.ws.wssec11.server.Server11;
import org.apache.cxf.systest.ws.wssec11.server.Server12;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;

import wssec.wssec11.IPingService;
import wssec.wssec11.PingService11;


/**
 *
 */
public class WSSecurity11Common extends AbstractBusClientServerTestBase {
       
    private static final String INPUT = "foo";

    public void runClientServer(
        String[] argv, boolean unrestrictedPoliciesInstalled, boolean wssecurity12
    ) throws IOException {
        
        Bus bus = null;
        if (unrestrictedPoliciesInstalled) {
            bus = new SpringBusFactory().createBus("org/apache/cxf/systest/ws/wssec11/client.xml");
        } else {
            bus = new SpringBusFactory().createBus(
                    "org/apache/cxf/systest/ws/wssec11/client_restricted.xml");
        }
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        String portNumber = null;
        if (wssecurity12) {
            portNumber = Server12.PORT;
        } else {
            portNumber = Server11.PORT;
        }
        
        URL wsdlLocation = null;
        for (String portPrefix : argv) {
            PingService11 svc = null; 
            wsdlLocation = getWsdlLocation(portPrefix, portNumber); 
            svc = new PingService11(wsdlLocation);
            final IPingService port = 
                svc.getPort(
                    new QName(
                        "http://WSSec/wssec11",
                        portPrefix + "_IPingService"
                    ),
                    IPingService.class
                );
            
            final String output = port.echo(INPUT);
            assertEquals(INPUT, output);
            
            ((java.io.Closeable)port).close();
        }
        
        bus.shutdown(true);
    }
    
    public void runClientServerStreaming(
        String[] argv, boolean unrestrictedPoliciesInstalled, boolean wssecurity12
    ) throws IOException {

        Bus bus = null;
        if (unrestrictedPoliciesInstalled) {
            bus = new SpringBusFactory().createBus("org/apache/cxf/systest/ws/wssec11/client.xml");
        } else {
            bus = new SpringBusFactory().createBus(
                      "org/apache/cxf/systest/ws/wssec11/client_restricted.xml");
        }
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        String portNumber = null;
        if (wssecurity12) {
            portNumber = Server12.PORT;
        } else {
            portNumber = Server11.PORT;
        }

        URL wsdlLocation = null;
        for (String portPrefix : argv) {
            PingService11 svc = null; 
            wsdlLocation = getWsdlLocation(portPrefix, portNumber); 
            svc = new PingService11(wsdlLocation);
            final IPingService port = 
                svc.getPort(
                    new QName(
                        "http://WSSec/wssec11",
                        portPrefix + "_IPingService"
                    ),
                    IPingService.class
                );

            ((BindingProvider)port).getRequestContext().put(
                SecurityConstants.ENABLE_STREAMING_SECURITY, "true"
            );
            ((BindingProvider)port).getResponseContext().put(
                SecurityConstants.ENABLE_STREAMING_SECURITY, "true"
            );
            
            final String output = port.echo(INPUT);
            assertEquals(INPUT, output);

            ((java.io.Closeable)port).close();
        }

        bus.shutdown(true);
    }



    private static URL getWsdlLocation(String portPrefix, String portNumber) {
        try {
            return new URL("http://localhost:" + portNumber + "/" + portPrefix + "PingService?wsdl");
        } catch (MalformedURLException mue) {
            return null;
        }
    }


    public static boolean isIBMJDK16() {
        String fullVersion = System.getProperty("java.fullversion");
        if (fullVersion == null) {
            //Maybe one of the non IBM JDKs dont set this property, but
            //the IBM one definitely does
            return false;
        }
        if (fullVersion.indexOf("IBM") == -1) {
            return false;
        }

        String javaVersion = System.getProperty("java.version");
        double javaVersionNum = 0.0;
        if (javaVersion.length() > 3) {
            javaVersionNum = new Double(javaVersion.substring(0, 3)).doubleValue();
        } else {
            javaVersionNum = new Double(javaVersion).doubleValue();
        }
        if (javaVersionNum < 1.6) {
            return false;
        }
        
        return true;
    }
}
