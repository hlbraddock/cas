/**
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.jasig.cas.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;

import org.jasig.cas.server.CentralAuthenticationService;
import org.jasig.cas.authentication.principal.WebApplicationService;
import org.jasig.cas.server.authentication.Credential;
import org.jasig.cas.server.authentication.DefaultUrlCredentialImpl;
import org.jasig.cas.server.session.Assertion;
import org.jasig.cas.server.session.UnauthorizedServiceException;
import org.jasig.cas.ticket.proxy.ProxyHandler;
import org.jasig.cas.validation.ValidationSpecification;
import org.jasig.cas.validation.Cas20ProtocolValidationSpecification;
import org.jasig.cas.web.support.ArgumentExtractor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

/**
 * Process the /validate and /serviceValidate URL requests.
 * <p>
 * Obtain the Service Ticket and Service information and present them to the CAS
 * validation services. Receive back an Assertion containing the user Principal
 * and (possibly) a chain of Proxy Principals. Store the Assertion in the Model
 * and chain to a View to generate the appropriate response (CAS 1, CAS 2 XML,
 * SAML, ...).
 * 
 * @author Scott Battaglia
 * @version $Revision$ $Date$
 * @since 3.0
 */
public class ServiceValidateController extends AbstractController {

    /** View if Service Ticket Validation Fails. */
    private static final String DEFAULT_SERVICE_FAILURE_VIEW_NAME = "casServiceFailureView";

    /** View if Service Ticket Validation Succeeds. */
    private static final String DEFAULT_SERVICE_SUCCESS_VIEW_NAME = "casServiceSuccessView";

    /** Constant representing the PGTIOU in the model. */
    private static final String MODEL_PROXY_GRANTING_TICKET_IOU = "pgtIou";

    /** Constant representing the Assertion in the model. */
    private static final String MODEL_ASSERTION = "assertion";

    /** The CORE which we will delegate all requests to. */
    @NotNull
    private CentralAuthenticationService centralAuthenticationService;

    /** The validation protocol we want to use. */
    @NotNull
    private Class<?> validationSpecificationClass = Cas20ProtocolValidationSpecification.class;

    /** The proxy handler we want to use with the controller. */
    @NotNull
    private ProxyHandler proxyHandler;

    /** The view to redirect to on a successful validation. */
    @NotNull
    private String successView = DEFAULT_SERVICE_SUCCESS_VIEW_NAME;

    /** The view to redirect to on a validation failure. */
    @NotNull
    private String failureView = DEFAULT_SERVICE_FAILURE_VIEW_NAME;

    /** Extracts parameters from Request object. */
    @NotNull
    private ArgumentExtractor argumentExtractor;

    /**
     * Overrideable method to determine which credentials to use to grant a
     * proxy granting ticket. Default is to use the pgtUrl.
     * 
     * @param request the HttpServletRequest object.
     * @return the credentials or null if there was an error or no credentials
     * provided.
     */
    protected Credential getServiceCredentialsFromRequest(
        final HttpServletRequest request) {
        final String pgtUrl = request.getParameter("pgtUrl");
        if (StringUtils.hasText(pgtUrl)) {
            try {
                return new DefaultUrlCredentialImpl(pgtUrl);
            } catch (final Exception e) {
                logger.error("Error constructing pgtUrl", e);
            }
        }

        return null;
    }

    protected void initBinder(final HttpServletRequest request,
        final ServletRequestDataBinder binder) {
        binder.setRequiredFields("renew");
    }

    protected final ModelAndView handleRequestInternal(
        final HttpServletRequest request, final HttpServletResponse response)
        throws Exception {
        final WebApplicationService service = this.argumentExtractor.extractService(request);
        final String serviceTicketId = service != null ? service.getArtifactId() : null;

        if (service == null
            || serviceTicketId == null) {
            return generateErrorView("INVALID_REQUEST", "INVALID_REQUEST", null);
        }

        try {
            final Credential serviceCredentials = getServiceCredentialsFromRequest(request);
            String proxyGrantingTicketId = null;

            // XXX should be able to validate AND THEN use
            if (serviceCredentials != null) {
                    proxyGrantingTicketId = this.centralAuthenticationService
                        .delegateTicketGrantingTicket(serviceTicketId,
                            serviceCredentials);
            }

            final Assertion assertion = this.centralAuthenticationService.validateServiceTicket(serviceTicketId, service);

            final ValidationSpecification validationSpecification = this.getCommandClass();
            final ServletRequestDataBinder binder = new ServletRequestDataBinder(validationSpecification, "validationSpecification");
            initBinder(request, binder);
            binder.bind(request);

            if (!validationSpecification.isSatisfiedBy(assertion)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("ServiceTicket [" + serviceTicketId + "] does not satisfy validation specification.");
                }
                return generateErrorView("INVALID_TICKET", "INVALID_TICKET_SPEC", null);
            }

            final ModelAndView success = new ModelAndView(this.successView);
            success.addObject(MODEL_ASSERTION, assertion);

            if (serviceCredentials != null && proxyGrantingTicketId != null) {
                final String proxyIou = this.proxyHandler.handle(serviceCredentials, proxyGrantingTicketId);
                success.addObject(MODEL_PROXY_GRANTING_TICKET_IOU, proxyIou);
            }

            return success;
        } catch (final UnauthorizedServiceException e) {
            return generateErrorView(e.getMessage(), e.getMessage(), null);
        }
    }

    private ModelAndView generateErrorView(final String code, final String description, final Object[] args) {
        final ModelAndView modelAndView = new ModelAndView(this.failureView);
        final String convertedDescription = getMessageSourceAccessor().getMessage(description, args, description);
        modelAndView.addObject("code", code);
        modelAndView.addObject("description", convertedDescription);

        return modelAndView;
    }

    private ValidationSpecification getCommandClass() {
        try {
            return (ValidationSpecification) this.validationSpecificationClass.newInstance();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * @param centralAuthenticationService The centralAuthenticationService to
     * set.
     */
    public void setCentralAuthenticationService(
        final CentralAuthenticationService centralAuthenticationService) {
        this.centralAuthenticationService = centralAuthenticationService;
    }

    public void setArgumentExtractor(
        final ArgumentExtractor argumentExtractor) {
        this.argumentExtractor = argumentExtractor;
    }

    /**
     * @param validationSpecificationClass The authenticationSpecificationClass
     * to set.
     */
    public void setValidationSpecificationClass(
        final Class<?> validationSpecificationClass) {
        this.validationSpecificationClass = validationSpecificationClass;
    }

    /**
     * @param failureView The failureView to set.
     */
    public void setFailureView(final String failureView) {
        this.failureView = failureView;
    }

    /**
     * @param successView The successView to set.
     */
    public void setSuccessView(final String successView) {
        this.successView = successView;
    }

    /**
     * @param proxyHandler The proxyHandler to set.
     */
    public void setProxyHandler(final ProxyHandler proxyHandler) {
        this.proxyHandler = proxyHandler;
    }
}
