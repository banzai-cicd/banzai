package com.ge.nola;

class BanzaiFlowdockCfg {
    String credId
    BanzaiFlowdockAuthorCfg author
    Boolean notifyPRs

    class BanzaiFlowdockAuthorCfg {
        String name
        String avatarUrl
        String email
    }
}