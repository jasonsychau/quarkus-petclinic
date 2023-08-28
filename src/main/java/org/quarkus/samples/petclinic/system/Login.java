package org.quarkus.samples.petclinic.system;

import java.time.LocalDateTime;
import java.util.Collection;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.FormParam;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.qute.TemplateExtension;

@Entity
@Table(name = "accounts",
		uniqueConstraints = @UniqueConstraint(columnNames={"email"}))
public class Login extends PanacheEntity {

	@Column(name = "email")
	@NotEmpty
	@FormParam("address")
	public String email;

	@Column(name = "password")
	@NotEmpty
	@FormParam("password")
	public String password;

	@Column(name = "sessionToken")
	public String sessionToken;

	@Column(name = "tokenExpiry")
	public LocalDateTime tokenExpiry;

	public static Collection<Login> findByEmail(String email) {
        return Login.list("email", email);
    }
}
