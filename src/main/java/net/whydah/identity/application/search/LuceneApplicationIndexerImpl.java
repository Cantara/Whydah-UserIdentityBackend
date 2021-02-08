package net.whydah.identity.application.search;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;

import net.whydah.identity.util.BaseLuceneIndexer;
import net.whydah.sso.application.mappers.ApplicationMapper;
import net.whydah.sso.application.types.Application;

public class LuceneApplicationIndexerImpl extends BaseLuceneIndexer<Application> {
	public static final String FIELD_APPLICATIONID = "applicationid";
	public static final String FIELD_APPLICATIONNAME = "applicationname";
	public static final String FIELD_FULLJSON = "fulljson";
	public static final String FIELD_FULLSEARCH = "fullsearch";

		
	public LuceneApplicationIndexerImpl(Directory luceneApplicationDirectory) throws IOException {
		super(luceneApplicationDirectory);
	}

	@Override
	protected Document createLuceneDocument(Application application) {
		FieldType ftNotTokenized = new FieldType(StringField.TYPE_STORED);
		ftNotTokenized.setTokenized(false);
		ftNotTokenized.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);

		FieldType ftTokenized = new FieldType(StringField.TYPE_STORED);
		ftTokenized.setTokenized(true);

		FieldType ftNotIndexed = new FieldType(StringField.TYPE_STORED);
		ftNotIndexed.setIndexOptions(IndexOptions.NONE);


		Document doc = new Document();
		doc.add(new Field(FIELD_APPLICATIONID, application.getId(), ftNotTokenized)); //Field.Index.NOT_ANALYZED
		doc.add(new Field(FIELD_APPLICATIONNAME, application.getName(), ftNotTokenized)); //Field.Index.NOT_ANALYZED
		doc.add(new Field(FIELD_FULLJSON, ApplicationMapper.toJson(application), ftTokenized));
		doc.add(new Field(FIELD_FULLSEARCH, ApplicationMapper.toPrettyJson(application), ftTokenized));
		return doc;
	}

	@Override
	protected Term getTermForDeletion(String id) {
		return new Term(FIELD_APPLICATIONID, id);
	}

	@Override
	protected Term getTermForUpdate(Application application) {
		return new Term(FIELD_APPLICATIONID, application.getId());
	}

	
}
