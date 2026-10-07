package io.github.ysuestc.offerflow.position.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.ysuestc.offerflow.common.api.*;
import io.github.ysuestc.offerflow.common.exception.BusinessException;
import io.github.ysuestc.offerflow.company.service.CompanyService;
import io.github.ysuestc.offerflow.position.dto.PositionRequest;
import io.github.ysuestc.offerflow.position.entity.JobPosition;
import io.github.ysuestc.offerflow.position.mapper.JobPositionMapper;
import io.github.ysuestc.offerflow.position.vo.PositionView;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("mysql")
@RequiredArgsConstructor
public class PositionService {
    private final JobPositionMapper mapper;
    private final CompanyService companies;

    public PageResponse<PositionView> list(String q, Long companyId, int page, int size) {
        String search = InputText.search(q);
        return new PageResponse<>(mapper.listViews(search, companyId, size, (long) (page - 1) * size),
                mapper.countViews(search, companyId), page, size);
    }

    public PositionView get(long id) {
        PositionView view = mapper.findView(id);
        if (view == null) throw new BusinessException(ApiErrorCode.NOT_FOUND, "岗位不存在");
        return view;
    }

    @Transactional
    public PositionView create(PositionRequest request) {
        companies.require(request.companyId());
        JobPosition entity = new JobPosition();
        entity.setCompanyId(request.companyId());
        entity.setName(request.name().strip());
        entity.setLocation(InputText.optional(request.location()));
        entity.setDirection(InputText.optional(request.direction()));
        entity.setJd(InputText.optional(request.jd()));
        entity.setRecruitmentBatch(InputText.optional(request.recruitmentBatch()));
        mapper.insert(entity);
        return get(entity.getId());
    }

    @Transactional
    public PositionView update(long id, PositionRequest request) {
        get(id);
        companies.require(request.companyId());
        mapper.update(null, Wrappers.<JobPosition>lambdaUpdate().eq(JobPosition::getId, id)
                .set(JobPosition::getCompanyId, request.companyId()).set(JobPosition::getName, request.name().strip())
                .set(JobPosition::getLocation, InputText.optional(request.location()))
                .set(JobPosition::getDirection, InputText.optional(request.direction()))
                .set(JobPosition::getJd, InputText.optional(request.jd()))
                .set(JobPosition::getRecruitmentBatch, InputText.optional(request.recruitmentBatch())));
        return get(id);
    }

    @Transactional
    public void delete(long id) {
        if (mapper.deleteById(id) == 0) throw new BusinessException(ApiErrorCode.NOT_FOUND, "岗位不存在");
    }
}
