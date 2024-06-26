package net.whydah.identity.user.search;

import net.whydah.identity.user.identity.LuceneUserIdentity;
import net.whydah.identity.util.BaseLuceneReader;
import net.whydah.sso.user.types.UserIdentity;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class LuceneUserSearchImpl extends BaseLuceneReader {
    private static final Logger logger = LoggerFactory.getLogger(LuceneUserSearchImpl.class);
    protected static final Analyzer ANALYZER = new StandardAnalyzer();  //use LuceneUserIndexer.ANALYZER?
    private static final Pattern uuidPattern = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    protected static final Analyzer uuidAnalyzer = new WhitespaceAnalyzer();

    public static final int MAX_HITS = 250;


    public LuceneUserSearchImpl(Directory luceneUserDirectory) {
        super(luceneUserDirectory);
    }
    
    public synchronized boolean usernameExists(String username) {
        String wildCardQuery = username;
        String[] fields = {
                LuceneUserIndexer.FIELD_USERNAME,
        };
        HashMap<String, Float> boosts = new HashMap<>();
        boosts.put(LuceneUserIndexer.FIELD_USERNAME, 1.5f);
        MultiFieldQueryParser multiFieldQueryParser = new MultiFieldQueryParser(fields, ANALYZER, boosts);
        multiFieldQueryParser.setAllowLeadingWildcard(true);
        Query q;
        try {
            q = multiFieldQueryParser.parse(wildCardQuery);
        } catch (ParseException e) {
            logger.error("Could not parse wildCardQuery={}. Returning empty search result.", wildCardQuery, e);
            return false;
        }
        IndexReader directoryReader = null;
        try {
        	directoryReader = getIndexReader();
            IndexSearcher searcher = new IndexSearcher(directoryReader);
            TopDocs topDocs = searcher.search(q, 1);
            if (topDocs.totalHits.value>0){
            	for (ScoreDoc hit : topDocs.scoreDocs) {
                    int docId = hit.doc;
                    Document d = searcher.doc(docId);
                    LuceneUserIdentity user = new LuceneUserIdentity();
                    user.setFirstName(d.get(LuceneUserIndexer.FIELD_FIRSTNAME));
                    user.setLastName(d.get(LuceneUserIndexer.FIELD_LASTNAME));
                    user.setUid(d.get(LuceneUserIndexer.FIELD_UID));
                    user.setUsername(d.get(LuceneUserIndexer.FIELD_USERNAME));
                    user.setPersonRef(d.get(LuceneUserIndexer.FIELD_PERSONREF));
                    user.setCellPhone(d.get(LuceneUserIndexer.FIELD_MOBILE));
                    user.setEmail(d.get(LuceneUserIndexer.FIELD_EMAIL));
                    logger.debug("lucene search found user {}", user);
                }
            	
                return true;
            }
        } catch (IOException e) {
            logger.error("Error when searching.", e);
        } finally {
            if (directoryReader != null) {
                try {
                    directoryReader.close();
                    closeDirectory();
                } catch (IOException e) {
                    logger.info("searcher.close() failed. Ignore. {}", e.getMessage());
                }
            }
        }
        return false;
    }

    public synchronized int getUserIndexSize() {
        return getIndexSize();
    }

    public synchronized UserIdentity getUserIdentityIfExists(String username) {
        String wildCardQuery = username;
        String[] fields = {
                LuceneUserIndexer.FIELD_USERNAME,
        };
        HashMap<String, Float> boosts = new HashMap<>();
        boosts.put(LuceneUserIndexer.FIELD_USERNAME, 1.5f);
        MultiFieldQueryParser multiFieldQueryParser = new MultiFieldQueryParser(fields, ANALYZER, boosts);
        multiFieldQueryParser.setAllowLeadingWildcard(true);
        Query q;
        try {
            q = multiFieldQueryParser.parse(wildCardQuery);
        } catch (ParseException e) {
            logger.error("Could not parse wildCardQuery={}. Returning empty search result.", wildCardQuery, e);
            return null;
        }
      
        IndexReader directoryReader = null;
        try {
        	directoryReader = getIndexReader();
            IndexSearcher searcher = new IndexSearcher(directoryReader);
            TopDocs topDocs = searcher.search(q, 1);
            if (topDocs.totalHits.value>0){
            	
            	 for (ScoreDoc hit : topDocs.scoreDocs) {
                     int docId = hit.doc;
                     Document d = searcher.doc(docId);
                     LuceneUserIdentity user = new LuceneUserIdentity();
                     user.setFirstName(d.get(LuceneUserIndexer.FIELD_FIRSTNAME));
                     user.setLastName(d.get(LuceneUserIndexer.FIELD_LASTNAME));
                     user.setUid(d.get(LuceneUserIndexer.FIELD_UID));
                     user.setUsername(d.get(LuceneUserIndexer.FIELD_USERNAME));
                     user.setPersonRef(d.get(LuceneUserIndexer.FIELD_PERSONREF));
                     user.setCellPhone(d.get(LuceneUserIndexer.FIELD_MOBILE));
                     user.setEmail(d.get(LuceneUserIndexer.FIELD_EMAIL));
                     return user;
                 }
            	 
            }
        } catch (IOException e) {
            logger.error("Error when searching.", e);
        } finally {
            if (directoryReader != null) {
                try {
                    directoryReader.close();
                    closeDirectory();
                } catch (IOException e) {
                    logger.info("searcher.close() failed. Ignore. {}", e.getMessage());
                }
            }
        }
        return null;
    }

    
    public synchronized List<UserIdentity> search(String queryString) {
        String[] fields;
        HashMap<String, Float> boosts = new HashMap<>();
        Analyzer analyzer;
        String effectiveQuery;

        if (uuidPattern.matcher(queryString.trim()).matches()) {
            // search query is an uuid, simplify search to be a case-insensitive exact-match search against personref field
            analyzer = uuidAnalyzer;
            effectiveQuery = queryString.trim().toLowerCase();
            fields = new String[]{
                    LuceneUserIndexer.FIELD_PERSONREF_LC
            };
        }

        else {
            // normal wide query
            analyzer = ANALYZER;
            effectiveQuery = buildWildCardQuery(queryString);
            fields = new String[]{
                    LuceneUserIndexer.FIELD_FIRSTNAME,
                    LuceneUserIndexer.FIELD_LASTNAME,
                    LuceneUserIndexer.FIELD_EMAIL,
                    LuceneUserIndexer.FIELD_USERNAME,
                    LuceneUserIndexer.FIELD_MOBILE,
                    LuceneUserIndexer.FIELD_PERSONREF,
                    LuceneUserIndexer.FIELD_PERSONREF_LC
            };
            boosts = new HashMap<>();
            boosts.put(LuceneUserIndexer.FIELD_FIRSTNAME, 2.5f);
            boosts.put(LuceneUserIndexer.FIELD_LASTNAME, 2f);
            boosts.put(LuceneUserIndexer.FIELD_USERNAME, 1.5f);
            boosts.put(LuceneUserIndexer.FIELD_PERSONREF, 1.5f);
            boosts.put(LuceneUserIndexer.FIELD_PERSONREF_LC, 1.5f);
        }

        MultiFieldQueryParser multiFieldQueryParser = new MultiFieldQueryParser(fields, analyzer, boosts);
        multiFieldQueryParser.setAllowLeadingWildcard(true);
        Query q;
        try {
            q = multiFieldQueryParser.parse(effectiveQuery);
        } catch (ParseException e) {
            logger.error("Could not parse wildCardQuery={}. Returning empty search result.", effectiveQuery, e);
            return new ArrayList<>();
        }

        List<UserIdentity> result = new ArrayList<>();
        IndexReader directoryReader = null;
        try {
        	directoryReader = getIndexReader();
            IndexSearcher searcher = new IndexSearcher(directoryReader);
            TopDocs topDocs = searcher.search(q, MAX_HITS);

            for (ScoreDoc hit : topDocs.scoreDocs) {
                int docId = hit.doc;
                Document d = searcher.doc(docId);
                LuceneUserIdentity user = new LuceneUserIdentity();
                user.setFirstName(d.get(LuceneUserIndexer.FIELD_FIRSTNAME));
                user.setLastName(d.get(LuceneUserIndexer.FIELD_LASTNAME));
                user.setUid(d.get(LuceneUserIndexer.FIELD_UID));
                user.setUsername(d.get(LuceneUserIndexer.FIELD_USERNAME));
                user.setPersonRef(d.get(LuceneUserIndexer.FIELD_PERSONREF));
                user.setCellPhone(d.get(LuceneUserIndexer.FIELD_MOBILE));
                user.setEmail(d.get(LuceneUserIndexer.FIELD_EMAIL));
                //log.debug(user.getUsername() + " : " + hit.score);
                result.add(user);
            }
        } catch (IOException e) {
            logger.error("Error when searching.", e);
        } finally {
            if (directoryReader != null) {
                try {
                    directoryReader.close();
                    closeDirectory();
                } catch (IOException e) {
                    logger.info("searcher.close() failed. Ignore. {}", e.getMessage());
                }
            }
        }

        return result;
    }

    private String buildWildCardQuery(String queryString) {
        queryString=queryString.replace("_","").trim();
        String[] queryitems = queryString.split(" ");
        StringBuilder strb = new StringBuilder();
        for (String queryitem : queryitems) {
            strb.append(queryitem).append("^2 ");
            strb.append(queryitem).append("* ");
        }
        String wildCardQuery = strb.toString();
        logger.debug("Original query={}, wildcard query= {}", queryString, wildCardQuery);
        return wildCardQuery;
    }

    public synchronized PaginatedUserIdentityDataList query(int pageNumber, String queryString) {
        String wildCardQuery = buildWildCardQuery(queryString);
        String[] fields = {
                LuceneUserIndexer.FIELD_FIRSTNAME,
                LuceneUserIndexer.FIELD_LASTNAME,
                LuceneUserIndexer.FIELD_EMAIL,
                LuceneUserIndexer.FIELD_USERNAME,
                LuceneUserIndexer.FIELD_MOBILE,
                LuceneUserIndexer.FIELD_PERSONREF
        };
        HashMap<String, Float> boosts = new HashMap<>();
        boosts.put(LuceneUserIndexer.FIELD_FIRSTNAME, 2.5f);
        boosts.put(LuceneUserIndexer.FIELD_LASTNAME, 2f);
        boosts.put(LuceneUserIndexer.FIELD_USERNAME, 1.5f);
        boosts.put(LuceneUserIndexer.FIELD_PERSONREF, 1.5f);
      
        MultiFieldQueryParser multiFieldQueryParser = new MultiFieldQueryParser(fields, ANALYZER, boosts);
        multiFieldQueryParser.setAllowLeadingWildcard(true);
        Query q;
        try {
            q = multiFieldQueryParser.parse(wildCardQuery);
        } catch (ParseException e) {
            logger.error("Could not parse wildCardQuery={}. Returning empty search result.", wildCardQuery, e);
            return new PaginatedUserIdentityDataList();
        }

        List<UserIdentity> result = new ArrayList<>();
        long hits = 0;
        IndexReader directoryReader = null;
        try {
        	directoryReader = getIndexReader();
            IndexSearcher searcher = new IndexSearcher(directoryReader);
            TotalHitCountCollector collector = new TotalHitCountCollector();
            TopDocs topDocs = searcher.search(q, Integer.MAX_VALUE);
            hits = topDocs.totalHits.value;
            List<String> foundUserIds = new LinkedList<>();
            ArrayLocation arrayLocation = Paginator.calculateArrayLocation(hits, pageNumber);
            if(hits>0 && arrayLocation.getStart() !=0){
            	for (int i = arrayLocation.getStart() - 1; i < arrayLocation.getEnd(); i++) {

                    int docId = topDocs.scoreDocs[i].doc;

                    Document d = searcher.doc(docId);
                    if (!foundUserIds.contains(d.get(LuceneUserIndexer.FIELD_UID))) {
                        LuceneUserIdentity user = new LuceneUserIdentity();
                        user.setFirstName(d.get(LuceneUserIndexer.FIELD_FIRSTNAME));
                        user.setLastName(d.get(LuceneUserIndexer.FIELD_LASTNAME));
                        user.setUid(d.get(LuceneUserIndexer.FIELD_UID));
                        user.setUsername(d.get(LuceneUserIndexer.FIELD_USERNAME));
                        user.setPersonRef(d.get(LuceneUserIndexer.FIELD_PERSONREF));
                        user.setCellPhone(d.get(LuceneUserIndexer.FIELD_MOBILE));
                        user.setEmail(d.get(LuceneUserIndexer.FIELD_EMAIL));
                        //log.debug(user.getUsername() + " : " + hit.score);
                        result.add(user);
                        foundUserIds.add(user.getUid());
                    }
                }
            }
            
          
          
        } catch (IOException e) {
            logger.error("Error when searching.", e);
        } finally {
            if (directoryReader != null) {
                try {
                    directoryReader.close();
                    closeDirectory();
                } catch (IOException e) {
                    logger.info("searcher.close() failed. Ignore. {}", e.getMessage());
                }
            }
        }

        return new PaginatedUserIdentityDataList(pageNumber, hits, result);

        
    }
    
}
