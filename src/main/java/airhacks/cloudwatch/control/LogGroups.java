package airhacks.cloudwatch.control;

import software.amazon.awscdk.services.events.targets.CloudWatchLogGroup;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;

public interface LogGroups {
    
    static CloudWatchLogGroup completedBuildsTarget(Construct scope){
        var logGroup = LogGroup.Builder.create(scope,"CompletedBuilds")
                .retention(RetentionDays.FIVE_DAYS)
                .logGroupName("/airhacks/completed-builds")
                .build();
        return new CloudWatchLogGroup(logGroup);
    }

    static LogGroup stepMachineExecution(Construct scope){
        return LogGroup.Builder.create(scope,"StepFunctionsExecution")
                .retention(RetentionDays.FIVE_DAYS)
                .logGroupName("/airhacks/step-functions")
                .build();
    }
}
