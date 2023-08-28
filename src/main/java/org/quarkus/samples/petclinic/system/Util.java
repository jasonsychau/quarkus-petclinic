package org.quarkus.samples.petclinic.system;

import org.quarkus.samples.petclinic.system.TemplatesLocale;
import org.quarkus.samples.petclinic.system.Login;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;

import io.quarkus.qute.TemplateInstance;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class Util {

    static public boolean authenticateRequest(HttpHeaders headers) {
        Map<String, Cookie> cookies = headers.getCookies();
        if (!cookies.containsKey("sessionToken") || !cookies.containsKey("email")) {
            return false;
        } else {
            String token = cookies.get("sessionToken").getValue();
            String email = cookies.get("email").getValue();
            Collection<Login> matchingAccounts = Login.findByEmail(email);
            if (matchingAccounts.isEmpty())
                return false;
        
            Login account = matchingAccounts.iterator().next();
            if (account.sessionToken == null || (account.sessionToken != null && !account.sessionToken.equals(token)) ||
                    account.tokenExpiry == null || (account.tokenExpiry != null && LocalDateTime.now().isAfter(account.tokenExpiry)))
                return false;

            return true;
        }
    }

    static public Map<String, String> tryLogin(String email, String password) {
        final Map<String, String> feedback = new HashMap<>();
        // check valid email
        if (email == null || password == null) {
            feedback.put("email", "Please enter email");
            feedback.put("password", "Please enter password");
            return feedback;
        }
        Pattern pattern = Pattern.compile("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(email);
        boolean matchFound = matcher.find();
        if (!matchFound) {
            feedback.put("email", "Please check entered email");
            return feedback;
        }

        // validate password
        pattern = Pattern.compile("^[\\w-!@]{6,20}$", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(password);
        matchFound = matcher.find();
        if (!matchFound) {
            feedback.put("password", "Please check entered password");
            return feedback;
        }

        Collection<Login> matchingAccounts = Login.findByEmail(email);
        if (matchingAccounts.isEmpty()) {
        // create account
            String pwHash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
            feedback.put("email", "Account not found");
            return feedback;
        } else {
            // authenticate password
            Login firstMatchingAccount = matchingAccounts.iterator().next();
            BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), firstMatchingAccount.password);
            if (!result.verified) {
                feedback.put("password", "Password is incorrect");
                return feedback;
            }
            
            // generate login token
            feedback.put("success", "true");
            String sessionToken = UUID.randomUUID().toString();
            LocalDateTime tokenExpiry = LocalDateTime.now().plusDays(1);
            firstMatchingAccount.sessionToken = sessionToken;
            firstMatchingAccount.tokenExpiry = tokenExpiry;
            firstMatchingAccount.persist();
            
            // grant access
            feedback.put("email", email);
            feedback.put("sessionToken", sessionToken);
            return feedback;
        }
    }
}
