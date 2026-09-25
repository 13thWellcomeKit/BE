package com.likelion13th.Welcomekit_BE.service;

import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.likelion13th.Welcomekit_BE.domain.Attendance;
import com.likelion13th.Welcomekit_BE.domain.AttendanceSession;
import com.likelion13th.Welcomekit_BE.domain.User;
import com.likelion13th.Welcomekit_BE.domain.dto.AttendanceStatusCount;
import com.likelion13th.Welcomekit_BE.domain.dto.response.AttendanceSessionSummaryResponse;
import com.likelion13th.Welcomekit_BE.domain.dto.response.GetTodayAttendanceResponse;
import com.likelion13th.Welcomekit_BE.domain.dto.response.MemberAttendanceSummaryResponse;
import com.likelion13th.Welcomekit_BE.domain.dto.response.MyAttendanceResponse;
import com.likelion13th.Welcomekit_BE.domain.enums.AttendanceStatus;
import com.likelion13th.Welcomekit_BE.exception.CustomException;
import com.likelion13th.Welcomekit_BE.exception.ErrorCode;
import com.likelion13th.Welcomekit_BE.repository.AttendanceRepository;
import com.likelion13th.Welcomekit_BE.repository.AttendanceSessionRepository;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceSessionService {
	// 세션별 설정화는 P5(다음 기수) 범위. 그 전까지는 상수.
	public static final int LATE_AFTER_MINUTES = 20;
	public static final int QR_VALID_MINUTES = 30;
	public static final double LATE_WEIGHT = 0.5;

	@Autowired
	private final AttendanceSessionRepository attendanceSessionRepository;
	@Autowired
	private final AttendanceRepository attendanceRepository;

	// QR에 담을 FE 주소. BE가 React 빌드도 서빙하므로(FrontendController) 기본값은 BE 도메인.
	@Value("${app.frontend-url:https://welcomekitbe.lion.it.kr}")
	private String frontendUrl;

	// 매주 새로운 출석 세션을 생성하는 메서드
	public AttendanceSession createNewSession() {
		AttendanceSession session = AttendanceSession.builder()
			.sessionDate(LocalDateTime.now()) // 현재 날짜로 출석 세션 생성
			.build();
		return attendanceSessionRepository.save(session);
	}

	public AttendanceSession createNewSession(List<User> totalBabyLion) {
		AttendanceSession session = AttendanceSession.builder()
			.sessionDate(LocalDateTime.now()) // 현재 날짜로 출석 세션 생성
			.build();
		AttendanceSession save = attendanceSessionRepository.save(session);
		totalBabyLion.forEach(babyLion -> {
			Attendance attendance = new Attendance();
			attendance.setAttendanceSession(save);
			attendance.setUser(babyLion);
			attendance.setStatus(AttendanceStatus.ABSENT);
			attendanceRepository.save(attendance);
		});
		return save;
	}

	// 오늘 생성된 출석 세션이 있는지 확인
	public AttendanceSession getTodaySession() {
		return attendanceSessionRepository.findTopBySessionDateAfter(LocalDateTime.now().toLocalDate().atStartOfDay())
			.orElseGet(this::createNewSession); // 없으면 새로 생성
	}

	// 오늘 생성된 출석 세션이 있는지 확인
	public AttendanceSession getTodaySession(List<User> totalBabyLion) {
		return attendanceSessionRepository.findTopBySessionDateAfter(LocalDateTime.now().toLocalDate().atStartOfDay())
			.orElseGet(() -> createNewSession(totalBabyLion));
	}

	// 호출할 때마다 새 토큰을 발급한다. 이전에 띄운 QR은 이 시점부터 무효.
	public AttendanceSession issueQrToken(AttendanceSession session) {
		session.setQrToken(UUID.randomUUID().toString().replace("-", ""));
		session.setQrExpiresAt(LocalDateTime.now().plusMinutes(QR_VALID_MINUTES));
		return attendanceSessionRepository.save(session);
	}

	public void generateQR(AttendanceSession session, HttpServletResponse response) {
		String qrUrl = frontendUrl + "/check?token=" + session.getQrToken();

		int width = 300;
		int height = 300;

		try {
			BitMatrix bitMatrix = new MultiFormatWriter().encode(qrUrl, BarcodeFormat.QR_CODE, width, height);
			BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

			// 절대 시각 대신 남은 초를 준다. 클라이언트 시계가 틀려도 카운트다운이 맞도록.
			long validSeconds = Math.max(0,
				Duration.between(LocalDateTime.now(), session.getQrExpiresAt()).getSeconds());
			response.setHeader("X-QR-Valid-Seconds", String.valueOf(validSeconds));
			response.setContentType("image/png");
			OutputStream outputStream = response.getOutputStream();
			ImageIO.write(qrImage, "png", outputStream);

			outputStream.flush();
			outputStream.close();

		} catch (Exception e) {
			throw new RuntimeException("QR 코드 생성 중 오류 발생", e);
		}
	}

	public String markAttendance(User user, String token) {
		// 오늘 세션이 없으면 유효한 QR도 있을 수 없다. 예전처럼 빈 세션을 만들지 않는다.
		AttendanceSession session = attendanceSessionRepository.findTopBySessionDateAfter(
				LocalDateTime.now().toLocalDate().atStartOfDay())
			.orElseThrow(() -> new CustomException(ErrorCode.INVALID_QR));
		validateQrToken(session, token, LocalDateTime.now());

		Optional<Attendance> existingAttendance = attendanceRepository.findByUserAndAttendanceSession(user, session);
		if (existingAttendance.isPresent()) {
			if (existingAttendance.get().getStatus() == AttendanceStatus.PRESENT) {
				return "이미 출석한 기록이 있습니다.";
			}
		}

		// 출석 상태 결정 (지각 여부 판단 가능)
		AttendanceStatus status =
			LocalDateTime.now().isBefore(session.getSessionDate().plusMinutes(LATE_AFTER_MINUTES))
				? AttendanceStatus.PRESENT
				: AttendanceStatus.LATE;

		Attendance attendance = existingAttendance
			.orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));
		attendance.setAttendanceTime(LocalDateTime.now());
		attendance.setStatus(status);

		attendanceRepository.save(attendance);
		return user.getUserName() + "님, " + (status == AttendanceStatus.PRESENT ? "출석 완료" : "지각 처리되었습니다.");
	}

	void validateQrToken(AttendanceSession session, String token, LocalDateTime now) {
		String expected = session.getQrToken();
		boolean matches = token != null && expected != null
			&& MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
			token.getBytes(StandardCharsets.UTF_8));
		boolean expired = session.getQrExpiresAt() == null || !now.isBefore(session.getQrExpiresAt());
		if (!matches || expired) {
			log.error("유효하지 않은 QR 토큰 (matches={}, expired={})", matches, expired);
			throw new CustomException(ErrorCode.INVALID_QR);
		}
	}

	public List<MyAttendanceResponse> getMyAttendance(User user) {
		List<Attendance> myAttendances = attendanceRepository.findAllByUserOrderByAttendanceTime(user);
		List<MyAttendanceResponse> list = myAttendances.stream().map(myAttendance -> {
			MyAttendanceResponse myAttendanceResponse = new MyAttendanceResponse();
			myAttendanceResponse.setAttendanceStatus(myAttendance.getStatus());
			myAttendanceResponse.setDate(myAttendance.getAttendanceSession().getSessionDate().toLocalDate());
			myAttendanceResponse.setAttendanceTime(myAttendance.getAttendanceTime());
			return myAttendanceResponse;
		}).toList();
		return list;
	}

	public List<GetTodayAttendanceResponse> getTodayAttendance(User user) {
		List<GetTodayAttendanceResponse> attendanceResponses =
			attendanceRepository.findTodayAttendance(LocalDateTime.now().toLocalDate().atStartOfDay());
		if (attendanceResponses.isEmpty()) {
			log.error("세션이 없습니다.");
			throw new CustomException(ErrorCode.SESSION_NOT_FOUND);
		}
		return attendanceResponses;
	}

	public GetTodayAttendanceResponse updateAttendanceStatus(Long attendanceId, AttendanceStatus status, User admin) {
		if (status == null) {
			throw new CustomException(ErrorCode.INVALID_ARGUMENT);
		}
		Attendance attendance = attendanceRepository.findById(attendanceId)
			.orElseThrow(() -> new CustomException(ErrorCode.ATTENDANCE_NOT_FOUND));

		LocalDateTime now = LocalDateTime.now();
		attendance.setStatus(status);
		if (status == AttendanceStatus.ABSENT) {
			attendance.setAttendanceTime(null);
		} else if (attendance.getAttendanceTime() == null) {
			// 실제 스캔 시각이 있으면 그대로 둔다(지각→출석 정정 시 원래 도착 시각 보존)
			attendance.setAttendanceTime(now);
		}
		attendance.setModifiedBy(admin.getStudentNum());
		attendance.setModifiedAt(now);
		attendanceRepository.save(attendance);

		User member = attendance.getUser();
		return new GetTodayAttendanceResponse(
			attendance.getId(),
			member.getStudentNum(),
			member.getTeam() != null ? member.getTeam().getTeamName() : null,
			member.getUserName(),
			attendance.getStatus(),
			attendance.getAttendanceTime());
	}

	public List<AttendanceSessionSummaryResponse> getSessionSummaries() {
		Map<Long, Map<AttendanceStatus, Long>> counts = groupCounts(attendanceRepository.countBySessionAndStatus());
		return attendanceSessionRepository.findAll(Sort.by(Sort.Direction.DESC, "sessionDate")).stream()
			.map(session -> {
				Map<AttendanceStatus, Long> c = counts.getOrDefault(session.getId(), Map.of());
				return new AttendanceSessionSummaryResponse(
					session.getId(),
					session.getSessionDate(),
					c.getOrDefault(AttendanceStatus.PRESENT, 0L),
					c.getOrDefault(AttendanceStatus.LATE, 0L),
					c.getOrDefault(AttendanceStatus.ABSENT, 0L));
			})
			.toList();
	}

	public List<GetTodayAttendanceResponse> getSessionAttendance(Long sessionId) {
		if (!attendanceSessionRepository.existsById(sessionId)) {
			throw new CustomException(ErrorCode.SESSION_NOT_FOUND);
		}
		return attendanceRepository.findSessionAttendance(sessionId);
	}

	// 분모는 전체 세션 수가 아니라 그 부원에게 출석 행이 만들어진 세션 수.
	// 세션 생성 시점에 가입해 있던 부원에게만 행이 생기므로, 늦게 가입한 부원이 이전 세션 때문에 결석 처리되지 않는다.
	public List<MemberAttendanceSummaryResponse> getMemberSummaries(List<User> babyLions) {
		Map<Long, Map<AttendanceStatus, Long>> counts = groupCounts(attendanceRepository.countByUserAndStatus());
		return babyLions.stream()
			.map(user -> {
				Map<AttendanceStatus, Long> c = counts.getOrDefault(user.getId(), Map.of());
				long present = c.getOrDefault(AttendanceStatus.PRESENT, 0L);
				long late = c.getOrDefault(AttendanceStatus.LATE, 0L);
				long absent = c.getOrDefault(AttendanceStatus.ABSENT, 0L);
				return new MemberAttendanceSummaryResponse(
					user.getId(),
					user.getUserName(),
					user.getTeam() != null ? user.getTeam().getTeamName() : null,
					user.getDevPart(),
					present, late, absent,
					attendanceRate(present, late, absent));
			})
			.sorted(Comparator.comparing(MemberAttendanceSummaryResponse::getTeamName,
					Comparator.nullsLast(Comparator.<String>naturalOrder()))
				.thenComparing(MemberAttendanceSummaryResponse::getUserName))
			.toList();
	}

	static Double attendanceRate(long present, long late, long absent) {
		long total = present + late + absent;
		if (total == 0) {
			return null;
		}
		double rate = (present + late * LATE_WEIGHT) / total;
		return Math.round(rate * 1000) / 1000.0;
	}

	private static Map<Long, Map<AttendanceStatus, Long>> groupCounts(List<AttendanceStatusCount> rows) {
		Map<Long, Map<AttendanceStatus, Long>> result = new HashMap<>();
		for (AttendanceStatusCount row : rows) {
			result.computeIfAbsent(row.groupId(), id -> new EnumMap<>(AttendanceStatus.class))
				.merge(row.status(), row.count(), Long::sum);
		}
		return result;
	}
}
