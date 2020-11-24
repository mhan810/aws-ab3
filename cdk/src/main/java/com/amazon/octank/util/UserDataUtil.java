package com.amazon.octank.util;

import software.amazon.awscdk.services.ec2.UserData;

import java.util.Map;
import java.util.Scanner;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Michael C. Han (mhnmz)
 */
public class UserDataUtil {

	public static UserData createUserData(String shellScriptFile) {
		return createUserData(shellScriptFile, null);
	}

	public static UserData createUserData(String shellScriptFile, Map<String, String> replacements) {
		Scanner scanner = new Scanner(UserDataUtil.class.getResourceAsStream(shellScriptFile)).useDelimiter("\\A");

		String userDataString = scanner.hasNext() ? scanner.next() : "";

		if (replacements != null) {
			for (Map.Entry<String, String> entry : replacements.entrySet()) {
				Pattern tokenPattern = Pattern.compile(entry.getKey());

				userDataString = replaceTokens(userDataString, tokenPattern,
					matcher -> replacements.get(entry.getKey()));
			}
		}

		return UserData.custom(userDataString);
	}

	public static String replaceTokens(
		final String original, final Pattern tokenPattern, final Function<Matcher, String> converter) {

		int lastIndex = 0;

		StringBuilder output = new StringBuilder();

		Matcher matcher = tokenPattern.matcher(original);

		while (matcher.find()) {
			output.append(original, lastIndex, matcher.start());
			output.append(converter.apply(matcher));

			lastIndex = matcher.end();
		}

		if (lastIndex < original.length()) {
			output.append(original, lastIndex, original.length());
		}

		return output.toString();
	}


}
