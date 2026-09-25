package com.likelion13th.Welcomekit_BE.repository;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import com.likelion13th.Welcomekit_BE.domain.Attendance;
import com.likelion13th.Welcomekit_BE.domain.AttendanceSession;
import com.likelion13th.Welcomekit_BE.domain.Team;
import com.likelion13th.Welcomekit_BE.domain.User;
import com.likelion13th.Welcomekit_BE.domain.dto.AttendanceStatusCount;
import com.likelion13th.Welcomekit_BE.domain.dto.response.GetTodayAttendanceResponse;
import com.likelion13th.Welcomekit_BE.domain.enums.AttendanceStatus;
import com.likelion13th.Welcomekit_BE.domain.enums.DevPart;
import com.likelion13th.Welcomekit_BE.domain.enums.UserType;

// 운영 DB는 MySQL. 엔티티 이름 "user"가 H2 예약어라 NON_KEYWORDS로 풀어준다.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
	"spring.datasource.url=jdbc:h2:mem:attendance;MODE=MySQL;NON_KEYWORDS=USER",
	"spring.datasource.driver-class-name=org.h2.Driver",
	"spring.jpa.hibernate.ddl-auto=create-drop"
})
class AttendanceRepositoryTest {

	@Autowired
	TestEntityManager em;
	@Autowired
	AttendanceRepository attendanceRepository;

	@Test
	void 출석부_쿼리는_팀과_이름을_제자리에_넣고_팀_미배정도_포함한다() {
		Team team = em.persist(Team.builder().teamName("1팀").build());
		User kim = em.persist(user("김사자", "201", team));
		User lee = em.persist(user("이사자", "202", null));
		AttendanceSession today = em.persist(AttendanceSession.builder().sessionDate(LocalDateTime.now()).build());
		Attendance a = em.persist(attendance(kim, today, AttendanceStatus.LATE));
		em.persist(attendance(lee, today, AttendanceStatus.ABSENT));
		em.flush();

		List<GetTodayAttendanceResponse> rows =
			attendanceRepository.findTodayAttendance(LocalDateTime.now().toLocalDate().atStartOfDay());

		assertThat(rows).hasSize(2);
		GetTodayAttendanceResponse kimRow = rows.stream().filter(r -> "201".equals(r.getStudentNum())).findFirst()
			.orElseThrow();
		assertThat(kimRow.getName()).isEqualTo("김사자");
		assertThat(kimRow.getTeamName()).isEqualTo("1팀");
		assertThat(kimRow.getAttendanceId()).isEqualTo(a.getId());
		assertThat(kimRow.getAttendanceStatus()).isEqualTo(AttendanceStatus.LATE);

		assertThat(attendanceRepository.findSessionAttendance(today.getId())).hasSize(2);
	}

	@Test
	void 집계_쿼리는_세션별_유저별로_상태를_센다() {
		User kim = em.persist(user("김사자", "201", null));
		User lee = em.persist(user("이사자", "202", null));
		AttendanceSession s1 = em.persist(AttendanceSession.builder().sessionDate(LocalDateTime.now().minusDays(7)).build());
		AttendanceSession s2 = em.persist(AttendanceSession.builder().sessionDate(LocalDateTime.now()).build());
		em.persist(attendance(kim, s1, AttendanceStatus.PRESENT));
		em.persist(attendance(lee, s1, AttendanceStatus.PRESENT));
		em.persist(attendance(kim, s2, AttendanceStatus.ABSENT));
		em.persist(attendance(lee, s2, AttendanceStatus.PRESENT));
		em.flush();

		assertThat(attendanceRepository.countBySessionAndStatus()).containsExactlyInAnyOrder(
			new AttendanceStatusCount(s1.getId(), AttendanceStatus.PRESENT, 2L),
			new AttendanceStatusCount(s2.getId(), AttendanceStatus.ABSENT, 1L),
			new AttendanceStatusCount(s2.getId(), AttendanceStatus.PRESENT, 1L));
		assertThat(attendanceRepository.countByUserAndStatus()).containsExactlyInAnyOrder(
			new AttendanceStatusCount(kim.getId(), AttendanceStatus.PRESENT, 1L),
			new AttendanceStatusCount(kim.getId(), AttendanceStatus.ABSENT, 1L),
			new AttendanceStatusCount(lee.getId(), AttendanceStatus.PRESENT, 2L));
	}

	private User user(String name, String studentNum, Team team) {
		return User.builder().userName(name).studentNum(studentNum).password("x")
			.userType(UserType.BABY_LION).devPart(DevPart.FRONT_END).team(team).build();
	}

	private Attendance attendance(User user, AttendanceSession session, AttendanceStatus status) {
		return Attendance.builder().user(user).attendanceSession(session).status(status).build();
	}
}
