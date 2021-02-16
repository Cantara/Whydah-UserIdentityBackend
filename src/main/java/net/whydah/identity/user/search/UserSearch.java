package net.whydah.identity.user.search;

import net.whydah.identity.user.identity.LDAPUserIdentity;
import net.whydah.identity.user.identity.LdapUserIdentityDao;
import net.whydah.sso.user.types.UserIdentity;
import net.whydah.sso.util.Lock;
import org.constretto.annotation.Configuration;
import org.constretto.annotation.Configure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.naming.NamingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author <a href="bard.lind@gmail.com">Bard Lind</a>
 */
@Service
public class UserSearch {
	private static final Logger log = LoggerFactory.getLogger(UserSearch.class);
	private final LdapUserIdentityDao ldapUserIdentityDao;
	private final LuceneUserSearch luceneUserSearch;
	private final LuceneUserIndexer luceneUserIndexer;
	private final boolean alwayslookupinexternaldirectory;
	ScheduledExecutorService scheduler;
	Lock locker = new Lock();

	@Autowired
	@Configure
	public UserSearch(LdapUserIdentityDao ldapUserIdentityDao, LuceneUserSearch luceneSearch, LuceneUserIndexer luceneIndexer, @Configuration("ldap.primary.alwayslookupinexternaldirectory") boolean _alwayslookupinexternaldirectory) {
		this.ldapUserIdentityDao = ldapUserIdentityDao;
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

					List<LDAPUserIdentity> list;
					try {
						list = ldapUserIdentityDao.getAllUsersPaged(); //getAllUsers();
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

			try {
				user = ldapUserIdentityDao.getUserIndentity(username); //???
			} catch (NamingException e) {
				log.warn("Could not find username from ldap/AD. Query: {}", username, e);
			}
		}
		return user;
	}
	
	public boolean isUserIdentityIfExists(String username) throws NamingException {
		boolean existing = luceneUserSearch.usernameExists(username);
		if (!existing && alwayslookupinexternaldirectory) {
			return ldapUserIdentityDao.usernameExist(username);
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
