package org.sunbird.common.quartz.scheduler;

import org.quartz.*;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;

import java.util.HashMap;
import java.util.Map;

import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

public class OnDemandSchedulerManager extends SchedulerManager{

  private static OnDemandSchedulerManager onDemandSchedulerManager = null;

  public static Map<String, String> schedulerMap = new HashMap<>();

  private OnDemandSchedulerManager() {
   super();
   if(schedulerMap == null) {
    schedulerMap.put("bulkupload","uploadVerify");
    schedulerMap.put("shadowuser","migrateShadowUser");
   }
  }

  private void scheduleOnDemand(String identifier, String job) {
   ProjectLogger.log(
           "onDemandSchedulerManager:scheduleOnDemand:scheduler started",
           LoggerEnum.INFO.name());
   String jobName = schedulerMap.get(job);
   JobDetail migrateShadowUserJob =
           JobBuilder.newJob(ShadowUserMigrationScheduler.class)
                   .requestRecovery(true)
                   .withDescription("Scheduler for migrating shadow user ")
                   .withIdentity(jobName+"Scheduler", identifier)
                   .build();
   Trigger migrateShadowUserTrigger =
           TriggerBuilder.newTrigger()
                   .withIdentity(jobName+"Trigger", identifier).startNow()
                   .withSchedule(simpleSchedule().withIntervalInSeconds(1).withRepeatCount(0))
                   .build();
   try {
    if (scheduler.checkExists(migrateShadowUserJob.getKey())) {
     scheduler.deleteJob(migrateShadowUserJob.getKey());
    }
    scheduler.scheduleJob(migrateShadowUserJob, migrateShadowUserTrigger);
    scheduler.start();
    ProjectLogger.log(
            "onDemandSchedulerManager:scheduleOnDemand:scheduler ended",
            LoggerEnum.INFO.name());
   } catch (Exception e) {
    ProjectLogger.log(
            "onDemandSchedulerManager:scheduleOnDemand Error occurred " + e.getMessage(),
            LoggerEnum.ERROR.name());
   }
  }

  public static OnDemandSchedulerManager getInstance() {
   if (onDemandSchedulerManager == null) {
     synchronized (OnDemandSchedulerManager.class) {
     if (onDemandSchedulerManager == null) {
      onDemandSchedulerManager = new OnDemandSchedulerManager();
     }
    }
   }
   return onDemandSchedulerManager;
  }

  public void triggerScheduler(String[] jobs) {
   String identifier = "NetOps-PC1502295457753";
   for(String job: jobs) {
     switch (job) {
      case "bulkupload":
      case "shadowuser":
       scheduleOnDemand(identifier, job);
       break;
      default:
       ProjectLogger.log(
               "OnDemandSchedulerManager:triggerScheduler: There is no such job",
               LoggerEnum.INFO.name());
     }
   }
  }
}
