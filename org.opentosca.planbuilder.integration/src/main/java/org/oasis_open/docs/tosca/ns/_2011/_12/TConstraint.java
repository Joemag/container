//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference
// Implementation, vJAXB 2.1.10 in JDK 6
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2013.04.02 at 04:58:44 PM CEST
//


package org.oasis_open.docs.tosca.ns._2011._12;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import org.w3c.dom.Element;


/**
 * <p>
 * Java class for tConstraint complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tConstraint">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;any processContents='lax' namespace='##other'/>
 *       &lt;/sequence>
 *       &lt;attribute name="constraintType" use="required" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tConstraint", propOrder = {"any"})
@XmlSeeAlso( {TPropertyConstraint.class})
public class TConstraint {

  @XmlAnyElement(lax = true)
  protected Object any;
  @XmlAttribute(required = true)
  @XmlSchemaType(name = "anyURI")
  protected String constraintType;

  /**
   * Gets the value of the any property.
   *
   * @return possible object is {@link Object } {@link Element }
   */
  public Object getAny() {
    return this.any;
  }

  /**
   * Sets the value of the any property.
   *
   * @param value allowed object is {@link Object } {@link Element }
   */
  public void setAny(final Object value) {
    this.any = value;
  }

  /**
   * Gets the value of the constraintType property.
   *
   * @return possible object is {@link String }
   */
  public String getConstraintType() {
    return this.constraintType;
  }

  /**
   * Sets the value of the constraintType property.
   *
   * @param value allowed object is {@link String }
   */
  public void setConstraintType(final String value) {
    this.constraintType = value;
  }

}
