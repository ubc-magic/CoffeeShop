//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.2-147 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2010.03.29 at 03:49:06 PM PDT 
//


package ca.ubc.magic.coffeeshop.jaxb;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the ca.ubc.magic.coffeeshop.jaxb package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _PluginApp_QNAME = new QName("http://pspi.magic.ubc.ca", "pluginApp");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: ca.ubc.magic.coffeeshop.jaxb
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Parameters }
     * 
     */
    public Parameters createParameters() {
        return new Parameters();
    }

    /**
     * Create an instance of {@link CheckboxList }
     * 
     */
    public CheckboxList createCheckboxList() {
        return new CheckboxList();
    }

    /**
     * Create an instance of {@link Password }
     * 
     */
    public Password createPassword() {
        return new Password();
    }

    /**
     * Create an instance of {@link Listbox }
     * 
     */
    public Listbox createListbox() {
        return new Listbox();
    }

    /**
     * Create an instance of {@link Textbox }
     * 
     */
    public Textbox createTextbox() {
        return new Textbox();
    }

    /**
     * Create an instance of {@link Connection }
     * 
     */
    public Connection createConnection() {
        return new Connection();
    }

    /**
     * Create an instance of {@link Parameter }
     * 
     */
    public Parameter createParameter() {
        return new Parameter();
    }

    /**
     * Create an instance of {@link Textarea }
     * 
     */
    public Textarea createTextarea() {
        return new Textarea();
    }

    /**
     * Create an instance of {@link Application }
     * 
     */
    public Application createApplication() {
        return new Application();
    }

    /**
     * Create an instance of {@link RadioList }
     * 
     */
    public RadioList createRadioList() {
        return new RadioList();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Application }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://pspi.magic.ubc.ca", name = "pluginApp")
    public JAXBElement<Application> createPluginApp(Application value) {
        return new JAXBElement<Application>(_PluginApp_QNAME, Application.class, null, value);
    }

}
