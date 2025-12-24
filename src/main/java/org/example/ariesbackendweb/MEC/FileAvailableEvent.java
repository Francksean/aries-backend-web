package org.example.ariesbackendweb.MEC;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class FileAvailableEvent extends ApplicationEvent {
    private final String availableFileName;
    private final String testId;
    private final double duration;

    public FileAvailableEvent(Object source, String availableFileName, String testId,
                              double duration
    ) {
        super(source);
        this.availableFileName = availableFileName;
        this.testId = testId;
        this.duration = duration;
    }

}
