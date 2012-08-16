package controllers;

import com.avaje.ebean.Ebean;
import models.Document;
import models.Topic;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import play.libs.Json;
import play.mvc.*;

import java.io.*;
import java.util.List;
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
                TopicModel model = new TopicModel(modelName, 150, 1/150.0, 1/150.0, dataReader);
                model.saveObjectGraph();

                output.put("status", "OK");
                return ok(output);

            } catch (Exception e) {
                dataReader.close();
                System.out.println(e);
                e.printStackTrace();
                //output.put("err", "a model of that name already exists");
                return status(409, e.toString());

            } finally {
                dataReader.close();
            }
        } catch(IOException e) {
            return internalServerError("error!");
        }
    }

    public static Result delete(String modelName) {
        Ebean.beginTransaction();
        ObjectNode output = Json.newObject();
        try {
            TopicModel model = TopicModel.find.where().eq("name", modelName).findUnique();
            long modelId = model.getId();

            // TODO: these don't have to be select queries followed by delete queries
            Ebean.delete(Topic.find.where().eq("topic_model_id", modelId).findList());
            Ebean.delete(Document.find.where().eq("topic_model_id", modelId).findList());
            Ebean.delete(model);

            Ebean.commitTransaction();

            output.put("status", "OK");
            return ok(output);
        } catch (NullPointerException e) {
            output.put("err", "topic not found");
            return notFound(output);
        } finally {
            Ebean.endTransaction();
        }
    }

    @BodyParser.Of(BodyParser.Json.class)
    public static Result recommend(String modelName) {
        JsonNode jsonData = request().body().asJson();
        ObjectNode output = Json.newObject();
        try {
            TopicModel model = TopicModel.fetch(modelName);
            List inferences = model.recommend(jsonData, 0.1, 5);

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            return ok(mapper.writeValueAsString(inferences));
            //model.infer(title, text);
            //model.getInferencer()
        } catch(NullPointerException e) {
            output.put("err", "topic model not found");
            return notFound(output);
        } catch(Exception e) {
            output.put("err", "unknown error");
            System.out.println(e);
            e.printStackTrace();
            return internalServerError(output);
        }
    }

    @BodyParser.Of(BodyParser.Json.class)
    public static Result train(String modelName) {
        //InstanceList previousInstanceList = null;
        return ok("ok");
    }
}
