package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import views.html.*;


/**
 * Created with IntelliJ IDEA.
 * User: oyiptong
 * Date: 2012-08-16
 * Time: 2:58 PM
 */
public class QueryPageController extends Controller {
    public static Result query(String modelName) {
        response().setContentType("text/html");
        return ok(query.render());
    }
}
