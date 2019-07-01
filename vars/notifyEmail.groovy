#!/usr/bin/env groovy

import com.github.banzaicicd.BanzaiEvent
import com.github.banzaicicd.BanzaiEmailUtil
import com.github.banzaicicd.cfg.BanzaiCfg
import com.github.banzaicicd.cfg.BanzaiNotificationsEmailCfg

void call(BanzaiCfg cfg, BanzaiEvent event) {
    logger "notifyEmail"
    if (!cfg.email || !cfg.notifications?.email) {
        return
    }
    // determine if there is an email config for this branch
    BanzaiNotificationsEmailCfg emailCfg = findValueInRegexObject(cfg.notifications.email, BRANCH_NAME)
    if (emailCfg == null) {
        return 
    }
    logger "email configuration for branch '${BRANCH_NAME}' detected"
    Set<String> addresses = BanzaiEmailUtil.getAddressesForEvent(cfg, emailCfg, event)

    if (addresses.size() > 0) {
        String subject = "${env.JOB_NAME} ${event.scope} ${event.status}"
        String body = "Message: ${event.message}"
        sendEmail(cfg.email.admin, addresses.join(','), subject, body, event.attachmentPattern)
    }
}