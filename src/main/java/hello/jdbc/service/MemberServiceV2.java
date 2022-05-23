package hello.jdbc.service;

import hello.jdbc.domain.Member;
import hello.jdbc.repository.MemberRepositoryV1;
import hello.jdbc.repository.MemberRepositoryV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 트랜잭션 - 파라미터 연동, 풀을 고려한 종료
 */
@Slf4j
@RequiredArgsConstructor
public class MemberServiceV2 {

    private final DataSource dataSource;
    private final MemberRepositoryV2 memberRepository;

    public void accountTransfer(String fromId, String toId, int money) throws SQLException {
        Connection con = dataSource.getConnection();
        try {
            con.setAutoCommit(false); // 트랜잭션 시작

            // 비즈니스 로직
            Member fromMember = memberRepository.findById(con, fromId);
            Member toMember = memberRepository.findById(con, toId);

            memberRepository.update(fromId, fromMember.getMoney() - money);
            if (toMember.getMemberId().equals("ex")) {
                throw new IllegalStateException("이체중 예외 발생");
            }
            memberRepository.update(toId, toMember.getMoney() + money);
            con.commit(); // 성공 시 커밋

        } catch (Exception e) {
            con.rollback(); // 실패 시 롤백
            throw new IllegalStateException(e);

        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true); // 오토 커밋을 바꿔주고 다시 커넥션 풀로 보내야 함
                    con.close();
                } catch (Exception e) {
                    log.info("error", e);
                }
            }
        }
    }
}
