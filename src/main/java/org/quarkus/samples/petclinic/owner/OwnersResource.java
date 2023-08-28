package org.quarkus.samples.petclinic.owner;

import org.quarkus.samples.petclinic.system.TemplatesLocale;
import org.quarkus.samples.petclinic.visit.Visit;
import org.quarkus.samples.petclinic.system.Util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import io.quarkus.qute.TemplateInstance;

@Path("/owners")
public class OwnersResource {

    @Inject
    TemplatesLocale templates;

    @Inject
    Validator validator;

    @GET
    @Path("/find")
    @Produces(MediaType.TEXT_HTML)
    /**
     * Renders the findOwners.html
     * 
     * @return
     */
    public TemplateInstance findTemplate(@Context HttpHeaders headers) {
        if (Util.authenticateRequest(headers)) {
            return templates.findOwners(Collections.EMPTY_LIST, null, null);
        } else {
            return templates.enterLoginForm(null, new HashMap<>());
        }
    }
    @POST
    @Path("/find")
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
            return templates.findOwners(Collections.EMPTY_LIST, result.get("email"), result.get("sessionToken"));
        } else {
            return templates.enterLoginForm(null, result);
        }
    }

    @GET
    @Path("new")
    @Produces(MediaType.TEXT_HTML)
    /**
     * Renders the createOrUpdateOwnerForm.html
     * 
     * @return
     */
    public TemplateInstance createTemplate(@Context HttpHeaders headers) {
        if (Util.authenticateRequest(headers)) {
            return templates.createOrUpdateOwnerForm(null, new HashMap<>(), null, null);
        } else {
            return templates.enterLoginForm(null, new HashMap<>());
        }
    }

    @GET
    @Path("{ownerId}/edit")
    @Produces(MediaType.TEXT_HTML)
    /**
     * Renders the createOrUpdateOwnerForm.html
     * 
     * @return
     */
    public TemplateInstance editTemplate(@PathParam("ownerId") Long ownerId) {
        return templates.createOrUpdateOwnerForm(Owner.findById(ownerId), new  HashMap<>(), null, null);
    }

    @GET
    @Path("{ownerId}")
    @Produces(MediaType.TEXT_HTML)
    /**
     * Renders the createOrUpdateOwnerForm.html
     * 
     * @return
     */
    public TemplateInstance showOwner(@PathParam("ownerId") Long ownerId) {
        return templates.ownerDetails(Owner.findById(ownerId));
    }

    @POST
    @Path("new")
    @Produces(MediaType.TEXT_HTML)
    @Transactional
    /**
     * Renders the createOrUpdateOwnerForm.html
     * 
     * @return
     */
    public TemplateInstance processCreationForm(@BeanParam Owner owner, @Context HttpHeaders headers, @FormParam("email") String email, @FormParam("password") String password) {
        if (!Util.authenticateRequest(headers)) {
            Map<String, String> result = Util.tryLogin(email, password);
            if (!result.containsKey("success")) {
                return templates.enterLoginForm(null, result);
            }
        }

        final Set<ConstraintViolation<Owner>> violations = validator.validate(owner);
        final Map<String, String> errors = new HashMap<>();
        if (!violations.isEmpty()) {
            
            for (ConstraintViolation<Owner> violation : violations) {
                errors.put(violation.getPropertyPath().toString(), violation.getMessage());
            }

            return templates.createOrUpdateOwnerForm(null, errors, email, password);

        } else {
            owner.persist();
            return templates.ownerDetails(owner);
        }
    }
    
    @POST
    @Path("{ownerId}/edit")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    /**
     * Renders the createOrUpdateOwnerForm.html
     * 
     * @return
     */
    public TemplateInstance processUpdateOwnerForm(@BeanParam Owner owner, @PathParam("ownerId") Long ownerId) {
        final Set<ConstraintViolation<Owner>> violations = validator.validate(owner);
        final Map<String, String> errors = new HashMap<>();
        if (!violations.isEmpty()) {
            
            for (ConstraintViolation<Owner> violation : violations) {
                errors.put(violation.getPropertyPath().toString(), violation.getMessage());
            }

            return templates.createOrUpdateOwnerForm(owner, errors, null, null);

        } else {
            // We need to reattach the Owner object. Since method is transactional, the update occurs automatically.
            return templates.ownerDetails(owner.attach());
        }
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    /**
     * Process the findOwners form
     */
    public TemplateInstance processFindForm(@QueryParam("lastName") String lastName) {
        Collection<Owner> owners = null;

        // allow parameterless GET request for /owners to return all records
        if (lastName == null || "".equals(lastName.trim())) {
            owners = Owner.listAll(); // empty string signifies broadest possible search
        } else {
            owners = Owner.findByLastName(lastName);
        }

        // find owners by last name
        if (owners.isEmpty()) {
            // no owners found
            return templates.findOwners(Arrays.asList("lastName not found"), null, null);
        }
        if (owners.size() == 1) {
            // 1 owner found
            Owner owner = owners.iterator().next();
            return templates.ownerDetails(setVisits(owner));
        }
        
        return templates.ownersList(owners);
    }

    protected Owner setVisits(Owner owner) {
        for (Pet pet : owner.pets) {
            pet.setVisitsInternal(Visit.findByPetId(pet.id));
        }
        return owner;
    }

}
