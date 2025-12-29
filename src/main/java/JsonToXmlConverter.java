import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.File;
import java.io.FileReader;
import java.util.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class JsonToXmlConverter {

    public static void main(String[] args) throws Exception {

        // Input JSON
        String inputFile = "inputFiles/json_Input.json";

        // Output (always to Downloads, like your XML→JSON converter)
        String inputName = new File(inputFile).getName();
        String baseName = inputName.contains("_")
                ? inputName.substring(0, inputName.lastIndexOf('_'))
                : inputName;
        String outputFileName = baseName + "_Output" + ".xml";

        String userDir = System.getProperty("user.dir");
        File outputDir = new File(userDir, "outputFiles");

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        String nameWithoutExt = outputFileName.substring(0, outputFileName.lastIndexOf('.'));
        String extension = outputFileName.substring(outputFileName.lastIndexOf('.'));

        File outputFile = new File(outputDir, outputFileName);
        int count = 1;

        while (outputFile.exists()) {
            outputFile = new File(outputDir,
                    nameWithoutExt + "_" + count + extension);
            count++;
        }

        // Read JSON into Map
        Gson gson = new Gson();
        Map<String, Object> jsonMap = gson.fromJson(new FileReader(inputFile),
                new TypeToken<Map<String, Object>>() {
                }.getType());

        // Convert JSON → XML Document
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        // JSON root should have exactly one key
        if (jsonMap.size() != 1) {
            throw new RuntimeException("JSON root must contain exactly one top-level element.");
        }

        String rootName = jsonMap.keySet().iterator().next();
        Element rootElement = doc.createElement(rootName);
        doc.appendChild(rootElement);

        // Convert recursively
        buildXml(doc, rootElement, jsonMap.get(rootName));

        // Write XML
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        transformer.transform(new DOMSource(doc), new StreamResult(outputFile));

        System.out.println("✅ XML written to: " + outputFile);
    }

    // Recursively build XML from Objects, Maps, and Lists
    private static void buildXml(Document doc, Element parent, Object data) {

        if (data instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) data;
            for (Object keyObj : map.keySet()) {
                String key = keyObj.toString();
                Object value = map.get(key);

                if (value instanceof List) {
                    for (Object item : (List<?>) value) {
                        Element child = doc.createElement(key);
                        parent.appendChild(child);
                        buildXml(doc, child, item);
                    }
                } else {
                    Element child = doc.createElement(key);
                    parent.appendChild(child);
                    buildXml(doc, child, value);
                }
            }

        } else if (data instanceof List) {
            for (Object item : (List<?>) data) {
                Element child = doc.createElement(parent.getNodeName());
                parent.getParentNode().appendChild(child);
                buildXml(doc, child, item);
            }

        } else if (data instanceof String) {
            parent.setTextContent((String) data);
        }
    }
}
