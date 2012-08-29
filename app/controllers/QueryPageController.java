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
import java.util.*;


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
        List<String> recommendations = new ArrayList<String>(0);
        return ok(views.html.QueryPageController.query.render(queryForm, modelName, results, recommendations));
    }

    public static Result queryPost(String modelName) {
        response().setContentType("text/html");

        Map results;
        List<String> recommendations;

        Form<InferenceQuery> inputForm = queryForm.bindFromRequest();
        if(inputForm.hasErrors())
        {
            System.out.println(inputForm.errors());
            results = new HashMap();
            recommendations = new ArrayList<String>(0);
            return badRequest(views.html.QueryPageController.query.render(queryForm, modelName, results, recommendations));
        }
        InferenceQuery query = inputForm.get();

        int maxTopics;
        int maxRecommendations;
        try
        {
            maxTopics = Integer.parseInt(query.maxTopics);
            maxRecommendations = Integer.parseInt(query.maxRecommendations);
        } catch(Exception e)
        {
            return badRequest("bad input: numeric parameters invalid");
        }
        F.Promise<WS.Response> diffbotQuery;

        try {
            diffbotQuery = WS.url(diffbotUrl).setQueryParameter("token", "xxx").setQueryParameter("url", query.urlInput).get();
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

        try
        {
            if (maxRecommendations > 0)
            {
                List rec = topicModel.recommend(inputNode, maxTopics, maxRecommendations);
                results = (Map) rec.get(0);
                recommendations = (List<String>) rec.get(1);
                results.remove(recommendations);
            } else
            {
                results = topicModel.inferString(inputNode, maxTopics);
                recommendations = new ArrayList<String>(0);
            }
        } catch (Exception e)
        {
            e.printStackTrace();
            return internalServerError("error occurred during inference. Sorry");
        }
        return ok(views.html.QueryPageController.query.render(queryForm, modelName, results, recommendations));
    }
}
