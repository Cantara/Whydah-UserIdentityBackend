package net.whydah.identity.user.search;

import net.whydah.identity.util.BaseLuceneIndexer;
import net.whydah.sso.user.mappers.UserAggregateMapper;
import net.whydah.sso.user.types.UserAggregate;
import net.whydah.sso.user.types.UserIdentity;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;

import java.io.IOException;

public class LuceneUserIndexerImpl extends BaseLuceneIndexer<UserIdentity> {


	public static final String FIELD_FIRSTNAME = "firstname";
	public static final String FIELD_LASTNAME = "surname";
	public static final String FIELD_UID = "uid";
	public static final String FIELD_USERNAME = "username";
	public static final String FIELD_EMAIL = "email";
	public static final String FIELD_PERSONREF = "personref";
	public static final String FIELD_MOBILE = "mobile";
	public static final String FIELD_FULLJSON = "fulljson";
	
	public LuceneUserIndexerImpl(Directory luceneDirectory) throws IOException {
		super(luceneDirectory);
	}

	@Override
	protected Term getTermForDeletion(String id) {
		return new Term(FIELD_UID, id);
	}

	@Override
	protected Term getTermForUpdate(UserIdentity obj) {
		return new Term(FIELD_UID, obj.getUid());
	}

	@Override
	protected Document createLuceneDocument(UserIdentity user) {

		try {
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
				doc.add(new Field(FIELD_PERSONREF, user.getPersonRef(), ftNotTokenized));  //Field.Index.NOT_ANALYZED
			}

			if (user.getCellPhone() != null) {
				doc.add(new Field(FIELD_MOBILE, user.getCellPhone(), ftTokenized));
			}

			if(user instanceof UserAggregate) {

				doc.add(new Field(FIELD_FULLJSON, UserAggregateMapper.toJson((UserAggregate)user), ftTokenized));
			}
			return doc;
		}	catch(IllegalArgumentException e) {
			throw e;
		}
	}

}
