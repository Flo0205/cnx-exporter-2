package de.uniko.bas.cnx.services;

import de.uniko.bas.cnx.Config;
import de.uniko.bas.cnx.RestBroker;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class InitService {
    private static final RestBroker broker = new RestBroker();

    private InitService() {}

    public static void getServiceConfigs() throws IOException, InterruptedException {
        String url = Config.URLS.get("host") + Config.URLS.get("serviceconfigs");
        String response = broker.doGet(url);

        try {
            Map<String, String> services = parse(response);
            Config.URLS.putAll(services);

            services.forEach((k, v) ->
                    System.out.println(k + " -> " + v)
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse service configs", e);
        }
    }

    private static Map<String, String> parse(String xml) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        Map<String, String> services = new HashMap<>();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);

        // ---- XXE hardening ----
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);

        Document doc = dbf.newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)));

        XPath xpath = XPathFactory.newInstance().newXPath();

        NodeList entries = (NodeList) xpath.evaluate(
                "/*[local-name()='feed']/*[local-name()='entry']",
                doc,
                XPathConstants.NODESET
        );

        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);

            String serviceName = xpath.evaluate(
                    "*[local-name()='title']/text()",
                    entry
            );

            String httpsLink = xpath.evaluate(
                    "*[local-name()='link'][@rel='http://www.ibm.com/xmlns/prod/sn/alternate-ssl']/@href",
                    entry
            );

            if (!serviceName.isEmpty() && !httpsLink.isEmpty()) {
                services.put(serviceName, httpsLink);
            }
        }

        return services;
    }
}
