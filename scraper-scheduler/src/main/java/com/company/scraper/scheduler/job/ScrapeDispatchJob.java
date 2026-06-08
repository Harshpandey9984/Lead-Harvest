package com.company.scraper.scheduler.job;

import com.company.scraper.scheduler.service.ScheduleService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.beans.factory.annotation.Autowired;

public class ScrapeDispatchJob extends QuartzJobBean {

    private ScheduleService scheduleService;

    @Autowired
    public void setScheduleService(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        scheduleService.dispatchDueJobs();
    }
}
