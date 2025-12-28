import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileWriter;
import java.util.*;

public class XmlToJsonConverter {

    public static void main(String[] args) throws Exception {
        // Input XML
        String inputFile = "convert.xml";

        // Derive output file name from input
        String inputName = new File(inputFile).getName();
        String baseName = inputName.contains(".")
                ? inputName.substring(0, inputName.lastIndexOf('.'))
                : inputName;
        String outputFileName = baseName + ".json";

        // Always write output to Downloads folder
        String userHome = System.getProperty("user.home");
        String outputFile = userHome + File.separator + "Downloads" +
                File.separator + outputFileName;
        // Parse XML
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false); // ignore namespaces
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(inputFile));
        doc.getDocumentElement().normalize();

        // Convert XML → Map
        Map<String, Object> jsonMap = elementToMap(doc.getDocumentElement());

        // Write JSON manually
        String jsonString = toJson(jsonMap, 0);

        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(jsonString);
        }

        System.out.println("✅ JSON written to: " + outputFile);
    }

    // Convert Element → Map
    private static Map<String, Object> elementToMap(Element element) {
        Map<String, Object> map = new LinkedHashMap<>();
        NodeList childNodes = element.getChildNodes();

        Map<String, Object> childMap = new LinkedHashMap<>();
        boolean hasElementChild = false;

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);

            if (node instanceof Element) {
                hasElementChild = true;
                Element childElement = (Element) node;
                String tagName = childElement.getTagName().replaceAll(".*:", ""); // remove ns

                Object value = elementToMap(childElement).get(tagName);

                if (childMap.containsKey(tagName)) {
                    Object existing = childMap.get(tagName);
                    if (existing instanceof List) {
                        ((List<Object>) existing).add(value);
                    } else {
                        List<Object> newList = new ArrayList<>();
                        newList.add(existing);
                        newList.add(value);
                        childMap.put(tagName, newList);
                    }
                } else {
                    childMap.put(tagName, value);
                }
            }
        }

        if (!hasElementChild) {
            map.put(element.getTagName().replaceAll(".*:", ""), element.getTextContent());
        } else {
            map.put(element.getTagName().replaceAll(".*:", ""), childMap);
        }

        return map;
    }

    // Very simple Map/List → JSON string
    private static String toJson(Object obj, int indent) {
        StringBuilder sb = new StringBuilder();
        String pad = String.join("", Collections.nCopies(indent, "  "));

        if (obj instanceof Map) {
            sb.append("{\n");
            Iterator<? extends Map.Entry<?, ?>> it = ((Map<?, ?>) obj).entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<?, ?> entry = it.next();
                sb.append(pad).append("  \"").append(entry.getKey()).append("\": ");
                sb.append(toJson(entry.getValue(), indent + 1));
                if (it.hasNext()) sb.append(",");
                sb.append("\n");
            }
            sb.append(pad).append("}");
        } else if (obj instanceof List) {
            sb.append("[\n");
            Iterator<?> it = ((List<?>) obj).iterator();
            while (it.hasNext()) {
                sb.append(pad).append("  ").append(toJson(it.next(), indent + 1));
                if (it.hasNext()) sb.append(",");
                sb.append("\n");
            }
            sb.append(pad).append("]");
        } else if (obj instanceof String) {
            sb.append("\"").append(((String) obj).replace("\"", "\\\"")).append("\"");
        } else {
            sb.append(obj);
        }

        return sb.toString();
    }
}
