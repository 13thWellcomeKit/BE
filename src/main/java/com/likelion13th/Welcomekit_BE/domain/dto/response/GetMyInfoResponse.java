package com.likelion13th.Welcomekit_BE.domain.dto.response;

import com.likelion13th.Welcomekit_BE.domain.enums.DevPart;
import com.likelion13th.Welcomekit_BE.domain.enums.UserType;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GetMyInfoResponse {
	private String name;
	private String studentName;
	private String teamName;
	private DevPart devPart;
	private String profileImage;
	private UserType userType;
}
