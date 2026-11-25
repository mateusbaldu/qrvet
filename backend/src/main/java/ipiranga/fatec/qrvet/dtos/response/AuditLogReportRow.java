package ipiranga.fatec.qrvet.dtos.response;

import java.time.Instant;

public interface AuditLogReportRow {
    Long getId();

    Instant getTimestamp();

    Long getUserId();

    String getUserEmail();

    String getIpAddress();

    String getAction();

    String getResourceName();

    String getResourceId();

    String getDetails();
}
