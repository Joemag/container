//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference
// Implementation, vJAXB 2.1.10 in JDK 6
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2013.04.02 at 04:58:44 PM CEST
//

package org.oasis_open.docs.tosca.ns._2011._12;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

import org.w3c.dom.Element;

/**
 * <p>
 * Java class for tDocumentation complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tDocumentation">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;any processContents='lax' maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="source" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *       &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tDocumentation", propOrder = {"content"})
public class TDocumentation {

    @XmlMixed
    @XmlAnyElement(lax = true)
    protected List<Object> content;
    @XmlAttribute
    @XmlSchemaType(name = "anyURI")
    protected String source;
    @XmlAttribute(namespace = "http://www.w3.org/XML/1998/namespace")
    protected String lang;

    /**
     * Gets the value of the content property.
     *
     * <p>
     * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
     * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
     * the content property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getContent().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String } {@link Object } {@link Element }
     */
    public List<Object> getContent() {
        if (this.content == null) {
            this.content = new ArrayList<>();
        }
        return this.content;
    }

    /**
     * Gets the value of the source property.
     *
     * @return possible object is {@link String }
     */
    public String getSource() {
        return this.source;
    }

    /**
     * Sets the value of the source property.
     *
     * @param value allowed object is {@link String }
     */
    public void setSource(final String value) {
        this.source = value;
    }

    /**
     * Gets the value of the lang property.
     *
     * @return possible object is {@link String }
     */
    public String getLang() {
        return this.lang;
    }

    /**
     * Sets the value of the lang property.
     *
     * @param value allowed object is {@link String }
     */
    public void setLang(final String value) {
        this.lang = value;
    }
}
