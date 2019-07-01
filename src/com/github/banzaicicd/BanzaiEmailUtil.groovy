package com.github.banzaicicd;

import com.github.banzaicicd.cfg.BanzaiCfg
import com.github.banzaicicd.BanzaiEvent

class BanzaiEmailUtil {

    static void sendEmail(String from, String to, String subject, String body, String attachmentsPattern) {
        logger "Sending Email to ${to}: ${subject}"
        String url = env.RUN_DISPLAY_URL ?: env.BUILD_URL
        String jobInfo = "Job: ${env.JOB_NAME} #${env.BUILD_NUMBER} \nBuild URL: ${url}\n\n"
        emailext from: from,
            to: to,
            subject:"Banzai: ${subject}",
            body: "${jobInfo}${body}",
            attachmentsPattern: attachmentsPattern
    }

    Set<String> getAddressesForEvent(BanzaiCfg cfg, BanzaiEvent event) {
        String currentEvent = event.getEventLabel()
        Set<String> addresses = []
        if (emailCfg.groups) {
            // find groupIds
            Set<String> groupIds = emailCfg.groups.keySet().findAll { groupId ->
                emailCfg.groups[groupId].find { regex -> currentEvent ==~ regex }
            }
            
            groupIds.each { groupId ->
                List<String> emailIds = cfg.email.groups[groupId]
                emailIds.each { addresses.add(cfg.email.addresses[it]) }
            }
        }
        if (emailCfg.individuals) {
            Set<String> emailIds = emailCfg.individuals.keySet().findAll { emailId ->
                emailCfg.individuals[emailId].find { regex -> currentEvent ==~ regex }
            }

            emailIds.each { addresses.add(cfg.email.addresses[it]) }
        }

        return addresses
    }

}