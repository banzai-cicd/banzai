package com.github.banzaicicd;

class BanzaiEvent {
    static final enum Scope {
        PIPELINE,
        STAGE,
        VULNERABILITY,
        QUALITY,
        GITOPS
    }
    static final enum Status {
        PENDING,
        SUCCESS,
        FAILURE,
        ABORTED,
        APPROVAL,
        APPROVED
    }
    
    BanzaiEvent.Scope scope
    BanzaiEvent.Status status
    String stage
    String message
    String attachmentPattern

    public BanzaiEvent(Map props) {
        this.scope = props.scope
        this.status = props.status
        this.stage = props.stage ?: 'Pipeline'
        this.message = props.message
        this.attachmentPattern = props.attachmentPattern
    }

    String getEventLabel() {
        return "${this.scope}:${this.status}"
    }
}