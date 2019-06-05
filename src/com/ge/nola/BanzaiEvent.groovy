package com.ge.nola

class BanzaiEvent {
    static final enum Scope {
        PIPELINE,
        STAGE
    }
    static final enum Status {
        PENDING,
        SUCCESS,
        FAILURE
    }
    
    String scope
    String status
    String stage
    String message

    public BanzaiEvent(Map props) {
        this.scope = props.scope
        this.status = props.status
        this.stage = props.stage ?: 'Pipeline'
        this.message = props.message
    }
}