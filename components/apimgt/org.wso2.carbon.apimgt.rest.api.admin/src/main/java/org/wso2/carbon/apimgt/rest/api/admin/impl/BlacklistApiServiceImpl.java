package org.wso2.carbon.apimgt.rest.api.admin.impl;

import org.wso2.carbon.apimgt.rest.api.admin.*;
import org.wso2.carbon.apimgt.rest.api.admin.dto.*;


import java.util.List;
import org.wso2.carbon.apimgt.rest.api.admin.NotFoundException;

import java.io.InputStream;

import org.wso2.msf4j.formparam.FormDataParam;
import org.wso2.msf4j.formparam.FileInfo;
import org.wso2.msf4j.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

public class BlacklistApiServiceImpl extends BlacklistApiService {
    @Override
    public Response blacklistConditionIdDelete(String conditionId
, String ifMatch
, String ifUnmodifiedSince
 , Request request) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response blacklistConditionIdGet(String conditionId
, String ifNoneMatch
, String ifModifiedSince
 , Request request) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response blacklistGet(String accept
, String ifNoneMatch
, String ifModifiedSince
 , Request request) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
    @Override
    public Response blacklistPost(BlockingConditionDTO body
, String contentType
 , Request request) throws NotFoundException {
        // do some magic!
        return Response.ok().entity(new ApiResponseMessage(ApiResponseMessage.OK, "magic!")).build();
    }
}
