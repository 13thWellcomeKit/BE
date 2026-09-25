package com.likelion13th.Welcomekit_BE.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public enum ErrorCode {
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User does not exist"),
	INVALID_ARGUMENT(HttpStatus.BAD_REQUEST, "Invalid argument provided"),
	NULL_POINTER(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected null value encountered"),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"),
	CUSTOM_ERROR(HttpStatus.BAD_REQUEST, "This is a custom error"),
	PERMISSION_ERROR(HttpStatus.FORBIDDEN, "Permission error"),
	SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "Session does not exist"),
	ATTENDANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Attendance does not exist"),
	INVALID_QR(HttpStatus.BAD_REQUEST, "QR code is invalid or expired"),
	TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "Team does not exist"),
	CELL_NOT_FOUND(HttpStatus.NOT_FOUND, "Cell does not exist"),
	PASSWORD_NOT_MATCHES(HttpStatus.BAD_REQUEST, "Password is not correct"),
	PROFILE_UPDATE_ERROR(HttpStatus.BAD_REQUEST, "Profile update Error"),
	INVALID_IMAGE_FORMAT(HttpStatus.BAD_REQUEST, "INVALID IMAGE FORMAT"),
	IMAGE_NOT_FOUND(HttpStatus.BAD_REQUEST, "IMAGE NOT FOUND"),
	UNAUTHORIZED_USER(HttpStatus.UNAUTHORIZED, "User authentication is required."); // ✅ 추가

	private final HttpStatus httpStatus;
	private final String message;

	ErrorCode(HttpStatus httpStatus, String message) {
		this.httpStatus = httpStatus;
		this.message = message;
	}
}