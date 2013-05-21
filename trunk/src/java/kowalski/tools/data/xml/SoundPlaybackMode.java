//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.9-03/31/2009 04:14 PM(snajper)-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.09.14 at 06:13:26 PM CEST 
//


package kowalski.tools.data.xml;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for SoundPlaybackMode.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="SoundPlaybackMode">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="RANDOM"/>
 *     &lt;enumeration value="RANDOM_NO_REPEAT"/>
 *     &lt;enumeration value="SEQUENTIAL"/>
 *     &lt;enumeration value="SEQUENTIAL_NO_RESET"/>
 *     &lt;enumeration value="IN_RANDOM_OUT"/>
 *     &lt;enumeration value="IN_RANDOM_NO_REPEAT_OUT"/>
 *     &lt;enumeration value="IN_SEQUENTIAL_OUT"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "SoundPlaybackMode")
@XmlEnum
public enum SoundPlaybackMode {

    RANDOM,
    RANDOM_NO_REPEAT,
    SEQUENTIAL,
    SEQUENTIAL_NO_RESET,
    IN_RANDOM_OUT,
    IN_RANDOM_NO_REPEAT_OUT,
    IN_SEQUENTIAL_OUT;

    public String value() {
        return name();
    }

    public static SoundPlaybackMode fromValue(String v) {
        return valueOf(v);
    }

}