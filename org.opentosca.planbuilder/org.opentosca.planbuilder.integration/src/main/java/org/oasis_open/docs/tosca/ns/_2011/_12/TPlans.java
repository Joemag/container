//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB)
// Reference Implementation, vJAXB 2.1.10 in JDK 6
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source
// schema.
// Generated on: 2013.04.02 at 04:58:44 PM CEST
//

package org.oasis_open.docs.tosca.ns._2011._12;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for tPlans complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tPlans">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="BuildPlan" type="{http://docs.oasis-open.org/tosca/ns/2011/12}tPlan" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *       &lt;attribute name="targetNamespace" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tPlans", propOrder = {"plan"})
public class TPlans {

    @XmlElement(name = "Plan", required = true)
    protected List<TPlan> plan;
    @XmlAttribute
    @XmlSchemaType(name = "anyURI")
    protected String targetNamespace;

    /**
     * Gets the value of the plan property.
     *
     * <p>
     * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
     * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
     * the plan property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getPlan().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link TPlan }
     */
    public List<TPlan> getPlan() {
        if (this.plan == null) {
            this.plan = new ArrayList<>();
        }
        return this.plan;
    }

    /**
     * Gets the value of the targetNamespace property.
     *
     * @return possible object is {@link String }
     */
    public String getTargetNamespace() {
        return this.targetNamespace;
    }

    /**
     * Sets the value of the targetNamespace property.
     *
     * @param value allowed object is {@link String }
     */
    public void setTargetNamespace(final String value) {
        this.targetNamespace = value;
    }
}
