package net.whydah.identity.user.search;

import net.whydah.sso.user.types.UserIdentity;

import java.util.ArrayList;
import java.util.List;

public class PaginatedUserIdentityDataList {

    public int pageNumber=0;
	public int pageSize=Paginator.pageSize;
	public long totalCount=0;
    public List<UserIdentity> data = new ArrayList<UserIdentity>();

    public PaginatedUserIdentityDataList() {

    }

    public PaginatedUserIdentityDataList(int pageNumber, long totalCount, List<UserIdentity> data) {
        this.pageNumber = pageNumber;
        this.totalCount = totalCount;
        this.data = data;
    }


}
