package io.github.ysuestc.offerflow.company.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.ysuestc.offerflow.common.api.*;
import io.github.ysuestc.offerflow.common.exception.BusinessException;
import io.github.ysuestc.offerflow.company.dto.CompanyRequest;
import io.github.ysuestc.offerflow.company.entity.Company;
import io.github.ysuestc.offerflow.company.mapper.CompanyMapper;
import io.github.ysuestc.offerflow.company.vo.CompanyView;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("mysql")
@RequiredArgsConstructor
public class CompanyService {
    private final CompanyMapper mapper;

    public PageResponse<CompanyView> list(String q, int page, int size) {
        String search = InputText.search(q);
        var filter = Wrappers.<Company>query();
        if (search != null) filter.apply("name LIKE CONCAT('%', {0}, '%') ESCAPE '!'", search);
        long total = mapper.selectCount(filter);
        filter.orderByDesc("id").last("LIMIT " + size + " OFFSET " + ((long) (page - 1) * size));
        return new PageResponse<>(mapper.selectList(filter).stream().map(CompanyView::from).toList(),
                total, page, size);
    }

    public CompanyView get(long id) { return CompanyView.from(require(id)); }

    public Company require(long id) {
        Company entity = mapper.selectById(id);
        if (entity == null) throw new BusinessException(ApiErrorCode.NOT_FOUND, "公司不存在");
        return entity;
    }

    @Transactional
    public CompanyView create(CompanyRequest request) {
        Company entity = new Company();
        entity.setName(request.name().strip());
        entity.setType(request.type());
        entity.setWebsite(InputText.website(request.website()));
        entity.setNotes(InputText.optional(request.notes()));
        mapper.insert(entity);
        return get(entity.getId());
    }

    @Transactional
    public CompanyView update(long id, CompanyRequest request) {
        require(id);
        mapper.update(null, Wrappers.<Company>lambdaUpdate().eq(Company::getId, id)
                .set(Company::getName, request.name().strip()).set(Company::getType, request.type())
                .set(Company::getWebsite, InputText.website(request.website()))
                .set(Company::getNotes, InputText.optional(request.notes())));
        return get(id);
    }

    @Transactional
    public void delete(long id) {
        if (mapper.deleteById(id) == 0) throw new BusinessException(ApiErrorCode.NOT_FOUND, "公司不存在");
    }
}
