package io.github.ysuestc.offerflow.application.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.ysuestc.offerflow.application.dto.ApplicationEntryRequest;
import io.github.ysuestc.offerflow.application.dto.ApplicationRequest;
import io.github.ysuestc.offerflow.application.vo.ApplicationView;
import io.github.ysuestc.offerflow.common.api.ApiErrorCode;
import io.github.ysuestc.offerflow.common.api.InputText;
import io.github.ysuestc.offerflow.common.exception.BusinessException;
import io.github.ysuestc.offerflow.company.dto.CompanyRequest;
import io.github.ysuestc.offerflow.company.entity.Company;
import io.github.ysuestc.offerflow.company.entity.CompanyType;
import io.github.ysuestc.offerflow.company.mapper.CompanyMapper;
import io.github.ysuestc.offerflow.company.service.CompanyService;
import io.github.ysuestc.offerflow.position.dto.PositionRequest;
import io.github.ysuestc.offerflow.position.entity.JobPosition;
import io.github.ysuestc.offerflow.position.mapper.JobPositionMapper;
import io.github.ysuestc.offerflow.position.service.PositionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("mysql")
@RequiredArgsConstructor
public class ApplicationEntryService {
    private final CompanyMapper companyMapper;
    private final JobPositionMapper positionMapper;
    private final CompanyService companies;
    private final PositionService positions;
    private final ApplicationService applications;

    @Transactional
    public ApplicationView create(ApplicationEntryRequest request) {
        long companyId = resolveCompany(request);
        String name = request.positionName().strip();
        String location = InputText.optional(request.location());
        String direction = InputText.optional(request.direction());
        String batch = InputText.optional(request.recruitmentBatch());
        var candidates = positionMapper.selectList(Wrappers.<JobPosition>lambdaQuery()
                .eq(JobPosition::getCompanyId, companyId).eq(JobPosition::getName, name)
                .eq(location != null, JobPosition::getLocation, location).isNull(location == null, JobPosition::getLocation)
                .eq(direction != null, JobPosition::getDirection, direction).isNull(direction == null, JobPosition::getDirection)
                .eq(batch != null, JobPosition::getRecruitmentBatch, batch).isNull(batch == null, JobPosition::getRecruitmentBatch)
                .last("LIMIT 2"));
        if (candidates.size() > 1) {
            throw new BusinessException(ApiErrorCode.CONFLICT, "有多个匹配岗位，请切换选择已有岗位并选中具体岗位");
        }
        // Reuse never edits catalog metadata; JD belongs to the catalog editor.
        long positionId = candidates.isEmpty()
                ? Long.parseLong(positions.create(new PositionRequest(companyId, name, location, direction,
                        request.jd(), batch)).id())
                : candidates.getFirst().getId();
        return applications.create(new ApplicationRequest(positionId, request.channel(), request.appliedOn(),
                request.stage(), request.stageOn(), request.endReason(), request.submitted(), request.notes()));
    }

    private long resolveCompany(ApplicationEntryRequest request) {
        if (request.companyId() != null) return companies.require(request.companyId()).getId();
        var candidates = companyMapper.selectList(Wrappers.<Company>lambdaQuery()
                .eq(Company::getName, request.companyName().strip()).last("LIMIT 2"));
        if (candidates.size() > 1) {
            throw new BusinessException(ApiErrorCode.CONFLICT, "存在同名公司，请选择已有公司并确认具体公司");
        }
        return candidates.isEmpty()
                ? Long.parseLong(companies.create(new CompanyRequest(request.companyName(),
                        request.companyType() == null ? CompanyType.OTHER : request.companyType(), null, null)).id())
                : candidates.getFirst().getId();
    }
}
