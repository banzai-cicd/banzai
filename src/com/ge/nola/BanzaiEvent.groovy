package com.ge.nola

class BanzaiEvent {
    static final enum scope = {
        PIPELINE,
        STAGE
    }
    static final enum status = {
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
        this.staetus = props.status
        this.stage = props.stage ?: 'Pipeline'
        this.message = props.message
    }
}