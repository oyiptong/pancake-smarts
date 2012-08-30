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

        Map inferredWords = new HashMap<String, List<String>>();
        List<String> recommendations = new ArrayList<String>(0);
        HashMap<String, List<Double>> distributionWeights = new HashMap<String, List<Double>>();
        List<String> recommendationWeights = new ArrayList<String>(0);
        return ok(views.html.QueryPageController.query.render(queryForm, modelName, inferredWords, recommendations, distributionWeights, recommendationWeights));
    }

    public static Result queryPost(String modelName) {
        response().setContentType("text/html");

        Map inferredWords;
        List<String> recommendations;
        HashMap<String, List<Double>> distributionWeights;
        List<String> recommendationWeights;

        Form<InferenceQuery> inputForm = queryForm.bindFromRequest();
        if(inputForm.hasErrors())
        {
            System.out.println(inputForm.errors());
            inferredWords = new HashMap();
            recommendations = new ArrayList<String>(0);
            distributionWeights = new HashMap<String, List<Double>>();
            recommendationWeights = new ArrayList<String>();
            return badRequest(views.html.QueryPageController.query.render(queryForm, modelName, inferredWords, recommendations, distributionWeights, recommendationWeights));
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
                inferredWords = (Map) rec.get(0);
                recommendations = (List<String>) rec.get(1);
                // remove the document itself from recommendations if it exists
                inferredWords.remove(recommendations);
                distributionWeights = (HashMap<String, List<Double>>) rec.get(2);
                recommendationWeights = (List<String>) rec.get(3);
            } else
            {
                inferredWords = topicModel.inferString(inputNode, maxTopics);
                recommendations = new ArrayList<String>(0);
                distributionWeights = new HashMap<String, List<Double>>();
                recommendationWeights = new ArrayList<String>(0);
            }
        } catch (Exception e)
        {
            e.printStackTrace();
            return internalServerError("error occurred during inference. Sorry");
        }
        return ok(views.html.QueryPageController.query.render(queryForm, modelName, inferredWords, recommendations, distributionWeights, recommendationWeights));
    }
}
