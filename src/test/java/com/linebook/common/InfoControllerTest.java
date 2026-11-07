package com.linebook.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.linebook.common.config.LinebookProperties;
import com.linebook.common.web.InfoController;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class InfoControllerTest {

    @Test
    void returnsTheConfiguredBusinessNameAndZone() {
        LinebookProperties props = new LinebookProperties("Asia/Kolkata", "Surya Cable Network", "ag-01", true,
                new LinebookProperties.Cors(List.of("http://localhost:8080"), Duration.ofHours(1)));

        InfoController.Info info = new InfoController(props).info();

        assertThat(info.operatorName()).isEqualTo("Surya Cable Network");
        assertThat(info.timeZone()).isEqualTo("Asia/Kolkata");
    }
}
