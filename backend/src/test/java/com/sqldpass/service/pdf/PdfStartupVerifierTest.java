package com.sqldpass.service.pdf;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PdfStartupVerifierTest {

    @Mock
    private PdfRenderService pdfRenderService;

    @InjectMocks
    private PdfStartupVerifier verifier;

    @Test
    void run_verifiesPdfEngine() throws Exception {
        verifier.run(null);

        verify(pdfRenderService, times(1)).verifyEngine();
    }
}
