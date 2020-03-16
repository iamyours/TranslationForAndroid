import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Test {
    /**
     * google translate api (https://cloud.google.com/translate/docs/basic/setup-basic)
     */
    private static final String GOOGLE_API_KEY = "xxx";

    public static void main(String[] args) {
        translateXml();
    }


    private static void translateXml() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .build();
        String path = "/xxx/TestApp/app/src/main/res/";
        String enString = path + "values-en/strings.xml";
        File enFile = new File(enString);
        String[] codes = new String[]{"nl", "pl", "de", "fr", "tr"};
        for (String code : codes) {
            String dstPath = "values-" + code + "/";
            File file = new File(path + dstPath);
            if (!file.exists()) file.mkdirs();
            File xmlFile = new File(file, "strings.xml");
            try {
                System.out.println("=========translate to " + code + "==========");
                translateFile(client, code, enFile, xmlFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void translateFile(OkHttpClient client, String code, File xmlFile, File outFile) throws Exception {
        DocumentBuilder newDocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = newDocumentBuilder.parse(xmlFile);
        Element root = doc.getDocumentElement();
        NodeList nodeList = root.getElementsByTagName("string");
        System.out.println(nodeList.getLength());
        for (int i = 0; i < nodeList.getLength(); i++) {
            String content = nodeList.item(i).getTextContent();
            if (content.contains("<")) {
                System.out.println(">>>skip " + content);
                continue;
            }
            String translateText = translate(content, code, client);
            if (translateText != null) {
                nodeList.item(i).setTextContent(translateText);
                System.out.println("translating " + content + " to " + translateText + "(" + code + ")");
            } else {
                System.out.println("translating " + content + " to " + code + " failed...");
            }

        }
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        //DOMSource source = new DOMSource(doc);
        Source source = new DOMSource(doc);
        Result result = new StreamResult(outFile);
        transformer.transform(source, result);
    }

    private static String translate(String text, String target, OkHttpClient client) {
        String apiUrl = "https://translation.googleapis.com/language/translate/v2?key=" + GOOGLE_API_KEY;
        String totalUrl = apiUrl + "&q=" + text + "&target=" + target;

        MediaType mediaType = MediaType.parse("application/json;charset=UTF-8");
        Request request = new Request.Builder()
                .url(totalUrl)
                .method("POST", RequestBody.create(mediaType, ""))
                .build();
        Call call = client.newCall(request);
        try {
            String json = call.execute().body().string();
            JSONObject obj = JSONObject.parseObject(json);
            String result = obj.getJSONObject("data").getJSONArray("translations").getJSONObject(0).getString("translatedText");
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
