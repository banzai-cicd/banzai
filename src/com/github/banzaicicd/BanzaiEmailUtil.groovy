package com.github.banzaicicd;

import com.github.banzaicicd.cfg.BanzaiCfg
import com.github.banzaicicd.BanzaiEvent

class BanzaiEmailUtil {

    /*
        determine if there are groups or individuals configured with a regex
        pattern matching this event
    */
    static Set<String> getAddressesForEvent(BanzaiCfg cfg, BanzaiEvent event) {
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