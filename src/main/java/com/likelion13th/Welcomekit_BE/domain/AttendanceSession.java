package com.likelion13th.Welcomekit_BE.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity(name = "attendance_session")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AttendanceSession {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name = "session_date", nullable = false)
	private LocalDateTime sessionDate; // 출석 체크 날짜

	@Column(name = "qr_token", length = 32)
	private String qrToken; // generate-qr 호출마다 재발급, 이전 토큰은 무효

	@Column(name = "qr_expires_at")
	private LocalDateTime qrExpiresAt;

	@OneToMany(mappedBy = "attendanceSession", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Attendance> attendanceList; // 출석한 유저 목록
}
