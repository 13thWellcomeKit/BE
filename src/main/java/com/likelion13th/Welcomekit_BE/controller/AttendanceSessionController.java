package com.likelion13th.Welcomekit_BE.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.likelion13th.Welcomekit_BE.domain.dto.request.MarkAttendanceRequest;
import com.likelion13th.Welcomekit_BE.domain.dto.request.UpdateAttendanceStatusRequest;
import com.likelion13th.Welcomekit_BE.domain.dto.response.MyAttendanceResponse;
import com.likelion13th.Welcomekit_BE.manager.AttendanceSessionManager;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/attendance")
public class AttendanceSessionController {

	@Autowired
	private final AttendanceSessionManager attendanceSessionManager;

	@Operation(summary = "출석 QR 생성 (ADMIN)", description = "호출마다 1회성 토큰을 새로 발급한다. 남은 유효 시간은 X-QR-Valid-Seconds 헤더.")
	@GetMapping("generate-qr")
	void generateQRCode(@AuthenticationPrincipal UserDetails userDetails, HttpServletResponse response) {
		attendanceSessionManager.generateQR(userDetails.getUsername(), response);
	}

	@Operation(summary = "QR 출석 처리", description = "body { token }. 오늘 세션의 유효한 토큰이 아니면 INVALID_QR.")
	@PostMapping("/success")
	public ResponseEntity<String> qrSuccess(
		@AuthenticationPrincipal UserDetails userDetails,
		@RequestBody(required = false) MarkAttendanceRequest request
	) {
		String token = request != null ? request.resolveToken() : null;
		return ResponseEntity.ok(attendanceSessionManager.markAttendance(userDetails.getUsername(), token));
	}

	@Operation(summary = "내 출석 이력")
	@GetMapping("/my-attendance")
	public ResponseEntity<?> getMyAttendance(
		@AuthenticationPrincipal UserDetails userDetails) {
		List<MyAttendanceResponse> myAttendance = attendanceSessionManager.getMyAttendance(userDetails.getUsername());
		return ResponseEntity.ok(myAttendance);
	}

	@Operation(summary = "오늘 출석부")
	@GetMapping("/today/attendance")
	public ResponseEntity<?> getTodayAttendance(@AuthenticationPrincipal UserDetails userDetails) {
		return ResponseEntity.ok(attendanceSessionManager.getTodayAttendance(userDetails.getUsername()));
	}

	@Operation(summary = "출석 상태 수정 (ADMIN)", description = "body { status: PRESENT | LATE | ABSENT }")
	@PatchMapping("/{attendanceId}")
	public ResponseEntity<?> updateAttendanceStatus(
		@AuthenticationPrincipal UserDetails userDetails,
		@PathVariable Long attendanceId,
		@RequestBody UpdateAttendanceStatusRequest request
	) {
		return ResponseEntity.ok(attendanceSessionManager.updateAttendanceStatus(
			userDetails.getUsername(), attendanceId, request.getStatus()));
	}

	@Operation(summary = "세션 목록과 상태별 인원 (ADMIN)")
	@GetMapping("/sessions")
	public ResponseEntity<?> getSessions(@AuthenticationPrincipal UserDetails userDetails) {
		return ResponseEntity.ok(attendanceSessionManager.getSessionSummaries(userDetails.getUsername()));
	}

	@Operation(summary = "세션 출석부 (ADMIN)")
	@GetMapping("/sessions/{sessionId}")
	public ResponseEntity<?> getSessionAttendance(
		@AuthenticationPrincipal UserDetails userDetails,
		@PathVariable Long sessionId
	) {
		return ResponseEntity.ok(attendanceSessionManager.getSessionAttendance(userDetails.getUsername(), sessionId));
	}

	@Operation(summary = "부원별 누적 출석 통계 (ADMIN)", description = "rate = (출석 + 지각×0.5) / 해당 부원의 기록된 세션 수")
	@GetMapping("/summary")
	public ResponseEntity<?> getSummary(@AuthenticationPrincipal UserDetails userDetails) {
		return ResponseEntity.ok(attendanceSessionManager.getMemberSummaries(userDetails.getUsername()));
	}
}
