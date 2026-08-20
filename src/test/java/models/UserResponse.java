package models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A user as the API returns it.
 *
 * <p>One class covers every user-shaped response the endpoints produce, because
 * reqres returns different subsets depending on the call:
 *
 * <ul>
 *   <li>GET /api/users/{id} and GET /api/users — id, email, first_name,
 *       last_name, avatar</li>
 *   <li>POST /api/users — name, job, id, createdAt</li>
 *   <li>PUT /api/users/{id} — name, job, updatedAt</li>
 * </ul>
 *
 * <p>{@code ignoreUnknown = true} matters here: it means a field added by the
 * API in future does not break deserialisation, which is the difference between
 * a suite that survives an upstream change and one that fails wholesale.
 *
 * <p>{@code id} is a String rather than an int on purpose — the read endpoints
 * return a number, but POST returns a quoted string. A numeric type would throw
 * on create.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserResponse {

    /** Identifier. String because POST returns it quoted and GET returns a number. */
    private String id;

    private String email;

    /** Mapped explicitly: the API uses snake_case, Java uses camelCase. */
    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    private String avatar;

    /** Present on create and update responses. */
    private String name;

    /** Present on create and update responses. */
    private String job;

    /** ISO-8601 timestamp returned by POST. */
    private String createdAt;

    /** ISO-8601 timestamp returned by PUT and PATCH. */
    private String updatedAt;

    /**
     * @return the numeric id, or -1 when it is absent or non-numeric
     */
    public int getIdAsInt() {
        try {
            return id == null ? -1 : Integer.parseInt(id.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
