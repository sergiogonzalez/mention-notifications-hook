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

package com.sympo.portlet.messageboards.service;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PrefsPropsUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.User;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.SubscriptionSender;
import com.liferay.portlet.expando.model.ExpandoBridge;
import com.liferay.portlet.messageboards.model.MBMessage;
import com.liferay.portlet.messageboards.service.MBMessageLocalService;
import com.liferay.portlet.messageboards.service.MBMessageLocalServiceWrapper;
import com.liferay.util.ContentUtil;

/**
 * @author Sergio González
 */
public class MentionUsersMessageServiceImpl
	extends MBMessageLocalServiceWrapper {

	public MentionUsersMessageServiceImpl(
		MBMessageLocalService mbMessageLocalService) {

		super(mbMessageLocalService);
	}

	public MBMessage addDiscussionMessage(
			long userId, String userName, long groupId, String className,
			long classPK, long threadId, long parentMessageId, String subject,
			String body, ServiceContext serviceContext)
		throws PortalException, SystemException {

		MBMessage message = super.addDiscussionMessage(
			userId, userName, groupId, className, classPK, threadId,
			parentMessageId, subject, body, serviceContext);

		return notifyMentions(message, serviceContext);
	}

	public MBMessage updateDiscussionMessage(
			long userId, long messageId, String className, long classPK,
			String subject, String body, ServiceContext serviceContext)
		throws PortalException, SystemException {

		MBMessage message = super.updateDiscussionMessage(
			userId, messageId, className, classPK, subject, body,
			serviceContext);

		return notifyMentions(message, serviceContext);
	}

	private MBMessage notifyMentions(
			MBMessage message, ServiceContext serviceContext)
		throws PortalException, SystemException {

		if (!message.isDiscussion()) {
			return message;
		}

		ExpandoBridge expandoBridge = message.getExpandoBridge();

		String mentionedUsers = GetterUtil.getString(
			expandoBridge.getAttribute("mentionedUsers"));

		if (Validator.isNull(mentionedUsers)) {
			return message;
		}

		long companyId = message.getCompanyId();

		String contentURL = (String)serviceContext.getAttribute("contentURL");

		String mailUserAddress = PortalUtil.getUserEmailAddress(
			message.getUserId());
		String mailUserName = PortalUtil.getUserName(
			message.getUserId(), StringPool.BLANK);

		String fromName = PrefsPropsUtil.getString(
			message.getCompanyId(), PropsKeys.ADMIN_EMAIL_FROM_NAME);
		String fromAddress = PrefsPropsUtil.getString(
			message.getCompanyId(), PropsKeys.ADMIN_EMAIL_FROM_ADDRESS);

		String mailSubject = ContentUtil.get(PropsUtil.get(
			"discussion.mentioned.email.subject"));
		String mailBody = ContentUtil.get(PropsUtil.get(
			"discussion.mentioned.email.body"));

		SubscriptionSender subscriptionSender = new SubscriptionSender();

		subscriptionSender.setBody(mailBody);
		subscriptionSender.setCompanyId(companyId);
		subscriptionSender.setContextAttribute(
			"[$COMMENTS_BODY$]", message.getBody(true), false);
		subscriptionSender.setContextAttributes(
			"[$COMMENTS_USER_ADDRESS$]", mailUserAddress,
			"[$COMMENTS_USER_NAME$]", mailUserName, "[$CONTENT_URL$]",
			contentURL);
		subscriptionSender.setFrom(fromAddress, fromName);
		subscriptionSender.setHtmlFormat(true);
		subscriptionSender.setMailId(
			"mb_discussion", message.getCategoryId(), message.getMessageId());
		subscriptionSender.setScopeGroupId(message.getGroupId());
		subscriptionSender.setServiceContext(serviceContext);
		subscriptionSender.setSubject(mailSubject);
		subscriptionSender.setUserId(message.getUserId());

		String[] mentionedUsersArray = StringUtil.split(mentionedUsers);

		for (int i = 0; i < mentionedUsersArray.length; i++) {
			String screenName = mentionedUsersArray[i];

			try {
				User mentionedUser = UserLocalServiceUtil.getUserByScreenName(
					companyId, screenName);

				subscriptionSender.addRuntimeSubscribers(
					mentionedUser.getEmailAddress(),
					mentionedUser.getFullName());
			}
			catch (Exception e) {
			}
		}

		return message;
	}

}