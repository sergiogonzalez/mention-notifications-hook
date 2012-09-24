/**
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.sympo.sanitizer;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.sanitizer.Sanitizer;
import com.liferay.portal.kernel.sanitizer.SanitizerException;
import com.liferay.portal.kernel.util.StreamUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portlet.expando.model.ExpandoTableConstants;
import com.liferay.portlet.expando.service.ExpandoValueLocalServiceUtil;
import com.liferay.portlet.messageboards.model.MBMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.lang.String;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Sergio González
 */
public class MentionSanitizerImpl implements Sanitizer {

	public byte[] sanitize(
		long companyId, long groupId, long userId, String className,
		long classPK, String contentType, String[] modes, byte[] bytes,
		Map<String, Object> options) {

		if (_log.isDebugEnabled()) {
			_log.debug("Sanitizing " + className + "#" + classPK);
		}

		return bytes;
	}

	public void sanitize(
			long companyId, long groupId, long userId, String className,
			long classPK, String contentType, String[] modes,
			InputStream inputStream, OutputStream outputStream,
			Map<String, Object> options)
		throws SanitizerException {

		if (_log.isDebugEnabled()) {
			_log.debug("Sanitizing " + className + "#" + classPK);
		}

		try {
			StreamUtil.transfer(inputStream, outputStream);
		}
		catch (IOException ioe) {
			throw new SanitizerException(ioe);
		}
	}

	public String sanitize(
		long companyId, long groupId, long userId, String className,
		long classPK, String contentType, String[] modes, String s,
		Map<String, Object> options) {

		if (_log.isDebugEnabled()) {
			_log.debug("Sanitizing " + className + "#" + classPK);
		}

		if (className.equals(MBMessage.class.getName()) &&
				(contentType.equals("text/bbcode") ||
					contentType.equals("text/html"))) {

			Matcher matcher = _pattern.matcher(s);

			Set<String> mentionedUsers = new HashSet<String>();

			while (matcher.find()) {
				String screenName = matcher.group(1);

				try {
					User user = UserLocalServiceUtil.getUserByScreenName(
						companyId, screenName);

					mentionedUsers.add(screenName);
				}
				catch (Exception e) {
				}
			}

			String[] mentionedUsersArray = (String[])mentionedUsers.toArray(
				new String[mentionedUsers.size()]);

			String mentionedUsersString = StringUtil.merge(mentionedUsersArray);

			try {
				ExpandoValueLocalServiceUtil.addValue(
					companyId, MBMessage.class.getName(),
					ExpandoTableConstants.DEFAULT_TABLE_NAME, "mentionedUsers",
					classPK, mentionedUsersString);
			}
			catch (Exception e) {
			}
		}

		return s;
	}

	private static Log _log = LogFactoryUtil.getLog(MentionSanitizerImpl.class);

	private static Pattern _pattern = Pattern.compile(
		"(?:\\s|^)@([^@\\s]+)", Pattern.CASE_INSENSITIVE);

}