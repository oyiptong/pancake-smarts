package controllers;

import models.TextInputCleaner;
import models.TopicModel;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import play.cache.Cache;
import play.data.Form;
import play.libs.F;
import play.libs.WS;
import play.mvc.Controller;
import play.mvc.Result;
import models.InferenceQuery;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created with IntelliJ IDEA.
 * User: oyiptong
 * Date: 2012-08-16
 * Time: 2:58 PM
 */
public class QueryPageController extends Controller {
    final static Form<InferenceQuery> queryForm = form(InferenceQuery.class);
    final static String diffbotUrl = "http://www.diffbot.com/api/article";

    public static Result queryGet(String modelName) {
        response().setContentType("text/html");

        String cache_key = "topicModel." + modelName;
        TopicModel topicModel = (TopicModel) Cache.get(cache_key);
        if(topicModel == null) {
            try
            {
                topicModel = TopicModel.fetch(modelName);
                Cache.set(cache_key, topicModel);
            } catch(NullPointerException e)
            {
                return notFound("This topic model cannot be found");
            } catch(Exception e)
            {
                return internalServerError("Internal Server Error. Sorry");
            }
        }

        Map results = new HashMap<String, List<String>>();
        return ok(views.html.QueryPageController.query.render(queryForm, modelName, results));
    }

    public static Result queryPost(String modelName) {
        response().setContentType("text/html");

        Form<InferenceQuery> inputForm = queryForm.bindFromRequest();
        if(inputForm.hasErrors()) {
            return badRequest("bad input");
        }
        InferenceQuery query = inputForm.get();
        F.Promise<WS.Response> diffbotQuery;

        try {
            diffbotQuery = WS.url(diffbotUrl).setQueryParameter("token", "XXX").setQueryParameter("url", query.urlInput).get();
        } catch (Exception e) {
            return badRequest("please enter a valid url");
        }

        String cache_key = "topicModel." + modelName;

        TopicModel topicModel = (TopicModel) Cache.get(cache_key);
        if(topicModel == null) {
            try
            {
                topicModel = TopicModel.fetch(modelName);
                Cache.set(cache_key, topicModel);
            } catch(NullPointerException e)
            {
                return notFound("This topic model cannot be found");
            } catch(Exception e)
            {
                return internalServerError("Internal Server Error. Sorry");
            }
        }
        WS.Response resp = diffbotQuery.get(Long.valueOf(10000));
        if (resp.getStatus() != 200)
        {
            System.out.println("diffbot status was: " + resp.getStatus());
            System.out.println(resp.getBody());
            return internalServerError("external API not working.\nStatus :" + resp.getStatus() + "\nBody : "+ resp.getBody());

        }
        JsonNode respJson = resp.asJson();

        if (!respJson.has("text")) {
            return badRequest("the supplied url does not contain text");
        }
        String rawText = respJson.get("text").getTextValue();

        List input = new ArrayList();
        Map doc = new HashMap();
        doc.put("name", respJson.get("url").getTextValue());
        doc.put("text", TextInputCleaner.clean(respJson.get("text").getTextValue()));
        doc.put("group", "en");
        input.add(doc);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode inputNode = mapper.valueToTree(input);

        Map results;
        try
        {
            results = topicModel.inferString(inputNode);
        } catch (Exception e)
        {
            e.printStackTrace();
            return internalServerError("error occurred during inference. Sorry");
        }
        return ok(views.html.QueryPageController.query.render(queryForm, modelName, results));
    }
}
