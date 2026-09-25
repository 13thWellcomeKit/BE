package com.likelion13th.Welcomekit_BE.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.likelion13th.Welcomekit_BE.domain.Attendance;
import com.likelion13th.Welcomekit_BE.domain.AttendanceSession;
import com.likelion13th.Welcomekit_BE.domain.Team;
import com.likelion13th.Welcomekit_BE.domain.User;
import com.likelion13th.Welcomekit_BE.domain.dto.AttendanceStatusCount;
import com.likelion13th.Welcomekit_BE.domain.dto.request.MarkAttendanceRequest;
import com.likelion13th.Welcomekit_BE.domain.dto.response.GetTodayAttendanceResponse;
import com.likelion13th.Welcomekit_BE.domain.dto.response.MemberAttendanceSummaryResponse;
import com.likelion13th.Welcomekit_BE.domain.enums.AttendanceStatus;
import com.likelion13th.Welcomekit_BE.domain.enums.DevPart;
import com.likelion13th.Welcomekit_BE.domain.enums.UserType;
import com.likelion13th.Welcomekit_BE.exception.CustomException;
import com.likelion13th.Welcomekit_BE.exception.ErrorCode;
import com.likelion13th.Welcomekit_BE.repository.AttendanceRepository;
import com.likelion13th.Welcomekit_BE.repository.AttendanceSessionRepository;

@ExtendWith(MockitoExtension.class)
class AttendanceSessionServiceTest {

	@Mock
	AttendanceSessionRepository sessionRepository;
	@Mock
	AttendanceRepository attendanceRepository;
	@InjectMocks
	AttendanceSessionService service;

	User admin;
	User member;

	@BeforeEach
	void setUp() {
		admin = User.builder().id(1L).userName("운영진").studentNum("100").userType(UserType.ADMIN).build();
		member = User.builder().id(2L).userName("아기사자").studentNum("200").userType(UserType.BABY_LION)
			.devPart(DevPart.FRONT_END).team(Team.builder().id(1L).teamName("1팀").build()).build();
	}

	// ---------- P1: 출석 상태 수정 ----------

	@Test
	void 결석으로_바꾸면_출석시각을_지운다() {
		Attendance a = attendance(AttendanceStatus.PRESENT, LocalDateTime.now().minusMinutes(5));
		when(attendanceRepository.findById(10L)).thenReturn(Optional.of(a));

		GetTodayAttendanceResponse res = service.updateAttendanceStatus(10L, AttendanceStatus.ABSENT, admin);

		assertThat(a.getStatus()).isEqualTo(AttendanceStatus.ABSENT);
		assertThat(a.getAttendanceTime()).isNull();
		assertThat(a.getModifiedBy()).isEqualTo("100");
		assertThat(a.getModifiedAt()).isNotNull();
		assertThat(res.getName()).isEqualTo("아기사자");
		assertThat(res.getTeamName()).isEqualTo("1팀");
		verify(attendanceRepository).save(a);
	}

	@Test
	void 결석에서_출석으로_바꾸면_현재시각을_채운다() {
		Attendance a = attendance(AttendanceStatus.ABSENT, null);
		when(attendanceRepository.findById(10L)).thenReturn(Optional.of(a));

		service.updateAttendanceStatus(10L, AttendanceStatus.PRESENT, admin);

		assertThat(a.getAttendanceTime()).isNotNull();
	}

	@Test
	void 지각에서_출석으로_바꾸면_원래_스캔시각을_유지한다() {
		LocalDateTime scanned = LocalDateTime.of(2026, 9, 22, 19, 25);
		Attendance a = attendance(AttendanceStatus.LATE, scanned);
		when(attendanceRepository.findById(10L)).thenReturn(Optional.of(a));

		service.updateAttendanceStatus(10L, AttendanceStatus.PRESENT, admin);

		assertThat(a.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
		assertThat(a.getAttendanceTime()).isEqualTo(scanned);
	}

	@Test
	void 없는_출석행은_ATTENDANCE_NOT_FOUND() {
		when(attendanceRepository.findById(99L)).thenReturn(Optional.empty());

		assertErrorCode(() -> service.updateAttendanceStatus(99L, AttendanceStatus.LATE, admin),
			ErrorCode.ATTENDANCE_NOT_FOUND);
	}

	@Test
	void 상태가_비어있으면_INVALID_ARGUMENT() {
		assertErrorCode(() -> service.updateAttendanceStatus(10L, null, admin), ErrorCode.INVALID_ARGUMENT);
		verifyNoInteractions(attendanceRepository);
	}

	// ---------- P4: 1회성 QR ----------

	@Test
	void QR_재발급하면_토큰이_바뀌고_만료는_30분뒤() {
		AttendanceSession session = AttendanceSession.builder().id(1L).sessionDate(LocalDateTime.now()).build();
		when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		service.issueQrToken(session);
		String first = session.getQrToken();
		service.issueQrToken(session);

		assertThat(first).hasSize(32);
		assertThat(session.getQrToken()).hasSize(32).isNotEqualTo(first);
		assertThat(session.getQrExpiresAt())
			.isAfter(LocalDateTime.now().plusMinutes(29))
			.isBefore(LocalDateTime.now().plusMinutes(31));
	}

	@Test
	void 일치하고_만료전인_토큰으로_출석() {
		AttendanceSession session = sessionWithToken("a".repeat(32), LocalDateTime.now().plusMinutes(10));
		Attendance a = attendance(AttendanceStatus.ABSENT, null);
		when(sessionRepository.findTopBySessionDateAfter(any())).thenReturn(Optional.of(session));
		when(attendanceRepository.findByUserAndAttendanceSession(member, session)).thenReturn(Optional.of(a));

		String message = service.markAttendance(member, "a".repeat(32));

		assertThat(message).contains("출석 완료");
		assertThat(a.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
	}

	@Test
	void 재발급_이전_토큰은_거부() {
		AttendanceSession session = sessionWithToken("new".repeat(10) + "xx", LocalDateTime.now().plusMinutes(10));
		when(sessionRepository.findTopBySessionDateAfter(any())).thenReturn(Optional.of(session));

		assertErrorCode(() -> service.markAttendance(member, "old".repeat(10) + "xx"), ErrorCode.INVALID_QR);
		verify(attendanceRepository, never()).save(any());
	}

	@Test
	void 만료된_토큰은_거부() {
		AttendanceSession session = sessionWithToken("a".repeat(32), LocalDateTime.now().minusSeconds(1));
		when(sessionRepository.findTopBySessionDateAfter(any())).thenReturn(Optional.of(session));

		assertErrorCode(() -> service.markAttendance(member, "a".repeat(32)), ErrorCode.INVALID_QR);
	}

	@Test
	void 토큰_없이_보내면_거부() {
		AttendanceSession session = sessionWithToken("a".repeat(32), LocalDateTime.now().plusMinutes(10));
		when(sessionRepository.findTopBySessionDateAfter(any())).thenReturn(Optional.of(session));

		assertErrorCode(() -> service.markAttendance(member, null), ErrorCode.INVALID_QR);
	}

	@Test
	void 오늘_세션이_없으면_거부하고_빈_세션을_만들지_않는다() {
		when(sessionRepository.findTopBySessionDateAfter(any())).thenReturn(Optional.empty());

		assertErrorCode(() -> service.markAttendance(member, "a".repeat(32)), ErrorCode.INVALID_QR);
		verify(sessionRepository, never()).save(any());
	}

	@Test
	void 구버전_FE의_qrData에서_토큰을_꺼낸다() {
		MarkAttendanceRequest legacy = new MarkAttendanceRequest();
		legacy.setQrData("https://welcomekitbe.lion.it.kr/check?token=abc123");
		assertThat(legacy.resolveToken()).isEqualTo("abc123");

		MarkAttendanceRequest fixedUrl = new MarkAttendanceRequest();
		fixedUrl.setQrData("https://welcomekitbe.lion.it.kr/api/attendance/success");
		assertThat(fixedUrl.resolveToken()).isNull();

		MarkAttendanceRequest both = new MarkAttendanceRequest();
		both.setToken("direct");
		both.setQrData("https://x/check?token=ignored");
		assertThat(both.resolveToken()).isEqualTo("direct");
	}

	// ---------- P3: 통계 ----------

	@Test
	void 출석률은_지각을_절반으로_계산하고_기록없으면_null() {
		assertThat(AttendanceSessionService.attendanceRate(3, 1, 0)).isEqualTo(0.875);
		assertThat(AttendanceSessionService.attendanceRate(1, 0, 2)).isEqualTo(0.333);
		assertThat(AttendanceSessionService.attendanceRate(0, 0, 0)).isNull();
	}

	@Test
	void 부원별_누적은_상태별로_합산하고_팀_미배정은_뒤로() {
		User noTeam = User.builder().id(3L).userName("가나다").devPart(DevPart.BACK_END).build();
		when(attendanceRepository.countByUserAndStatus()).thenReturn(List.of(
			new AttendanceStatusCount(2L, AttendanceStatus.PRESENT, 2L),
			new AttendanceStatusCount(2L, AttendanceStatus.ABSENT, 2L),
			new AttendanceStatusCount(3L, AttendanceStatus.LATE, 1L)));

		List<MemberAttendanceSummaryResponse> result = service.getMemberSummaries(List.of(noTeam, member));

		assertThat(result).extracting(MemberAttendanceSummaryResponse::getUserName).containsExactly("아기사자", "가나다");
		MemberAttendanceSummaryResponse m = result.get(0);
		assertThat(m.getPresent()).isEqualTo(2);
		assertThat(m.getLate()).isZero();
		assertThat(m.getAbsent()).isEqualTo(2);
		assertThat(m.getRate()).isEqualTo(0.5);
		assertThat(result.get(1).getRate()).isEqualTo(0.5);
	}

	private Attendance attendance(AttendanceStatus status, LocalDateTime time) {
		return Attendance.builder().id(10L).user(member).status(status).attendanceTime(time).build();
	}

	private AttendanceSession sessionWithToken(String token, LocalDateTime expiresAt) {
		return AttendanceSession.builder().id(1L).sessionDate(LocalDateTime.now().minusMinutes(1))
			.qrToken(token).qrExpiresAt(expiresAt).build();
	}

	private void assertErrorCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, ErrorCode code) {
		assertThatThrownBy(call).isInstanceOf(CustomException.class)
			.extracting(e -> ((CustomException)e).getErrorCode()).isEqualTo(code);
	}
}
