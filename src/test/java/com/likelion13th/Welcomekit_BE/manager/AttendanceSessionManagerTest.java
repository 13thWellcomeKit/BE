package com.likelion13th.Welcomekit_BE.manager;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.likelion13th.Welcomekit_BE.domain.User;
import com.likelion13th.Welcomekit_BE.domain.enums.AttendanceStatus;
import com.likelion13th.Welcomekit_BE.domain.enums.UserType;
import com.likelion13th.Welcomekit_BE.exception.CustomException;
import com.likelion13th.Welcomekit_BE.service.AttendanceSessionService;
import com.likelion13th.Welcomekit_BE.service.UserService;

@ExtendWith(MockitoExtension.class)
class AttendanceSessionManagerTest {

	@Mock
	AttendanceSessionService attendanceSessionService;
	@Mock
	UserService userService;
	@InjectMocks
	AttendanceSessionManager manager;

	@Test
	void 아기사자는_ADMIN_엔드포인트에서_403() {
		when(userService.getUserByStudentNum("200"))
			.thenReturn(User.builder().studentNum("200").userType(UserType.BABY_LION).build());

		assertForbidden(() -> manager.updateAttendanceStatus("200", 1L, AttendanceStatus.PRESENT));
		assertForbidden(() -> manager.getSessionSummaries("200"));
		assertForbidden(() -> manager.getSessionAttendance("200", 1L));
		assertForbidden(() -> manager.getMemberSummaries("200"));
		assertForbidden(() -> manager.generateQR("200", null));
		verifyNoInteractions(attendanceSessionService);
	}

	@Test
	void 운영진은_수정할_수_있다() {
		User admin = User.builder().studentNum("100").userType(UserType.ADMIN).build();
		when(userService.getUserByStudentNum("100")).thenReturn(admin);

		manager.updateAttendanceStatus("100", 1L, AttendanceStatus.LATE);

		verify(attendanceSessionService).updateAttendanceStatus(eq(1L), eq(AttendanceStatus.LATE), same(admin));
	}

	private void assertForbidden(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
		assertThatThrownBy(call).isInstanceOf(CustomException.class)
			.extracting(e -> ((CustomException)e).getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
	}
}
