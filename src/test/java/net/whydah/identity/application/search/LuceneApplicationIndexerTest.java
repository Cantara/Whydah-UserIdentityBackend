package net.whydah.identity.application.search;

import net.whydah.sso.application.helpers.ApplicationHelper;
import net.whydah.sso.application.mappers.ApplicationMapper;
import net.whydah.sso.application.types.Application;
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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class LuceneApplicationIndexerTest {
	private static final Logger log = LoggerFactory.getLogger(LuceneApplicationIndexerTest.class);
	LuceneApplicationIndexerImpl indexer;
	DirectoryType type = DirectoryType.NIOF; //we can switch to RAM
	Directory dir = null;

	public enum DirectoryType {
		RAM,
		NIOF
	}

	@Before
	public void beforeTest() throws Exception {		
		log.debug("initializing before tests...");
		dir = null;
		if(type == DirectoryType.NIOF) {
			File path = new File("lunceneApplciationIndexDirectoryTest");
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
			indexer = new LuceneApplicationIndexerImpl(dir);
			log.debug("initialized");
		} else {
			fail("Initialization failed. No directory found");
		}
	}
	
	@After
	public void afterTest() throws Exception {		
		log.debug("tear down after tests...");
		
		File path = new File("lunceneApplciationIndexDirectoryTest");
		FileSystemUtils.deleteRecursively(path);
	}

	private static List<Application> getDummyApps() {
		List<Application> applications = ApplicationMapper.fromJsonList(ApplicationHelper.getDummyAppllicationListJson());
		Application extensionApplication = applications.get(8);
		for (int n = 0; n < 100; n++) {
			Application newApp = ApplicationMapper.fromJson(ApplicationMapper.toJson(extensionApplication));
			newApp.setName("Simulated application " + n);
			newApp.setId(UUID.randomUUID().toString());
			applications.add(newApp);
		}
		return applications;
	}


	//testing adding with multithreading
	@Test
	public void testAddingASingleApp() throws Exception {
		List<Thread> ts = new ArrayList<>();
		
		for(Application app : getDummyApps()) {			
			Thread t = new Thread(new Runnable() {
				
				@Override
				public void run() {		
					
					boolean ok = indexer.addToIndex(app); //should always return true under normal condition (i.e. enough memory/disk space)
					log.debug(Thread.currentThread().getName() + ( ok? " added item to index":" added item to queue"));					
				}
			});
			
			t.setName("thread_" + app.getName());
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
	public void testUpdatingASingleApp() throws Exception {		
		Application i = getDummyApps().get(0);
		boolean addResult = indexer.addToIndex(i);
		assertTrue(addResult);
		assertTrue(indexer.numDocs()==1);
		i.setCompany("Delta");
		boolean updateResult = indexer.updateIndex(i);		
		assertTrue(updateResult);
		
		//we have to reopen the directory (the directory is closed after every operation in order to avoid the "too many open files" exception in Linux)
		if(type == DirectoryType.NIOF) {
			File path = new File("lunceneApplciationIndexDirectoryTest");
			dir = new NIOFSDirectory(Paths.get(path.getPath()));
		}

		LuceneApplicationSearchImpl luceneSearch = new LuceneApplicationSearchImpl(dir);
		List<Application> result = luceneSearch.search("Delta");
		assertTrue(result.size()==1);
	}
	
	@Test
	public void testRemovingASingleApp() {
		Application i = getDummyApps().get(0);
		boolean addResult = indexer.addToIndex(i);
		assertTrue(addResult);
		assertTrue(indexer.numDocs()==1);
		boolean removeResult = indexer.removeFromIndex(i.getId());	
		assertTrue(removeResult);
		assertTrue(indexer.numDocs()==0);
	}
	
	@Test
	public void testAllOperationsWithMultiThreads() throws Exception {
		List<Thread> ts = new ArrayList<>();
		AtomicInteger numberOfItemsRemoved = new AtomicInteger(0);
		AtomicInteger numberOfItemsUpdated = new AtomicInteger(0);
		
		for(Application app : getDummyApps()) {			
			Thread t = new Thread(new Runnable() {
				
				@Override
				public void run() {		
					
					boolean addResult = indexer.addToIndex(app);
					log.debug(Thread.currentThread().getName() + " added its item to the index");
					assertTrue(addResult);//should return true under normal condition (i.e. enough memory/disk space)
					if(new Random().nextBoolean()) {
						app.setCompany("Capra test");
						boolean updateResult = indexer.updateIndex(app);
						assertTrue(updateResult);
						log.debug(Thread.currentThread().getName() + " updated its item");	
						numberOfItemsUpdated.getAndIncrement();
					} else {
						boolean removeResult = indexer.removeFromIndex(app.getId());
						assertTrue(removeResult);
						log.debug(Thread.currentThread().getName() + " removed its item out of the index list");	
						numberOfItemsRemoved.getAndIncrement();
					}
					
					
				}
			});
			
			t.setName("thread_" + app.getName());
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
		
		List<Application> apps = getDummyApps();
		indexer.addToIndex(getDummyApps());
		assertTrue(indexer.numDocs()==apps.size());
	}

	@Test
	public void testUpdatingInBulk() throws Exception {
		
		List<Application> apps = getDummyApps();
		indexer.addToIndex(getDummyApps());
		assertTrue(indexer.numDocs()==apps.size());
		
		for(Application app: apps) {	
			app.setCompany("Delta");
		}
		indexer.updateIndex(apps);
		
		//we have to reopen the directory (the directory is closed after every operation in order to avoid the "too many open files" exception in Linux)
		if(type == DirectoryType.NIOF) {
			File path = new File("lunceneApplciationIndexDirectoryTest");
			dir = new NIOFSDirectory(Paths.get(path.getPath()));
		}
		
		LuceneApplicationSearchImpl luceneSearch = new LuceneApplicationSearchImpl(dir);
		List<Application> result = luceneSearch.search("Delta");
		assertTrue(result.size()==apps.size());
	}
	
	@Test
	public void testDeletingInBulk() throws Exception {
		
		List<Application> apps = getDummyApps();
		List<String> idList = new ArrayList<String>();
		for(Application app: apps) {			
			idList.add(app.getId());
		}
		indexer.addToIndex(apps);
		assertTrue(indexer.numDocs()==apps.size());
		
		indexer.removeFromIndex(idList);
		assertTrue(indexer.numDocs()==0);
	}
	
	

}
