package net.whydah.identity.util;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

public abstract class BaseLuceneIndexer<T> {

	public static final Version LUCENE_VERSION = Version.LUCENE_8_8_0;
	protected static final Analyzer ANALYZER = new StandardAnalyzer();

	protected final Logger log = LoggerFactory.getLogger(BaseLuceneIndexer.class);


	protected FieldType ftNotTokenized = new FieldType(StringField.TYPE_STORED);
	protected FieldType ftTokenized = new FieldType(StringField.TYPE_STORED);
	protected FieldType ftNotIndexed = new FieldType(StringField.TYPE_STORED);

	Directory directory;
	String indexPath;
	
	public BaseLuceneIndexer(Directory luceneDirectory) throws IOException {
		this.directory = luceneDirectory;
		
		if (directory instanceof FSDirectory) {	
			indexPath = ((FSDirectory) directory).getDirectory().toString();
			File path = new File(indexPath);
			if (!path.exists()) {
				path.mkdir();
			}
		}
		
		ftNotTokenized.setTokenized(false);
		ftNotTokenized.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
		ftTokenized.setTokenized(true);
		ftNotIndexed.setIndexOptions(IndexOptions.NONE);
	}

	public synchronized boolean addToIndex(T obj) {
		IndexWriter w = null;
		try {
			w = getIndexWriter();
			Document doc = createLuceneDocument(obj);
			w.addDocument(doc);
			return true;
		} catch (Exception e) {
			log.error("addToIndex failed for {}, error: {}", obj.toString(), e);
		} finally {
			if(w!=null) {
				try {
					w.close();
					closeDirectory();
				} catch (IOException e) {
					log.error("", e);
				}
				
			}
		}
		return false;
	}


	public synchronized void addToIndex(List<T> objs) {
		IndexWriter w = null;
		try {
			w = getIndexWriter();
			int n=0;
			for (T obj : objs) {
				try {				
					Document doc = createLuceneDocument(obj);
					w.addDocument(doc);
				} catch (Exception e) {
					log.error("addToIndex {} failed for {}. This item was not added to lucene index. {}", n++, obj.toString(), e);
				}
			}

		}  catch (Exception e) {
			log.error("", e);
		} finally {
			
			if(w!=null) {
				try {
					w.close();
					closeDirectory();
				} catch (IOException e) {
					log.error("", e);
				}
			}
		}
	}


	public synchronized boolean updateIndex(T obj) {
		IndexWriter w = null;
		try {
			w = getIndexWriter();
			w.updateDocument(getTermForUpdate(obj), createLuceneDocument(obj));
			return true;
		}  catch (Exception e) {
			log.error("updateIndex {} failed. Error {}", obj.toString(), e);
			
		} finally {
			
			if(w!=null) {
				try {
					w.close();
					closeDirectory();
				} catch (IOException e) {
					log.error("", e);
				}
			}
		}
		return false;
	}

	public synchronized void updateIndex(List<T> objs) {
		IndexWriter w = null;
		try {
			w = getIndexWriter();
			for (T obj : objs) {
				try {
					w.updateDocument(getTermForUpdate(obj), createLuceneDocument(obj));
				} catch(IllegalArgumentException e){
					log.error("", e);
				} catch (Exception e) {
					log.error("updateIndex {} failed.", obj.toString(), e);		
					
				}
			}

		}  catch (Exception e) {
			log.error("", e);
		} finally {
			if(w!=null) {
				try {
					w.close();
					closeDirectory();
				} catch (IOException e) {
					log.error("", e);
				}
			}
		}
	}



	public synchronized boolean removeFromIndex(String id) {
		IndexWriter w = null;
		try {
			w = getIndexWriter();
			w.deleteDocuments(getTermForDeletion(id));		
			return true;
		} catch (Exception e) {
			log.error("removeFromIndex failed. uid={}. Error {}", id, e);
		} finally {
			
			if(w!=null) {
				try {
					w.close();
					closeDirectory();
				} catch (IOException e) {
					log.error("", e);
				}
				
			}
		}

		return false;
	}

	protected abstract Term getTermForDeletion(String id);

	protected abstract Term getTermForUpdate(T obj);

	public synchronized void removeFromIndex(List<String> ids) {
		IndexWriter w = null;
		try {
			w = getIndexWriter();
			for (String id : ids) {
				try {
					w.deleteDocuments(getTermForDeletion(id));
				}catch (Exception e) {
					log.error("removeFromIndex {} failed.", id, e);						
				}
			}
		
		} catch (Exception e) {
			log.error("", e);
		}  finally {
			
			if(w!=null) {
				try {
					w.close();
					closeDirectory();
				} catch (IOException e) {
					log.error("", e);
				}
				
			}
		}

	}

	protected abstract Document createLuceneDocument(T obj);



	public synchronized int numDocs() {
		IndexWriter w = null;
		try {
			w = getIndexWriter();
			return w.getDocStats().numDocs;
		} catch (IOException e) {

			log.error("", e);
		} finally {
			if(w!=null) {
				try {
					w.close();
					closeDirectory();
				} catch (IOException e) {
					log.error("", e);
				}
				
			}
		}
		return 0;
	}

	public boolean isDirectoryOpen() {
		if (directory instanceof FSDirectory) {
			try {
				((FSDirectory) directory).getDirectory();	//this will call ensureOpen();			
			}catch(AlreadyClosedException ex) {
				return false;
			}
		}
		return true;
	}


	/** 
	 * Gets an index writer for the repository. The index will be created if it does not already exist or if forceCreate is specified.
	 * @return an IndexWriter
	 * @throws IOException
	 */
	public synchronized IndexWriter getIndexWriter() throws IOException {
		IndexWriterConfig indexWriterConfig = new IndexWriterConfig(ANALYZER);
		indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
		indexWriterConfig.setMaxBufferedDocs(500);
		indexWriterConfig.setRAMBufferSizeMB(300);		
		if(!isDirectoryOpen()) {
			directory = FSDirectory.open(Paths.get(indexPath));
		}

//		Locking/unlocking is not a problem after lucene 5.x
		
//		if (IndexWriter.isLocked(directory)) {
//			IndexWriter.unlock(directory);
//			log.info("Removed Lucene lock file in " + directory);
//		}
		IndexWriter writer = new IndexWriter(directory, indexWriterConfig);
		return writer;
	}
	
	public synchronized void closeDirectory() throws IOException{
		 //the file-based-directory is closed after every operation in order to avoid the "too many open files" exception in Linux
		 if (directory instanceof FSDirectory) {
			 IOUtils.close(directory);
		 }
	 }
}
