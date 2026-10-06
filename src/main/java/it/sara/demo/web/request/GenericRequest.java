package it.sara.demo.web.request;

import lombok.Getter;
import lombok.Setter;

/**
 * Base type of the request bodies of the web layer. Requests never reach the service layer:
 * web assemblers convert them into criteria.
 */
@Getter
@Setter
public class GenericRequest {
}
