package com.quikko.service;

import com.quikko.config.AppProperties;
import com.quikko.service.store.ModerationStore;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Tracks one-tap Reports and applies temporary IP bans once a session's report count crosses the configured threshold.
 */
@Service
public class ReportService {

    private final ModerationStore store;
    private final AppProperties props;

    public ReportService(ModerationStore store, AppProperties props) {
        this.store = store;
        this.props = props;
    }

    /**
     * Records a report against the given IP and bans it if the threshold is reached. Returns true if this report caused a ban.
     */
    public boolean report(String reportedIp) {
        if (reportedIp == null) {
            return false;
        }
        int count = store.recordReport(reportedIp);
        if (count >= props.getModeration().getReportBanThreshold()) {
            store.ban(reportedIp, Duration.ofMinutes(props.getModeration().getBanDurationMinutes()));
            return true;
        }
        return false;
    }

    public boolean isBanned(String ip) {
        return store.isBanned(ip);
    }
}
