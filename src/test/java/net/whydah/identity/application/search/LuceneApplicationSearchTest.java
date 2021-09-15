package net.whydah.identity.application.search;

import net.whydah.sso.application.helpers.ApplicationHelper;
import net.whydah.sso.application.mappers.ApplicationMapper;
import net.whydah.sso.application.types.Application;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LuceneApplicationSearchTest {




    @Test
    public void testApplicationSearch() throws IOException {
        RAMDirectory index = new RAMDirectory();
        addApplications(index);

        LuceneApplicationSearch luceneApplicationSearch = new LuceneApplicationSearch(index);


        List<Application> result = luceneApplicationSearch.search("Whydah");
        assertTrue(result.size() > 8);
        List<Application> result2 = luceneApplicationSearch.search("SecurityTokenService");
        assertEquals(1, result2.size());
        List<Application> result3 = luceneApplicationSearch.search("ACS");
        assertEquals(2, result3.size());
        List<Application> result4 = luceneApplicationSearch.search("whydahdev");
        assertEquals(6, result4.size());
        List<Application> result5 = luceneApplicationSearch.search("*");
        assertTrue(result5.size() >= LuceneApplicationSearch.MAX_HITS);
        assertEquals(true, luceneApplicationSearch.isApplicationExists("2212"));
        List<Application> result7 = luceneApplicationSearch.searchApplicationName("Simulated");
        assertEquals(LuceneApplicationSearch.MAX_HITS, result7.size());
        List<Application> result8 = luceneApplicationSearch.searchApplicationName("SecurityTokenService");
        assertEquals(1, result8.size());

    }


    @Test
    public void testWildcardSearch() throws Exception {
        Directory index = new RAMDirectory();
        addApplications(index);

        LuceneApplicationSearch luceneApplicationSearch = new LuceneApplicationSearch(index);
        List<Application> result = luceneApplicationSearch.search("Whydah");
        assertTrue(result.size() > 8);
        result = luceneApplicationSearch.search("ACS");
        assertEquals(2, result.size());
        result = luceneApplicationSearch.search("SecurityTokenService");
        assertEquals(1, result.size());

    }

    private LuceneApplicationIndexer addApplications(Directory index) throws IOException {
        LuceneApplicationIndexer luceneIndexer = new LuceneApplicationIndexer(index);
        List<Application> applications = ApplicationMapper.fromJsonList(ApplicationHelper.getDummyAppllicationListJson());
        Application extensionApplication = applications.get(8);
        for (int n = 0; n < 10000; n++) {
            Application newApp = ApplicationMapper.fromJson(ApplicationMapper.toJson(extensionApplication));
            newApp.setName("Simulated application " + n);
            newApp.setId(UUID.randomUUID().toString());
            applications.add(newApp);
        }
        luceneIndexer.addToIndex(applications);
        return luceneIndexer;
    }

}
