import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

public class СalcXML {

    public static void main(String[] args) throws ParserConfigurationException, IOException, TransformerException {

        //Проверим входящий файл на соответствие схеме
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder parser = documentBuilderFactory.newDocumentBuilder();

        Document document = null;


        try {
            document = parser.parse(new File("sampleTest.xml"));

            SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            Schema schema = factory.newSchema();

            Validator validator = schema.newValidator();
            validator.validate(new DOMSource(document));
            System.out.println("Source file is valid");
        } catch (SAXException e) {
            System.out.println("Source file is invalid \n" + e.getLocalizedMessage());
        }

        //Создание результирующего документа
        DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
        Document resultDoc = builder.newDocument();
        resultDoc.setXmlStandalone(true);

        Element simpleCalculator = resultDoc.createElement("simpleCalculator");
        resultDoc.appendChild(simpleCalculator);
        Element expressionResults = resultDoc.createElement("expressionResults");
        simpleCalculator.appendChild(expressionResults);

        /*Разделим на expression'ы

        Создаем две очереди (используя LinkedList): первую первоначально заполняем рекурсивным проходом по дереву
        Далее проходим по ней в цикле: извлекаем элемент - если операция, то, если следующий элемент число,
        производим операцию, поочедедно извлекая два числа, добавляем результат во вторую очередь, если в ней есть хоть одна операция.
        Если нет - это конечный результат, добавляем его в результирующий файл.
        Если снова операция - добавляем во вторую очередь.
        Выполняем пока обе очереди не останутся пустынми*/

        NodeList expressions = document.getElementsByTagName("expression");

        for (int i = 0; i < expressions.getLength(); i++) {
            LinkedList<String> list = new LinkedList<String>();
            LinkedList<String> list1 = new LinkedList<String>();

            System.out.println("NEW EXPRESSION =====================================================");

            Element expressionResult = resultDoc.createElement("expressionResult");
            expressionResults.appendChild(expressionResult);
            Element result = resultDoc.createElement("result");
            expressionResult.appendChild(result);

            visit(expressions.item(i), list);

            String ex, res;

            while (list.size() > 0 || list1.size() > 0) {

                while (list.size() > 0) {
                    ex = list.pop();
                    if (isNumber(list.getFirst())) {
                        res = Double.toString(expressionResult(ex, list.pop(), list.pop()));
                        if (list1.size() > 0)
                            list1.add(res);
                        else {
                            System.out.println(res);
                            Text text = resultDoc.createTextNode(res);
                            result.appendChild(text);
                        }
                    } else
                        list1.add(ex);
                }

                while (list1.size() > 0) {
                    ex = list1.pop();
                    if (isNumber(list1.getFirst())) {
                        res = Double.toString(expressionResult(ex, list1.pop(), list1.pop()));
                        if (list.size() > 0)
                            list.add(res);
                        else {
                            System.out.println(res);
                            Text text = resultDoc.createTextNode(res);
                            result.appendChild(text);
                        }
                    } else
                        list.add(ex);
                }
            }

        }

        //Сохранение файла-результата
        File file = new File("Result.xml");
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "5");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new DOMSource(resultDoc), new StreamResult(file));

        //Проверим валидность результирующего файла
        documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        parser = documentBuilderFactory.newDocumentBuilder();

        try {
            document = parser.parse(new File("Result.xml"));

            SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            File schemaLocation = new File("Calculator.xsd");
            Schema schema = factory.newSchema(schemaLocation);

            Validator validator = schema.newValidator();
            validator.validate(new DOMSource(document));
            System.out.println("Result file is valid");
        } catch (SAXException e) {
            System.out.println("Result file is invalid \n" + e.getLocalizedMessage());
        }

    }

    public static void visit(Node node, LinkedList<String> list) {
        for (int i = node.getChildNodes().getLength()-1; i >= 0; i--) {
            visit(node.getChildNodes().item(i), list);
        }
        Element e = null;
        if (node.getNodeType() == node.ELEMENT_NODE) {
            e = (Element) node;
            if (e.getTagName().equals("arg"))
                list.push(e.getTextContent());
            if (e.getTagName().equals("operation"))
                list.push(e.getAttribute("OperationType"));
        }
    }

    public static boolean isNumber(String s){
        try{
            Double.parseDouble(s);
            return true;
        }
        catch (NumberFormatException e){
            return false;
        }
    }

    public static Double expressionResult(String ex, String sa, String sb){

        Double a = Double.parseDouble(sa);
        Double b = Double.parseDouble(sb);

        if (ex.equals("SUB"))
            return a - b;
        if (ex.equals("SUM"))
            return a + b;
        if (ex.equals("MUL"))
            return a * b;
        if (ex.equals("DIV"))
            return a / b;

        return null;
    }

}

