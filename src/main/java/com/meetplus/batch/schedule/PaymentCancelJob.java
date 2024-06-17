package com.meetplus.batch.schedule;

import com.meetplus.batch.common.PaymentStatus;
import com.meetplus.batch.domain.payment.Payment;
import com.meetplus.batch.infrastructure.payment.PaymentRepository;
import jakarta.persistence.EntityManagerFactory;
import java.util.Iterator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@Slf4j
public class PaymentCancelJob {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final PaymentRepository paymentRepository;
    private final EntityManagerFactory entityManagerFactory;

    @Autowired
    public PaymentCancelJob(JobRepository jobRepository,
        @Qualifier("paymentTransactionManager") PlatformTransactionManager transactionManager,
        PaymentRepository paymentRepository,
        @Qualifier("paymentEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.paymentRepository = paymentRepository;
        this.entityManagerFactory = entityManagerFactory;
    }

    @Bean
    public ItemReader<Payment> paymentCancelReader() {
        return new ItemReader<Payment>() {
            private Iterator<Payment> paymentsIterator;

            @Override
            public Payment read() throws Exception {
                if (paymentsIterator == null) {
                    List<Payment> payments = paymentRepository.findByPaymentStatus(
                        PaymentStatus.PENDING);
                    paymentsIterator = payments.iterator();
                }

                if (paymentsIterator.hasNext()) {
                    return paymentsIterator.next();
                } else {
                    return null; // 더 이상 읽을 데이터가 없음
                }
            }
        };
    }

    @Bean
    public ItemProcessor<Payment, Payment> paymentCancelProcessor() {
        return payment -> {
            payment.setPaymentStatus(PaymentStatus.CANCEL);
            return payment;
        };
    }

    @Bean
    public JpaItemWriter<Payment> paymentCancelWriter() {
        return new JpaItemWriterBuilder<Payment>()
            .entityManagerFactory(entityManagerFactory)
            .build();
    }

    @Bean
    @Qualifier("updatePaymentStatusStep")
    public Step updatePaymentStatusStep() {
        return new StepBuilder("updatePaymentStatusStep", jobRepository)
            .<Payment, Payment>chunk(10, transactionManager)
            .reader(paymentCancelReader())
            .processor(paymentCancelProcessor())
            .writer(paymentCancelWriter())
            .build();
    }

    @Bean
    @Qualifier("updatePaymentStatusJob")
    public Job updatePaymentStatusJob(@Qualifier("updatePaymentStatusStep") Step updatePaymentStatusStep) {
        return new JobBuilder("updatePaymentStatusJob", jobRepository)
            .start(updatePaymentStatusStep())
            .build();
    }
}
