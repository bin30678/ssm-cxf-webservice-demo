package com.example.cxfdemo.rest;

import com.example.cxfdemo.dto.PolicyInquiryRequest;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/policies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface PolicyResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Response getPolicies();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response createPolicy(com.example.cxfdemo.model.PolicyInfo policy);

    @GET
    @Path("/test-async-bank")
    @Produces(MediaType.APPLICATION_JSON)
    Response testAsyncBank();

    @GET
    @Path("/test-external-jar")
    @Produces(MediaType.APPLICATION_JSON)
    Response testExternalJar();

    @GET
    @Path("/test-rest-template")
    @Produces(MediaType.APPLICATION_JSON)
    Response testRestTemplate();

    @GET
    @Path("/test-clean-backup")
    @Produces(MediaType.APPLICATION_JSON)
    Response testCleanBackup(@QueryParam("dir") String dir, @QueryParam("days") Integer days);

    @POST
    @Path("/inquiry")
    Response inquire(PolicyInquiryRequest request);
}
