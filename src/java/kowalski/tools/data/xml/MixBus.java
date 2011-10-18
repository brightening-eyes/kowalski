//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.9-03/31/2009 04:14 PM(snajper)-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.09.14 at 06:13:26 PM CEST 
//


package kowalski.tools.data.xml;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * A node in a mix bus hierarchy. Note that this element is only used to define the structure of the hierarcy, mix bus parameters (e.g pitch and gain) are stored in mix presets.
 * 
 * <p>Java class for MixBus complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="MixBus">
 *   &lt;complexContent>
 *     &lt;extension base="{}NodeWithIDAndComments">
 *       &lt;sequence>
 *         &lt;element name="MixBus" type="{}MixBus" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MixBus", propOrder = {
    "subBuses"
})
public class MixBus
    extends NodeWithIDAndComments
{

    @XmlElement(name = "MixBus")
    protected List<MixBus> subBuses;

    /**
     * Gets the value of the subBuses property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the subBuses property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSubBuses().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link MixBus }
     * 
     * 
     */
    public List<MixBus> getSubBuses() {
        if (subBuses == null) {
            subBuses = new ArrayList<MixBus>();
        }
        return this.subBuses;
    }

}
