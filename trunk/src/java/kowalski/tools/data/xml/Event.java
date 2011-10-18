//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.9-03/31/2009 04:14 PM(snajper)-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.09.14 at 06:13:26 PM CEST 
//


package kowalski.tools.data.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;


/**
 * An event, using a Sound or AudioData as its sound source.
 * 
 * <p>Java class for Event complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Event">
 *   &lt;complexContent>
 *     &lt;extension base="{}NodeWithIDAndComments">
 *       &lt;choice minOccurs="0">
 *         &lt;element ref="{}AudioDataReference"/>
 *         &lt;element ref="{}SoundReference"/>
 *       &lt;/choice>
 *       &lt;attribute name="positional" type="{http://www.w3.org/2001/XMLSchema}boolean" default="true" />
 *       &lt;attribute name="bus" use="required" type="{}identifierString" />
 *       &lt;attribute name="pitch" type="{}pitchFloat" default="1.0" />
 *       &lt;attribute name="gain" type="{}gainFloat" default="1.0" />
 *       &lt;attribute name="innerConeAngle" type="{}coneAngleFloat" default="0" />
 *       &lt;attribute name="outerConeAngle" type="{}coneAngleFloat" default="360" />
 *       &lt;attribute name="outerConeGain" type="{}gainFloat" default="1.0" />
 *       &lt;attribute name="retriggerMode" type="{}EventRetriggerMode" default="RETRIGGER" />
 *       &lt;attribute name="istanceStealingMode" type="{}EventInstanceStealingMode" default="STEAL_QUIETEST" />
 *       &lt;attribute name="instanceCount" type="{}nonNegativeInt" default="1" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Event", propOrder = {
    "audioDataReferenceOrSoundReference"
})
public class Event
    extends NodeWithIDAndComments
{

    @XmlElements({
        @XmlElement(name = "AudioDataReference", type = AudioDataReference.class),
        @XmlElement(name = "SoundReference", type = SoundReference.class)
    })
    protected Object audioDataReferenceOrSoundReference;
    @XmlAttribute
    protected Boolean positional;
    @XmlAttribute(required = true)
    protected String bus;
    @XmlAttribute
    protected Float pitch;
    @XmlAttribute
    protected Float gain;
    @XmlAttribute
    protected Float innerConeAngle;
    @XmlAttribute
    protected Float outerConeAngle;
    @XmlAttribute
    protected Float outerConeGain;
    @XmlAttribute
    protected EventRetriggerMode retriggerMode;
    @XmlAttribute
    protected EventInstanceStealingMode istanceStealingMode;
    @XmlAttribute
    protected Integer instanceCount;

    /**
     * Gets the value of the audioDataReferenceOrSoundReference property.
     * 
     * @return
     *     possible object is
     *     {@link AudioDataReference }
     *     {@link SoundReference }
     *     
     */
    public Object getAudioDataReferenceOrSoundReference() {
        return audioDataReferenceOrSoundReference;
    }

    /**
     * Sets the value of the audioDataReferenceOrSoundReference property.
     * 
     * @param value
     *     allowed object is
     *     {@link AudioDataReference }
     *     {@link SoundReference }
     *     
     */
    public void setAudioDataReferenceOrSoundReference(Object value) {
        this.audioDataReferenceOrSoundReference = value;
    }

    /**
     * Gets the value of the positional property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public boolean isPositional() {
        if (positional == null) {
            return true;
        } else {
            return positional;
        }
    }

    /**
     * Sets the value of the positional property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setPositional(Boolean value) {
        this.positional = value;
    }

    /**
     * Gets the value of the bus property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBus() {
        return bus;
    }

    /**
     * Sets the value of the bus property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBus(String value) {
        this.bus = value;
    }

    /**
     * Gets the value of the pitch property.
     * 
     * @return
     *     possible object is
     *     {@link Float }
     *     
     */
    public float getPitch() {
        if (pitch == null) {
            return  1.0F;
        } else {
            return pitch;
        }
    }

    /**
     * Sets the value of the pitch property.
     * 
     * @param value
     *     allowed object is
     *     {@link Float }
     *     
     */
    public void setPitch(Float value) {
        this.pitch = value;
    }

    /**
     * Gets the value of the gain property.
     * 
     * @return
     *     possible object is
     *     {@link Float }
     *     
     */
    public float getGain() {
        if (gain == null) {
            return  1.0F;
        } else {
            return gain;
        }
    }

    /**
     * Sets the value of the gain property.
     * 
     * @param value
     *     allowed object is
     *     {@link Float }
     *     
     */
    public void setGain(Float value) {
        this.gain = value;
    }

    /**
     * Gets the value of the innerConeAngle property.
     * 
     * @return
     *     possible object is
     *     {@link Float }
     *     
     */
    public float getInnerConeAngle() {
        if (innerConeAngle == null) {
            return  0.0F;
        } else {
            return innerConeAngle;
        }
    }

    /**
     * Sets the value of the innerConeAngle property.
     * 
     * @param value
     *     allowed object is
     *     {@link Float }
     *     
     */
    public void setInnerConeAngle(Float value) {
        this.innerConeAngle = value;
    }

    /**
     * Gets the value of the outerConeAngle property.
     * 
     * @return
     *     possible object is
     *     {@link Float }
     *     
     */
    public float getOuterConeAngle() {
        if (outerConeAngle == null) {
            return  360.0F;
        } else {
            return outerConeAngle;
        }
    }

    /**
     * Sets the value of the outerConeAngle property.
     * 
     * @param value
     *     allowed object is
     *     {@link Float }
     *     
     */
    public void setOuterConeAngle(Float value) {
        this.outerConeAngle = value;
    }

    /**
     * Gets the value of the outerConeGain property.
     * 
     * @return
     *     possible object is
     *     {@link Float }
     *     
     */
    public float getOuterConeGain() {
        if (outerConeGain == null) {
            return  1.0F;
        } else {
            return outerConeGain;
        }
    }

    /**
     * Sets the value of the outerConeGain property.
     * 
     * @param value
     *     allowed object is
     *     {@link Float }
     *     
     */
    public void setOuterConeGain(Float value) {
        this.outerConeGain = value;
    }

    /**
     * Gets the value of the retriggerMode property.
     * 
     * @return
     *     possible object is
     *     {@link EventRetriggerMode }
     *     
     */
    public EventRetriggerMode getRetriggerMode() {
        if (retriggerMode == null) {
            return EventRetriggerMode.RETRIGGER;
        } else {
            return retriggerMode;
        }
    }

    /**
     * Sets the value of the retriggerMode property.
     * 
     * @param value
     *     allowed object is
     *     {@link EventRetriggerMode }
     *     
     */
    public void setRetriggerMode(EventRetriggerMode value) {
        this.retriggerMode = value;
    }

    /**
     * Gets the value of the istanceStealingMode property.
     * 
     * @return
     *     possible object is
     *     {@link EventInstanceStealingMode }
     *     
     */
    public EventInstanceStealingMode getIstanceStealingMode() {
        if (istanceStealingMode == null) {
            return EventInstanceStealingMode.STEAL_QUIETEST;
        } else {
            return istanceStealingMode;
        }
    }

    /**
     * Sets the value of the istanceStealingMode property.
     * 
     * @param value
     *     allowed object is
     *     {@link EventInstanceStealingMode }
     *     
     */
    public void setIstanceStealingMode(EventInstanceStealingMode value) {
        this.istanceStealingMode = value;
    }

    /**
     * Gets the value of the instanceCount property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public int getInstanceCount() {
        if (instanceCount == null) {
            return  1;
        } else {
            return instanceCount;
        }
    }

    /**
     * Sets the value of the instanceCount property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setInstanceCount(Integer value) {
        this.instanceCount = value;
    }

}
