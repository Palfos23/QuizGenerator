package com.quizapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// A problem the browser noticed that the server can't see on its own - e.g. the
// suggestion list for an answer box failed to load or came back empty. Only ever
// logged, never stored.
public class ClientEventRequest {

    @NotBlank
    @Size(max = 60)
    private String area; // e.g. "tension-suggestions"

    @NotBlank
    @Size(max = 40)
    private String kind; // e.g. "FETCH_FAILED", "EMPTY_LIST"

    @Size(max = 200)
    private String key; // e.g. the sport / category name being looked up

    private Integer httpStatus;

    @Size(max = 300)
    private String detail;

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public Integer getHttpStatus() { return httpStatus; }
    public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
