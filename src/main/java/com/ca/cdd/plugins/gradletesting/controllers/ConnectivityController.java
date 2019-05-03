package com.ca.cdd.plugins.gradletesting.controllers;

import com.ca.cdd.plugins.shared.utils.SourceControlUtils;
import com.ca.rp.plugins.dto.model.ConnectivityInput;
import com.ca.rp.plugins.dto.model.ConnectivityResult;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Created by menyo01 on 21/12/2017.
 */

@Path("connectivity-tests")
public class ConnectivityController {

    @POST
    @Path("/connect")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ConnectivityResult connect(ConnectivityInput connectivityInput) {
        return SourceControlUtils.testConnectivity(connectivityInput.getEndPointProperties());
    }

    @GET
    @Path("/ping")
    public Response ping() {
        return Response.ok().build();
    }
}
