package net.whydah.identity.user.search;

import net.whydah.sso.user.types.UserAggregate;

import java.util.ArrayList;
import java.util.List;

public class PaginatedUserAggregateDataList {

    public int pageNumber=0;
	public int pageSize=Paginator.pageSize;
	public long totalCount=0;
    public List<UserAggregate> data = new ArrayList<UserAggregate>();

    public PaginatedUserAggregateDataList() {

    }

    public PaginatedUserAggregateDataList(int pageNumber, long totalCount, List<UserAggregate> data) {
        this.pageNumber = pageNumber;
        this.totalCount = totalCount;
        this.data = data;
    }
    



}
