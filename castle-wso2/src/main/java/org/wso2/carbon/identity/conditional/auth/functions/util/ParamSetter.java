/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.conditional.auth.functions.util;

import io.castle.client.internal.utils.CastleContextBuilder;
import io.castle.client.model.CastleContext;
import io.castle.client.model.CastleHeader;
import io.castle.client.model.CastleHeaders;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.authentication.framework.config.model.graph.js.JsAuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.config.model.graph.js.nashorn.JsNashornServletRequest;
import org.wso2.carbon.identity.conditional.auth.functions.constant.ErrorMessageConstants;
import org.wso2.carbon.identity.conditional.auth.functions.internal.CastleIntegrationDataHolder;
import org.wso2.carbon.identity.conditional.auth.functions.model.User;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.UniqueIDUserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.ArrayList;
import java.util.List;

/**
 * Class with the functionality to set up all the required parameters.
 */
public class ParamSetter {

    private JsAuthenticationContext context;
    private JsNashornServletRequest request;
    private UniqueIDUserStoreManager uniqueIDUserStoreManager;
    private static String userId = "userId";
    private static int tenantId = 0;
    private static String userEmail = "userEmail";
    private static String userAgent = "User-Agent";
    private static String host = "Host";
    private static String ip = "ip";
    private static final Log LOG = LogFactory.getLog(ParamSetter.class);

    public ParamSetter(JsNashornServletRequest request, JsAuthenticationContext context) {
        this.context = context;
        this.request = request;
        setupParameters();
    }

    public User getUser() {

        return new User(userId, userEmail);
    }

    public CastleContext createContext() {

        CastleContextBuilder contextBuilder = new CastleContextBuilder(null, null);

        //set ip
        contextBuilder = contextBuilder.ip(this.ip);

        //set headers
        CastleHeader castleHeader1 = new CastleHeader("User-Agent", this.userAgent);
        CastleHeader castleHeader2 = new CastleHeader("Host", this.host);
        List<CastleHeader> headers = new ArrayList<>();
        headers.add(castleHeader1);
        headers.add(castleHeader2);
        contextBuilder = contextBuilder.headers(new CastleHeaders(headers));

        //creating context
        CastleContext castleContext = contextBuilder.build();

        return castleContext;
    }

    private UniqueIDUserStoreManager getUniqueIdEnabledUserStoreManager(int tenantId) throws UserStoreException {

        RealmService realmService = CastleIntegrationDataHolder.getInstance().getRealmService();
        UserStoreManager userStoreManager = realmService.getTenantUserRealm(tenantId).getUserStoreManager();

        if (!(userStoreManager instanceof UniqueIDUserStoreManager)) {
            LOG.error(ErrorMessageConstants.ERROR_GETTING_USER_STORE);
        }

        return (UniqueIDUserStoreManager) userStoreManager;
    }

    private void setupParameters() {

        try {
            userId = context.getContext().getSubject().getUserId();
        } catch (Exception e) {
            LOG.error(ErrorMessageConstants.ERROR_USER_ID);
        }

            this.userAgent = request.getWrapped().getWrapped().getHeader("User-Agent");
            this.host = request.getWrapped().getWrapped().getHeader("Host");
            this.ip = IdentityUtil.getClientIpAddress(request.getWrapped().getWrapped());
            this.tenantId = IdentityTenantUtil.getTenantId(context.getContext().getTenantDomain());

        try {
            uniqueIDUserStoreManager = getUniqueIdEnabledUserStoreManager(tenantId);
            userEmail = uniqueIDUserStoreManager.getUserClaimValueWithID(userId,
                    "http://wso2.org/claims/emailaddress", null);
        } catch (UserStoreException e) {
            LOG.error(ErrorMessageConstants.ERROR_USER_EMAIL);
        }

    }

}