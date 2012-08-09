package controllers;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import play.*;
import play.libs.Json;
import play.mvc.*;

import java.io.*;
import java.util.zip.GZIPInputStream;

import models.TopicModel;

/**
 * Created with IntelliJ IDEA.
 * User: oyiptong
 * Date: 2012-08-03
 * Time: 4:34 PM
 */
public class TopicModelController extends Controller {


    @BodyParser.Of(BodyParser.Raw.class)
    public static Result create(String modelName) {
        ObjectNode output = Json.newObject();

        File file = request().body().asRaw().asFile();

        InputStream rawInput;
        InputStream input;
        try {
            rawInput = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            output.put("err", "we lost the file");
            return internalServerError(output);
        }

        try {
            input = new GZIPInputStream(rawInput);
        } catch (EOFException e) {
            output.put("err", "upload was truncated");
            return badRequest(output);
        } catch (IOException e) {
            // input not gzipped
            input = rawInput;
        }
        Reader dataReader = new BufferedReader(new InputStreamReader(input));
        try {
            try {
                TopicModel model = new TopicModel(modelName, 50, 0.02, 0.02, dataReader);

                output.put("status", "OK");
                return ok(output);

            } catch (Exception e) {
                dataReader.close();
                System.out.println(e);
                output.put("err", "a model of that name already exists");
                return status(409, output);

            } finally {
                dataReader.close();
            }
        } catch(IOException e) {
            return internalServerError("error!");
        }
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
        //InstanceList previousInstanceList = null;
        return ok("ok");
    }
}
