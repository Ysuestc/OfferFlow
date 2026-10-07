package io.github.ysuestc.offerflow.health;

import io.github.ysuestc.offerflow.common.api.ApiResponse;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WorkspaceController {
    private final Environment environment;
    public WorkspaceController(Environment environment) { this.environment = environment; }

    @GetMapping("/api/v1/workspace")
    public ApiResponse<WorkspaceMode> mode() {
        return ApiResponse.success(new WorkspaceMode(environment.matchesProfiles("mysql")));
    }

    public record WorkspaceMode(boolean available) { }
}
