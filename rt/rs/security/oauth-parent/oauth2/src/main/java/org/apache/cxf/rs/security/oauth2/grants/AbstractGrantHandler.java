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

package org.apache.cxf.rs.security.oauth2.grants;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenRegistration;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthError;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.provider.AccessTokenGrantHandler;
import org.apache.cxf.rs.security.oauth2.provider.OAuthDataProvider;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;


/**
 * Abstract access token grant handler
 */
public abstract class AbstractGrantHandler implements AccessTokenGrantHandler {
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractGrantHandler.class);
    
    private List<String> supportedGrants;
    private OAuthDataProvider dataProvider;
    private boolean partialMatchScopeValidation;
    private boolean canSupportPublicClients;
    protected AbstractGrantHandler(String grant) {
        supportedGrants = Collections.singletonList(grant);
    }
    
    protected AbstractGrantHandler(List<String> grants) {
        if (grants.isEmpty()) {
            throw new IllegalArgumentException("The list of grant types can not be empty");
        }
        supportedGrants = grants;
    }
    
    public void setDataProvider(OAuthDataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }
    public OAuthDataProvider getDataProvider() {
        return dataProvider;
    }
    
    public List<String> getSupportedGrantTypes() {
        return Collections.unmodifiableList(supportedGrants);
    }
    
    @Deprecated
    protected void checkIfGrantSupported(Client client) {
        checkIfGrantSupported(client, getSingleGrantType());
    }
    
    private void checkIfGrantSupported(Client client, String requestedGrant) {
        if (!OAuthUtils.isGrantSupportedForClient(client, 
                                                  canSupportPublicClients,
                                                  requestedGrant)) {
            throw new OAuthServiceException(OAuthConstants.UNAUTHORIZED_CLIENT);    
        }
    }
    
    protected ServerAccessToken doCreateAccessToken(Client client,
                                                    UserSubject subject,
                                                    List<String> requestedScope) {
        
        return doCreateAccessToken(client, subject, getSingleGrantType(), requestedScope);
    }
    
    private String getSingleGrantType() {
        if (supportedGrants.size() > 1) {
            String errorMessage = "Request grant type must be specified";
            LOG.warning(errorMessage);
            throw new WebApplicationException(500);
        }
        return supportedGrants.get(0);
    }
    
    protected ServerAccessToken doCreateAccessToken(Client client,
                                                    UserSubject subject,
                                                    String requestedGrant,
                                                    List<String> requestedScope) {
        if (!OAuthUtils.validateScopes(requestedScope, client.getRegisteredScopes(), 
                                       partialMatchScopeValidation)) {
            throw new OAuthServiceException(new OAuthError(OAuthConstants.INVALID_SCOPE));     
        }
        // Check if a pre-authorized  token available
        ServerAccessToken token = dataProvider.getPreauthorizedToken(
                                     client, requestedScope, subject, requestedGrant);
        if (token != null) {
            return token;
        }
        
        // Delegate to the data provider to create the one
        AccessTokenRegistration reg = new AccessTokenRegistration();
        reg.setClient(client);
        reg.setGrantType(requestedGrant);
        reg.setSubject(subject);
        reg.setRequestedScope(requestedScope);        
        
        return dataProvider.createAccessToken(reg);
    }
    
    public void setPartialMatchScopeValidation(boolean partialMatchScopeValidation) {
        this.partialMatchScopeValidation = partialMatchScopeValidation;
    }
    
    public void setCanSupportPublicClients(boolean support) {
        canSupportPublicClients = support;
    }
    
    public boolean isCanSupportPublicClients() {
        return canSupportPublicClients;
    }
}
