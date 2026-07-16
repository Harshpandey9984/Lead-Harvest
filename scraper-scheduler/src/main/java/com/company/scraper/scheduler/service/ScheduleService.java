package com.company.scraper.scheduler.service;

import com.company.scraper.common.model.JobStatus;
import com.company.scraper.common.model.ScrapeJob;
import com.company.scraper.common.model.ScrapeTarget;
import com.company.scraper.common.repository.ScrapeJobRepository;
import com.company.scraper.common.repository.ScrapeTargetRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduleService {

    private final ScrapeJobRepository jobRepository;
    private final ScrapeTargetRepository targetRepository;
    private final SchedulerTaskPublisher publisher;

    public ScheduleService(ScrapeJobRepository jobRepository,
                           ScrapeTargetRepository targetRepository,
                           SchedulerTaskPublisher publisher) {
        this.jobRepository = jobRepository;
        this.targetRepository = targetRepository;
        this.publisher = publisher;
    }

    @Transactional
    public void dispatchDueJobs() {
        Instant now = Instant.now();
        List<ScrapeJob> jobs = jobRepository.findByStatus(JobStatus.ACTIVE);
        for (ScrapeJob job : jobs) {
            if (job.getNextRunAt() != null && job.getNextRunAt().isAfter(now)) {
                continue;
            }
            List<ScrapeTarget> targets = targetRepository.findByJobId(job.getId());
            targets.forEach(publisher::publish);
            job.setNextRunAt(nextRun(job.getSchedule(), now));
            jobRepository.save(job);
        }
    }

    private Instant nextRun(String cronExpression, Instant from) {
        String normalizedCron = cronExpression;
        if (cronExpression != null) {
            String[] parts = cronExpression.trim().split("\\s+");
            if (parts.length == 5) {
                normalizedCron = "0 " + cronExpression.trim();
            }
        }
        CronExpression cron = CronExpression.parse(normalizedCron);
        return cron.next(from.atZone(ZoneId.of("UTC"))).toInstant();
    }
}
