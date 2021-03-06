/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
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

package com.liferay.headless.delivery.internal.resource.v1_0;

import com.liferay.headless.common.spi.service.context.ServiceContextUtil;
import com.liferay.headless.delivery.dto.v1_0.MessageBoardSection;
import com.liferay.headless.delivery.internal.dto.v1_0.util.CreatorUtil;
import com.liferay.headless.delivery.internal.odata.entity.v1_0.MessageBoardSectionEntityModel;
import com.liferay.headless.delivery.resource.v1_0.MessageBoardSectionResource;
import com.liferay.message.boards.model.MBCategory;
import com.liferay.message.boards.service.MBCategoryService;
import com.liferay.petra.function.UnsafeConsumer;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.search.BooleanClauseOccur;
import com.liferay.portal.kernel.search.BooleanQuery;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.Sort;
import com.liferay.portal.kernel.search.filter.BooleanFilter;
import com.liferay.portal.kernel.search.filter.Filter;
import com.liferay.portal.kernel.search.filter.TermFilter;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.odata.entity.EntityModel;
import com.liferay.portal.vulcan.pagination.Page;
import com.liferay.portal.vulcan.pagination.Pagination;
import com.liferay.portal.vulcan.resource.EntityModelResource;
import com.liferay.portal.vulcan.util.SearchUtil;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

/**
 * @author Javier Gamarra
 */
@Component(
	properties = "OSGI-INF/liferay/rest/v1_0/message-board-section.properties",
	scope = ServiceScope.PROTOTYPE, service = MessageBoardSectionResource.class
)
public class MessageBoardSectionResourceImpl
	extends BaseMessageBoardSectionResourceImpl implements EntityModelResource {

	@Override
	public void deleteMessageBoardSection(Long messageBoardSectionId)
		throws Exception {

		_mbCategoryService.deleteCategory(messageBoardSectionId, true);
	}

	@Override
	public EntityModel getEntityModel(MultivaluedMap multivaluedMap) {
		return _entityModel;
	}

	@Override
	public MessageBoardSection getMessageBoardSection(
			Long messageBoardSectionId)
		throws Exception {

		return _toMessageBoardSection(
			_mbCategoryService.getCategory(messageBoardSectionId));
	}

	@Override
	public Page<MessageBoardSection>
			getMessageBoardSectionMessageBoardSectionsPage(
				Long parentMessageBoardSectionId, String search, Filter filter,
				Pagination pagination, Sort[] sorts)
		throws Exception {

		MBCategory mbCategory = _mbCategoryService.getCategory(
			parentMessageBoardSectionId);

		return _getSiteMessageBoardSectionsPage(
			booleanQuery -> {
				BooleanFilter booleanFilter =
					booleanQuery.getPreBooleanFilter();

				booleanFilter.add(
					new TermFilter(
						Field.ASSET_PARENT_CATEGORY_ID,
						String.valueOf(mbCategory.getCategoryId())),
					BooleanClauseOccur.MUST);
			},
			mbCategory.getGroupId(), search, filter, pagination, sorts);
	}

	@Override
	public Page<MessageBoardSection> getSiteMessageBoardSectionsPage(
			Long siteId, Boolean flatten, String search, Filter filter,
			Pagination pagination, Sort[] sorts)
		throws Exception {

		return _getSiteMessageBoardSectionsPage(
			booleanQuery -> {
				if (!GetterUtil.getBoolean(flatten)) {
					BooleanFilter booleanFilter =
						booleanQuery.getPreBooleanFilter();

					booleanFilter.add(
						new TermFilter(Field.ASSET_PARENT_CATEGORY_ID, "0"),
						BooleanClauseOccur.MUST);
				}
			},
			siteId, search, filter, pagination, sorts);
	}

	@Override
	public MessageBoardSection postMessageBoardSectionMessageBoardSection(
			Long parentMessageBoardSectionId,
			MessageBoardSection messageBoardSection)
		throws Exception {

		MBCategory mbCategory = _mbCategoryService.getCategory(
			parentMessageBoardSectionId);

		return _addMessageBoardSection(
			mbCategory.getGroupId(), parentMessageBoardSectionId,
			messageBoardSection);
	}

	@Override
	public MessageBoardSection postSiteMessageBoardSection(
			Long siteId, MessageBoardSection messageBoardSection)
		throws Exception {

		return _addMessageBoardSection(siteId, 0L, messageBoardSection);
	}

	@Override
	public MessageBoardSection putMessageBoardSection(
			Long messageBoardSectionId, MessageBoardSection messageBoardSection)
		throws Exception {

		MBCategory mbCategory = _mbCategoryService.getCategory(
			messageBoardSectionId);

		return _toMessageBoardSection(
			_mbCategoryService.updateCategory(
				messageBoardSectionId, mbCategory.getParentCategoryId(),
				messageBoardSection.getTitle(),
				messageBoardSection.getDescription(),
				mbCategory.getDisplayStyle(), "", "", "", 0, false, "", "", 0,
				"", false, "", 0, false, "", "", false, false, false,
				new ServiceContext()));
	}

	private MessageBoardSection _addMessageBoardSection(
			long siteId, Long parentMessageBoardSectionId,
			MessageBoardSection messageBoardSection)
		throws Exception {

		return _toMessageBoardSection(
			_mbCategoryService.addCategory(
				_user.getUserId(), parentMessageBoardSectionId,
				messageBoardSection.getTitle(),
				messageBoardSection.getDescription(),
				ServiceContextUtil.createServiceContext(
					siteId, messageBoardSection.getViewableByAsString())));
	}

	private Page<MessageBoardSection> _getSiteMessageBoardSectionsPage(
			UnsafeConsumer<BooleanQuery, Exception> booleanQueryUnsafeConsumer,
			Long siteId, String search, Filter filter, Pagination pagination,
			Sort[] sorts)
		throws Exception {

		return SearchUtil.search(
			booleanQueryUnsafeConsumer, filter, MBCategory.class, search,
			pagination,
			queryConfig -> queryConfig.setSelectedFieldNames(
				Field.ENTRY_CLASS_PK),
			searchContext -> {
				searchContext.setCompanyId(contextCompany.getCompanyId());
				searchContext.setGroupIds(new long[] {siteId});
			},
			document -> _toMessageBoardSection(
				_mbCategoryService.getCategory(
					GetterUtil.getLong(document.get(Field.ENTRY_CLASS_PK)))),
			sorts);
	}

	private MessageBoardSection _toMessageBoardSection(MBCategory mbCategory)
		throws Exception {

		return new MessageBoardSection() {
			{
				creator = CreatorUtil.toCreator(
					_portal,
					_userLocalService.getUserById(mbCategory.getUserId()));
				dateCreated = mbCategory.getCreateDate();
				dateModified = mbCategory.getModifiedDate();
				description = mbCategory.getDescription();
				id = mbCategory.getCategoryId();
				numberOfMessageBoardSections =
					_mbCategoryService.getCategoriesCount(
						mbCategory.getGroupId(), mbCategory.getCategoryId());
				numberOfMessageBoardThreads = mbCategory.getThreadCount();
				siteId = mbCategory.getGroupId();
				title = mbCategory.getName();
			}
		};
	}

	private static final EntityModel _entityModel =
		new MessageBoardSectionEntityModel();

	@Reference
	private MBCategoryService _mbCategoryService;

	@Reference
	private Portal _portal;

	@Context
	private User _user;

	@Reference
	private UserLocalService _userLocalService;

}