package com.likelion13th.Welcomekit_BE.manager;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.likelion13th.Welcomekit_BE.domain.AttendanceSession;
import com.likelion13th.Welcomekit_BE.domain.User;
import com.likelion13th.Welcomekit_BE.domain.dto.response.AttendanceSessionSummaryResponse;
import com.likelion13th.Welcomekit_BE.domain.dto.response.GetTodayAttendanceResponse;
import com.likelion13th.Welcomekit_BE.domain.dto.response.MemberAttendanceSummaryResponse;
import com.likelion13th.Welcomekit_BE.domain.dto.response.MyAttendanceResponse;
import com.likelion13th.Welcomekit_BE.domain.enums.AttendanceStatus;
import com.likelion13th.Welcomekit_BE.domain.enums.UserType;
import com.likelion13th.Welcomekit_BE.exception.CustomException;
import com.likelion13th.Welcomekit_BE.exception.ErrorCode;
import com.likelion13th.Welcomekit_BE.service.AttendanceSessionService;
import com.likelion13th.Welcomekit_BE.service.UserService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceSessionManager {

	@Autowired
	private final AttendanceSessionService attendanceSessionService;
	@Autowired
	private final UserService userService;

	public void generateQR(String studentNum, HttpServletResponse response) {
		requireAdmin(studentNum, "QR 생성");
		List<User> totalBabyLion = userService.getTotalBabyLionUser();
		AttendanceSession session = attendanceSessionService.getTodaySession(totalBabyLion);
		attendanceSessionService.generateQR(attendanceSessionService.issueQrToken(session), response);
	}

	public String markAttendance(String studentNum, String token) {
		User user = userService.getUserByStudentNum(studentNum);
		return attendanceSessionService.markAttendance(user, token);
	}

	public List<MyAttendanceResponse> getMyAttendance(String studentNum) {
		User user = userService.getUserByStudentNum(studentNum);
		return attendanceSessionService.getMyAttendance(user);
	}

	public List<GetTodayAttendanceResponse> getTodayAttendance(String studentNum) {
		User user = userService.getUserByStudentNum(studentNum);
		return attendanceSessionService.getTodayAttendance(user);
	}

	public GetTodayAttendanceResponse updateAttendanceStatus(String studentNum, Long attendanceId,
		AttendanceStatus status) {
		User admin = requireAdmin(studentNum, "출석 상태 수정");
		return attendanceSessionService.updateAttendanceStatus(attendanceId, status, admin);
	}

	public List<AttendanceSessionSummaryResponse> getSessionSummaries(String studentNum) {
		requireAdmin(studentNum, "세션 목록 조회");
		return attendanceSessionService.getSessionSummaries();
	}

	public List<GetTodayAttendanceResponse> getSessionAttendance(String studentNum, Long sessionId) {
		requireAdmin(studentNum, "세션 출석부 조회");
		return attendanceSessionService.getSessionAttendance(sessionId);
	}

	public List<MemberAttendanceSummaryResponse> getMemberSummaries(String studentNum) {
		requireAdmin(studentNum, "출석 통계 조회");
		return attendanceSessionService.getMemberSummaries(userService.getTotalBabyLionUser());
	}

	private User requireAdmin(String studentNum, String action) {
		User user = userService.getUserByStudentNum(studentNum);
		if (user.getUserType() != UserType.ADMIN) {
			log.error("{}할때 permission error", action);
			throw new CustomException(ErrorCode.PERMISSION_ERROR);
		}
		return user;
	}
}
