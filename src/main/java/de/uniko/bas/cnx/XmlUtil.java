package de.uniko.bas.cnx;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

public final class XmlUtil {

    private final Document doc;
    private final XPath xp;

    private XmlUtil(Document doc, XPath xp) {
        this.doc = doc;
        this.xp = xp;
    }

    /** Parse XML into a reusable wrapper (namespace-aware). */
    public static XmlUtil parse(String xml) throws ParserConfigurationException, IOException, SAXException {
        if (xml == null || xml.isBlank()) {
            throw new IllegalArgumentException("XML input is empty/blank (nothing to parse).");
        }

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

        Document doc = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        XPath xp = XPathFactory.newInstance().newXPath();
        xp.setNamespaceContext(defaultNamespaceContext());

        return new XmlUtil(doc, xp);
    }

    /** Default namespaces you use a lot (Atom + SNX + a few common ones). */
    public static NamespaceContext defaultNamespaceContext() {
        Map<String, String> ns = new HashMap<>();
        ns.put("atom", "http://www.w3.org/2005/Atom");
        ns.put("snx", "http://www.ibm.com/xmlns/prod/sn");
        ns.put("thr", "http://purl.org/syndication/thread/1.0");
        ns.put("td", "urn:ibm.com/td");
        ns.put("ca", "http://www.ibm.com/xmlns/prod/composite-applications/v1.0");
        ns.put("app", "http://www.w3.org/2007/app");
        ns.put("opensearch", "http://a9.com/-/spec/opensearch/1.1/");

        return namespaceContext(ns);
    }

    /** Create a NamespaceContext from a prefix->uri map. */
    public static NamespaceContext namespaceContext(Map<String, String> prefixToUri) {
        Map<String, String> ns = new HashMap<>(prefixToUri);

        return new NamespaceContext() {
            @Override public String getNamespaceURI(String prefix) {
                if (prefix == null) return XMLConstants.NULL_NS_URI;
                return ns.getOrDefault(prefix, XMLConstants.NULL_NS_URI);
            }
            @Override public String getPrefix(String namespaceURI) {
                for (var e : ns.entrySet()) if (Objects.equals(e.getValue(), namespaceURI)) return e.getKey();
                return null;
            }
            @Override public Iterator<String> getPrefixes(String namespaceURI) {
                List<String> prefixes = new ArrayList<>();
                for (var e : ns.entrySet()) if (Objects.equals(e.getValue(), namespaceURI)) prefixes.add(e.getKey());
                return prefixes.iterator();
            }
        };
    }

    /** Allow services to add/override namespaces easily. */
    public XmlUtil withNamespaces(Map<String, String> prefixToUri) {
        XPath newXp = XPathFactory.newInstance().newXPath();
        Map<String, String> merged = new HashMap<>();
        // merge defaults + custom (custom wins)
        defaultNamespaceContext().getPrefixes("").forEachRemaining(p -> {}); // no-op; keep simple
        merged.put("atom", "http://www.w3.org/2005/Atom");
        merged.put("snx", "http://www.ibm.com/xmlns/prod/sn");
        merged.put("thr", "http://purl.org/syndication/thread/1.0");
        merged.put("td", "urn:ibm.com/td");
        merged.put("ca", "http://www.ibm.com/xmlns/prod/composite-applications/v1.0");
        if (prefixToUri != null) merged.putAll(prefixToUri);

        newXp.setNamespaceContext(namespaceContext(merged));
        return new XmlUtil(this.doc, newXp);
    }

    // ---------------------------
    // Typed getters
    // ---------------------------

    /** Returns trimmed string or null if missing/blank. */
    public String text(String expr) throws XPathExpressionException {
        String v = (String) xp.evaluate(expr, doc, XPathConstants.STRING);
        if (v == null) return null;
        v = v.trim();
        return v.isEmpty() ? null : v;
    }

    /** Returns trimmed string or default if missing/blank. */
    public String textOr(String expr, String defaultValue) throws XPathExpressionException {
        String v = text(expr);
        return v == null ? defaultValue : v;
    }

    /** true if value equalsIgnoreCase("true"), otherwise false. */
    public boolean bool(String expr) throws XPathExpressionException {
        String v = text(expr);
        return v != null && v.equalsIgnoreCase("true");
    }

    /** Parses int or returns defaultValue if missing/blank. */
    public int intVal(String expr, int defaultValue) throws XPathExpressionException {
        String v = text(expr);
        if (v == null) return defaultValue;
        return Integer.parseInt(v);
    }

    public int intVal(String expr) throws XPathExpressionException {
        return intVal(expr, 0);
    }

    /** Returns a NodeList for iteration. */
    public org.w3c.dom.NodeList nodes(String expr) throws XPathExpressionException {
        return (org.w3c.dom.NodeList) xp.evaluate(expr, doc, XPathConstants.NODESET);
    }

    /** Returns first Node or null. */
    public Node node(String expr) throws XPathExpressionException {
        return (Node) xp.evaluate(expr, doc, XPathConstants.NODE);
    }

    /** Convenience: attribute (same as text("@attr") when you are already on a node, but for doc-level paths) */
    public String attr(String exprToAttr) throws XPathExpressionException {
        // Just alias; caller passes something like: "/atom:entry/snx:communityTheme/@snx:uuid"
        return text(exprToAttr);
    }

    // With context node
    public String text(String expr, Node context) throws XPathExpressionException {
        String v = (String) xp.evaluate(expr, context, XPathConstants.STRING);
        if (v == null) return null;
        v = v.trim();
        return v.isEmpty() ? null : v;
    }

    public boolean bool(String expr, Node context) throws XPathExpressionException {
        String v = text(expr, context);
        return v != null && v.equalsIgnoreCase("true");
    }

    public int intVal(String expr, Node context, int defaultValue) throws XPathExpressionException {
        String v = text(expr, context);
        if (v == null) return defaultValue;
        return Integer.parseInt(v);
    }

    public int intVal(String expr, Node context) throws XPathExpressionException {
        return intVal(expr, context, 0);
    }

    public org.w3c.dom.NodeList nodes(String expr, Node context) throws XPathExpressionException {
        return (org.w3c.dom.NodeList) xp.evaluate(expr, context, XPathConstants.NODESET);
    }

    public Node node(String expr, Node context) throws XPathExpressionException {
        return (Node) xp.evaluate(expr, context, XPathConstants.NODE);
    }
}
