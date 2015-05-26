package net.whydah.identity.user.search;

import net.whydah.identity.user.identity.UserIdentity;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Indexer for adding users to the index.
 */
public class LuceneIndexer {
    public static final String FIELD_FIRSTNAME = "firstname";
    public static final String FIELD_LASTNAME = "surname";
    public static final String FIELD_UID = "uid";
    public static final String FIELD_USERNAME = "username";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_PERSONREF = "personref";
    public static final String FIELD_MOBILE = "mobile";

    public static final Version LUCENE_VERSION = Version.LUCENE_4_10_3;
    protected static final Analyzer ANALYZER = new StandardAnalyzer();

    private static final Logger LOGGER = LoggerFactory.getLogger(LuceneIndexer.class);
    private final Directory index;

    public LuceneIndexer(Directory index) {
        this.index = index;

        //Open a writer to ensure segments* file is created.
        IndexWriter w = null;
        try {
            w = getWriter();
            LOGGER.debug("LuceneIndexer initialized. lockId={}", index.getLockID());
        } catch (IOException e) {
            LOGGER.error("getWriter failed.", e);
        } finally {
            closeWriter(w);
        }

        if (index instanceof FSDirectory) {
            LOGGER.info("Lucene is using a FSDirectory index with path {}", ((FSDirectory) index).getDirectory().getPath());
        } else if (index instanceof RAMDirectory) {
            LOGGER.info("Lucene is using RAMDirectory index");
        }
    }

    public void addToIndex(UserIdentity user) {
        IndexWriter w = null;
        try {
            w = getWriter();
            Document doc = createLuceneDocument(user);
            w.addDocument(doc);
        } catch (IOException e) {
            LOGGER.error("addToIndex failed for {}.", user.toString(), e);
        } finally {
            closeWriter(w);
        }
    }


    public void addToIndex(List<UserIdentity> users) throws IOException {
        IndexWriter w = null;
        try {
            w = getWriter();
            for (UserIdentity user : users) {
                try {
                    Document doc = createLuceneDocument(user);
                    w.addDocument(doc);
                } catch (IOException e) {
                    LOGGER.error("addToIndex failed for {}. User was not added to lucene index.", user.toString(), e);
                }
            }
        } finally {
            closeWriter(w);
        }
    }

    public void update(UserIdentity user) {
        IndexWriter w = null;
        try {
            w = getWriter();
            w.updateDocument(new Term(FIELD_UID, user.getUid()), createLuceneDocument(user));
        } catch (IOException e) {
            LOGGER.error("updating {} failed.", user.toString(), e);
        } finally {
            closeWriter(w);
        }
    }

    public void removeFromIndex(String uid) {
        IndexWriter w = null;
        try {
            w = getWriter();
            w.deleteDocuments(new Term(FIELD_UID, uid));
        } catch (IOException e) {
            LOGGER.error("removeFromIndex failed. uid={}", uid, e);
        } finally {
            closeWriter(w);
        }
    }

    /**
     * Use with caution. Close writer after use.
     * @return IndexWriter
     * @throws org.apache.lucene.index.CorruptIndexException if the index is corrupt
     * @throws org.apache.lucene.store.LockObtainFailedException if another writer
     *  has this index open (<code>write.lock</code> could not
     *  be obtained)
     * @throws IOException if the directory cannot be
     *  read/written to or if there is any other low-level
     *  IO error
     */
    private IndexWriter getWriter() throws IOException {
        return new IndexWriter(index, new IndexWriterConfig(LUCENE_VERSION, ANALYZER));
    }

    private Document createLuceneDocument(UserIdentity user) {
        FieldType ftNotTokenized = new FieldType(StringField.TYPE_STORED);
        ftNotTokenized.setTokenized(false);

        FieldType ftTokenized = new FieldType(StringField.TYPE_STORED);
        ftTokenized.setTokenized(true);

        FieldType ftNotIndexed = new FieldType(StringField.TYPE_STORED);
        ftNotIndexed.setIndexed(false);


        Document doc = new Document();
        doc.add(new Field(FIELD_UID, user.getUid(), ftNotTokenized)); //Field.Index.NOT_ANALYZED
        doc.add(new Field(FIELD_USERNAME, user.getUsername(), ftTokenized));
        doc.add(new Field(FIELD_EMAIL, user.getEmail(), ftTokenized));

        if (user.getFirstName() != null) {
            doc.add(new Field(FIELD_FIRSTNAME, user.getFirstName(), ftTokenized));
        }
        if (user.getLastName() != null) {
            doc.add(new Field(FIELD_LASTNAME, user.getLastName(), ftTokenized));
        }
        if (user.getPersonRef() != null) {
            doc.add(new Field(FIELD_PERSONREF, user.getPersonRef(), ftNotIndexed));  //Field.Index.NO
            //For Lucene 5
            //doc.add(new StoredField(FIELD_PERSONREF, user.getPersonRef()));
        }

        if (user.getCellPhone() != null) {
            doc.add(new Field(FIELD_MOBILE, user.getCellPhone(), ftTokenized));
        }
        return doc;
    }


    private void closeWriter(IndexWriter w) {
        if (w != null) {
            try {
                //w.optimize();
                w.close();
            } catch (IOException e) {
                //intentionally ignore
            }
        }
    }
}
