package net.whydah.identity.user.search;


public class Paginator {

    static int pageSize = LuceneUserSearch.MAX_HITS;

    public static ArrayLocation calculateArrayLocation(long totalHits, int pageNumber) {
        ArrayLocation al = new ArrayLocation();
 
        if (totalHits < 1 || pageNumber < 1 || pageSize < 1) {
            al.setStart(0);
            al.setEnd(0);
            return al;
        }
 
        int start= 1 + (pageNumber -1) * pageSize;
        int end = Math.min(pageNumber * pageSize, Math.toIntExact(totalHits));
        if (start > end) {
            start = Math.max(1, end - pageSize);
        }
 
        al.setStart(start);
        al.setEnd(end);
        return al;
    }
}