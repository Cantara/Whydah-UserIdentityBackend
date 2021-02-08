package net.whydah.identity.user.search;

import net.whydah.identity.user.identity.LDAPUserIdentity;
import net.whydah.sso.user.types.UserIdentity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class LuceneUserIndexerTest {
	private static final Logger log = LoggerFactory.getLogger(LuceneUserIndexerTest.class);

	LuceneUserIndexerImpl indexer;
	DirectoryType type = DirectoryType.NIOF; //we can switch to RAM
	Directory dir = null;

	public enum DirectoryType {
		RAM,
		NIOF
	}

	@Before
	public void beforeTest() throws Exception {		
		log.debug("initializing before tests...");
		initDirectory();
	}

	private void initDirectory() throws IOException {
		dir = null;
		if(type == DirectoryType.NIOF) {
			File path = new File("lunceneUserIndexDirectoryTest");
			if (!path.exists()) {
				path.mkdir();
			} else {
				FileSystemUtils.deleteRecursively(path);
				path.mkdir();
			}
			dir = new NIOFSDirectory(Paths.get(path.getPath()));
		} else if(type ==  DirectoryType.RAM){
			dir = new RAMDirectory();
		}

		if(dir!=null) {
			indexer = new LuceneUserIndexerImpl(dir);
			log.debug("initialized");
		} else {
			fail("Initialization failed. No directory found");
		}
	}
	
	@After
	public void afterTest() throws Exception {		
		log.debug("tear down after tests...");
		
		File path = new File("lunceneUserIndexDirectoryTest");
		FileSystemUtils.deleteRecursively(path);
	}

	private static LDAPUserIdentity getRandomUser() {
		String uuid = UUID.randomUUID().toString();
		LDAPUserIdentity user1 = new LDAPUserIdentity();
		user1.setUsername(uuid);
		user1.setFirstName(randomIdentifier());
		user1.setLastName(randomIdentifier());
		user1.setEmail(uuid + "_email@gmail.com");
		user1.setUid(uuid);
		user1.setPersonRef("r_" + uuid);
		return user1;
	}
	

	public static String randomIdentifier() {
		final String lexicon = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		final java.util.Random rand = new java.util.Random();
	    StringBuilder builder = new StringBuilder();
	    while(builder.toString().length() == 0) {
	        int length = rand.nextInt(5)+5;
	        for(int i = 0; i < length; i++) {
	            builder.append(lexicon.charAt(rand.nextInt(lexicon.length())));
	        }
	    }
	    return builder.toString();
	}

	//testing adding with multithreading
	@Test
	public void testAddingASingleUser() throws Exception {
		List<Thread> ts = new ArrayList<>();
		
		for(int i=0; i<100; i++) {			
			Thread t = new Thread(new Runnable() {
				
				@Override
				public void run() {		
					UserIdentity i = getRandomUser();
					boolean ok = indexer.addToIndex(i); //should always return true under normal condition (i.e. enough memory/disk space)
					log.debug(Thread.currentThread().getName() + ( ok? " added item to index":" added item to queue"));					
				}
			});
			
			t.setName("thread_" + i);
			t.start();
			ts.add(t);
		}

		//wait for all threads to complete before we check the result
		for(Thread t : ts) {
			t.join();
		}
		
		
		Thread.sleep(2000);
		log.debug("Thread count " + ts.size());
		log.debug("Total records: " + indexer.numDocs());
		assertTrue(indexer.numDocs()==ts.size());

	}
	
	@Test
	public void testUpdatingASingleUser() throws Exception {		
		UserIdentity i = getRandomUser();
		boolean addResult = indexer.addToIndex(i);
		assertTrue(addResult);
		assertTrue(indexer.numDocs()==1);
		i.setFirstName("Shakespeare");
		i.setLastName("William");
		boolean updateResult = indexer.updateIndex(i);		
		assertTrue(updateResult);
		
		//we have to reopen the directory (the directory is closed after every operation in order to avoid the "too many open files" exception in Linux)
		if(type == DirectoryType.NIOF) {
			File path = new File("lunceneUserIndexDirectoryTest");
			dir = new NIOFSDirectory(Paths.get(path.getPath()));
		}
		
		LuceneUserSearchImpl luceneSearch = new LuceneUserSearchImpl(dir);
		List<UserIdentity> result = luceneSearch.search("William Shakespeare");
		assertTrue(result.size()==1);
	}
	
	@Test
	public void testRemovingASingleUser() {
		UserIdentity i = getRandomUser();
		boolean addResult = indexer.addToIndex(i);
		assertTrue(addResult);
		assertTrue(indexer.numDocs()==1);
		boolean removeResult = indexer.removeFromIndex(i.getUid());	
		assertTrue(removeResult);
		assertTrue(indexer.numDocs()==0);
	}
	
	@Test
	public void testAllOperationsWithMultiThreads() throws Exception {
		List<Thread> ts = new ArrayList<>();
		AtomicInteger numberOfItemsRemoved = new AtomicInteger(0);
		AtomicInteger numberOfItemsUpdated = new AtomicInteger(0);
		
		for(int i=0; i<100; i++) {			
			Thread t = new Thread(new Runnable() {
				
				@Override
				public void run() {		
					UserIdentity i = getRandomUser();
					boolean addResult = indexer.addToIndex(i);
					log.debug(Thread.currentThread().getName() + " added its user to the index");
					assertTrue(addResult);//should return true under normal condition (i.e. enough memory/disk space)
					if(new Random().nextBoolean()) {
						i.setFirstName("Shakespeare");
						i.setLastName("William");
						boolean updateResult = indexer.updateIndex(i);
						assertTrue(updateResult);
						log.debug(Thread.currentThread().getName() + " updated its user");	
						numberOfItemsUpdated.getAndIncrement();
					} else {
						boolean removeResult = indexer.removeFromIndex(i.getUid());
						assertTrue(removeResult);
						log.debug(Thread.currentThread().getName() + " removed its user out of the index list");	
						numberOfItemsRemoved.getAndIncrement();
					}
					
					
				}
			});
			
			t.setName("thread_" + i);
			t.start();
			ts.add(t);
		}
		
		
		
		//wait for all threads to complete before we check the result
		for(Thread t : ts) {
			t.join();
		}
		
	
		
		Thread.sleep(5000);
		log.debug("Thread count " + ts.size());	
		log.debug("Total items updated: " + numberOfItemsUpdated.get());
		log.debug("Total items removed: " + numberOfItemsRemoved.get());
		log.debug("Total records (exlude deletion): " + indexer.numDocs());
		assertTrue(indexer.numDocs()==ts.size() - numberOfItemsRemoved.get());

	}

	@Test
	public void testAddingInBulk() throws Exception {
		
		List<UserIdentity> list = new ArrayList<UserIdentity>();
		for(int i=0; i<1000; i++) {	
			list.add(getRandomUser());
		}
		indexer.addToIndex(list);
		assertTrue(indexer.numDocs()==1000);
	}

	@Test
	public void testUpdatingInBulk() throws Exception {
		
		List<UserIdentity> list = new ArrayList<UserIdentity>();
		for(int i=0; i<100; i++) {	
			list.add(getRandomUser());
		}
		indexer.addToIndex(list);
		assertTrue(indexer.numDocs()==100);
		
		for(int i=0; i<100; i++) {	
			list.get(i).setFirstName("Henry");
		}
		indexer.updateIndex(list);
		
		//we have to reopen the directory (the directory is closed after every operation in order to avoid the "too many open files" exception in Linux)
		if(type == DirectoryType.NIOF) {
			File path = new File("lunceneUserIndexDirectoryTest");
			dir = new NIOFSDirectory(Paths.get(path.getPath()));
		}

		LuceneUserSearchImpl luceneSearch = new LuceneUserSearchImpl(dir);
		List<UserIdentity> result = luceneSearch.search("Henry");
		assertTrue(result.size()==100);
	}
	
	@Test
	public void testDeletingInBulk() throws Exception {
		
		List<UserIdentity> list = new ArrayList<UserIdentity>();
		List<String> uuidList = new ArrayList<String>();
		for(int i=0; i<100; i++) {	
			UserIdentity u = getRandomUser();
			list.add(u);
			uuidList.add(u.getUid());
		}
		indexer.addToIndex(list);
		assertTrue(indexer.numDocs()==100);
		
		indexer.removeFromIndex(uuidList);
		assertTrue(indexer.numDocs()==0);
	}
	
	

}
