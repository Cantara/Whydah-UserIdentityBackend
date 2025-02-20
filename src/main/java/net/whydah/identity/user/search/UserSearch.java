package net.whydah.identity.user.search;

import net.whydah.identity.user.identity.RDBMSUserIdentity;
import net.whydah.identity.user.identity.RDBMSUserIdentityDao;
import net.whydah.identity.user.identity.RDBMSUserIdentityRepository;
import net.whydah.sso.user.types.UserIdentity;
import org.constretto.annotation.Configure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author <a href="bard.lind@gmail.com">Bard Lind</a>
 */
@Service
public class UserSearch {
	private static final Logger log = LoggerFactory.getLogger(UserSearch.class);
	private final RDBMSUserIdentityDao rdbmsUserIdentityDao;
	private final LuceneUserSearch luceneUserSearch;
	private final LuceneUserIndexer luceneUserIndexer;
	RDBMSUserIdentityRepository userIdentityRepository;
	final Lock importLock = new ReentrantLock();
	
	
	@Autowired
	@Configure
	public UserSearch(RDBMSUserIdentityRepository userIdentityRepository, RDBMSUserIdentityDao rdbmsUserIdentityDao, LuceneUserSearch luceneSearch, LuceneUserIndexer luceneIndexer) {
		this.rdbmsUserIdentityDao = rdbmsUserIdentityDao;
		this.luceneUserSearch = luceneSearch;
		this.luceneUserIndexer = luceneIndexer;
		this.userIdentityRepository = userIdentityRepository;
	}

	private void importUsers() {
		
		new Thread(new Runnable() {

			@Override
			public void run() {

				if(!importLock.tryLock()){
					return;
				}

				log.debug("Trying to import users from DB...");
				List<RDBMSUserIdentity> list;
				try {					
					luceneUserIndexer.closeDirectory();
					luceneUserIndexer.deleteAll();
					list = rdbmsUserIdentityDao.allUsersList();
					List<UserIdentity> clones = new ArrayList<UserIdentity>(list);
					log.debug("Found DB user list size: {}", list.size());
					luceneUserIndexer.addToIndex(clones);
				} catch (Exception e) {
					log.error("failed to import users, exception: " + e);
				} finally {
					importLock.unlock();
				}

			}
		}).start();

		
	}

	public List<UserIdentity> search(String query) {
		List<UserIdentity> users = luceneUserSearch.search(query);
		if (users == null) {
			users = new ArrayList<>();
		}
		log.debug("lucene search with query={} returned {} users.", query, users.size());
		importUsers();
		if(getUserIndexSize() != rdbmsUserIdentityDao.countUsers()) {
			log.warn("DB count and lucence size mismatched - lucene index size {} but DB count {}", getUserIndexSize(), rdbmsUserIdentityDao.countUsers() );
			importUsers();
		}
		
		
		return users;
	}

	public boolean isUserIdentityIfExists(String username) {
		//boolean existing = luceneUserSearch.usernameExists(username);
//		boolean existing = false;
//		log.debug("Result from luceneUserSearch existing={}", existing);
//		if (!existing) {
//			return rdbmsUserIdentityDao.getWithUsername(username) != null;
//		}
//		return existing;
		return userIdentityRepository.usernameExist(username);

	}

	public PaginatedUserIdentityDataList query(int page, String query) {
		PaginatedUserIdentityDataList paginatedDL = luceneUserSearch.query(page, query);
		List<UserIdentity> users = paginatedDL.data;
		if (users == null) {
			users = new ArrayList<>();
		}
		log.debug("lucene search with query={} returned {} users.", query, users.size());
		if(getUserIndexSize() != rdbmsUserIdentityDao.countUsers()) {
			log.warn("DB count and lucence size mismatched - lucene index size {} but DB count {}", getUserIndexSize(), rdbmsUserIdentityDao.countUsers() );
			importUsers();
		}
		return paginatedDL;
	}
	

	public int getUserIndexSize() {
		return luceneUserSearch.getUserIndexSize();
	}
}
