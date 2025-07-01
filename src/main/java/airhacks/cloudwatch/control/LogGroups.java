package airhacks.cloudwatch.control;

import software.amazon.awscdk.services.events.targets.CloudWatchLogGroup;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;

public interface LogGroups {
    
    static CloudWatchLogGroup completedBuilds(Construct scope){
        var logGroup = LogGroup.Builder.create(scope,"CompletedBuilds")
                .retention(RetentionDays.FIVE_DAYS)
                .logGroupName("/airhacks/completed-builds")
                .build();
        return new CloudWatchLogGroup(logGroup);
    }
}
