//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference
// Implementation, v2.2.4-2
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2016.07.12 at 02:53:01 PM CEST
//

package org.opentosca.bus.management.invocation.plugin.script.model.artifacttypes;

import javax.xml.bind.annotation.XmlRegistry;

/**
 * This object contains factory methods for each Java content interface and Java element interface generated in the
 * org.opentosca.bus.management.invocation.plugin.script.model.artifacttypes package.
 * <p>
 * An ObjectFactory allows you to programatically construct new instances of the Java representation for XML content.
 * The Java representation of XML content can consist of schema derived interfaces and classes representing the binding
 * of schema type definitions, element declarations and model groups. Factory methods for each of these are provided in
 * this class.
 */
@XmlRegistry
public class ObjectFactory {

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package:
     * org.opentosca.bus.management.plugins.remote.service.impl.artifacttypes
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Artifacttype }
     */
    public Artifacttype createArtifacttype() {
        return new Artifacttype();
    }

    /**
     * Create an instance of {@link Packagestype }
     */
    public Packagestype createPackagestype() {
        return new Packagestype();
    }

    /**
     * Create an instance of {@link Commandstype }
     */
    public Commandstype createCommandstype() {
        return new Commandstype();
    }
}
