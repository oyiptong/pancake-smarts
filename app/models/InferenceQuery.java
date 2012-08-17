package models;

import play.data.validation.Constraints;

/**
 * Created with IntelliJ IDEA.
 * User: oyiptong
 * Date: 2012-08-17
 * Time: 9:56 AM
 */
public class InferenceQuery {
    @Constraints.Required
    public String urlInput;
}
