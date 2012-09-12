import models.ElasticSearch;
import play.Application;
import play.GlobalSettings;
import play.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: oyiptong
 * Date: 2012-09-12
 * Time: 10:15 AM
 */
public class Global extends GlobalSettings
{
    @Override
    public void onStart(Application app)
    {
        ElasticSearch es = ElasticSearch.getElasticSearch();
        Logger.info("Started ElasticSearch");
    }

    @Override
    public void onStop(Application app)
    {
        ElasticSearch es = ElasticSearch.getElasticSearch();
        es.shutdown();
        Logger.info("Shutdown ElasticSearch");
    }
}
