package net.whydah.identity.user.search;

import net.whydah.identity.user.identity.LdapUserIdentityDao;
import net.whydah.identity.user.identity.UserIdentity;
import net.whydah.identity.user.identity.UserIdentityRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="bard.lind@gmail.com">Bard Lind</a>
 */
@Service
public class UserSearch {
    private static final Logger log = LoggerFactory.getLogger(UserSearch.class);
    private final LdapUserIdentityDao ldapUserIdentityDao;
    private final LuceneSearch luceneSearch;
    private final LuceneIndexer luceneIndexer;

    @Autowired
    public UserSearch(LdapUserIdentityDao ldapUserIdentityDao, LuceneSearch luceneSearch, LuceneIndexer luceneIndexer) {
        this.ldapUserIdentityDao = ldapUserIdentityDao;
        this.luceneSearch = luceneSearch;
        this.luceneIndexer = luceneIndexer;
    }

    public List<UserIdentityRepresentation> search(String query) {
        List<UserIdentityRepresentation> users = luceneSearch.search(query);
        if (users == null) {
            users = new ArrayList<>();
        }
        log.debug("lucene search with query={} returned {} users.", query, users.size());

        //If user is not found in lucene, try to search AD.
        if (users.isEmpty()) {
            try {
                UserIdentity user = ldapUserIdentityDao.getUserIndentity(query);
                if (user != null) {
                    users.add(user);
                    //Update user to lucene.
                    log.trace("Added a user found in LDAP to lucene index: {}", user.toString());
                    //luceneIndexer.update(user);
                    luceneIndexer.addToIndex(user);
                }
            } catch (NamingException e) {
                log.warn("Could not find users from ldap/AD. Query: {}", query, e);
            }
        }
        return users;
    }
}
