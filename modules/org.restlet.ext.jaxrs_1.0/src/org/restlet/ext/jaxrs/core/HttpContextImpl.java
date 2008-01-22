/*
 * Copyright 2005-2008 Noelios Consulting.
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the "License"). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * http://www.opensource.org/licenses/cddl1.txt See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL HEADER in each file and
 * include the License file at http://www.opensource.org/licenses/cddl1.txt If
 * applicable, add the following below this CDDL HEADER, with the fields
 * enclosed by brackets "[]" replaced with your own identifying information:
 * Portions Copyright [yyyy] [name of copyright owner]
 */
package org.restlet.ext.jaxrs.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.restlet.data.Conditions;
import org.restlet.data.Language;
import org.restlet.data.Method;
import org.restlet.data.Parameter;
import org.restlet.data.Preference;
import org.restlet.data.Status;
import org.restlet.data.Tag;
import org.restlet.ext.jaxrs.util.Util;
import org.restlet.util.Series;

/**
 * Implemetation of the JAX-RS interfaces {@link HttpHeaders}, {@link UriInfo}
 * and {@link Request}
 * 
 * @author Stephan Koops
 * 
 */
public class HttpContextImpl extends JaxRsUriInfo implements UriInfo, Request,
        HttpHeaders {

    private static final int STATUS_PREC_FAILED = Status.CLIENT_ERROR_PRECONDITION_FAILED
            .getCode();

    private org.restlet.data.Request restletRequest;

    private List<MediaType> acceptedMediaTypes;

    private Map<String, Cookie> cookies;

    private String language;

    private MediaType mediaType;

    private FormMulvaltivaluedMap requestHeaders;

    /**
     * 
     * @param restletRequest
     *                The Restlet request to wrap. Must not be null.
     * @param templateParametersEncoded
     *                The template parameters. Must not be null.
     */
    public HttpContextImpl(org.restlet.data.Request restletRequest,
            MultivaluedMap<String, String> templateParametersEncoded) {
        super(restletRequest.getResourceRef(), templateParametersEncoded);
        if (templateParametersEncoded == null)
            throw new IllegalArgumentException(
                    "The templateParameter must not be null");
        this.restletRequest = restletRequest;
    }

    // HttpHeaders methods

    /**
     * @see HttpHeaders#getAcceptableMediaTypes()
     */
    public List<MediaType> getAcceptableMediaTypes() {
        if (this.acceptedMediaTypes == null) {
            List<Preference<org.restlet.data.MediaType>> restletAccMediaTypes = restletRequest
                    .getClientInfo().getAcceptedMediaTypes();
            List<MediaType> accMediaTypes = new ArrayList<MediaType>(
                    restletAccMediaTypes.size());
            for (Preference<org.restlet.data.MediaType> mediaTypePref : restletAccMediaTypes)
                accMediaTypes.add(createJaxRsMediaType(mediaTypePref));
            this.acceptedMediaTypes = accMediaTypes;
        }
        return this.acceptedMediaTypes;
    }

    private MediaType createJaxRsMediaType(
            Preference<org.restlet.data.MediaType> mediaTypePref) {
        org.restlet.data.MediaType restletMediaType = mediaTypePref
                .getMetadata();
        Series<Parameter> rlMediaTypeParams = restletMediaType.getParameters();
        Map<String, String> parameters = null;
        if (!rlMediaTypeParams.isEmpty()) {
            parameters = new HashMap<String, String>();
            for (Parameter p : rlMediaTypeParams)
                parameters.put(p.getName(), p.getValue());
        }
        return new MediaType(restletMediaType.getMainType(), restletMediaType
                .getSubType());
    }

    /**
     * Get any cookies that accompanied the request.
     * 
     * @return a map of cookie name (String) to Cookie.
     * @see HttpHeaders#getCookies()
     */
    public Map<String, Cookie> getCookies() {
        if (this.cookies == null) {
            Map<String, Cookie> c = new HashMap<String, Cookie>();
            for (org.restlet.data.Cookie rc : restletRequest.getCookies()) {
                Cookie cookie = Util.convertCookie(rc);
                c.put(cookie.getName(), cookie);
            }
            this.cookies = c;
        }
        return this.cookies;
    }

    /**
     * @see HttpHeaders#getLanguage()
     */
    public String getLanguage() {
        return language;
    }

    /**
     * @see HttpHeaders#getMediaType()
     */
    public MediaType getMediaType() {
        return this.mediaType;
    }

    /**
     * @see HttpHeaders#getRequestHeaders()
     */
    public MultivaluedMap<String, String> getRequestHeaders() {
        if (this.requestHeaders == null) {
            this.requestHeaders = new FormMulvaltivaluedMap(Util
                    .getHttpHeaders(restletRequest));
        }
        return this.requestHeaders;
    }

    /**
     * Evaluate request preconditions based on the passed in value.
     * 
     * @param entityTag
     *                an ETag for the current state of the resource
     * @return null if the preconditions are met or a Response that should be
     *         returned if the preconditions are not met.
     * 
     * @see javax.ws.rs.core.Request#evaluatePreconditions(javax.ws.rs.core.EntityTag)
     * @see #evaluatePreconditions(Date, EntityTag)
     */
    public Response evaluatePreconditions(EntityTag entityTag) {
        return evaluatePreconditions(null, entityTag);
    }

    /**
     * Evaluate request preconditions based on the passed in value.
     * 
     * @param lastModified
     *                a date that specifies the modification date of the
     *                resource
     * @return null if the preconditions are met or a Response that should be
     *         returned if the preconditions are not met.
     * @see javax.ws.rs.core.Request#evaluatePreconditions(java.util.Date)
     * @see #evaluatePreconditions(Date, EntityTag)
     */
    public Response evaluatePreconditions(Date lastModified) {
        return evaluatePreconditions(lastModified, null);
    }

    /**
     * Evaluate request preconditions based on the passed in value.
     * 
     * @param lastModified
     *                a date that specifies the modification date of the
     *                resource
     * @param entityTag
     *                an ETag for the current state of the resource
     * @return null if the preconditions are met or a Response that should be
     *         returned if the preconditions are not met.<br                 />
     *         TODO JSR311: die evaluatePrecondition kann auch be erfüllten
     *         Precodutions ne Response zurückeben, z.B. bei
     *         "if-Unmodified-Since"
     * @see javax.ws.rs.core.Request#evaluatePreconditions(java.util.Date,
     *      javax.ws.rs.core.EntityTag)
     * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.3.5">RFC
     *      2616, section 10.3.5: Status 304: Not Modiied</a>
     * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.13">RFC
     *      2616, section 10.4.13: Status 412: Precondition Failed</a>
     * @see <a href="http://tools.ietf.org/html/rfc2616#section-13.3">RFC 2616,
     *      section 13.3: (Caching) Validation Model</a>
     * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.24">RFC 2616,
     *      section 14.24: Header "If-Match"</a>
     * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.25">RFC 2616,
     *      section 14.25: Header "If-Modified-Since"</a>
     * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.26">RFC 2616,
     *      section 14.26: Header "If-None-Match"</a>
     * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.28">RFC 2616,
     *      section 14.28: Header "If-Unmodified-Since"</a>
     */
    public Response evaluatePreconditions(Date lastModified, EntityTag entityTag) {
        if (lastModified == null && entityTag == null)
            return null;
        ResponseBuilder rb = null;
        Method requestMethod = restletRequest.getMethod();
        Conditions conditions = this.restletRequest.getConditions();
        if (lastModified != null) {
            // Header "If-Modified-Since"
            Date modSinceCond = conditions.getModifiedSince();
            if (modSinceCond != null) {
                if (modSinceCond.after(lastModified)) {
                    // the Entity was not changed
                    boolean readRequest = requestMethod.equals(Method.GET)
                            || requestMethod.equals(Method.HEAD);
                    if (readRequest) {
                        rb = Response.notModified();
                        rb.lastModified(lastModified);
                        rb.tag(entityTag);
                    } else {
                        return precFailed("The entity was not modified since "
                                + Util.formatDate(modSinceCond, false));
                    }
                } else {
                    // entity was changed -> check for other precoditions
                }
            }
            // Header "If-Unmodified-Since"
            Date unmodSinceCond = conditions.getUnmodifiedSince();
            if (unmodSinceCond != null) {
                if (unmodSinceCond.after(lastModified)) {
                    // entity was not changed -> Web Service must recalculate it
                    return null;
                } else {
                    // the Entity was changed
                    return precFailed("The entity was modified since "
                            + Util.formatDate(unmodSinceCond, false));
                }
            }
        }
        if (entityTag != null) {
            Tag actualEntityTag = Util.convertEntityTag(entityTag);
            // Header "If-Match"
            List<Tag> requestMatchETags = conditions.getMatch();
            if (!requestMatchETags.isEmpty()) {
                boolean match = checkIfOneMatch(requestMatchETags,
                        actualEntityTag);
                if (!match) {
                    return precFailed("The entity does not match Entity Tag "
                            + entityTag);
                }
            } else {
                // default answer to the request
            }
            // Header "If-None-Match"
            List<Tag> requestNoneMatchETags = conditions.getNoneMatch();
            if (!requestNoneMatchETags.isEmpty()) {
                boolean match = checkIfOneMatch(requestNoneMatchETags,
                        actualEntityTag);
                if (match) {
                    return precFailed("The entity matches Entity Tag "
                            + entityTag);
                }
            } else {
                // default answer to the request
            }
        }
        if (rb != null)
            return rb.build();
        return null;
    }

    /**
     * Creates a response with status 412 (Precondition Failed).
     * 
     * @param message
     *                Plain Text error message. Will be returned as entity.
     * @return Returns a response with status 412 (Precondition Failed) and the
     *         given message as entity.
     */
    private Response precFailed(String message) {
        ResponseBuilder rb = Response.status(STATUS_PREC_FAILED);
        rb.entity(message);
        rb.language(Language.ENGLISH.getName());
        rb.type(Util.convertMediaType(org.restlet.data.MediaType.TEXT_PLAIN,
                null));
        return rb.build();
    }

    private boolean checkIfOneMatch(List<Tag> requestETags, Tag entityTag) {
        if (entityTag.isWeak())
            return false;
        for (Tag requestETag : requestETags) {
            if (entityTag.equals(requestETag))
                return true;
        }
        return false;
    }

    /**
     * Select the representation variant that best matches the request. More
     * explicit variants are chosen ahead of less explicit ones. A vary header
     * is computed from the supplied list and automatically added to the
     * response.
     * 
     * @param variants
     *                a list of Variant that describe all of the available
     *                representation variants.
     * @return the variant that best matches the request.
     * @see Variant.VariantListBuilder
     * @throws IllegalArgumentException
     *                 if variants is null or empty.
     * @see Request#selectVariant(List)
     */
    public Variant selectVariant(List<Variant> variants)
            throws IllegalArgumentException {
        if (variants == null || variants.isEmpty())
            throw new IllegalArgumentException();
        List<org.restlet.resource.Variant> restletVariants = Util
                .convertVariants(variants);
        org.restlet.resource.Variant bestRestlVar = restletRequest
                .getClientInfo().getPreferredVariant(restletVariants, null);
        Variant bestVariant = Util.convertVariant(bestRestlVar);
        // TODO Response.setVaryHeader(bestVariant)
        return bestVariant;
    }
}