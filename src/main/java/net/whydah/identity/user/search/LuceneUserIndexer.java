package net.whydah.identity.user.search;

import org.apache.lucene.store.Directory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Indexer for adding users to the index.
 */
@Service
public class LuceneUserIndexer extends LuceneUserIndexerImpl {

    @Autowired
    public LuceneUserIndexer(@Qualifier("luceneUserDirectory") Directory luceneUserDirectory) throws IOException {
        super(luceneUserDirectory);
    }
}
