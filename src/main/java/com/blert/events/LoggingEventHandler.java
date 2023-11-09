package com.blert.events;

import lombok.extern.slf4j.Slf4j;

@Slf4j

public class LoggingEventHandler implements EventHandler {
    @Override
    public void handleEvent(Event event) {
        log.info(event.toString());
    }
}
