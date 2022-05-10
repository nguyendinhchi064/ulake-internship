package org.usth.ict.ulake.dashboard.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.usth.ict.ulake.dashboard.extension.CoreService;
import org.usth.ict.ulake.dashboard.model.ObjectModel;
import org.usth.ict.ulake.dashboard.model.extension.ExtensionModel;
import org.usth.ict.ulake.dashboard.model.query.FilterModel;

@Path("/object")
@Tag(name = "Object")
public class ObjectResource {
    @Inject
    JsonWebToken jwt;

    @GET
    @Path("/health")
    @PermitAll
    @Produces(MediaType.TEXT_PLAIN)
    public String health() {
        return "OK";
    }

    @Inject
    @RestClient
    CoreService coreSvc;

    @GET
    @RolesAllowed({"User", "Admin"})
    @Produces(MediaType.APPLICATION_JSON)
    public ExtensionModel<List<ObjectModel>> object(
        @QueryParam("filter") List<String> filterStr) {
        String bearer = "bearer " + jwt.getRawToken();
        var filters = new ArrayList<FilterModel>();
        for (String f : filterStr) {
            filters.add(new FilterModel(f));
        }
        var objects =  coreSvc.getListObject(bearer);
        if (objects.getCode() == 200) {
            for (var filter : filters) {
                objects.setResp(
                    objects.getResp()
                    .stream()
                    .filter(o -> filter.filter(o))
                    .collect(Collectors.toList())
                );
            }
        }
        return objects;
    }

    @GET
    @Path("/{cid}/data")
    @RolesAllowed({"User", "Admin"})
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response objectData(@PathParam("cid") String cid) {
        String bearer = "Bearer " + jwt.getRawToken();
        InputStream is = coreSvc.getObjectData(cid, bearer);
        var stream = new StreamingOutput() {
            @Override
            public void write(OutputStream os) throws IOException {
                is.transferTo(os);
            }
        };
        return Response.ok(stream).build();
    }
}