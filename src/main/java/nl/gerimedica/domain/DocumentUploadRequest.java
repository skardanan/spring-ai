package nl.gerimedica.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class DocumentUploadRequest {
    private String id;
    private String text;
    private Map<String, Object> metadata;
}
