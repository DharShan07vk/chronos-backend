package com.chronos.chronos.Scheduler;

import com.chronos.chronos.model.CapsuleModel;
import com.chronos.chronos.repositiory.CapsuleRepo;
import com.chronos.chronos.service.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class MailScheduler {

    private final CapsuleRepo capsuleRepo;
    private final MailService mailService;

    @Autowired
    public MailScheduler(CapsuleRepo capsuleRepo, MailService mailService) {
        this.capsuleRepo = capsuleRepo;
        this.mailService = mailService;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void FetchCapsules() {

        System.out.println("Scheduler running at: " + LocalDateTime.now());

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay().minusNanos(1);

        List<CapsuleModel> unlockedToday = capsuleRepo.findByUnlockDateBetween(startOfDay, endOfDay);

        System.out.println("Capsules found: " + unlockedToday.size());

        for (CapsuleModel capsule : unlockedToday) {
            System.out.println("Sending mail to: " + capsule.getUser().getEmail());
            mailService.UnlockMail(
                    capsule.getUser().getEmail(),
                    capsule.getUser().getName());
        }
    }

}
