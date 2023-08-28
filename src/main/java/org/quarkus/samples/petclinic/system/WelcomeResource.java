package org.quarkus.samples.petclinic.system;

import org.quarkus.samples.petclinic.system.Login;
import org.quarkus.samples.petclinic.system.Util;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import io.quarkus.qute.TemplateInstance;

@Path("/")
public class WelcomeResource {
    
    @Inject
    TemplatesLocale templates;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get(@Context HttpHeaders headers) {
        if (Util.authenticateRequest(headers)) {
            return templates.welcome(null, null);
        } else {
            return templates.enterLoginForm(null, new HashMap<>());
        }
    }
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Transactional
    /**
     * Renders the enterLogin.html
     * 
     * @return
     */
    public TemplateInstance enterLoginForm(@FormParam("email") String email, @FormParam("password") String password) {
        Map<String, String> result = Util.tryLogin(email, password);
        if (result.containsKey("success")) {
            return templates.welcome(result.get("email"), result.get("sessionToken"));
        } else {
            return templates.enterLoginForm(null, result);
        }
    }
}
