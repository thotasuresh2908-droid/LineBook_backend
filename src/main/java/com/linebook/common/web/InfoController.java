package com.linebook.common.web;

import com.linebook.common.config.LinebookProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Settings the UI shows but does not own, so the business name is configured once, on the server. */
@Tag(name = "Settings", description = "Business settings for the UI")
@RestController
@RequestMapping("/api/info")
public class InfoController {

    /** {@code operatorName} is printed in the sidebar, on receipts, on shared bills and on reports. */
    public record Info(String operatorName, String timeZone) {
    }

    private final LinebookProperties props;

    public InfoController(LinebookProperties props) {
        this.props = props;
    }

    @Operation(summary = "Business name and time zone")
    @GetMapping
    public Info info() {
        return new Info(props.operatorName(), props.zone());
    }
}
