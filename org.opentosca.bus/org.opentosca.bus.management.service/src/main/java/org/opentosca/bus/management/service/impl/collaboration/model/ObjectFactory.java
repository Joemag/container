// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference
// Implementation, v2.2.4-2
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2018.07.05 at 09:07:58 PM CEST

package org.opentosca.bus.management.service.impl.collaboration.model;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

/**
 * This object contains factory methods for each Java content interface and Java element interface generated in the
 * org.opentosca.bus.management.service.impl.collaboration.model package.
 * <p>
 * An ObjectFactory allows you to programatically construct new instances of the Java representation for XML content.
 * The Java representation of XML content can consist of schema derived interfaces and classes representing the binding
 * of schema type definitions, element declarations and model groups. Factory methods for each of these are provided in
 * this class.
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _CollaborationMessage_QNAME =
        new QName("http://collaboration.org/schema", "CollaborationMessage");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package:
     * org.opentosca.bus.management.service.impl.collaboration.model
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link CollaborationMessage }
     */
    public CollaborationMessage createCollaborationMessage() {
        return new CollaborationMessage();
    }

    /**
     * Create an instance of {@link KeyValueMap }
     */
    public KeyValueMap createKeyValueMap() {
        return new KeyValueMap();
    }

    /**
     * Create an instance of {@link KeyValueType }
     */
    public KeyValueType createKeyValueType() {
        return new KeyValueType();
    }

    /**
     * Create an instance of {@link Doc }
     */
    public Doc createDoc() {
        return new Doc();
    }

    /**
     * Create an instance of {@link InstanceDataMatchingRequest }
     */
    public InstanceDataMatchingRequest createInstanceDataMatchingRequest() {
        return new InstanceDataMatchingRequest();
    }

    /**
     * Create an instance of {@link BodyType }
     */
    public BodyType createBodyType() {
        return new BodyType();
    }

    /**
     * Create an instance of {@link IAInvocationRequest }
     */
    public IAInvocationRequest createIAInvocationRequest() {
        return new IAInvocationRequest();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CollaborationMessage }{@code >}}
     */
    @XmlElementDecl(namespace = "http://collaboration.org/schema", name = "CollaborationMessage")
    public JAXBElement<CollaborationMessage> createCollaborationMessage(final CollaborationMessage value) {
        return new JAXBElement<>(_CollaborationMessage_QNAME, CollaborationMessage.class, null, value);
    }
}
