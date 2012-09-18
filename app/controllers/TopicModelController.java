/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package controllers;

import com.avaje.ebean.Ebean;
import models.Document;
import models.Topic;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import play.cache.Cache;
import play.libs.Json;
import play.mvc.*;

import java.io.*;
import java.util.List;
import java.util.Map;
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
    public static Result create(String modelName, Integer numTopics) {
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
                TopicModel model = new TopicModel(modelName, numTopics.intValue(), 1/150.0, 1/150.0, dataReader);
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
    public static Result infer(String modelName) {
        JsonNode jsonData = request().body().asJson();
        ObjectNode output = Json.newObject();

        String cache_key = "topicModel." + modelName;

        try {
            TopicModel topicModel = (TopicModel) Cache.get(cache_key);
            if (topicModel == null) {
                topicModel = TopicModel.fetch(modelName);
            }
            Map<String, List<String>> inferences = topicModel.inferString(jsonData, 5);
            Cache.set(cache_key, topicModel);

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            return ok(mapper.writeValueAsString(inferences));
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
