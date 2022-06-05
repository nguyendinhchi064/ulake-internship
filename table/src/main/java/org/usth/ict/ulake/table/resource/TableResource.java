package org.usth.ict.ulake.table.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usth.ict.ulake.table.model.TableMetadata;
import org.usth.ict.ulake.table.persistence.TableCellRepository;
import org.usth.ict.ulake.table.persistence.TableColumnRepository;
import org.usth.ict.ulake.table.persistence.TableRepository;
import org.usth.ict.ulake.table.persistence.TableRowRepository;
import org.usth.ict.ulake.common.model.LakeHttpResponse;


@Path("/table")
@Produces(MediaType.APPLICATION_JSON)
public class TableResource {
    private static final Logger log = LoggerFactory.getLogger(TableResource.class);

    @Inject
    LakeHttpResponse response;

    @Inject
    TableRepository repo;

    @Inject
    TableRowRepository repoRow;

    @Inject
    TableColumnRepository repoColumn;

    @Inject
    TableCellRepository repoCell;

    @GET
    @Operation(summary = "List all tables")
    @RolesAllowed({ "User", "Admin" })
    public Response all() {
        return response.build(200, "", repo.listAll());
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({ "User", "Admin" })
    @Operation(summary = "Get one table info")
    public Response one(@PathParam("id") @Parameter(description = "User id to search") Long id) {
        return response.build(200, null, repo.findById(id));
    }


    @GET
    @Path("/{id}/columns")
    @RolesAllowed({ "User", "Admin" })
    @Operation(summary = "Get table columns")
    public Response column(@PathParam("id") @Parameter(description = "User id to search") Long id) {
        HashMap<String, String> colInfo = new HashMap<>();
        var cols = repoColumn.find("table.id", id);
        for (var col: cols.list()) {
            colInfo.put(col.columnName, col.dataType);
        }
        return response.build(200, null, colInfo);
    }

    @GET
    @Path("/{id}/data")
    @RolesAllowed({ "User", "Admin" })
    @Operation(summary = "Get one table data")
    public Response data(@PathParam("id") @Parameter(description = "User id to search") Long id) {
        return response.build(200, null, repo.findById(id));
    }

    @POST
    @RolesAllowed({ "User", "Admin" })
    @Operation(summary = "Make a new table")
    public Response post(@RequestBody(description = "Multipart form data. metadata: extra json info " +
                            "{name:'table name', format: 'csv/xls'}). file: csv/xls data to save")
                        MultipartFormDataInput input) throws IOException {
        TableMetadata meta = null;
        InputStream is = null;

        // iterate through form data to extract metadata and file
        Map<String, List<InputPart>> formDataMap = input.getFormDataMap();
        for (var formData : formDataMap.entrySet()) {
            // log.info("POST: {} {}", formData.getKey(), formData.getValue().get(0).getBodyAsString());
            if (formData.getKey().equals("metadata")) {
                try {
                    String metaJson = formData.getValue().get(0).getBodyAsString();
                    meta = mapper.readValue(metaJson, LakeObjectMetadata.class);
                } catch (JsonProcessingException e) {
                    log.error("error parsing metadata json {}", e.getMessage());
                }
            } else if (formData.getKey().equals("file")) {
                is = formData.getValue().get(0).getBody(InputStream.class, null);
            }
        }

        if (meta == null || is == null) {
            return response.build(403);
        }

        // make a new object, if any
        LakeGroup group = null;
        if (meta.getGroupId() != null) {
            group = groupRepo.find("gid", meta.getGroupId()).firstResult();
        }

        // save to backend
        String cid = fs.create(meta.getName(), meta.getLength(), is);
        log.info("POST: object storage returned cid={}", cid);

        // save a new object to metadata RDBMS
        LakeObject object = new LakeObject();
        object.setCid(cid);
        Long now = new Date().getTime();
        object.setCreateTime(now);
        object.setAccessTime(now);
        object.setParentId(0L);
        object.setGroup(group);
        repo.persist(object);
        return response.build(200, null, object);

        return response.build(200, null, results);
    }

    @GET
    @Path("/stats")
    @Operation(summary = "Statistics about tabular datas")
    @RolesAllowed({ "User", "Admin" })
    public Response tableStats(@HeaderParam("Authorization") String bearer) {
        // get requests from other service
        HashMap<String, Object> ret = new HashMap<>();
        return response.build(200, "", ret);
    }
}