package com.meetplus.batch.config;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobInstanceAlreadyExistsException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentCancelSchedule {

    private final JobLauncher jobLauncher;

    private final Job updatePaymentStatusJob;

    @Scheduled(cron = "0 0 2 * * ?")
    public void runJob() throws Exception {
//        jobLauncher.run(updatePaymentStatusJob, new JobParametersBuilder().toJobParameters());
        JobParameters jobParameters = new JobParametersBuilder()
            .addString("time", String.valueOf(System.currentTimeMillis()))  // 고유한 파라미터 추가
            .toJobParameters();

        try {
            jobLauncher.run(updatePaymentStatusJob, jobParameters);
        } catch (JobExecutionAlreadyRunningException | JobRestartException |
                 JobInstanceAlreadyCompleteException e) {
            System.out.println(e.getMessage());;
        }
    }
}
