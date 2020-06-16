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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.w3c.dom.Element;

/**
 * <p>
 * Java class for tPlan complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="tPlan">
 *   &lt;complexContent>
 *     &lt;extension base="{http://docs.oasis-open.org/tosca/ns/2011/12}tExtensibleElements">
 *       &lt;sequence>
 *         &lt;element name="Precondition" type="{http://docs.oasis-open.org/tosca/ns/2011/12}tCondition" minOccurs="0"/>
 *         &lt;element name="InputParameters" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="InputParameter" type="{http://docs.oasis-open.org/tosca/ns/2011/12}tParameter" maxOccurs="unbounded"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="OutputParameters" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="OutputParameter" type="{http://docs.oasis-open.org/tosca/ns/2011/12}tParameter" maxOccurs="unbounded"/>
 *                 &lt;/sequence>
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;choice>
 *           &lt;element name="PlanModel">
 *             &lt;complexType>
 *               &lt;complexContent>
 *                 &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                   &lt;sequence>
 *                     &lt;any processContents='lax' namespace='##other'/>
 *                   &lt;/sequence>
 *                 &lt;/restriction>
 *               &lt;/complexContent>
 *             &lt;/complexType>
 *           &lt;/element>
 *           &lt;element name="PlanModelReference">
 *             &lt;complexType>
 *               &lt;complexContent>
 *                 &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                   &lt;attribute name="reference" use="required" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *                 &lt;/restriction>
 *               &lt;/complexContent>
 *             &lt;/complexType>
 *           &lt;/element>
 *         &lt;/choice>
 *       &lt;/sequence>
 *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="planType" use="required" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *       &lt;attribute name="planLanguage" use="required" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *       &lt;anyAttribute processContents='lax' namespace='##other'/>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tPlan",
    propOrder = {"precondition", "inputParameters", "outputParameters", "planModel", "planModelReference"})
public class TPlan extends TExtensibleElements {

    @XmlElement(name = "Precondition")
    protected TCondition precondition;
    @XmlElement(name = "InputParameters")
    protected TPlan.InputParameters inputParameters;
    @XmlElement(name = "OutputParameters")
    protected TPlan.OutputParameters outputParameters;
    @XmlElement(name = "PlanModel")
    protected TPlan.PlanModel planModel;
    @XmlElement(name = "PlanModelReference")
    protected TPlan.PlanModelReference planModelReference;
    @XmlAttribute(required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String id;
    @XmlAttribute
    protected String name;
    @XmlAttribute(required = true)
    @XmlSchemaType(name = "anyURI")
    protected String planType;
    @XmlAttribute(required = true)
    @XmlSchemaType(name = "anyURI")
    protected String planLanguage;

    /**
     * Gets the value of the precondition property.
     *
     * @return possible object is {@link TCondition }
     */
    public TCondition getPrecondition() {
        return this.precondition;
    }

    /**
     * Sets the value of the precondition property.
     *
     * @param value allowed object is {@link TCondition }
     */
    public void setPrecondition(final TCondition value) {
        this.precondition = value;
    }

    /**
     * Gets the value of the inputParameters property.
     *
     * @return possible object is {@link TPlan.InputParameters }
     */
    public TPlan.InputParameters getInputParameters() {
        return this.inputParameters;
    }

    /**
     * Sets the value of the inputParameters property.
     *
     * @param value allowed object is {@link TPlan.InputParameters }
     */
    public void setInputParameters(final TPlan.InputParameters value) {
        this.inputParameters = value;
    }

    /**
     * Gets the value of the outputParameters property.
     *
     * @return possible object is {@link TPlan.OutputParameters }
     */
    public TPlan.OutputParameters getOutputParameters() {
        return this.outputParameters;
    }

    /**
     * Sets the value of the outputParameters property.
     *
     * @param value allowed object is {@link TPlan.OutputParameters }
     */
    public void setOutputParameters(final TPlan.OutputParameters value) {
        this.outputParameters = value;
    }

    /**
     * Gets the value of the planModel property.
     *
     * @return possible object is {@link TPlan.PlanModel }
     */
    public TPlan.PlanModel getPlanModel() {
        return this.planModel;
    }

    /**
     * Sets the value of the planModel property.
     *
     * @param value allowed object is {@link TPlan.PlanModel }
     */
    public void setPlanModel(final TPlan.PlanModel value) {
        this.planModel = value;
    }

    /**
     * Gets the value of the planModelReference property.
     *
     * @return possible object is {@link TPlan.PlanModelReference }
     */
    public TPlan.PlanModelReference getPlanModelReference() {
        return this.planModelReference;
    }

    /**
     * Sets the value of the planModelReference property.
     *
     * @param value allowed object is {@link TPlan.PlanModelReference }
     */
    public void setPlanModelReference(final TPlan.PlanModelReference value) {
        this.planModelReference = value;
    }

    /**
     * Gets the value of the id property.
     *
     * @return possible object is {@link String }
     */
    public String getId() {
        return this.id;
    }

    /**
     * Sets the value of the id property.
     *
     * @param value allowed object is {@link String }
     */
    public void setId(final String value) {
        this.id = value;
    }

    /**
     * Gets the value of the name property.
     *
     * @return possible object is {@link String }
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param value allowed object is {@link String }
     */
    public void setName(final String value) {
        this.name = value;
    }

    /**
     * Gets the value of the planType property.
     *
     * @return possible object is {@link String }
     */
    public String getPlanType() {
        return this.planType;
    }

    /**
     * Sets the value of the planType property.
     *
     * @param value allowed object is {@link String }
     */
    public void setPlanType(final String value) {
        this.planType = value;
    }

    /**
     * Gets the value of the planLanguage property.
     *
     * @return possible object is {@link String }
     */
    public String getPlanLanguage() {
        return this.planLanguage;
    }

    /**
     * Sets the value of the planLanguage property.
     *
     * @param value allowed object is {@link String }
     */
    public void setPlanLanguage(final String value) {
        this.planLanguage = value;
    }

    /**
     * <p>
     * Java class for anonymous complex type.
     *
     * <p>
     * The following schema fragment specifies the expected content contained within this class.
     *
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="InputParameter" type="{http://docs.oasis-open.org/tosca/ns/2011/12}tParameter" maxOccurs="unbounded"/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {"inputParameter"})
    public static class InputParameters {

        @XmlElement(name = "InputParameter", required = true)
        protected List<TParameter> inputParameter;

        /**
         * Gets the value of the inputParameter property.
         *
         * <p>
         * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you
         * make to the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE>
         * method for the inputParameter property.
         *
         * <p>
         * For example, to add a new item, do as follows:
         *
         * <pre>
         * getInputParameter().add(newItem);
         * </pre>
         *
         *
         * <p>
         * Objects of the following type(s) are allowed in the list {@link TParameter }
         */
        public List<TParameter> getInputParameter() {
            if (this.inputParameter == null) {
                this.inputParameter = new ArrayList<>();
            }
            return this.inputParameter;
        }
    }

    /**
     * <p>
     * Java class for anonymous complex type.
     *
     * <p>
     * The following schema fragment specifies the expected content contained within this class.
     *
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="OutputParameter" type="{http://docs.oasis-open.org/tosca/ns/2011/12}tParameter" maxOccurs="unbounded"/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {"outputParameter"})
    public static class OutputParameters {

        @XmlElement(name = "OutputParameter", required = true)
        protected List<TParameter> outputParameter;

        /**
         * Gets the value of the outputParameter property.
         *
         * <p>
         * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you
         * make to the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE>
         * method for the outputParameter property.
         *
         * <p>
         * For example, to add a new item, do as follows:
         *
         * <pre>
         * getOutputParameter().add(newItem);
         * </pre>
         *
         *
         * <p>
         * Objects of the following type(s) are allowed in the list {@link TParameter }
         */
        public List<TParameter> getOutputParameter() {
            if (this.outputParameter == null) {
                this.outputParameter = new ArrayList<>();
            }
            return this.outputParameter;
        }
    }

    /**
     * <p>
     * Java class for anonymous complex type.
     *
     * <p>
     * The following schema fragment specifies the expected content contained within this class.
     *
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;any processContents='lax' namespace='##other'/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {"any"})
    public static class PlanModel {

        @XmlAnyElement(lax = true)
        protected Object any;

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
    }

    /**
     * <p>
     * Java class for anonymous complex type.
     *
     * <p>
     * The following schema fragment specifies the expected content contained within this class.
     *
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;attribute name="reference" use="required" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class PlanModelReference {

        @XmlAttribute(required = true)
        @XmlSchemaType(name = "anyURI")
        protected String reference;

        /**
         * Gets the value of the reference property.
         *
         * @return possible object is {@link String }
         */
        public String getReference() {
            return this.reference;
        }

        /**
         * Sets the value of the reference property.
         *
         * @param value allowed object is {@link String }
         */
        public void setReference(final String value) {
            this.reference = value;
        }
    }
}
