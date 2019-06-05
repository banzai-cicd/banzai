#!/usr/bin/env groovy

import com.ge.nola.BanzaiCfg
import com.ge.nola.BanzaiEvent
import com.ge.nola.BanzaiNotificationsEmailCfg

void call(BanzaiCfg cfg, BanzaiEvent event) {
    if (!cfg.email 
        || (!cfg.email.addresses || !cfg.email.groups)
        || !cfg.notifications 
        || !cfg.notifications.email) {
        return
    }
    // determine if there is an email config for this branch
    BanzaiNotificationsEmailCfg emailCfg = findValueInRegexObject(cfg.notifications.email, BRANCH_NAME)
    if (emailCfg == null) {
        return 
    }

    String currentEvent = event.getEventLabel()
    logger "currentEvent: ${currentEvent}"
    Set<String> addresses = []
    if (emailCfg.groups) {
        // find groupIds
        Set<String> groupIds = emailCfg.groups.keySet().findAll { groupId ->
             emailCfg.groups[groupId].find { regex -> currentEvent ==~ regex }
        }
        if (groupIds.size() == 0) { return }

        logger "groupIds: ${groupIds}"
        
        groupIds.each { groupId ->
            List<String> emailIds = cfg.email.groups[groupId]
            emailIds.each { addresses.add(cfg.email.addresses[it]) }
        }
    }
    logger "addresses1: ${addresses}"
    if (emailCfg.individuals) {
        Set<String> emailIds = emailCfg.individuals.keySet().findAll { emailId ->
            emailCfg.individuals[emailId].find { regex -> currentEvent ==~ regex }
        }
        if (emailIds.size() == 0) { return }

        logger "emailIds: ${emailIds}"
        emailIds.each { addresses.add(cfg.email.addresses[it]) }
    }
    logger "addresses2: ${addresses}"
    if (addresses.size() > 0) {
        String details = "${env.JOB_NAME} ${event.scope} ${event.status}"
        String subject = "Banzai: ${details}"
        String body = "Message: ${event.message}"
        sendEmail(addresses.join(','), null, subject, body)
    }
}