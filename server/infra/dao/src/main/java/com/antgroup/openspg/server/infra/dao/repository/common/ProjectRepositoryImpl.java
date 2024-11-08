/*
 * Copyright 2023 OpenSPG Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 */

package com.antgroup.openspg.server.infra.dao.repository.common;

import com.antgroup.openspg.common.util.CollectionsUtils;
import com.antgroup.openspg.server.api.facade.Paged;
import com.antgroup.openspg.server.api.facade.dto.common.request.ProjectQueryRequest;
import com.antgroup.openspg.server.common.model.exception.ProjectException;
import com.antgroup.openspg.server.common.model.project.Project;
import com.antgroup.openspg.server.common.service.project.ProjectRepository;
import com.antgroup.openspg.server.infra.dao.dataobject.ProjectDO;
import com.antgroup.openspg.server.infra.dao.dataobject.ProjectDOExample;
import com.antgroup.openspg.server.infra.dao.mapper.ProjectDOMapper;
import com.antgroup.openspg.server.infra.dao.repository.common.convertor.ProjectConvertor;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class ProjectRepositoryImpl implements ProjectRepository {

  @Autowired private ProjectDOMapper projectDOMapper;

  @Override
  public Long save(Project project) {
    List<Project> existProjects1 = query(new ProjectQueryRequest().setName(project.getName()));
    if (CollectionUtils.isNotEmpty(existProjects1)) {
      throw ProjectException.projectNameAlreadyExist(project.getName());
    }

    List<Project> existProjects2 =
        query(new ProjectQueryRequest().setNamespace(project.getNamespace()));
    if (CollectionUtils.isNotEmpty(existProjects2)) {
      throw ProjectException.namespaceAlreadyExist(project.getNamespace());
    }
    ProjectDO projectDO = ProjectConvertor.toDO(project);
    projectDOMapper.insert(projectDO);
    return projectDO.getId();
  }

  @Override
  public Project update(Project project) {
    ProjectDO projectDO = ProjectConvertor.toDO(project);
    projectDOMapper.updateByPrimaryKeySelective(projectDO);
    return project;
  }

  @Override
  public Project queryById(Long projectId) {
    ProjectDO projectDO = projectDOMapper.selectByPrimaryKey(projectId);
    return ProjectConvertor.toModel(projectDO);
  }

  @Override
  public List<Project> query(ProjectQueryRequest request) {
    ProjectDOExample example = new ProjectDOExample();

    ProjectDOExample.Criteria criteria = example.createCriteria();
    if (request.getTenantId() != null) {
      criteria.andBizDomainIdEqualTo(request.getTenantId());
    }
    if (request.getProjectId() != null) {
      criteria.andIdEqualTo(request.getProjectId());
    }
    if (request.getName() != null) {
      criteria.andNameEqualTo(request.getName());
    }
    if (request.getNamespace() != null) {
      criteria.andNamespaceEqualTo(request.getNamespace());
    }

    List<ProjectDO> projectDOS = projectDOMapper.selectByExample(example);
    return CollectionsUtils.listMap(projectDOS, ProjectConvertor::toModel);
  }

  @Override
  public Paged<Project> queryPaged(ProjectQueryRequest request, int start, int size) {
    Paged<Project> result = new Paged<>();
    result.setPageIdx(start);
    result.setPageSize(size);
    ProjectDO projectDO = new ProjectDO();
    projectDO.setName(request.getName());
    projectDO.setBizDomainId(request.getTenantId());
    long count =
        projectDOMapper.selectCountByCondition(projectDO, request.getOrderByGmtCreateDesc());
    result.setTotal(count);
    List<Project> list = new ArrayList<>();
    start = start > 0 ? start : 1;
    int startPage = (start - 1) * size;
    List<ProjectDO> projectDOS =
        projectDOMapper.selectByCondition(
            projectDO, request.getOrderByGmtCreateDesc(), startPage, size);
    if (CollectionUtils.isNotEmpty(projectDOS)) {
      list = projectDOS.stream().map(ProjectConvertor::toModel).collect(Collectors.toList());
    }
    result.setResults(list);
    return result;
  }
}
