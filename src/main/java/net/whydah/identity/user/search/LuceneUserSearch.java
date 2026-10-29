package net.whydah.identity.user.search;

import java.io.IOException;

import org.apache.lucene.store.Directory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class LuceneUserSearch extends LuceneUserSearchImpl {

    @Autowired
    public LuceneUserSearch(@Qualifier("luceneUserDirectory") Directory luceneUserDirectory) throws IOException {
        super(luceneUserDirectory);
    }
}
