/*
 *
 *   Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.apimgt.core.impl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.apimgt.core.api.APIDefinition;
import org.wso2.carbon.apimgt.core.api.APIGateway;
import org.wso2.carbon.apimgt.core.api.APILifecycleManager;
import org.wso2.carbon.apimgt.core.api.APIManager;
import org.wso2.carbon.apimgt.core.api.GatewaySourceGenerator;
import org.wso2.carbon.apimgt.core.api.WorkflowExecutor;
import org.wso2.carbon.apimgt.core.dao.APISubscriptionDAO;
import org.wso2.carbon.apimgt.core.dao.ApiDAO;
import org.wso2.carbon.apimgt.core.dao.ApiType;
import org.wso2.carbon.apimgt.core.dao.ApplicationDAO;
import org.wso2.carbon.apimgt.core.dao.LabelDAO;
import org.wso2.carbon.apimgt.core.dao.PolicyDAO;
import org.wso2.carbon.apimgt.core.dao.WorkflowDAO;
import org.wso2.carbon.apimgt.core.exception.APIManagementException;
import org.wso2.carbon.apimgt.core.exception.APIMgtDAOException;
import org.wso2.carbon.apimgt.core.exception.APIMgtResourceAlreadyExistsException;
import org.wso2.carbon.apimgt.core.exception.ExceptionCodes;
import org.wso2.carbon.apimgt.core.exception.WorkflowException;
import org.wso2.carbon.apimgt.core.models.API;
import org.wso2.carbon.apimgt.core.models.APIResource;
import org.wso2.carbon.apimgt.core.models.Application;
import org.wso2.carbon.apimgt.core.models.DocumentContent;
import org.wso2.carbon.apimgt.core.models.DocumentInfo;
import org.wso2.carbon.apimgt.core.models.Label;
import org.wso2.carbon.apimgt.core.models.Subscription;
import org.wso2.carbon.apimgt.core.models.UriTemplate;
import org.wso2.carbon.apimgt.core.models.Workflow;
import org.wso2.carbon.apimgt.core.template.APITemplateException;
import org.wso2.carbon.apimgt.core.util.APIUtils;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
/**
 * This class contains the implementation of the common methods for Publisher and store
 */
public abstract class AbstractAPIManager implements APIManager {

    private static final Logger log = LoggerFactory.getLogger(AbstractAPIManager.class);

    private ApiDAO apiDAO;
    private ApplicationDAO applicationDAO;
    private APISubscriptionDAO apiSubscriptionDAO;
    private PolicyDAO policyDAO;
    private String username;
    private APILifecycleManager apiLifecycleManager;
    private LabelDAO labelDAO;
    private WorkflowDAO workflowDAO;
    private GatewaySourceGenerator gatewaySourceGenerator;
    private APIGateway apiGatewayPublisher;
    protected APIDefinition apiDefinitionFromSwagger20 = new APIDefinitionFromSwagger20();

    public AbstractAPIManager(String username, ApiDAO apiDAO, ApplicationDAO applicationDAO,
                              APISubscriptionDAO apiSubscriptionDAO, PolicyDAO policyDAO, APILifecycleManager
                                      apiLifecycleManager,
                              LabelDAO labelDAO, WorkflowDAO workflowDAO, GatewaySourceGenerator
                                      gatewaySourceGenerator, APIGateway apiGatewayPublisher) {

        this.username = username;
        this.apiDAO = apiDAO;
        this.applicationDAO = applicationDAO;
        this.apiSubscriptionDAO = apiSubscriptionDAO;
        this.policyDAO = policyDAO;
        this.apiLifecycleManager = apiLifecycleManager;
        this.labelDAO = labelDAO;
        this.workflowDAO = workflowDAO;
        this.gatewaySourceGenerator = gatewaySourceGenerator;
        this.apiGatewayPublisher = apiGatewayPublisher;
    }

    public AbstractAPIManager(String username, ApiDAO apiDAO, ApplicationDAO applicationDAO,
                              APISubscriptionDAO apiSubscriptionDAO, PolicyDAO policyDAO,
                              APILifecycleManager apiLifecycleManager, LabelDAO labelDAO, WorkflowDAO workflowDAO) {

        this(username, apiDAO, applicationDAO, apiSubscriptionDAO, policyDAO, apiLifecycleManager, labelDAO,
                workflowDAO, new GatewaySourceGeneratorImpl(), new APIGatewayPublisherImpl());
    }


    /**
     * Returns details of an API.
     *
     * @param uuid UUID of the API's registry artifact
     * @return An API object related to the given artifact id or null
     * @throws APIManagementException if failed get API from String
     */
    @Override
    public API getAPIbyUUID(String uuid) throws APIManagementException {
        API api = null;
        try {
            api = apiDAO.getAPI(uuid);
        } catch (APIMgtDAOException e) {
            String errorMsg = "Error occurred while retrieving API with id " + uuid;
            log.error(errorMsg, e);
            throw new APIManagementException(errorMsg, e, ExceptionCodes.APIMGT_DAO_EXCEPTION);
        }
        return api;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<API> getAPIsByProvider(String providerName, ApiType apiType) throws APIManagementException {
        try {
            return getApiDAO().getAPIsForProvider(providerName, apiType);
        } catch (APIMgtDAOException e) {
            String errorMsg = "Unable to fetch APIs of " + providerName;
            log.error(errorMsg, e);
            throw new APIManagementException(errorMsg, e, ExceptionCodes.APIMGT_DAO_EXCEPTION);
        }
    }

    /**
     * @see APIManager#getLastUpdatedTimeOfAPI(java.lang.String)
     */
    @Override
    public String getLastUpdatedTimeOfAPI(String apiId) throws APIManagementException {
        String lastUpdatedTime;
        try {
            lastUpdatedTime = apiDAO.getLastUpdatedTimeOfAPI(apiId);
        } catch (APIMgtDAOException e) {
            String errorMsg = "Error occurred while retrieving the last update time of API with id " + apiId;
            log.error(errorMsg, e);
            throw new APIManagementException(errorMsg, e, ExceptionCodes.APIMGT_DAO_EXCEPTION);
        }

        return lastUpdatedTime;
    }


    /**
     * @see APIManager#getLastUpdatedTimeOfSwaggerDefinition(java.lang.String)
     */
    @Override
    public String getLastUpdatedTimeOfSwaggerDefinition(String apiId) throws APIManagementException {
        String lastUpdatedTime;
        try {
            lastUpdatedTime = apiDAO.getLastUpdatedTimeOfSwaggerDefinition(apiId);
        } catch (APIMgtDAOException e) {
            String errorMsg =
                    "Error occurred while retrieving the last update time of the swagger definition of API with id "
                            + apiId;
            log.error(errorMsg, e);
            throw new APIManagementException(errorMsg, e, ExceptionCodes.APIMGT_DAO_EXCEPTION);
        }

        return lastUpdatedTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean checkIfAPIExists(String apiId) throws APIManagementException {
        boolean status;
        try {
            status = getApiDAO().getAPISummary(apiId) != null;
        } catch (APIMgtDAOException e) {
            String errorMsg = "Couldn't get APISummary for " + apiId;
            log.error(errorMsg, e);
            throw new APIManagementException(errorMsg, e, ExceptionCodes.APIMGT_DAO_EXCEPTION);
        }
        return status;
    }

    /**
     * Checks whether the given API context is already registered in the system
     *
     * @param context A String representing an API context
     * @return true if the context already exists and false otherwise
     * @throws APIManagementException if failed to check the context availability
     */
    @Override
    public boolean isContextExist(String context) throws APIManagementException {
        if (StringUtils.isNotEmpty(context)) {
            try {
                return getApiDAO().isAPIContextExists(context);
            } catch (APIMgtDAOException e) {
                String errorMsg = "Couldn't check API Context " + context + "Exists";
                log.error(errorMsg, e);
                throw new APIManagementException(errorMsg, e, ExceptionCodes.APIMGT_DAO_EXCEPTION);
            }
        } else {
            return false;
        }
    }

    /**
     * Checks whether the given API name is already registered in the system
     *
     * @param apiName A String representing an API name
     * @return true if the api name already exists and false otherwise
     * @throws APIManagementException if failed to check the context availability
     */
    @Override public boolean isApiNameExist(String apiName) throws APIManagementException {
        try {
            return getApiDAO().isAPINameExists(apiName, username, ApiType.STANDARD);

        } catch (APIMgtDAOException e) {
            String errorMsg = "Couldn't check API Name " + apiName + "Exists";
            log.error(errorMsg, e);
            throw new APIManagementException(errorMsg, e, ExceptionCodes.APIMGT_DAO_EXCEPTION);
        }
    }

    /**
     * Returns a set of API versions for the given provider and API name
     *
     * @param providerName name of the provider (common)
     * @param apiName      name of the api
     * @return Set of version strings (possibly empty)
     * @throws APIManagementException if failed to get version for api
     */
    @Override
    public Set<String> getAPIVersions(String providerName, String apiName) throws APIManagementException {
        return null;
    }

    /**
     * Returns the swagger v2.0 definition as a string
     *
     * @param api id of the String
     * @return swagger string
     * @throws APIManagementException   If failed to retrieve swagger definition.
     */
    @Override public String getSwagger20Definition(String api) throws APIManagementException {
        try {
            return getApiDAO().getSwaggerDefinition(api);

        } catch (APIMgtDAOException e) {
            String errorMsg = "Couldn't retrieve swagger definition for apiId " + api;
            log.error(errorMsg + api, e);
            throw new APIManagementException(errorMsg + api, e, ExceptionCodes.APIMGT_DAO_EXCEPTION);
        }

    }

    /**
     * Returns a paginated list of documentation attached to a particular API
     *
     * @param apiId UUID of API
     * @param offset The number of results from the beginning that is to be ignored
     * @param limit The maximum number of results to be returned after the offset
     * @return {@code List<DocumentInfo>} Document meta data list
     * @throws APIManagementException if it failed to fetch Documentations
     */
    public List<DocumentInfo> getAllDocumentation(String apiId, int offset, int limit) throws APIManagementException {
        try {
            return getApiDAO().getDocumentsInfoList(apiId);
        } catch (APIMgtDAOException e) {
            String errorMsg = "Error occurred while retrieving documents";
            log.error(errorMsg, e);
            throw new APIManagementException(errorMsg, e, ExceptionCodes.APIMGT_DAO_EXCEPTION);
        }
    }

    /**
     * Get a summary of documentation by doc Id
     *
     * @param docId Document ID
     * @return {@link DocumentInfo} Documentation meta data
     * @throws APIManagementException if it failed to fetch Documentation
     */
    public DocumentInfo getDocumentationSummary(String docId) throws APIManagementException {
        try {
            return getApiDAO().getDocumentInfo(docId);
        } catch (APIMgtDAOException e) {
            String errorMsg = "Error occurred while retrieving documents";
            log.error(errorMsg, e);
            throw new APIMgtDAOException(errorMsg, e, ExceptionCodes.APIMGT_DAO_EXCEPTION);
        }
    }

    /**
     * This method used to get the content of a documentation
     *
     * @param docId Document ID
     * @return {@link InputStream} Input stream for document content
     * @throws APIManagementException if the requested documentation content is not available
     */
    public DocumentContent getDocumentationContent(String docId) throws APIManagementException {
        try {
            DocumentInfo documentInfo = getDocumentationSummary(docId);
            DocumentContent.Builder documentContentBuilder = new DocumentContent.Builder();
            if (documentInfo != null) {
                documentContentBuilder.documentInfo(documentInfo);
                if (DocumentInfo.SourceType.FILE.equals(documentInfo.getSourceType())) {
                    InputStream inputStream = getApiDAO().getDocumentFileContent(docId);
                    if (inputStream != null) {
                        documentContentBuilder = documentContentBuilder.fileContent(inputStream);
                    } else {
                        throw new APIManagementException("Couldn't find file content of  document", ExceptionCodes
                                .DOCUMENT_CONTENT_NOT_FOUND);
                    }
                } else if (documentInfo.getSourceType().equals(DocumentInfo.SourceType.INLINE)) {
                    String inlineContent = getApiDAO().getDocumentInlineContent(docId);
                    if (inlineContent != null) {
                        documentContentBuilder = documentContentBuilder.inlineContent(inlineContent);
                    } else {
                        throw new APIManagementException("Couldn't find inline content of  document", ExceptionCodes
                                .DOCUMENT_CONTENT_NOT_FOUND);
                    }
                }
            } else {
                throw new APIManagementException("Couldn't fnd document", ExceptionCodes.DOCUMENT_NOT_FOUND);
            }
            return documentContentBuilder.build();
        } catch (APIMgtDAOException e) {
            String errorMsg = "Error occurred while retrieving document content";
            log.error(errorMsg, e);
            throw new APIManagementException(errorMsg, e, ExceptionCodes.APIMGT_DAO_EXCEPTION);
        }
    }

    /**
     * Returns the corresponding application given the uuid
     * @param uuid uuid of the Application
     * @param userId  Name of the User.
     * @param groupId Id of the group.
     * @return it will return Application corresponds to the uuid provided.
     * @throws APIManagementException   If failed to retrieve application.
     */
    public Application getApplication(String uuid, String userId, String groupId) throws APIManagementException {
        Application application = null;
        try {
           application = getApplicationDAO().getApplication(uuid);
        } catch (APIMgtDAOException e) {
            String errorMsg = "Error occurred while retrieving application - ";
            log.error(errorMsg, e);
            throw new APIManagementException(errorMsg, e, ExceptionCodes.APIMGT_DAO_EXCEPTION);
        }
        return application;
    }

    /**
     * Return {@link Subscription} of subscription id
     *
     * @param subId UUID of the subscription.
     * @return  Subscription object.
     * @throws APIManagementException   If failed to retrieve subscription.
     */
    public Subscription getSubscriptionByUUID(String subId) throws APIManagementException {
        try {
            return getApiSubscriptionDAO().getAPISubscription(subId);
        } catch (APIMgtDAOException e) {
            String errorMsg = "Couldn't retrieve subscription for id " + subId;
            log.error(errorMsg, e);
            throw new APIManagementException(errorMsg + subId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSwagger20Definition(String apiId, String jsonText) throws APIManagementException {
        try {
            LocalDateTime localDateTime = LocalDateTime.now();
            API api = getAPIbyUUID(apiId);
            Map<String, UriTemplate> oldUriTemplateMap = api.getUriTemplates();
            List<APIResource> apiResourceList = apiDefinitionFromSwagger20.parseSwaggerAPIResources(new StringBuilder
                    (jsonText));
            Map<String, UriTemplate> updatedUriTemplateMap = new HashMap<>();
            for (APIResource apiResource : apiResourceList) {
                updatedUriTemplateMap.put(apiResource.getUriTemplate().getTemplateId(), apiResource.getUriTemplate());
            }
            Map<String, UriTemplate> uriTemplateMapNeedTobeUpdate = APIUtils.getMergedUriTemplates(oldUriTemplateMap,
                    updatedUriTemplateMap);
            API.APIBuilder apiBuilder = new API.APIBuilder(api);
            apiBuilder.uriTemplates(uriTemplateMapNeedTobeUpdate);
            apiBuilder.updatedBy(getUsername());
            apiBuilder.lastUpdatedTime(localDateTime);

            api = apiBuilder.build();
            GatewaySourceGenerator gatewaySourceGenerator = getGatewaySourceGenerator();
            gatewaySourceGenerator.setAPI(api);
            String existingGatewayConfig = getApiGatewayConfig(apiId);
            String updatedGatewayConfig = gatewaySourceGenerator
                    .getGatewayConfigFromSwagger(existingGatewayConfig, jsonText);
            getApiDAO().updateAPI(apiId, api);
            getApiDAO().updateSwaggerDefinition(apiId, jsonText, getUsername());
            getApiDAO().updateGatewayConfig(apiId, updatedGatewayConfig, getUsername());
        } catch (APIMgtDAOException e) {
            String errorMsg = "Couldn't update the Swagger Definition";
            log.error(errorMsg, e);
            throw new APIManagementException(errorMsg, e, ExceptionCodes.APIMGT_DAO_EXCEPTION);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveThumbnailImage(String apiId, InputStream inputStream, String dataType)
            throws APIManagementException {
        try {
            getApiDAO().updateImage(apiId, inputStream, dataType, getUsername());
        } catch (APIMgtDAOException e) {
            String errorMsg = "Couldn't save the thumbnail image";
            log.error(errorMsg, e);
            throw new APIManagementException(errorMsg, e, ExceptionCodes.APIMGT_DAO_EXCEPTION);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getThumbnailImage(String apiId) throws APIManagementException {
        try {
            return getApiDAO().getImage(apiId);
        } catch (APIMgtDAOException e) {
            String errorMsg = "Couldn't retrieve thumbnail for api " + apiId;
            log.error(errorMsg, e);
            throw new APIManagementException(errorMsg, e, ExceptionCodes.APIMGT_DAO_EXCEPTION);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateApiGatewayConfig(String apiId, String configString) throws APIManagementException {
        API api = getAPIbyUUID(apiId);
        GatewaySourceGenerator gatewaySourceGenerator = getGatewaySourceGenerator();
        gatewaySourceGenerator.setAPI(api);
        try {
            String swagger = gatewaySourceGenerator.getSwaggerFromGatewayConfig(configString);
            getApiDAO().updateSwaggerDefinition(apiId, swagger, getUsername());
            getApiDAO().updateGatewayConfig(apiId, configString, getUsername());
        } catch (APIMgtDAOException e) {
            log.error("Couldn't update configuration for apiId " + apiId, e);
            throw new APIManagementException("Couldn't update configuration for apiId " + apiId,
                    ExceptionCodes.APIMGT_DAO_EXCEPTION);
        } catch (APITemplateException e) {
            log.error("Error generating swagger from gateway config " + apiId, e);
            throw new APIManagementException("Error generating swagger from gateway config " + apiId,
                    ExceptionCodes.TEMPLATE_EXCEPTION);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getApiGatewayConfig(String apiId) throws APIManagementException {
        try {
            return getApiDAO().getGatewayConfig(apiId);

        } catch (APIMgtDAOException e) {
            log.error("Couldn't retrieve swagger definition for apiId " + apiId, e);
            throw new APIManagementException("Couldn't retrieve gateway configuration for apiId " + apiId,
                    ExceptionCodes.APIMGT_DAO_EXCEPTION);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLastUpdatedTimeOfGatewayConfig(String apiId) throws APIManagementException {
        String lastUpdatedTime;
        try {
            lastUpdatedTime = getApiDAO().getLastUpdatedTimeOfGatewayConfig(apiId);
        } catch (APIMgtDAOException e) {
            String errorMsg =
                    "Error occurred while retrieving the last update time of the gateway configuration of API with id "
                            + apiId;
            log.error(errorMsg, e);
            throw new APIManagementException(errorMsg, e, ExceptionCodes.APIMGT_DAO_EXCEPTION);
        }

        return lastUpdatedTime;
    }

    /**
     * @see APIManager#getLastUpdatedTimeOfDocument(String)
     */
    @Override
    public String getLastUpdatedTimeOfDocument(String documentId) throws APIManagementException {
        String lastUpdatedTime;
        try {
            lastUpdatedTime = apiDAO.getLastUpdatedTimeOfDocument(documentId);
        } catch (APIMgtDAOException e) {
            String errorMsg = "Error occurred while retrieving the last updated time of document " + documentId;
            log.error(errorMsg, e);
            throw new APIManagementException(errorMsg, e, ExceptionCodes.APIMGT_DAO_EXCEPTION);
        }
        return lastUpdatedTime;
    }

    /**
     * @see APIManager#getLastUpdatedTimeOfDocumentContent(String, String)
     */
    @Override
    public String getLastUpdatedTimeOfDocumentContent(String apiId, String documentId) throws APIManagementException {
        String lastUpdatedTime;
        try {
            lastUpdatedTime = apiDAO.getLastUpdatedTimeOfDocumentContent(apiId, documentId);
        } catch (APIMgtDAOException e) {
            String errorMsg =
                    "Error occurred while retrieving the last updated time of the document's content " + documentId;
            log.error(errorMsg, e);
            throw new APIManagementException(errorMsg, e, ExceptionCodes.APIMGT_DAO_EXCEPTION);
        }
        return lastUpdatedTime;
    }

    /**
     * @see APIManager#getLastUpdatedTimeOfAPIThumbnailImage(String)
     */
    @Override
    public String getLastUpdatedTimeOfAPIThumbnailImage(String apiId) throws APIManagementException {
        String lastUpdatedTime;
        try {
            lastUpdatedTime = apiDAO.getLastUpdatedTimeOfAPIThumbnailImage(apiId);
        } catch (APIMgtDAOException e) {
            String errorMsg =
                    "Error occurred while retrieving the last updated time of the thumbnail image of the API " + apiId;
            log.error(errorMsg, e);
            throw new APIManagementException(errorMsg, e, ExceptionCodes.APIMGT_DAO_EXCEPTION);
        }
        return lastUpdatedTime;
    }

    /**
     * @see APIManager#getLastUpdatedTimeOfApplication(String)
     */
    @Override
    public String getLastUpdatedTimeOfApplication(String applicationId) throws APIManagementException {
        String lastUpdatedTime;
        try {
            lastUpdatedTime = applicationDAO.getLastUpdatedTimeOfApplication(applicationId);
        } catch (APIMgtDAOException e) {
            String errorMsg =
                    "Error occurred while retrieving the last updated time of the application " + applicationId;
            log.error(errorMsg, e);
            throw new APIManagementException(errorMsg, e, ExceptionCodes.APIMGT_DAO_EXCEPTION);
        }
        return lastUpdatedTime;
    }

    @Override
    public String getLastUpdatedTimeOfComment(String commentId) throws APIManagementException {
        String lastUpdatedTime;
        try {
            lastUpdatedTime = apiDAO.getLastUpdatedTimeOfComment(commentId);
        } catch (APIMgtDAOException e) {
            String errorMsg =
                    "Error occurred while retrieving the last updated time of the comment " + commentId;
            log.error(errorMsg, e);
            throw new APIManagementException(errorMsg, e, ExceptionCodes.APIMGT_DAO_EXCEPTION);
        }
        return lastUpdatedTime;
    }

    /**
     * @see APIManager#getLastUpdatedTimeOfSubscription(String)
     */
    @Override
    public String getLastUpdatedTimeOfSubscription(String subscriptionId) throws APIManagementException {
        String lastUpdatedTime;
        try {
            lastUpdatedTime = apiSubscriptionDAO.getLastUpdatedTimeOfSubscription(subscriptionId);
        } catch (APIMgtDAOException e) {
            String errorMsg =
                    "Error occurred while retrieving the last updated time of the subscription " + subscriptionId;
            log.error(errorMsg, e);
            throw new APIManagementException(errorMsg, e, ExceptionCodes.APIMGT_DAO_EXCEPTION);
        }
        return lastUpdatedTime;
    }

    /**
     * @see APIManager#getLastUpdatedTimeOfThrottlingPolicy(String, String)
     */
    @Override
    public String getLastUpdatedTimeOfThrottlingPolicy(String policyLevel, String policyName)
            throws APIManagementException {
        String lastUpdatedTime;
        try {
            lastUpdatedTime = getPolicyDAO().getLastUpdatedTimeOfThrottlingPolicy(policyLevel, policyName);
        } catch (APIMgtDAOException e) {
            String errorMsg = "Error while retrieving last updated time of policy :" + policyLevel + "/" + policyName;
            log.error(errorMsg, e);
            throw new APIManagementException(errorMsg, e, ExceptionCodes.APIMGT_DAO_EXCEPTION);
        }
        return lastUpdatedTime;
    }

    /**
     * Returns the subscriptions for api
     *
     * @param apiId UUID of the API.
     * @return List of the subscriptions.
     * @throws APIManagementException   If failed to get list of subscriptions.
     */
    @Override
    public List<Subscription> getSubscriptionsByAPI(String apiId) throws APIManagementException {
        try {
            return apiSubscriptionDAO.getAPISubscriptionsByAPI(apiId);
        } catch (APIMgtDAOException e) {
            String errorMsg = "Couldn't find subscriptions for apiId " + apiId;
            log.error(errorMsg, e);
            throw new APIManagementException(errorMsg, e, ExceptionCodes.APIMGT_DAO_EXCEPTION);
        }
    }

    protected ApiDAO getApiDAO() {
        return apiDAO;
    }

    protected ApplicationDAO getApplicationDAO() {
        return applicationDAO;
    }

    protected APISubscriptionDAO getApiSubscriptionDAO() {
        return apiSubscriptionDAO;
    }

    protected PolicyDAO getPolicyDAO() {
        return policyDAO;
    }

    protected LabelDAO getLabelDAO() {
        return labelDAO;
    }

    protected WorkflowDAO getWorkflowDAO() {
        return workflowDAO;
    }

    protected String getUsername() {
        return username;
    }

    protected void setUsername(String username) {
        this.username = username;
    }

    public APILifecycleManager getApiLifecycleManager() {
        return apiLifecycleManager;
    }

    protected final void handleResourceAlreadyExistsException(String msg) throws APIMgtResourceAlreadyExistsException {
        log.error(msg);
        throw new APIMgtResourceAlreadyExistsException(msg);
    }
    
    /**
     * Retrieve workflow for given internal ref id
     * 
     * @param workflowRefId workflow reference id
     * @return Workflow workflow.
     * @throws APIMgtDAOException  If failed to get list of subscriptions.
     */
    public Workflow retrieveWorkflow(String workflowRefId) throws APIMgtDAOException {       
        try {
            return workflowDAO.retrieveWorkflow(workflowRefId);    
        } catch (APIMgtDAOException e) {
            String message = "Error while updating workflow entry";
            log.error(message);
            throw new APIMgtDAOException(message, e, ExceptionCodes.APIMGT_DAO_EXCEPTION);
        }        
    }
    
    protected void updateWorkflowEntries(Workflow workflow) throws APIManagementException {
        workflow.setUpdatedTime(LocalDateTime.now());
        try {
            getWorkflowDAO().updateWorkflowStatus(workflow);
            // TODO stats stuff
        } catch (APIMgtDAOException e) {
            String message = "Error while updating workflow entry";
            log.error(message);
            throw new APIManagementException(message, e, ExceptionCodes.APIMGT_DAO_EXCEPTION);
        }

    }

    protected void addWorkflowEntries(Workflow workflow) throws APIManagementException {

        try {
            getWorkflowDAO().addWorkflowEntry(workflow);
            // TODO stats publish
        } catch (APIMgtDAOException e) {
            String message = "Error while adding workflow entry";
            log.error(message);
            throw new APIManagementException(message, e, ExceptionCodes.APIMGT_DAO_EXCEPTION);
        }

    }
    
    protected void cleanupPendingTask(WorkflowExecutor executor, String internalWFReference, String workflowType)
            throws APIMgtDAOException {
        String externalWfReferenceId = getWorkflowDAO().getExternalWorkflowReferenceForPendingTask(internalWFReference,
                workflowType);
        if (externalWfReferenceId != null) {
            try {
                executor.cleanUpPendingTask(externalWfReferenceId);
            } catch (WorkflowException e) {
                String warn = "Failed to clean pending task for " + internalWFReference + " of " + workflowType;
                // failed cleanup processes are ignored to prevent failing the deletion process
                log.warn(warn, e.getLocalizedMessage());
            }
            getWorkflowDAO().deleteWorkflowEntryforExternalReference(externalWfReferenceId);
        }
    }
    
    @Override
    public Label getLabelByName(String labelName) throws APIManagementException {
        try {
            return labelDAO.getLabelByName(labelName);
        } catch (APIMgtDAOException e) {
            String message = "Error occured while retrieving Label information for " + labelName;
            log.error(message);
            throw new APIManagementException(message, e, ExceptionCodes.APIMGT_DAO_EXCEPTION);
        }
    }

    public GatewaySourceGenerator getGatewaySourceGenerator() {
        return gatewaySourceGenerator;
    }

    public APIGateway getApiGateway() {
        return apiGatewayPublisher;
    }
}
