package com.likelion13th.Welcomekit_BE.domain.dto.request;

import org.springframework.web.util.UriComponentsBuilder;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MarkAttendanceRequest {
	private String token;
	// 구버전 FE(PWA 캐시 포함)는 스캔한 URL 전체를 qrData로 보낸다. 그 안의 token 쿼리를 대신 쓴다.
	private String qrData;

	public String resolveToken() {
		if (token != null && !token.isBlank()) {
			return token;
		}
		if (qrData == null || qrData.isBlank()) {
			return null;
		}
		try {
			return UriComponentsBuilder.fromUriString(qrData).build().getQueryParams().getFirst("token");
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
