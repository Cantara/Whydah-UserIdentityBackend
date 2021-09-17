package net.whydah.identity.user.search;

import net.whydah.identity.user.identity.LuceneUserIdentity;
import net.whydah.sso.user.types.UserIdentity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class LuceneUserSearchTest {
    private static final String emailWithUnderScore = "_temp_username_1434635221960@someDomain.com";

    @Test
    public void testSearch() throws IOException {
        RAMDirectory index = new RAMDirectory();
        addUsers(index);

        LuceneUserSearchImpl luceneSearch = new LuceneUserSearchImpl(index);
        List<UserIdentity> result = luceneSearch.search("Ola");
        assertEquals(1, result.size());
        result = luceneSearch.search("Norman");
        assertEquals(2, result.size());
        result = luceneSearch.search("ola@example.com");
        assertEquals(3, result.size());
        result = luceneSearch.search("Pølser");
        assertEquals(0, result.size());

    }


    @Test
    public void testRemoveuser() throws IOException {
        RAMDirectory index = new RAMDirectory();

        LuceneUserIndexer luceneIndexer = addUsers(index);

        LuceneUserSearchImpl luceneSearch = new LuceneUserSearchImpl(index);
        List<UserIdentity> result = luceneSearch.search("Ola");
        assertEquals(1, result.size());
        luceneIndexer.removeFromIndex("ola@example.com");
        result = luceneSearch.search("Ola");
        assertEquals(0, result.size());
    }


    @Test
    public void testModifyUser() throws IOException {
        RAMDirectory index = new RAMDirectory();
        LuceneUserIndexer luceneIndexer = addUsers(index);

        LuceneUserSearchImpl luceneSearch = new LuceneUserSearchImpl(index);
        List<UserIdentity> result = luceneSearch.search("Ola");
        assertEquals(1, result.size());
        result = luceneSearch.search("Ola");
        assertEquals(1, result.size());
        luceneIndexer.updateIndex(createUser("ola@example.com", "Ola", "Norman", "ola@example.com", "ola@example.com"));
        result = luceneSearch.search("Ola");
        assertEquals(1, result.size());
        result = luceneSearch.search("Ola");
        assertEquals(1, result.size());

    }

    @Test
    public void testWildcardSearch() throws Exception {
        Directory index = new RAMDirectory();
        addUsers(index);
        
        LuceneUserSearchImpl luceneSearch = new LuceneUserSearchImpl(index);
        List<UserIdentity> result = luceneSearch.search("Ola");
        assertEquals(1, result.size());
        result = luceneSearch.search("Ola");
        assertEquals(1, result.size());
        result = luceneSearch.search("ola@");
        assertEquals(1, result.size());

        result = luceneSearch.search(emailWithUnderScore);
        assertEquals(1, result.size());
    }

    @Test
    public void testWeights() throws IOException {
        Directory index = new RAMDirectory();
        addUsers(index);

        LuceneUserSearchImpl luceneSearch = new LuceneUserSearchImpl(index);
        List<UserIdentity> result = luceneSearch.search("Norman");
        assertEquals(2, result.size());
        assertEquals("ola@example.com", result.get(1).getUsername());
    }


    private LuceneUserIndexer addUsers(Directory index) throws IOException {
        LuceneUserIndexer luceneIndexer = new LuceneUserIndexer(index);
        List<UserIdentity> users = new ArrayList<UserIdentity>() {{
            add(createUser("kari.norman@example.com", "Kari", "norman", "kari.norman@example.com", "kari.norman@example.com"));
            add(createUser("ola@example.com", "Ola", "Norman", "ola@example.com", "ola@example.com"));
            add(createUser("medel.svenson@example.com", "Medel", "Svenson", "medel.svenson@example.com", "medel.svenson@example.com"));
            add(createUser("test@tull.no", "first", "last", emailWithUnderScore, UUID.randomUUID().toString()));
        }};
        luceneIndexer.addToIndex(users);
        return luceneIndexer;
    }

    private static LuceneUserIdentity createUser(String username, String fornavn, String etternavn, String email, String uid) {
        LuceneUserIdentity user1 = new LuceneUserIdentity();
        user1.setUsername(username);
        user1.setFirstName(fornavn);
        user1.setLastName(etternavn);
        user1.setEmail(email);
        user1.setUid(uid);
        user1.setPersonRef("r"+uid);
        return user1;
    }

}
