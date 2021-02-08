package net.whydah.identity.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.IOUtils;

public class BaseLuceneReader {
	
	Directory directory;
	String indexPath;

	public BaseLuceneReader(Directory dir) {
		this.directory = dir;

		if (directory instanceof FSDirectory) {
			indexPath = ((FSDirectory) directory).getDirectory().toString();
			File path = new File(indexPath);
			if (!path.exists()) {
				path.mkdir();
			}
		}

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
	
	public synchronized IndexReader getIndexReader() throws IOException {
		IndexReader r = null;
		//ensure open
		if(!isDirectoryOpen()) {
			directory = FSDirectory.open(Paths.get(indexPath));
		}
		r = DirectoryReader.open(directory);
		return r;
	}

	 public synchronized int getIndexSize() {
		 IndexReader r = null;
		 try {
			 r = getIndexReader();
			 return r.numDocs();
		 } catch (IOException e) {
			
		 } finally {       	
			 if (r != null) {
				 try {
					 r.close();
					 directory.close();
				 } catch (IOException e) {
					
				 }
			 }
		 }
		 return 0;

	    }

	 public synchronized void closeDirectory() throws IOException{
		 //the file-based-directory is closed after every operation in order to avoid the "too many open files" exception in Linux
		 if (directory instanceof FSDirectory) {
			 IOUtils.close(directory);
		 }
	 }
}
