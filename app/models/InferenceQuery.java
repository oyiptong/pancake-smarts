/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

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

    @Constraints.Required
    public String maxTopics;

    @Constraints.Required
    public String maxRecommendations;

    public InferenceQuery(String urlInput, String maxTopics, String maxRecommendations)
    {
        this.urlInput = urlInput;
        this.maxTopics = maxTopics;
        this.maxRecommendations = maxRecommendations;
    }

    public InferenceQuery()
    {

    }

    public String getUrlInput()
    {
        return urlInput;
    }
}
