package models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload sent to POST /api/users and PUT /api/users/{id}.
 *
 * <p>Lombok generates the getters, setters, constructors and builder; Jackson
 * serialises the instance straight onto the wire, so no hand-written JSON
 * strings appear anywhere in the framework.
 *
 * <p>{@code NON_NULL} inclusion means a partially populated request — a PATCH
 * that only changes the job, say — serialises without null members.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserRequest {

    /** The user's display name. */
    private String name;

    /** The user's job title. */
    private String job;
}
