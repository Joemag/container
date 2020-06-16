//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference
// Implementation, v2.2.4-2
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2016.02.25 at 04:54:56 PM CET
//

package org.opentosca.bus.application.api.soaphttp.model;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

/**
 * This object contains factory methods for each Java content interface and Java element interface generated in the
 * org.opentosca.bus.application.api.soaphttp.model package.
 * <p>
 * An ObjectFactory allows you to programatically construct new instances of the Java representation for XML content.
 * The Java representation of XML content can consist of schema derived interfaces and classes representing the binding
 * of schema type definitions, element declarations and model groups. Factory methods for each of these are provided in
 * this class.
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _GetResultResponse_QNAME =
        new QName("http://opentosca.org/appinvoker/", "getResultResponse");
    private final static QName _InvokeMethodWithNodeInstanceID_QNAME =
        new QName("http://opentosca.org/appinvoker/", "invokeMethodWithNodeInstanceID");
    private final static QName _InvokeMethodWithNodeInstanceIDResponse_QNAME =
        new QName("http://opentosca.org/appinvoker/", "invokeMethodWithNodeInstanceIDResponse");
    private final static QName _IsFinishedResponse_QNAME =
        new QName("http://opentosca.org/appinvoker/", "isFinishedResponse");
    private final static QName _InvokeMethodWithServiceInstanceIDResponse_QNAME =
        new QName("http://opentosca.org/appinvoker/", "invokeMethodWithServiceInstanceIDResponse");
    private final static QName _ApplicationBusException_QNAME =
        new QName("http://opentosca.org/appinvoker/", "ApplicationBusException");
    private final static QName _GetResult_QNAME = new QName("http://opentosca.org/appinvoker/", "getResult");
    private final static QName _InvokeMethodWithServiceInstanceID_QNAME =
        new QName("http://opentosca.org/appinvoker/", "invokeMethodWithServiceInstanceID");
    private final static QName _IsFinished_QNAME = new QName("http://opentosca.org/appinvoker/", "isFinished");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package:
     * org.opentosca.bus.application.api.soaphttp.model
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link IsFinishedResponse }
     */
    public IsFinishedResponse createIsFinishedResponse() {
        return new IsFinishedResponse();
    }

    /**
     * Create an instance of {@link InvokeMethodWithNodeInstanceIDResponse }
     */
    public InvokeMethodWithNodeInstanceIDResponse createInvokeMethodWithNodeInstanceIDResponse() {
        return new InvokeMethodWithNodeInstanceIDResponse();
    }

    /**
     * Create an instance of {@link InvokeMethodWithNodeInstanceID }
     */
    public InvokeMethodWithNodeInstanceID createInvokeMethodWithNodeInstanceID() {
        return new InvokeMethodWithNodeInstanceID();
    }

    /**
     * Create an instance of {@link GetResultResponse }
     */
    public GetResultResponse createGetResultResponse() {
        return new GetResultResponse();
    }

    /**
     * Create an instance of {@link IsFinished }
     */
    public IsFinished createIsFinished() {
        return new IsFinished();
    }

    /**
     * Create an instance of {@link InvokeMethodWithServiceInstanceID }
     */
    public InvokeMethodWithServiceInstanceID createInvokeMethodWithServiceInstanceID() {
        return new InvokeMethodWithServiceInstanceID();
    }

    /**
     * Create an instance of {@link GetResult }
     */
    public GetResult createGetResult() {
        return new GetResult();
    }

    /**
     * Create an instance of {@link ApplicationBusException }
     */
    public ApplicationBusException createApplicationBusException() {
        return new ApplicationBusException();
    }

    /**
     * Create an instance of {@link InvokeMethodWithServiceInstanceIDResponse }
     */
    public InvokeMethodWithServiceInstanceIDResponse createInvokeMethodWithServiceInstanceIDResponse() {
        return new InvokeMethodWithServiceInstanceIDResponse();
    }

    /**
     * Create an instance of {@link ParamsMapItemType }
     */
    public ParamsMapItemType createParamsMapItemType() {
        return new ParamsMapItemType();
    }

    /**
     * Create an instance of {@link ParamsMap }
     */
    public ParamsMap createParamsMap() {
        return new ParamsMap();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetResultResponse }{@code >}}
     */
    @XmlElementDecl(namespace = "http://opentosca.org/appinvoker/", name = "getResultResponse")
    public JAXBElement<GetResultResponse> createGetResultResponse(final GetResultResponse value) {
        return new JAXBElement<>(_GetResultResponse_QNAME, GetResultResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InvokeMethodWithNodeInstanceID }{@code >}}
     */
    @XmlElementDecl(namespace = "http://opentosca.org/appinvoker/", name = "invokeMethodWithNodeInstanceID")
    public JAXBElement<InvokeMethodWithNodeInstanceID> createInvokeMethodWithNodeInstanceID(final InvokeMethodWithNodeInstanceID value) {
        return new JAXBElement<>(_InvokeMethodWithNodeInstanceID_QNAME, InvokeMethodWithNodeInstanceID.class, null,
            value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InvokeMethodWithNodeInstanceIDResponse }{@code >}}
     */
    @XmlElementDecl(namespace = "http://opentosca.org/appinvoker/", name = "invokeMethodWithNodeInstanceIDResponse")
    public JAXBElement<InvokeMethodWithNodeInstanceIDResponse> createInvokeMethodWithNodeInstanceIDResponse(final InvokeMethodWithNodeInstanceIDResponse value) {
        return new JAXBElement<>(_InvokeMethodWithNodeInstanceIDResponse_QNAME,
            InvokeMethodWithNodeInstanceIDResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link IsFinishedResponse }{@code >}}
     */
    @XmlElementDecl(namespace = "http://opentosca.org/appinvoker/", name = "isFinishedResponse")
    public JAXBElement<IsFinishedResponse> createIsFinishedResponse(final IsFinishedResponse value) {
        return new JAXBElement<>(_IsFinishedResponse_QNAME, IsFinishedResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InvokeMethodWithServiceInstanceIDResponse }{@code >}}
     */
    @XmlElementDecl(namespace = "http://opentosca.org/appinvoker/", name = "invokeMethodWithServiceInstanceIDResponse")
    public JAXBElement<InvokeMethodWithServiceInstanceIDResponse> createInvokeMethodWithServiceInstanceIDResponse(final InvokeMethodWithServiceInstanceIDResponse value) {
        return new JAXBElement<>(_InvokeMethodWithServiceInstanceIDResponse_QNAME,
            InvokeMethodWithServiceInstanceIDResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ApplicationBusException }{@code >}}
     */
    @XmlElementDecl(namespace = "http://opentosca.org/appinvoker/", name = "ApplicationBusException")
    public JAXBElement<ApplicationBusException> createApplicationBusException(final ApplicationBusException value) {
        return new JAXBElement<>(_ApplicationBusException_QNAME, ApplicationBusException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetResult }{@code >}}
     */
    @XmlElementDecl(namespace = "http://opentosca.org/appinvoker/", name = "getResult")
    public JAXBElement<GetResult> createGetResult(final GetResult value) {
        return new JAXBElement<>(_GetResult_QNAME, GetResult.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InvokeMethodWithServiceInstanceID }{@code >}}
     */
    @XmlElementDecl(namespace = "http://opentosca.org/appinvoker/", name = "invokeMethodWithServiceInstanceID")
    public JAXBElement<InvokeMethodWithServiceInstanceID> createInvokeMethodWithServiceInstanceID(final InvokeMethodWithServiceInstanceID value) {
        return new JAXBElement<>(_InvokeMethodWithServiceInstanceID_QNAME, InvokeMethodWithServiceInstanceID.class,
            null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link IsFinished }{@code >}}
     */
    @XmlElementDecl(namespace = "http://opentosca.org/appinvoker/", name = "isFinished")
    public JAXBElement<IsFinished> createIsFinished(final IsFinished value) {
        return new JAXBElement<>(_IsFinished_QNAME, IsFinished.class, null, value);
    }
}
