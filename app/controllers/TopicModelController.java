package controllers;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import play.*;
import play.libs.Json;
import play.mvc.*;

/**
 * Created with IntelliJ IDEA.
 * User: oyiptong
 * Date: 2012-08-03
 * Time: 4:34 PM
 */
public class TopicModelController extends Controller {

    @BodyParser.Of(BodyParser.Json.class)
    public static Result create(String modelName) {

        JsonNode jsonData = request().body().asJson();
        ObjectNode output = Json.newObject();

        if (jsonData == null || jsonData.isContainerNode() == false) {
            output.put("err", "no json data");
            return badRequest(output);
        }

        JsonNode items = jsonData.findPath("docs");
        if (items == null || items.isArray() == false) {
            output.put("err", "no valid documents provided");
            return badRequest(output);
        }

        for(JsonNode item : items) {
            String title = item.findPath("title").getTextValue();
            String text = item.findPath("text").getTextValue();
            if (text == null || title == null){
                output.put("err", String.format("document \"%1$s\" is invalid", item.toString()));
                return badRequest(output);
            }
        }
        output.put("status", "OK");
        return ok(modelName);
    }

    public static Result delete(String modelName) {
        return ok("ok");
    }

    @BodyParser.Of(BodyParser.Json.class)
    public static Result infer(String modelName) {
        JsonNode jsonData = request().body().asJson();
        ObjectNode output = Json.newObject();
        if (jsonData == null) {
            output.put("err", "no json data");
            return badRequest(output);
        }
        String text = jsonData.findPath("text").getTextValue();
        String title = jsonData.findPath("title").getTextValue();
        if (text == null) {
            output.put("err", "missing text or title fields in input data");
            return badRequest(output);
        }
        output.put("status", "OK");
        return ok(output);
    }

    @BodyParser.Of(BodyParser.Json.class)
    public static Result train(String modelName) {
        return ok("ok");
    }
}
