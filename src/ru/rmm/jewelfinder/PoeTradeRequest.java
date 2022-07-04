package ru.rmm.jewelfinder;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PoeTradeRequest {
    public static final String POESESSID = "POESESSID";
    public static final int MAX_SEEDS_GUEST = 12;
    public static final int MAX_SEEDS_LOGGED = 68;
    public static final String request = """
                    {
                    "query": {
                        "status": {
                            "option": "online"
                        },
                        "name": "",
                        "type": "",
                        "stats": []
                    },
                    "sort": {
                        "price": "asc"
                    }
                }""";
    public static final String GET_LEAGUES = "https://www.pathofexile.com/api/trade/data/leagues";
    private List<String> seeds;
    private JewelType type;
    private String cookie;

    private String league;
    private static final String url ="https://www.pathofexile.com/api/trade/search/";

    private static final String countblock = """
                          {
                          "type": "count",
                          "filters": [
                          ],
                          "disabled": false,
                          "value": {
                            "min": 1
                          }
                        }""";

    private static final String filter = """
                            {
                              "id": "%s",
                              "disabled": false,
                              "value": {
                                "min": 0,
                                "max": 0
                              }
                            }""";


    public PoeTradeRequest( List<String> seeds, JewelType type, String league){
        this(seeds,type, league, null);
    }

    public PoeTradeRequest(List<String> seeds, JewelType type, String league,  String cookie){
        this.seeds = seeds;
        this.type = type;
        this.cookie = cookie;
        this.league = league;
    }
    /*
{
"query": {
    "status": {
        "option": "online"
    },
    "name": "The Pariah",
    "type": "Unset Ring",
    "stats": [{
        "type": "and",
        "filters": []
    }]
},
"sort": {
    "price": "asc"
}
}*/
    public static List<String> getLeagues() throws IOException {
        CloseableHttpClient httpclient = null;
        CloseableHttpResponse response = null;
        httpclient = HttpClients.createDefault();
        HttpGet get = new HttpGet(GET_LEAGUES);
        get.setHeader("Accept", "application/json");
        response = httpclient.execute(get);
        ObjectMapper mapper = new ObjectMapper();
        var result = mapper.readTree(response.getEntity().getContent().readAllBytes());
        var array = (ArrayNode)result.get("result");
        ArrayList<String> leagues = new ArrayList<>();
        array.elements().forEachRemaining(jsonNode -> leagues.add(jsonNode.get("id").asText()));
        return leagues;
    }
    private ObjectNode countBlock(ObjectMapper mapper) throws JsonProcessingException {
        var filterObject = (ObjectNode)mapper.readTree(filter);
        var array = mapper.createArrayNode();
        seeds.stream().forEach(seed -> {
            int seedNum = Integer.valueOf(seed);
            Arrays.stream(type.filters).forEach(s -> {
                var clone = filterObject.deepCopy();
                clone.put("id", s);
                var value = (ObjectNode)clone.get("value");
                value.put("min", seedNum);
                value.put("max", seedNum);
                array.add(clone);
            });
        });

        var countBlock = (ObjectNode)mapper.readTree(countblock);
        countBlock.set("filters", array);
        return countBlock;
    }
    private String doJSON() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode templateObj = (ObjectNode)mapper.readTree(request);
        var query = (ObjectNode)templateObj.get("query");
        query.put("name", this.type.name);
        query.put("type", this.type.type);
        var array = (ArrayNode)query.get("stats");
        array.add(countBlock(mapper));
        return templateObj.toString();
    }



    public PoeTradeRequestResult doRequest() {
        CloseableHttpClient httpclient = null;
        CloseableHttpResponse response = null;
        try {
            String body = doJSON();
            httpclient = HttpClients.createDefault();
            HttpPost httppost = new HttpPost(url + league   );
            StringEntity entity = new StringEntity(body);
            httppost.setEntity(entity);
            httppost.setHeader("Accept", "application/json");
            httppost.setHeader("Content-type", "application/json");
            if(cookie != null){
                String value = POESESSID + "=" + cookie;
                httppost.setHeader("Cookie", value);
            }
            response = httpclient.execute(httppost);
            System.out.println(response);
            if(response.getStatusLine().getStatusCode() != 200){
                return new PoeTradeRequestResult(response.getStatusLine());
            }
            var result = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
            System.out.println(result);
            ObjectMapper mapper = new ObjectMapper();
            var resultJson = mapper.readTree(result);
            String id = resultJson.get("id").asText();
            String resultUrl = "https://www.pathofexile.com/trade/search/" + league + "/" + id;
            return new PoeTradeRequestResult(resultUrl);
        } catch (Exception ex) {
            return new PoeTradeRequestResult(ex);
        }finally {
            try {
                if (httpclient != null) {
                    httpclient.close();
                }
                if(response != null){
                    response.close();
                }
            } catch (IOException e) {

            }
        }
    }
    public class PoeTradeRequestResult{
        public Exception ex = null;
        public String statusLine = null;
        public URI url = null;
        public PoeTradeRequestResult(Exception ex){
            this.ex = ex;
        }
        public PoeTradeRequestResult(StatusLine line){
            this.statusLine = line.toString();
        }
        public PoeTradeRequestResult(String url) throws URISyntaxException {
            this.url = new URI(url);
        }
    };

}
