package amgn.amu.repository;

import amgn.amu.entity.Listing;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ListingSearchRepositoryImpl implements ListingSearchRepository {

    private final EntityManager em;

    @Override
    public Page<Listing> searchByTitle(String title, Pageable pageable) {
        boolean hasTitle = StringUtils.hasText(title);

        String base = """
            FROM Listing l
            WHERE l.status = 'ACTIVE'
              """ + (hasTitle ? "AND LOWER(l.title) LIKE LOWER(:title) " : "");

        // 정렬 처리 (pageable.getSort() 사용, 없으면 createdAt DESC)
        String orderBy = toOrderBy(pageable.getSort(), "l", "createdAt");

        // 조회 쿼리
        TypedQuery<Listing> query = em.createQuery("SELECT l " + base + orderBy, Listing.class);
        if (hasTitle) query.setParameter("title", "%" + title + "%");

        // EntityGraph로 photos 로딩 (fetch join + pagination 이슈 회피)
        EntityGraph<?> graph = em.createEntityGraph(Listing.class);
        graph.addAttributeNodes("photos");
        query.setHint("jakarta.persistence.fetchgraph", graph);

        // 페이징
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        List<Listing> content = query.getResultList();

        // 카운트 쿼리
        TypedQuery<Long> countQuery = em.createQuery("SELECT COUNT(l) " + base, Long.class);
        if (hasTitle) countQuery.setParameter("title", "%" + title + "%");
        long total = countQuery.getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    private String toOrderBy(Sort sort, String alias, String defaultField) {
        if (sort == null || sort.isUnsorted()) {
            return " ORDER BY " + alias + "." + defaultField + " DESC";
        }
        List<String> orders = new ArrayList<>();
        for (Sort.Order o : sort) {
            String dir = o.isAscending() ? "ASC" : "DESC";
            String prop = o.getProperty();
            // 간단 방어: 알려진 필드만 허용하거나 화이트리스트로 관리해도 좋음
            orders.add(alias + "." + prop + " " + dir);
        }
        String orderBy = orders.stream().collect(Collectors.joining(", "));
        return " ORDER BY " + orderBy;
    }
}
