package com.amazon.octank.util;

import software.amazon.awscdk.services.ec2.UserData;

import java.util.Scanner;

/**
 * @author Michael C. Han (mhnmz)
 */
public class UserDataUtil {

	public static UserData createUserData(String shellScriptFile) {
		Scanner scanner = new Scanner(UserDataUtil.class.getResourceAsStream(shellScriptFile)).useDelimiter("\\A");

		String userDataString = scanner.hasNext() ? scanner.next() : "";

		return UserData.custom(userDataString);
	}

}
