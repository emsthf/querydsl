package study.querydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;

import java.util.List;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

public class MemberRepositoryImpl implements MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;  // Querydsl을 사용할 것이므로 주입

    public MemberRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);  // JPAQueryFactory를 빈 방식으로 구현하지 않았기 때문에 EntityManager를 주입받아 JPAQueryFactory 생성
    }

    @Override
    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .fetch();
    }

    private BooleanExpression usernameEq(String username) {  // 반환 타입은 Predicate 보다는 BooleanExpression을 사용하는 것이 컴포지션도 되고 쓸모가 많다.
        return hasText(username) ? member.username.eq(username) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return hasText(teamName) ? team.name.eq(teamName) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }

    private BooleanExpression ageBetween(int ageLoe, int ageGoe) {
        return ageGoe(ageGoe).and(ageLoe(ageLoe));
    }

    @Override
    public Page<MemberTeamDto> searchPageDeprecated(MemberSearchCondition condition, Pageable pageable) {  // 파라미터로 Pageable을 받아 페이징 처리. Pageable은 offset이나 전체 페이지 수를 알 수 있다.
        QueryResults<MemberTeamDto> results = queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .offset(pageable.getOffset())  // offset()은 몇 번째 row부터 조회할지 결정한다.
                .limit(pageable.getPageSize())  // limit()은 조회할 row 수를 결정한다.
                .fetchResults();  // fetchResults()는 deprecated 되었으므로 이 simple 방법은 사용하지 않는 것이 좋다.

        List<MemberTeamDto> content = results.getResults();  // 조회된 결과
        long total = results.getTotal();  // 전체 row 수

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<MemberTeamDto> searchPage(MemberSearchCondition condition, Pageable pageable) {
        // contents만 가져오는 쿼리
        List<MemberTeamDto> content = queryFactory
                .select(new QMemberTeamDto(
                        member.id,
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .offset(pageable.getOffset())  // offset()은 몇 번째 row부터 조회할지 결정한다.
                .limit(pageable.getPageSize())  // limit()은 조회할 row 수를 결정한다.
                .fetch();

        // total count를 가져오는 쿼리
        // 상황에 따라 다르지만 어떤 상황에는 count 할 때 조인이 필요없는 쿼리도 있다.(조인을 하든 안하든 카운트 수가 변함이 없거나 DB에 이미 카운트가 계산되어 저장되어 있거나...)
        // 조인이 필요 없게 되면 더 최적화가 되겠지? 그리고 카운트 쿼리의 결과가 없으면 contents 쿼리는 콜하지 않는다거나 해서 최적화 할 수도 있다.
        JPAQuery<Member> countQuery = queryFactory
                .select(member)
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                );  // fetch().size() 부분 제거한 반환형을 받는다.

        return PageableExecutionUtils.getPage(content, pageable, countQuery.fetch()::size);
    }
}
