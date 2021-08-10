package net.whydah.identity.user.search;

import net.whydah.identity.user.identity.RDBMSLdapUserIdentityDao;
import net.whydah.identity.user.identity.RDBMSUserIdentity;
import net.whydah.sso.user.types.UserIdentity;
import org.constretto.annotation.Configuration;
import org.constretto.annotation.Configure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="bard.lind@gmail.com">Bard Lind</a>
 */
@Service
public class UserSearch {
	private static final Logger log = LoggerFactory.getLogger(UserSearch.class);
	private final RDBMSLdapUserIdentityDao rdbmsUserIdentityDao;
	private final LuceneUserSearch luceneUserSearch;
	private final LuceneUserIndexer luceneUserIndexer;
	private final boolean alwayslookupinexternaldirectory;

	@Autowired
	@Configure
	public UserSearch(RDBMSLdapUserIdentityDao rdbmsUserIdentityDao, LuceneUserSearch luceneSearch, LuceneUserIndexer luceneIndexer, @Configuration("ldap.primary.alwayslookupinexternaldirectory") boolean _alwayslookupinexternaldirectory) {
		this.rdbmsUserIdentityDao = rdbmsUserIdentityDao;
		this.luceneUserSearch = luceneSearch;
		this.luceneUserIndexer = luceneIndexer;
		this.alwayslookupinexternaldirectory = _alwayslookupinexternaldirectory;
	}

	private void importUsersIfEmpty() {
		if(getUserIndexSize()==0){
			new Thread(new Runnable() {

				@Override
				public void run() {

					log.debug("lucene index is empty. Trying to import from LDAP...");

					List<RDBMSUserIdentity> list;
					try {
						list = rdbmsUserIdentityDao.allUsersList();
						List<UserIdentity> clones = new ArrayList<UserIdentity>(list);
						log.debug("Found LDAP user list size: {}", list.size());
						luceneUserIndexer.addToIndex(clones);
					} catch (Exception e) {
						log.error("failed to import users, exception: " + e);
					}



				}
			}).start();
		}        	

	}

	public List<UserIdentity> search(String query) {
		List<UserIdentity> users = luceneUserSearch.search(query);
		if (users == null) {
			users = new ArrayList<>();
		}
		log.debug("lucene search with query={} returned {} users.", query, users.size());

		importUsersIfEmpty();
		
		return users;
	}

	public UserIdentity getUserIdentityIfExists(String username) {
		UserIdentity user = luceneUserSearch.getUserIdentityIfExists(username);
		if (user == null && alwayslookupinexternaldirectory) {
			user = rdbmsUserIdentityDao.getWithUsername(username);
		}
		return user;
	}
	
	public boolean isUserIdentityIfExists(String username) {
		boolean existing = luceneUserSearch.usernameExists(username);
		if (!existing && alwayslookupinexternaldirectory) {
			return rdbmsUserIdentityDao.getWithUsername(username) != null;
		}
		return existing;

	}

	public PaginatedUserIdentityDataList query(int page, String query) {
		PaginatedUserIdentityDataList paginatedDL = luceneUserSearch.query(page, query);
		List<UserIdentity> users = paginatedDL.data;
		if (users == null) {
			users = new ArrayList<>();
		}
		log.debug("lucene search with query={} returned {} users.", query, users.size());
		importUsersIfEmpty();
		return paginatedDL;
	}

	public int getUserIndexSize() {
		return luceneUserSearch.getUserIndexSize();
	}
}
