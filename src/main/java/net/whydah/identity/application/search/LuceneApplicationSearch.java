package net.whydah.identity.application.search;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.Directory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class LuceneApplicationSearch extends LuceneApplicationSearchImpl {
   
    protected static final Analyzer ANALYZER = new StandardAnalyzer();  //use LuceneUserIndexer.ANALYZER?
    public static final int MAX_HITS = 500;
    
    @Autowired
    public LuceneApplicationSearch(@Qualifier("luceneApplicationDirectory") Directory luceneApplicationDirectory) {
       super(luceneApplicationDirectory);
    }

}
