#!/usr/bin/env groovy

import com.ge.nola.BanzaiCfg
import com.ge.nola.BanzaiEvent
import com.ge.nola.BanzaiNotificationsEmailCfg

void call(BanzaiCfg cfg, BanzaiEvent event) {
    if (!cfg.email || !cfg.notifications || !cfg.notifications.email) {
        return
    }
    // determine if there is an email config for this branch
    BanzaiNotificationsEmailCfg emailCfg = findValueInRegexObject(cfg.notifications.email, BRANCH_NAME)
    String currentEvent = event.getEventLabel()
    List<String> addresses = []
    if (emailCfg.groups) {
        // find groupIds
        Set<String> groupIds = emailCfg.groups.keySet().findAll { groupId ->
             emailCfg.groups[groupId].find { regex -> currentEvent ==~ regex }
        }

        // get email addresses of group(s)
        addresses + groupIds.inject([]) { acc, groupId ->
            List<String> emailIds = cfg.email.groups[groupId]
            acc + emailIds.collect { emailId -> cfg.email.addresses[emailId] }
        }
    }
    if (emailCfg.individuals) {
        Set<String> emailIds = emailCfg.individuals.keySet().findAll { emailId ->
            emailCfg.individuals[emailId].find { regex -> currentEvent ==~ regex }
        }

        addresses + emailIds.collect { cfg.email.addresses[it] }
    }

    if (addresses.size() > 0) {
        String details = "${env.JOB_NAME} ${event.scope} ${event.status}"
        String subject = "Banzai: ${details}"
        String body = "Message: ${event.message}"
        sendEmail(addresses.join(','), null, subject, body)
    }
}