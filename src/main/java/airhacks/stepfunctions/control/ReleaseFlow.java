package airhacks.stepfunctions.control;

import java.util.Map;

import airhacks.cloudwatch.control.LogGroups;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.stepfunctions.LogLevel;
import software.amazon.awscdk.services.stepfunctions.LogOptions;
import software.amazon.awscdk.services.stepfunctions.StateMachine;
import software.constructs.Construct;

public interface ReleaseFlow {

    static StateMachine create(Construct scope) {
        var logGroup = LogGroups.stepMachineExecution(scope);
        return StateMachine.Builder.create(scope, "ReleaseFlowMachine")
                .stateMachineName("ReleaseFlow")
                .definitionSubstitutions(Map.of("stage", "integration"))
                .logs(LogOptions.builder()
                        .destination(logGroup)
                        .level(LogLevel.ALL)
                        .includeExecutionData(true)
                        .build())
                .timeout(Duration.hours(24))
                .build();
    }
}
