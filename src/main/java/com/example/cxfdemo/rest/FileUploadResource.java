package com.example.cxfdemo.rest;

import com.example.cxfdemo.service.FileStorageService;
import javax.annotation.Resource;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Component
@Path("/files")
public class FileUploadResource {

    @Resource
    private FileStorageService fileStorageService;

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadFile(@Multipart(value = "file", required = false) Attachment fileAttachment,
                               @Multipart(value = "description", required = false) String description) {
        
        if (fileAttachment == null) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "BAD_REQUEST");
            error.put("message", "缺�? file 欄�?");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        try {
            String fileName = fileAttachment.getContentDisposition().getParameter("filename");
            if (fileName == null || fileName.trim().isEmpty()) {
                fileName = "unknown_file";
            }
            String contentType = fileAttachment.getContentType().toString();
            InputStream is = fileAttachment.getDataHandler().getInputStream();

            com.example.cxfdemo.model.FileUploadRecord record = fileStorageService.store(is, fileName, contentType, description);

            Map<String, String> result = new HashMap<>();
            result.put("status", "SUCCESS");
            result.put("storedName", record.getStoredName());
            return Response.ok(result).build();

        } catch (Exception ex) {
            Map<String, String> error = new HashMap<>();
            error.put("status", "FAILED");
            error.put("message", ex.getMessage() != null ? ex.getMessage() : "檔�?上傳失�?");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }
}
